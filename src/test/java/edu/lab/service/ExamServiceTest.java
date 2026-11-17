package edu.lab.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private RandomGenerator randomGenerator;
    private ExamService examService;

    @BeforeEach
    void setUp() {
        examService = new ExamService(jdbcTemplate, randomGenerator);
    }

    @Test
    void shouldCreateRandomPaperWithConfiguredSize() {
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(
                Map.of("id", 1L, "content", "题目1"),
                Map.of("id", 2L, "content", "题目2"),
                Map.of("id", 3L, "content", "题目3")));
        when(randomGenerator.nextInt(anyInt())).thenReturn(0);

        List<Map<String, Object>> paper = examService.createPaper(2);

        assertEquals(2, paper.size());
        assertEquals(List.of(2L, 3L), paper.stream().map(row -> row.get("id")).toList());
    }

    @Test
    void shouldScoreAndSaveExamRecord() {
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(
                Map.of("id", 1L, "answer", "A", "score", 40),
                Map.of("id", 2L, "answer", "B", "score", 60)));

        Map<String, Object> result = examService.submit(
                "张同学", Map.of("1", "A", "2", "C"), List.of(1L, 2L));

        assertEquals(40, result.get("score"));
        assertEquals(false, result.get("passed"));
        assertEquals(2, result.get("questionCount"));
        verify(jdbcTemplate).update(
                anyString(), eq("张同学"), eq(40), eq(false), any(String.class));
    }

    @Test
    void shouldRejectIncompleteOrForgedAnswers() {
        assertThrows(ResponseStatusException.class, () -> examService.submit(
                "张同学", Map.of("1", "A"), List.of(1L, 2L)));
        assertThrows(ResponseStatusException.class, () -> examService.submit(
                "张同学", Map.of("1", "A", "2", "B", "99", "C"), List.of(1L, 2L)));
        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectEmptyQuestionBank() {
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());

        assertThrows(ResponseStatusException.class, () -> examService.createPaper(10));
    }

    @Test
    void shouldRejectExpiredPaper() {
        assertThrows(ResponseStatusException.class, () -> examService.submit("张同学", Map.of("1", "A"), null));
        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectInvalidAnswerOption() {
        assertThrows(ResponseStatusException.class, () -> examService.submit(
                "张同学", Map.of("1", "E"), List.of(1L)));
        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectPaperWhenQuestionWasRemoved() {
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(
                Map.of("id", 1L, "answer", "A", "score", 100)));

        assertThrows(ResponseStatusException.class, () -> examService.submit(
                "张同学", Map.of("1", "A", "2", "B"), List.of(1L, 2L)));
        verify(jdbcTemplate, never()).update(anyString(), any(), any(), any(), any());
    }
}
