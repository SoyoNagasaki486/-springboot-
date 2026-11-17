package edu.lab.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

/** 用户数据访问对象。 */
@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Map<String, Object>> findByUsername(String username) {
        return jdbcTemplate.queryForList(
                        "select id, username, password, real_name, role, qualified "
                                + "from users where username = ?", username)
                .stream().findFirst();
    }

    public boolean exists(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from users where username = ?", Integer.class, username);
        return count != null && count > 0;
    }

    public void save(String username, String password, String realName, String role) {
        jdbcTemplate.update(
                "insert into users(username, password, real_name, role, qualified) "
                        + "values (?, ?, ?, ?, false)",
                username, password, realName, role);
    }

    public void updatePassword(long id, String password) {
        jdbcTemplate.update("update users set password = ? where id = ?", password, id);
    }
}
