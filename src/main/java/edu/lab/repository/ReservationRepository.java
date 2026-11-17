package edu.lab.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/** 实验室预约数据访问对象。 */
@Repository
public class ReservationRepository {
    private final JdbcTemplate jdbcTemplate;

    public ReservationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean hasQualifiedAccess(long userId, long labId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from users u join qualifications q on q.user_name = u.real_name "
                        + "join labs l on l.name = q.lab_name where u.id = ? and l.id = ? "
                        + "and q.status = '已通过' and (q.valid_until is null or q.valid_until = '' "
                        + "or q.valid_until >= date_format(curdate(), '%Y-%m-%d'))",
                Integer.class, userId, labId);
        return count != null && count > 0;
    }

    /** 预约事务先锁用户再锁实验室，串行化同一用户的跨实验室并发申请。 */
    public void lockUser(long userId) {
        jdbcTemplate.queryForList("select id from users where id = ? for update", Long.class, userId);
    }

    public Optional<String> findLabStatus(long labId) {
        return jdbcTemplate.queryForList(
                        "select status from labs where id = ? for update", String.class, labId)
                .stream().findFirst();
    }

    public boolean hasConflict(long labId, LocalDate date, String timeSlot) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from reservations where lab_id = ? and reserve_date = ? "
                        + "and time_slot = ? and status not in ('驳回', '已取消')",
                Integer.class, labId, date.toString(), timeSlot);
        return count != null && count > 0;
    }

    public boolean hasUserConflict(long userId, LocalDate date, String timeSlot) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from reservations where user_id = ? and reserve_date = ? "
                        + "and time_slot = ? and status not in ('驳回', '已取消')",
                Integer.class, userId, date.toString(), timeSlot);
        return count != null && count > 0;
    }

    public void save(long userId, long labId, LocalDate date, String timeSlot, String purpose) {
        jdbcTemplate.update(
                "insert into reservations(user_id, lab_id, reserve_date, time_slot, purpose, status) "
                        + "values (?, ?, ?, ?, ?, '待审核')",
                userId, labId, date.toString(), timeSlot, purpose);
    }

    public int audit(long id, String status, String reviewer) {
        return jdbcTemplate.update(
                "update reservations set status = ?, reviewer = ? where id = ? and status = '待审核'",
                status, reviewer, id);
    }

    public Optional<Map<String, Object>> findForAudit(long id) {
        return jdbcTemplate.queryForList(
                        "select reserve_date, status from reservations where id = ? for update", id)
                .stream().findFirst();
    }

    public Optional<Map<String, Object>> findForCancellation(long id) {
        return jdbcTemplate.queryForList(
                        "select user_id, reserve_date, status from reservations where id = ? for update", id)
                .stream().findFirst();
    }

    public int cancel(long id) {
        return jdbcTemplate.update(
                "update reservations set status = '已取消', reviewer = null "
                        + "where id = ? and status in ('待审核', '已通过')", id);
    }
}
