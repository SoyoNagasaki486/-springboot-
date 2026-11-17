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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningServiceTest {
    private static final String USER = "张同学";
    private static final String TITLE = "实验室通用安全基础";
    private static final String CHAPTERED_CONTENT =
            "## 学习目标\n目标\n## 入场前检查\n内容\n## 应急报告与疏散\n内容";
    private static final String SINGLE_SECTION_CONTENT = "涵盖个人防护、消防疏散、用水用电和应急报告流程。";
    private static final String COURSE_SQL = "select title, status, content from courses where id = ?";
    private static final String RECORD_SQL =
            "select coalesce(max(progress), 0) as progress, coalesce(max(completed), 0) as completed "
                    + "from study_records where user_name = ? and course_title = ?";
    @Mock
    private JdbcTemplate jdbcTemplate;
    private LearningService learningService;

    @BeforeEach
    void setUp() {
        learningService = new LearningService(jdbcTemplate);
    }

    @Test
    void shouldRejectCompletingCourseWithoutSectionProgress() {
        course(1L, CHAPTERED_CONTENT);
        currentRecord(0, false);

        assertThrows(ResponseStatusException.class, () -> learningService.saveProgress(USER, 1L, 100));
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void shouldRejectProgressRollback() {
        course(1L, CHAPTERED_CONTENT);
        currentRecord(60, false);

        assertThrows(ResponseStatusException.class, () -> learningService.saveProgress(USER, 1L, 30));
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void shouldAllowCompletionAfterSectionProgressReachedThreshold() {
        course(1L, CHAPTERED_CONTENT);
        currentRecord(90, false);

        assertDoesNotThrow(() -> learningService.saveProgress(USER, 1L, 100));
        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }

    @Test
    void shouldAllowCompletionForSingleSectionCourse() {
        course(2L, SINGLE_SECTION_CONTENT);
        currentRecord(0, false);

        assertDoesNotThrow(() -> learningService.saveProgress(USER, 2L, 100));
        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }

    @Test
    void shouldNotRewriteCompletedCourseRecord() {
        course(1L, CHAPTERED_CONTENT);
        currentRecord(100, true);

        assertDoesNotThrow(() -> learningService.saveProgress(USER, 1L, 100));
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    private void course(long courseId, String content) {
        when(jdbcTemplate.queryForList(COURSE_SQL, courseId))
                .thenReturn(List.of(Map.of("title", TITLE, "status", "已发布", "content", content)));
    }

    /** 存在重复行时按最大进度读取，避免命中历史低进度而拒绝完成课程。 */
    private void currentRecord(int progress, boolean completed) {
        when(jdbcTemplate.queryForMap(RECORD_SQL, USER, TITLE))
                .thenReturn(Map.of("progress", progress, "completed", completed));
    }
}
