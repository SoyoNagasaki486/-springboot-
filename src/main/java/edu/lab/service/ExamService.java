package edu.lab.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

/** 在线考试判分与成绩保存业务。 */
@Service
public class ExamService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final JdbcTemplate jdbcTemplate;
    private final RandomGenerator randomGenerator;

    public ExamService(JdbcTemplate jdbcTemplate, RandomGenerator randomGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.randomGenerator = randomGenerator;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> createPaper(int questionCount) {
        List<Map<String, Object>> questions = new ArrayList<>(jdbcTemplate.queryForList(
                "select id, content, option_a, option_b, option_c, option_d, score, category from questions"));
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "题库暂无可用题目");
        }
        shuffle(questions);
        int actualCount = Math.min(Math.max(questionCount, 1), questions.size());
        return List.copyOf(questions.subList(0, actualCount));
    }

    @Transactional
    public Map<String, Object> submit(
            String userName, Map<String, String> answers, List<Long> paperQuestionIds) {
        if (paperQuestionIds == null || paperQuestionIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "试卷已失效，请重新开始考试");
        }
        Set<String> expectedAnswerIds = new LinkedHashSet<>();
        paperQuestionIds.forEach(id -> expectedAnswerIds.add(String.valueOf(id)));
        if (expectedAnswerIds.size() != paperQuestionIds.size() || !answers.keySet().equals(expectedAnswerIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "试卷答案不完整或包含无效题目");
        }
        boolean invalidAnswer = answers.values().stream()
                .anyMatch(answer -> answer == null || !answer.matches("(?i)[A-D]"));
        if (invalidAnswer) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "答案选项无效，请重新选择");
        }

        List<Map<String, Object>> questions = jdbcTemplate.queryForList(
                "select id, answer, score from questions");
        Map<Long, Map<String, Object>> questionById = new HashMap<>(questions.size());
        for (Map<String, Object> question : questions) {
            questionById.put(((Number) question.get("id")).longValue(), question);
        }

        int earned = 0;
        int maxScore = 0;
        for (Long questionId : paperQuestionIds) {
            Map<String, Object> question = questionById.get(questionId);
            if (question == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "试卷题目已发生变化，请重新开始考试");
            }
            int points = ((Number) question.get("score")).intValue();
            maxScore += points;
            String submitted = answers.get(String.valueOf(questionId));
            if (String.valueOf(question.get("answer")).equalsIgnoreCase(submitted)) {
                earned += points;
            }
        }
        int score = maxScore == 0 ? 0 : (int) Math.round(earned * 100.0 / maxScore);
        boolean passed = score >= 60;
        jdbcTemplate.update(
                "insert into exam_records(user_name, score, passed, exam_time) values (?, ?, ?, ?)",
                userName.trim(), score, passed, LocalDateTime.now().format(FORMATTER));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", score);
        result.put("passed", passed);
        result.put("questionCount", paperQuestionIds.size());
        return result;
    }

    private void shuffle(List<Map<String, Object>> questions) {
        for (int index = questions.size() - 1; index > 0; index--) {
            int target = randomGenerator.nextInt(index + 1);
            Map<String, Object> current = questions.get(index);
            questions.set(index, questions.get(target));
            questions.set(target, current);
        }
    }
}
