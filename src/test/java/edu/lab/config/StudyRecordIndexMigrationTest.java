package edu.lab.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyRecordIndexMigrationTest {
    @Mock
    private JdbcTemplate jdbcTemplate;
    private StudyRecordIndexMigration migration;

    @BeforeEach
    void setUp() {
        migration = new StudyRecordIndexMigration(jdbcTemplate);
    }

    @Test
    void shouldSkipMigrationWhenUniqueKeyAlreadyExists() {
        indexCount(1);

        migration.run(null);

        verify(jdbcTemplate, never()).update(anyString());
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void shouldMergeDuplicatesAndCreateUniqueKeyWhenMissing() {
        indexCount(0);

        migration.run(null);

        verify(jdbcTemplate, times(2)).update(anyString());
        verify(jdbcTemplate).execute(contains("uk_study_user_course"));
    }

    private void indexCount(int count) {
        when(jdbcTemplate.queryForObject(
                contains("information_schema.tables"), eq(Integer.class), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(
                contains("information_schema.statistics"), eq(Integer.class), any(Object[].class)))
                .thenReturn(count);
    }
}
