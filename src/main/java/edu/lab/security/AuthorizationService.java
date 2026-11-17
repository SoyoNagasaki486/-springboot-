package edu.lab.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

/** 集中维护资源级角色权限，避免权限规则散落在控制器。 */
@Service
public class AuthorizationService {
    private static final Set<String> ALL_READ = Set.of("labs", "courses", "study-records", "qualifications");
    private static final Set<String> MANAGER_READ = Set.of(
            "equipment", "equipment-uses", "hazards", "violations", "checks");
    private static final Set<String> ADMIN_READ = Set.of("users", "questions", "notices", "logs");
    private static final Set<String> ADMIN_WRITE = Set.of("users", "labs", "courses", "questions", "notices");
    private static final Set<String> MANAGER_WRITE = Set.of(
            "equipment", "equipment-uses", "hazards", "violations", "checks");

    private final CurrentUserService currentUserService;

    public AuthorizationService(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    public SessionUser requireRead(String type, HttpSession session) {
        SessionUser user = currentUserService.require(session);
        boolean allowed = ALL_READ.contains(type)
                || MANAGER_READ.contains(type) && Set.of("TEACHER", "ADMIN").contains(user.role())
                || ADMIN_READ.contains(type) && "ADMIN".equals(user.role());
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号无权查看该资源");
        }
        return user;
    }

    public SessionUser requireWrite(String type, HttpSession session) {
        SessionUser user = currentUserService.require(session);
        boolean allowed = ADMIN_WRITE.contains(type) && "ADMIN".equals(user.role())
                || MANAGER_WRITE.contains(type) && Set.of("TEACHER", "ADMIN").contains(user.role());
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号无权维护该资源");
        }
        return user;
    }
}
