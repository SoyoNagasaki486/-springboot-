package edu.lab.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 兼容早期版本遗留的 study_records 表结构。
 *
 * <p>旧数据库缺少 (user_name, course_title) 唯一键，保存进度时 on duplicate key update
 * 不会生效，同一学员同一课程会不断插入新行；读取进度若命中历史低值行，课程就永远无法完成。
 * 启动时合并重复记录并补建唯一键，使初始化脚本中的唯一键约束在存量库上同样生效。</p>
 */
@Component
public class StudyRecordIndexMigration implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(StudyRecordIndexMigration.class);
    private static final String UNIQUE_KEY = "uk_study_user_course";
    private final JdbcTemplate jdbcTemplate;

    public StudyRecordIndexMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists() || indexExists()) {
            return;
        }
        mergeDuplicateRecords();
        jdbcTemplate.execute(
                "alter table study_records add unique key " + UNIQUE_KEY + " (user_name, course_title)");
        logger.info("已为 study_records 合并重复学习记录并补建唯一键 {}", UNIQUE_KEY);
    }

    private boolean tableExists() {
        return count("select count(*) from information_schema.tables "
                + "where table_schema = database() and table_name = 'study_records'") > 0;
    }

    private boolean indexExists() {
        return count("select count(*) from information_schema.statistics "
                + "where table_schema = database() and table_name = 'study_records' and index_name = ?",
                UNIQUE_KEY) > 0;
    }

    /** 同一学员同一课程只保留最早一行，进度取最大值，完成状态取并集，学习时间取最新值。 */
    private void mergeDuplicateRecords() {
        jdbcTemplate.update(
                "update study_records sr join (select user_name, course_title, min(id) keep_id, "
                        + "max(progress) max_progress, max(completed) any_completed, max(study_time) last_time "
                        + "from study_records group by user_name, course_title) agg on agg.keep_id = sr.id "
                        + "set sr.progress = agg.max_progress, sr.completed = agg.any_completed, "
                        + "sr.study_time = agg.last_time");
        jdbcTemplate.update(
                "delete sr from study_records sr join (select user_name, course_title, min(id) keep_id "
                        + "from study_records group by user_name, course_title) agg "
                        + "on agg.user_name = sr.user_name and agg.course_title = sr.course_title "
                        + "where sr.id <> agg.keep_id");
    }

    private int count(String sql, Object... arguments) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, arguments);
        return result == null ? 0 : result;
    }
}
