package edu.lab.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * 论文所列基础业务实体的白名单 CRUD 服务。
 *
 * <p>表名和字段名只能来自静态定义，所有字段值仍通过参数化 SQL 写入。</p>
 */
@Service
public class ResourceService {
    private static final Map<String, Definition> DEFINITIONS = definitions();
    private static final Map<String, Set<String>> ENUM_VALUES = Map.of(
            "role", Set.of("STUDENT", "TEACHER", "ADMIN"),
            "level", Set.of("低风险", "中风险", "高风险", "一般", "较大", "重大"),
            "status", Set.of("开放", "维护中", "停用", "停用整改", "已发布", "草稿", "正常", "使用中",
                    "维修中", "待处理", "整改中", "已闭环"),
            "answer", Set.of("A", "B", "C", "D"),
            "return_status", Set.of("使用中", "已归还", "异常"),
            "result", Set.of("正常", "限期整改", "停用整改"));
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public ResourceService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public List<Map<String, Object>> list(String type) {
        Definition definition = definition(type);
        return jdbcTemplate.queryForList(
                "select " + definition.selectColumns() + " from " + definition.table() + " order by id desc");
    }

    @Transactional
    public void create(String type, Map<String, Object> requestBody) {
        Definition definition = writableDefinition(type);
        Map<String, Object> body = new LinkedHashMap<>(requestBody);
        validateFields(definition, body, true);
        beforeWrite(type, null, body, true);
        List<String> columns = presentColumns(definition, body);
        if (columns.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "没有可保存的数据");
        }
        String marks = String.join(",", Collections.nCopies(columns.size(), "?"));
        jdbcTemplate.update(
                "insert into " + definition.table() + "(" + String.join(",", columns) + ") values(" + marks + ")",
                columns.stream().map(body::get).toArray());
        afterWrite(type, null, body);
        auditLogService.record(
                String.valueOf(requestBody.getOrDefault("operator", "系统管理员")),
                "新增记录", definition.title(), "新增数据");
    }

    @Transactional
    public void update(String type, long id, Map<String, Object> requestBody) {
        Definition definition = writableDefinition(type);
        ensureExists(definition, id);
        Map<String, Object> body = new LinkedHashMap<>(requestBody);
        validateFields(definition, body, false);
        protectBusinessKeyChange(type, definition, id, body);
        validateTransition(type, definition, id, body);
        beforeWrite(type, id, body, false);
        List<String> columns = presentColumns(definition, body);
        if (columns.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "没有可更新的数据");
        }
        List<Object> arguments = new ArrayList<>();
        columns.forEach(column -> arguments.add(body.get(column)));
        arguments.add(id);
        jdbcTemplate.update(
                "update " + definition.table() + " set " + String.join(" = ?, ", columns) + " = ? where id = ?",
                arguments.toArray());
        afterWrite(type, id, body);
        auditLogService.record(
                String.valueOf(requestBody.getOrDefault("operator", "系统管理员")),
                "修改记录", definition.title(), "记录ID：" + id);
    }

    @Transactional
    public void delete(String type, long id, String operator) {
        Definition definition = writableDefinition(type);
        ensureExists(definition, id);
        ensureNotReferenced(type, definition, id);
        String qualificationUser = null;
        if ("qualifications".equals(type)) {
            qualificationUser = stringValue(currentValue(definition, id, "user_name"));
        }
        int affected = jdbcTemplate.update("delete from " + definition.table() + " where id = ?", id);
        if (affected == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在");
        }
        if (qualificationUser != null) {
            syncQualifiedStatus(qualificationUser);
        }
        auditLogService.record(operator, "删除记录", definition.title(), "记录ID：" + id);
    }

    private void validateFields(Definition definition, Map<String, Object> body, boolean creating) {
        Set<String> allowed = new java.util.HashSet<>(definition.columns());
        allowed.add("operator");
        List<String> unknown = body.keySet().stream().filter(key -> !allowed.contains(key)).toList();
        if (!unknown.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "包含不可写字段：" + String.join("、", unknown));
        }
        if (creating) {
            List<String> missing = definition.columns().stream()
                    .filter(field -> !body.containsKey(field) || stringValue(body.get(field)).isBlank())
                    .toList();
            if (!missing.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请完整填写：" + String.join("、", missing));
            }
        }
        for (String field : definition.columns()) {
            if (!body.containsKey(field)) {
                continue;
            }
            Object raw = body.get(field);
            if (raw instanceof String value) {
                body.put(field, value.trim());
            }
            String value = stringValue(body.get(field));
            if (value.isBlank() && !("issue".equals(field) || "rectification".equals(field))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " 不能为空");
            }
            Set<String> options = ENUM_VALUES.get(field);
            if (options != null && !options.contains(value)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " 的取值不合法");
            }
            if (Set.of("capacity", "duration", "score").contains(field)) {
                validatePositiveInteger(field, raw);
            }
            if (Set.of("purchase_date", "check_date", "publish_time").contains(field)) {
                validateDate(field, value);
            }
            if (Set.of("use_time", "occur_time").contains(field)) {
                validateDateTime(field, value);
            }
        }
        validateReferences(definition, body);
    }

    private void validatePositiveInteger(String field, Object raw) {
        try {
            int value = raw instanceof Number number ? number.intValue() : Integer.parseInt(stringValue(raw));
            int maximum = "score".equals(field) ? 100 : 10000;
            if (value <= 0 || value > maximum) {
                throw new NumberFormatException();
            }
            if ("score".equals(field) && value > 100) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " 必须是有效的正整数");
        }
    }

    private void validateDate(String field, String value) {
        try {
            LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " 日期格式不正确");
        }
    }

    private void validateDateTime(String field, String value) {
        try {
            LocalDateTime.parse(value.replace(' ', 'T'));
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " 时间格式不正确");
        }
    }

    private void validateReferences(Definition definition, Map<String, Object> body) {
        if (body.containsKey("lab_name") && !"labs".equals(definition.table())) {
            ensureNamedReference("labs", "name", stringValue(body.get("lab_name")), "实验室不存在");
        }
        if (body.containsKey("user_name")) {
            ensureNamedReference("users", "real_name", stringValue(body.get("user_name")), "用户不存在");
        }
        if (body.containsKey("equipment_name")) {
            ensureNamedReference("equipment", "name", stringValue(body.get("equipment_name")), "设备不存在");
        }
    }

    private void ensureNamedReference(String table, String column, String value, String message) {
        if (count("select count(*) from " + table + " where " + column + " = ?", value) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    private void protectBusinessKeyChange(
            String type, Definition definition, long id, Map<String, Object> body) {
        String field = switch (type) {
            case "users" -> "real_name";
            case "labs", "courses", "equipment" -> "name";
            default -> null;
        };
        if ("courses".equals(type)) {
            field = "title";
        }
        if (field == null || !body.containsKey(field)) {
            return;
        }
        String before = stringValue(currentValue(definition, id, field));
        String after = stringValue(body.get(field));
        if (!before.equals(after) && isBusinessKeyReferenced(type, id, before)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该名称已被业务记录引用，不能直接修改");
        }
    }

    private boolean isBusinessKeyReferenced(String type, long id, String value) {
        return switch (type) {
            case "users" -> count("select count(*) from reservations where user_id = ?", id) > 0
                    || count("select count(*) from study_records where user_name = ?", value) > 0
                    || count("select count(*) from exam_records where user_name = ?", value) > 0
                    || count("select count(*) from qualifications where user_name = ?", value) > 0;
            case "labs" -> count("select count(*) from reservations where lab_id = ?", id) > 0
                    || count("select count(*) from equipment where lab_name = ?", value) > 0
                    || count("select count(*) from hazards where lab_name = ?", value) > 0
                    || count("select count(*) from safety_checks where lab_name = ?", value) > 0;
            case "courses" -> count("select count(*) from study_records where course_title = ?", value) > 0;
            case "equipment" -> count("select count(*) from equipment_uses where equipment_name = ?", value) > 0;
            default -> false;
        };
    }

    private void validateTransition(String type, Definition definition, long id, Map<String, Object> body) {
        if ("labs".equals(type) && "开放".equals(stringValue(body.get("status")))
                && "停用整改".equals(stringValue(currentValue(definition, id, "status")))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "停用整改实验室须通过安全检查复核后重新开放");
        }
        if ("violations".equals(type) && body.containsKey("status")) {
            String current = stringValue(currentValue(definition, id, "status"));
            String target = stringValue(body.get("status"));
            if (("已闭环".equals(current) && !current.equals(target))
                    || ("整改中".equals(current) && "待处理".equals(target))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "违规处理状态不能逆向流转");
            }
        }
        if ("equipment-uses".equals(type) && body.containsKey("return_status")) {
            String current = stringValue(currentValue(definition, id, "return_status"));
            if (Set.of("已归还", "异常").contains(current)
                    && "使用中".equals(stringValue(body.get("return_status")))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "已结束的设备使用记录不能重新变为使用中");
            }
        }
    }

    private void ensureNotReferenced(String type, Definition definition, long id) {
        String value = switch (type) {
            case "users" -> stringValue(currentValue(definition, id, "real_name"));
            case "labs", "equipment" -> stringValue(currentValue(definition, id, "name"));
            case "courses" -> stringValue(currentValue(definition, id, "title"));
            default -> "";
        };
        if (!value.isBlank() && isBusinessKeyReferenced(type, id, value)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该记录仍被业务流程引用，不能删除");
        }
    }

    private int count(String sql, Object... arguments) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, arguments);
        return result == null ? 0 : result;
    }

    private void beforeWrite(String type, Long id, Map<String, Object> body, boolean creating) {
        if ("courses".equals(type) && "已发布".equals(value(type, id, body, "status"))) {
            String content = value(type, id, body, "content");
            long sectionCount = content.lines().filter(line -> line.trim().startsWith("## ")).count();
            if (content.length() < 200 || sectionCount < 3) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "发布课程至少需要 3 个章节且正文不少于 200 字");
            }
        }
        if ("users".equals(type) && body.containsKey("password")) {
            String password = stringValue(body.get("password"));
            if (password.isBlank()) {
                if (creating) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户密码不能为空");
                }
                body.remove("password");
            } else if (!password.startsWith("$2a$") && !password.startsWith("$2b$")) {
                if (password.length() < 8) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户密码至少需要 8 个字符");
                }
                body.put("password", passwordEncoder.encode(password));
            }
        }
        if ("equipment-uses".equals(type)) {
            String returnStatus = value(type, id, body, "return_status");
            if (creating || "使用中".equals(returnStatus)) {
                ensureEquipmentAvailable(value(type, id, body, "equipment_name"));
            }
        }
    }

    private void afterWrite(String type, Long id, Map<String, Object> body) {
        if ("labs".equals(type) && id != null && body.containsKey("status")
                && !"开放".equals(stringValue(body.get("status")))) {
            jdbcTemplate.update(
                    "update reservations set status = '驳回', reviewer = ? where lab_id = ? "
                            + "and reserve_date >= date_format(curdate(), '%Y-%m-%d') "
                            + "and status in ('待审核','已通过')",
                    stringValue(body.getOrDefault("operator", "系统自动校验")), id);
        }
        if ("qualifications".equals(type)) {
            syncQualifiedStatus(value(type, id, body, "user_name"));
        }
        if ("equipment-uses".equals(type)) {
            String equipmentName = value(type, id, body, "equipment_name");
            String returnStatus = value(type, id, body, "return_status");
            String equipmentStatus = switch (returnStatus) {
                case "异常" -> "维修中";
                case "已归还" -> "正常";
                default -> "使用中";
            };
            jdbcTemplate.update("update equipment set status = ? where name = ?", equipmentStatus, equipmentName);
        }
        if ("checks".equals(type)) {
            String checkResult = value(type, id, body, "result");
            String labName = value(type, id, body, "lab_name");
            if ("停用整改".equals(checkResult)) {
                jdbcTemplate.update("update labs set status = '停用整改' where name = ?", labName);
            } else if ("正常".equals(checkResult)
                    && !value(type, id, body, "rectification").isBlank()) {
                jdbcTemplate.update(
                        "update labs set status = '开放' where name = ? and status = '停用整改'", labName);
            }
        }
    }

    private void ensureEquipmentAvailable(String equipmentName) {
        Map<String, Object> equipment = jdbcTemplate.queryForList(
                        "select status from equipment where name = ?", equipmentName)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "设备不存在"));
        String status = stringValue(equipment.get("status"));
        if ("维修中".equals(status) || "停用".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "维修中或停用设备不能登记正常使用");
        }
    }

    private void syncQualifiedStatus(String userName) {
        if (userName == null || userName.isBlank()) {
            return;
        }
        Integer approved = jdbcTemplate.queryForObject(
                "select count(*) from qualifications where user_name = ? and status = '已通过' "
                        + "and (valid_until is null or valid_until = '' or valid_until >= date_format(curdate(), '%Y-%m-%d'))",
                Integer.class, userName);
        jdbcTemplate.update(
                "update users set qualified = ? where real_name = ?",
                approved != null && approved > 0, userName);
    }

    private String value(String type, Long id, Map<String, Object> body, String field) {
        if (body.containsKey(field)) {
            return stringValue(body.get(field));
        }
        if (id == null) {
            return "";
        }
        return stringValue(currentValue(definition(type), id, field));
    }

    private Object currentValue(Definition definition, long id, String field) {
        if (!definition.columns().contains(field)) {
            return null;
        }
        return jdbcTemplate.queryForList(
                        "select " + field + " from " + definition.table() + " where id = ?", id)
                .stream().findFirst().map(row -> row.get(field)).orElse(null);
    }

    private List<String> presentColumns(Definition definition, Map<String, Object> body) {
        return definition.columns().stream().filter(body::containsKey).toList();
    }

    private void ensureExists(Definition definition, long id) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from " + definition.table() + " where id = ?", Integer.class, id);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "记录不存在");
        }
    }

    private Definition writableDefinition(String type) {
        Definition definition = definition(type);
        if (definition.readonly()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "该资源仅允许查询");
        }
        return definition;
    }

    private Definition definition(String type) {
        Definition definition = DEFINITIONS.get(type);
        if (definition == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未知资源");
        }
        return definition;
    }

    private String stringValue(Object value) {
        return Objects.toString(value, "").trim();
    }

    private static Map<String, Definition> definitions() {
        Map<String, Definition> result = new LinkedHashMap<>();
        add(result, "users", "用户管理", "users",
                "username,password,real_name,role", "id,username,real_name,role,qualified", false);
        add(result, "labs", "实验室管理", "labs",
                "name,location,level,capacity,status,open_time", "*", false);
        add(result, "courses", "安全培训", "courses",
                "title,category,duration,content,status", "*", false);
        add(result, "study-records", "学习记录", "study_records",
                "user_name,course_title,progress,completed,study_time", "*", true);
        add(result, "questions", "考试题库", "questions",
                "content,option_a,option_b,option_c,option_d,answer,score,category", "*", false);
        add(result, "qualifications", "准入资格", "qualifications",
                "user_name,lab_name,score,status,valid_until", "*", true);
        add(result, "equipment", "设备台账", "equipment",
                "code,name,lab_name,model,status,purchase_date", "*", false);
        add(result, "equipment-uses", "设备使用", "equipment_uses",
                "equipment_name,user_name,use_time,purpose,return_status", "*", false);
        add(result, "hazards", "危险源", "hazards",
                "name,lab_name,level,protection,emergency", "*", false);
        add(result, "violations", "违规记录", "violations",
                "user_name,type,occur_time,handling,status", "*", false);
        add(result, "checks", "安全检查", "safety_checks",
                "lab_name,inspector,check_date,result,issue,rectification", "*", false);
        add(result, "notices", "通知公告", "notices",
                "title,content,publish_time", "*", false);
        add(result, "logs", "操作日志", "operation_logs",
                "operator_name,action,module,operate_time,detail", "*", true);
        return Collections.unmodifiableMap(result);
    }

    private static void add(
            Map<String, Definition> target,
            String type,
            String title,
            String table,
            String columns,
            String selectColumns,
            boolean readonly) {
        target.put(type, new Definition(title, table, List.of(columns.split(",")), selectColumns, readonly));
    }

    private record Definition(
            String title, String table, List<String> columns, String selectColumns, boolean readonly) {}
}
