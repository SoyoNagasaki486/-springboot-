package edu.lab.security;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** 保存在服务端会话中的最小用户身份。 */
public record SessionUser(long id, String username, String realName, String role, boolean qualified)
        implements Serializable {

    public static SessionUser from(Map<String, Object> user) {
        return new SessionUser(
                ((Number) user.get("id")).longValue(),
                String.valueOf(user.get("username")),
                String.valueOf(user.get("real_name")),
                String.valueOf(user.get("role")),
                booleanValue(user.get("qualified")));
    }

    private static boolean booleanValue(Object value) {
        return Boolean.TRUE.equals(value) || value instanceof Number number && number.intValue() == 1;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("username", username);
        result.put("real_name", realName);
        result.put("role", role);
        result.put("qualified", qualified);
        return result;
    }
}
