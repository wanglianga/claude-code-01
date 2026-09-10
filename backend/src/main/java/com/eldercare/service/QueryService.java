package com.eldercare.service;

import com.eldercare.domain.Application;
import com.eldercare.domain.User;
import com.eldercare.repo.ApplicationRepository;
import com.eldercare.repo.UserRepository;
import com.eldercare.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QueryService {

    private final ApplicationRepository applications;
    private final UserRepository users;

    public QueryService(ApplicationRepository applications, UserRepository users) {
        this.applications = applications;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<Application> listFor(AuthUser me, String status) {
        List<Application> base = switch (me.role()) {
            case "FAMILY" -> applications.findByApplicantUserIdOrderBySubmittedAtDesc(me.id());
            case "ASSESSOR" -> applications.findByAssessorIdOrderByAssignedAtDesc(me.id());
            default -> applications.findAllByOrderBySubmittedAtDesc(); // 社区/街道/施工队看全部
        };
        if (status != null && !status.isBlank()) {
            return base.stream().filter(a -> status.equals(a.getStatus())).toList();
        }
        return base;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> usersByRole(String role) {
        return users.findByRole(role).stream()
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("realName", u.getRealName());
                    m.put("phone", u.getPhone());
                    m.put("organization", u.getOrganization());
                    return m;
                }).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats(AuthUser me) {
        List<Application> all = listFor(me, null);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Application a : all) {
            byStatus.merge(a.getStatus(), 1L, Long::sum);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", all.size());
        result.put("byStatus", byStatus);

        // 各角色待办数
        long todo = switch (me.role()) {
            case "FAMILY" -> all.stream().filter(a ->
                    "PLAN_REVIEW".equals(a.getStatus()) || "CHANGE_PENDING_FAMILY".equals(a.getStatus())).count();
            case "COMMUNITY" -> byStatus.getOrDefault("SUBMITTED", 0L)
                    + byStatus.getOrDefault("VERIFIED", 0L)
                    + byStatus.getOrDefault("PLAN_FAMILY_CONFIRMED", 0L)
                    + byStatus.getOrDefault("CHANGE_PENDING_COMMUNITY", 0L);
            case "ASSESSOR" -> byStatus.getOrDefault("ASSIGNED", 0L);
            case "TEAM" -> byStatus.getOrDefault("PLAN_APPROVED", 0L)
                    + byStatus.getOrDefault("SCHEDULED", 0L)
                    + byStatus.getOrDefault("IN_CONSTRUCTION", 0L)
                    + byStatus.getOrDefault("CHANGE_PENDING_FAMILY", 0L)
                    + byStatus.getOrDefault("CHANGE_PENDING_COMMUNITY", 0L);
            case "STREET" -> byStatus.getOrDefault("COMPLETED", 0L);
            default -> 0;
        };
        result.put("todo", todo);
        return result;
    }
}
