package edu.lab.controller;

import edu.lab.security.CurrentUserService;
import edu.lab.security.AuthorizationService;
import edu.lab.security.CsrfTokenService;
import edu.lab.security.LoginRateLimitService;
import edu.lab.security.SessionUser;
import edu.lab.config.LabGuardProperties;
import edu.lab.service.AuditLogService;
import edu.lab.service.AuthService;
import edu.lab.service.ExamService;
import edu.lab.service.LearningService;
import edu.lab.service.QualificationService;
import edu.lab.service.ReservationService;
import edu.lab.service.ResourceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 系统 REST 接口，身份、权限和数据归属均由后端校验。 */
@Validated
@RestController
@RequestMapping("/api")
public class ApiController {
    private static final String EXAM_PAPER_IDS_SESSION_KEY = "LAB_GUARD_EXAM_PAPER_IDS";
    private final JdbcTemplate jdbcTemplate;
    private final AuthService authService;
    private final ReservationService reservationService;
    private final ExamService examService;
    private final LearningService learningService;
    private final ResourceService resourceService;
    private final QualificationService qualificationService;
    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;
    private final AuthorizationService authorizationService;
    private final CsrfTokenService csrfTokenService;
    private final LoginRateLimitService loginRateLimitService;
    private final LabGuardProperties properties;

    public ApiController(
            JdbcTemplate jdbcTemplate,
            AuthService authService,
            ReservationService reservationService,
            ExamService examService,
            LearningService learningService,
            ResourceService resourceService,
            QualificationService qualificationService,
            AuditLogService auditLogService,
            CurrentUserService currentUserService,
            AuthorizationService authorizationService,
            CsrfTokenService csrfTokenService,
            LoginRateLimitService loginRateLimitService,
            LabGuardProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.authService = authService;
        this.reservationService = reservationService;
        this.examService = examService;
        this.learningService = learningService;
        this.resourceService = resourceService;
        this.qualificationService = qualificationService;
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.csrfTokenService = csrfTokenService;
        this.loginRateLimitService = loginRateLimitService;
        this.properties = properties;
    }

    @PostMapping("/login")
    public Map<String, Object> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest, HttpSession session) {
        String remoteAddress = servletRequest.getRemoteAddr();
        loginRateLimitService.checkAllowed(request.username(), remoteAddress);
        Map<String, Object> user;
        try {
            user = authService.login(request.username(), request.password());
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                loginRateLimitService.failed(request.username(), remoteAddress);
                auditLogService.record(request.username(), "登录失败", "认证", "账号或密码错误");
            }
            throw exception;
        }
        loginRateLimitService.succeeded(request.username(), remoteAddress);
        servletRequest.changeSessionId();
        session.setMaxInactiveInterval(properties.getSecurity().getSessionTimeoutMinutes() * 60);
        SessionUser sessionUser = SessionUser.from(user);
        session.setAttribute(CurrentUserService.SESSION_KEY, sessionUser);
        auditLogService.record(sessionUser.realName(), "登录系统", "认证", "用户登录成功");
        return sessionResponse(sessionUser, session);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.username(), request.password(), request.realName(), request.role());
        auditLogService.record(request.realName(), "注册账号", "认证", "新用户注册");
        return Map.of("message", "注册成功，请使用新账号登录");
    }

    @GetMapping("/session")
    public Map<String, Object> session(HttpSession session) {
        return sessionResponse(currentUserService.require(session), session);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        SessionUser user = currentUserService.require(session);
        auditLogService.record(user.realName(), "退出系统", "认证", "用户主动退出");
        session.invalidate();
        return Map.of("message", "已安全退出");
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users", count("users"));
        result.put("labs", count("labs"));
        result.put("pending", jdbcTemplate.queryForObject(
                "select count(*) from reservations where status = '待审核'", Integer.class));
        result.put("qualified", jdbcTemplate.queryForObject(
                "select count(*) from users where qualified = true", Integer.class));
        result.put("equipment", count("equipment"));
        result.put("hazards", count("hazards"));
        result.put("reservationTrend", jdbcTemplate.queryForList(
                "select reserve_date label, count(*) value from reservations "
                        + "group by reserve_date order by reserve_date desc limit 7"));
        result.put("labUsage", jdbcTemplate.queryForList(
                "select l.name label, count(r.id) value from labs l "
                        + "left join reservations r on l.id = r.lab_id "
                        + "group by l.id, l.name order by value desc"));
        return result;
    }

    @GetMapping("/labs")
    public List<Map<String, Object>> labs() {
        return jdbcTemplate.queryForList("select * from labs order by id desc");
    }

    @GetMapping("/notices")
    public List<Map<String, Object>> notices() {
        return jdbcTemplate.queryForList("select * from notices order by id desc");
    }

    @GetMapping("/reservations")
    public List<Map<String, Object>> reservations(HttpSession session) {
        SessionUser user = currentUserService.require(session);
        String sql = "select r.*, u.real_name, l.name lab_name from reservations r "
                + "join users u on r.user_id = u.id join labs l on r.lab_id = l.id ";
        if ("STUDENT".equals(user.role())) {
            return jdbcTemplate.queryForList(sql + "where r.user_id = ? order by r.id desc", user.id());
        }
        return jdbcTemplate.queryForList(sql + "order by r.id desc");
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> reserve(
            @Valid @RequestBody ReservationRequest request, HttpSession session) {
        SessionUser user = currentUserService.requireRole(session, "STUDENT");
        reservationService.create(user.id(), request.labId(), request.date(), request.slot(), request.purpose());
        auditLogService.record(user.realName(), "提交预约", "预约管理", request.purpose());
        return Map.of("message", "预约已提交，等待审核");
    }

    @PutMapping("/reservations/{id}")
    public Map<String, Object> audit(
            @PathVariable long id,
            @Valid @RequestBody ReservationAuditRequest request,
            HttpSession session) {
        SessionUser reviewer = currentUserService.requireRole(session, "TEACHER", "ADMIN");
        reservationService.audit(id, request.status(), reviewer.realName());
        auditLogService.record(reviewer.realName(), "预约审核", "预约管理", request.status());
        return Map.of("message", "审核完成");
    }

    @DeleteMapping("/reservations/{id}")
    public Map<String, Object> cancel(@PathVariable long id, HttpSession session) {
        SessionUser user = currentUserService.requireRole(session, "STUDENT");
        reservationService.cancel(id, user.id());
        auditLogService.record(user.realName(), "取消预约", "预约管理", "预约ID：" + id);
        return Map.of("message", "预约已取消");
    }

    @GetMapping("/manage/{type}")
    public List<Map<String, Object>> list(@PathVariable String type, HttpSession session) {
        SessionUser user = authorizationService.requireRead(type, session);
        List<Map<String, Object>> rows = resourceService.list(type);
        if ("STUDENT".equals(user.role()) && Set.of("study-records", "qualifications").contains(type)) {
            return rows.stream().filter(row -> user.realName().equals(String.valueOf(row.get("user_name")))).toList();
        }
        return rows;
    }

    @PostMapping("/manage/{type}")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(
            @PathVariable String type, @RequestBody Map<String, Object> body, HttpSession session) {
        SessionUser user = authorizationService.requireWrite(type, session);
        Map<String, Object> safeBody = new LinkedHashMap<>(body);
        safeBody.put("operator", user.realName());
        resourceService.create(type, safeBody);
        return Map.of("message", "保存成功");
    }

    @PutMapping("/manage/{type}/{id}")
    public Map<String, Object> update(
            @PathVariable String type,
            @PathVariable long id,
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        SessionUser user = authorizationService.requireWrite(type, session);
        Map<String, Object> safeBody = new LinkedHashMap<>(body);
        safeBody.put("operator", user.realName());
        resourceService.update(type, id, safeBody);
        return Map.of("message", "更新成功");
    }

    @DeleteMapping("/manage/{type}/{id}")
    public Map<String, Object> delete(@PathVariable String type, @PathVariable long id, HttpSession session) {
        SessionUser user = authorizationService.requireWrite(type, session);
        resourceService.delete(type, id, user.realName());
        return Map.of("message", "删除成功");
    }

    @PostMapping("/study/progress")
    public Map<String, Object> saveProgress(
            @Valid @RequestBody StudyProgressRequest request, HttpSession session) {
        SessionUser user = currentUserService.requireRole(session, "STUDENT", "TEACHER");
        learningService.saveProgress(user.realName(), request.courseId(), request.progress());
        return Map.of("message", request.progress() == 100 ? "课程学习已完成" : "学习进度已保存");
    }

    @GetMapping("/exam/questions")
    public List<Map<String, Object>> examQuestions(HttpSession session) {
        currentUserService.requireRole(session, "STUDENT", "TEACHER");
        List<Map<String, Object>> paper = examService.createPaper(properties.getExam().getQuestionCount());
        List<Long> questionIds = paper.stream()
                .map(question -> ((Number) question.get("id")).longValue())
                .toList();
        session.setAttribute(EXAM_PAPER_IDS_SESSION_KEY, questionIds);
        return paper;
    }

    @PostMapping("/exam/submit")
    public Map<String, Object> submitExam(@Valid @RequestBody ExamRequest request, HttpSession session) {
        SessionUser user = currentUserService.requireRole(session, "STUDENT", "TEACHER");
        Object value = session.getAttribute(EXAM_PAPER_IDS_SESSION_KEY);
        if (!(value instanceof List<?> rawQuestionIds)
                || rawQuestionIds.isEmpty()
                || rawQuestionIds.stream().anyMatch(item -> !(item instanceof Number))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "试卷已失效，请重新开始考试");
        }
        List<Long> questionIds = rawQuestionIds.stream()
                .map(item -> ((Number) item).longValue())
                .toList();
        Map<String, Object> result = examService.submit(user.realName(), request.answers(), questionIds);
        session.removeAttribute(EXAM_PAPER_IDS_SESSION_KEY);
        return result;
    }

    @GetMapping("/exam/records")
    public List<Map<String, Object>> examRecords(HttpSession session) {
        SessionUser user = currentUserService.requireRole(session, "STUDENT", "TEACHER", "ADMIN");
        if ("STUDENT".equals(user.role()) || "TEACHER".equals(user.role())) {
            return jdbcTemplate.queryForList(
                    "select * from exam_records where user_name = ? order by id desc", user.realName());
        }
        return jdbcTemplate.queryForList("select * from exam_records order by id desc");
    }

    @GetMapping("/qualifications/candidates")
    public List<Map<String, Object>> qualificationCandidates(HttpSession session) {
        currentUserService.requireRole(session, "TEACHER", "ADMIN");
        return qualificationService.candidates();
    }

    @PostMapping("/qualifications/audit")
    public Map<String, Object> auditQualification(
            @Valid @RequestBody QualificationAuditRequest request, HttpSession session) {
        SessionUser reviewer = currentUserService.requireRole(session, "TEACHER", "ADMIN");
        qualificationService.audit(
                request.userName(), request.labName(), request.status(), request.validUntil(), reviewer.realName());
        return Map.of("message", "准入资格审核完成");
    }

    private Map<String, Object> sessionResponse(SessionUser user, HttpSession session) {
        boolean qualified = qualificationService.refreshUserQualifiedStatus(user.realName());
        SessionUser refreshed = new SessionUser(
                user.id(), user.username(), user.realName(), user.role(), qualified);
        session.setAttribute(CurrentUserService.SESSION_KEY, refreshed);
        Map<String, Object> response = new LinkedHashMap<>(refreshed.toMap());
        response.put("csrf_token", csrfTokenService.token(session));
        return response;
    }

    private Integer count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 50) String realName,
            String role) {}

    public record ReservationRequest(
            @NotNull Long labId,
            @NotNull @FutureOrPresent LocalDate date,
            @NotBlank String slot,
            @NotBlank @Size(max = 255) String purpose) {}

    public record ReservationAuditRequest(@NotBlank String status) {}

    public record StudyProgressRequest(@NotNull Long courseId, @NotNull Integer progress) {}

    public record ExamRequest(@NotEmpty Map<String, String> answers) {}

    public record QualificationAuditRequest(
            @NotBlank String userName,
            @NotBlank String labName,
            @NotBlank String status,
            @FutureOrPresent LocalDate validUntil) {}
}
