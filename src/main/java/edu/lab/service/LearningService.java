package edu.lab.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** 课程开放校验与学习进度保存业务。 */
@Service
public class LearningService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final JdbcTemplate jdbcTemplate;

    public LearningService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void saveProgress(String userName, long courseId, int progress) {
        if (progress < 0 || progress > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学习进度必须在 0 到 100 之间");
        }
        Map<String, Object> course = jdbcTemplate.queryForList(
                        "select title, status, content from courses where id = ?", courseId)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在"));
        if (!"已发布".equals(String.valueOf(course.get("status")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "草稿课程暂未开放学习");
        }
        Map<String, Object> existing = currentRecord(userName.trim(), String.valueOf(course.get("title")));
        int currentProgress = ((Number) existing.get("progress")).intValue();
        boolean alreadyCompleted = Boolean.TRUE.equals(existing.get("completed"))
                || existing.get("completed") instanceof Number number && number.intValue() == 1;
        if (alreadyCompleted) {
            return;
        }
        if (progress < currentProgress) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "学习进度不能回退");
        }
        if (progress == 100 && hasChapterSteps(course.get("content")) && currentProgress < 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先完成课程章节学习，再确认完成课程");
        }
        jdbcTemplate.update(
                "insert into study_records(user_name, course_title, progress, completed, study_time) "
                        + "values (?, ?, ?, ?, ?) on duplicate key update progress = greatest(progress, values(progress)), "
                        + "completed = completed or values(completed), study_time = values(study_time)",
                userName.trim(), course.get("title"), progress, progress == 100,
                LocalDateTime.now().format(FORMATTER));
    }

    /**
     * 早期版本的 study_records 缺少唯一键时会为同一学员同一课程留下多行记录，
     * 这里按最大进度合并读取，避免读到旧的低进度导致课程永远无法完成。
     */
    private Map<String, Object> currentRecord(String userName, String courseTitle) {
        return jdbcTemplate.queryForMap(
                "select coalesce(max(progress), 0) as progress, coalesce(max(completed), 0) as completed "
                        + "from study_records where user_name = ? and course_title = ?",
                userName, courseTitle);
    }

    /** 单章节课程没有中间进度，可以直接确认完成。 */
    private boolean hasChapterSteps(Object content) {
        return content != null
                && String.valueOf(content).lines().filter(line -> line.trim().startsWith("## ")).count() > 1;
    }
}
