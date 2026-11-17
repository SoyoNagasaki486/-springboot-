package edu.lab.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 重要业务操作审计记录。 */
@Service
public class AuditLogService {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final JdbcTemplate jdbcTemplate;

    public AuditLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(String operator, String action, String module, String detail) {
        try {
            jdbcTemplate.update(
                    "insert into operation_logs(operator_name, action, module, operate_time, detail) "
                            + "values (?, ?, ?, ?, ?)",
                    safe(operator), action, module, LocalDateTime.now().format(FORMATTER), safe(detail));
        } catch (RuntimeException exception) {
            logger.warn("操作日志写入失败：{} / {}", module, action, exception);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "系统" : value;
    }
}
