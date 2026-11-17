package edu.lab.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 汇总培训、考试与资格状态，并完成准入审核闭环。 */
@Service
public class QualificationService {
    private static final Set<String> AUDIT_STATUS = Set.of("已通过", "未通过");
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    public QualificationService(JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }

    public List<Map<String, Object>> candidates() {
        refreshExpiredQualifications();
        return jdbcTemplate.queryForList(
                "select u.id user_id, u.real_name user_name, "
                        + "coalesce((select er.score from exam_records er where er.user_name = u.real_name "
                        + "order by er.id desc limit 1), 0) latest_score, "
                        + "coalesce((select er.passed from exam_records er where er.user_name = u.real_name "
                        + "order by er.id desc limit 1), false) exam_passed, "
                        + "(select count(*) from study_records sr where sr.user_name = u.real_name "
                        + "and sr.completed = true) completed_courses, u.qualified "
                        + "from users u where u.role in ('STUDENT','TEACHER') order by u.id");
    }

    /** 每日同步冗余的用户准入标记，预约校验仍以实验室维度的有效资格记录为准。 */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Shanghai")
    @Transactional
    public void refreshExpiredQualifications() {
        jdbcTemplate.update(
                "update users u set u.qualified = exists (select 1 from qualifications q "
                        + "where q.user_name = u.real_name and q.status = '已通过' "
                        + "and (q.valid_until is null or q.valid_until = '' "
                        + "or q.valid_until >= date_format(curdate(), '%Y-%m-%d'))) "
                        + "where u.role in ('STUDENT','TEACHER')");
        jdbcTemplate.update(
                "update reservations r join users u on u.id = r.user_id join labs l on l.id = r.lab_id "
                        + "left join qualifications q on q.user_name = u.real_name and q.lab_name = l.name "
                        + "and q.status = '已通过' and (q.valid_until is null or q.valid_until = '' "
                        + "or q.valid_until >= date_format(curdate(), '%Y-%m-%d')) "
                        + "set r.status = '驳回', r.reviewer = '系统自动校验' "
                        + "where r.status in ('待审核','已通过') and r.reserve_date >= date_format(curdate(), '%Y-%m-%d') "
                        + "and q.id is null");
    }

    @Transactional
    public boolean refreshUserQualifiedStatus(String userName) {
        syncQualifiedStatus(userName);
        return count("select count(*) from users where real_name = ? and qualified = true", userName) > 0;
    }

    @Transactional
    public void audit(String userName, String labName, String status, LocalDate validUntil, String reviewer) {
        if (!AUDIT_STATUS.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "资格审核状态只能为已通过或未通过");
        }
        ensureUserAndLab(userName, labName);
        int completedCourses = count(
                "select count(*) from study_records where user_name = ? and completed = true", userName);
        Map<String, Object> latestExam = jdbcTemplate.queryForList(
                        "select score, passed from exam_records where user_name = ? order by id desc limit 1", userName)
                .stream().findFirst().orElse(Map.of("score", 0, "passed", false));
        int score = ((Number) latestExam.get("score")).intValue();
        Object passedValue = latestExam.get("passed");
        boolean examPassed = Boolean.TRUE.equals(passedValue)
                || passedValue instanceof Number number && number.intValue() == 1;
        if ("已通过".equals(status)) {
            if (completedCourses == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该用户尚未完成安全培训，不能授予资格");
            }
            if (!examPassed || score < 60) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该用户最近一次考试未通过，不能授予资格");
            }
            if (validUntil == null || validUntil.isBefore(LocalDate.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "通过审核时必须设置有效的资格期限");
            }
        }
        List<Long> ids = jdbcTemplate.queryForList(
                "select id from qualifications where user_name = ? and lab_name = ? order by id desc limit 1",
                Long.class, userName, labName);
        String validDate = validUntil == null ? null : validUntil.toString();
        if (ids.isEmpty()) {
            jdbcTemplate.update(
                    "insert into qualifications(user_name, lab_name, score, status, valid_until) values (?, ?, ?, ?, ?)",
                    userName, labName, score, status, validDate);
        } else {
            jdbcTemplate.update(
                    "update qualifications set score = ?, status = ?, valid_until = ? where id = ?",
                    score, status, validDate, ids.getFirst());
        }
        syncQualifiedStatus(userName);
        rejectInvalidReservations(userName, labName, reviewer);
        auditLogService.record(reviewer, "准入资格审核", "准入考试", userName + "：" + status + "（" + labName + "）");
    }

    private void ensureUserAndLab(String userName, String labName) {
        if (count("select count(*) from users where real_name = ? and role in ('STUDENT','TEACHER')", userName) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "待审核用户不存在");
        }
        if (count("select count(*) from labs where name = ?", labName) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "适用实验室不存在");
        }
    }

    private void syncQualifiedStatus(String userName) {
        int approved = count(
                "select count(*) from qualifications where user_name = ? and status = '已通过' "
                        + "and (valid_until is null or valid_until = '' or valid_until >= date_format(curdate(), '%Y-%m-%d'))",
                userName);
        jdbcTemplate.update("update users set qualified = ? where real_name = ?", approved > 0, userName);
    }

    private void rejectInvalidReservations(String userName, String labName, String reviewer) {
        jdbcTemplate.update(
                "update reservations r join users u on u.id = r.user_id join labs l on l.id = r.lab_id "
                        + "left join qualifications q on q.user_name = u.real_name and q.lab_name = l.name "
                        + "and q.status = '已通过' and (q.valid_until is null or q.valid_until = '' "
                        + "or q.valid_until >= date_format(curdate(), '%Y-%m-%d')) "
                        + "set r.status = '驳回', r.reviewer = ? where u.real_name = ? and l.name = ? "
                        + "and r.status in ('待审核','已通过') and r.reserve_date >= date_format(curdate(), '%Y-%m-%d') "
                        + "and q.id is null",
                reviewer, userName, labName);
    }

    private int count(String sql, Object... arguments) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, arguments);
        return result == null ? 0 : result;
    }
}
