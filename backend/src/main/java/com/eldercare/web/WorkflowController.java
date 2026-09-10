package com.eldercare.web;

import com.eldercare.common.CurrentUser;
import com.eldercare.domain.*;
import com.eldercare.repo.ApplicationRepository;
import com.eldercare.security.AuthUser;
import com.eldercare.service.QueryService;
import com.eldercare.service.WorkflowService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WorkflowController {

    private final WorkflowService workflow;
    private final QueryService query;
    private final ApplicationRepository applications;

    public WorkflowController(WorkflowService workflow, QueryService query,
                              ApplicationRepository applications) {
        this.workflow = workflow;
        this.query = query;
        this.applications = applications;
    }

    // ---------------- 查询 ----------------

    @GetMapping("/applications")
    public List<Application> list(@RequestParam(required = false) String status) {
        return query.listFor(CurrentUser.get(), status);
    }

    @GetMapping("/applications/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        return workflow.detail(id);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return query.stats(CurrentUser.get());
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('COMMUNITY')")
    public List<Map<String, Object>> users(@RequestParam String role) {
        return query.usersByRole(role);
    }

    // ---------------- 申请提交 ----------------

    public record SubmitResponse(Long id, String status) {
    }

    @PostMapping("/applications")
    @PreAuthorize("hasRole('FAMILY')")
    public SubmitResponse submit(@RequestBody Application form) {
        Application saved = workflow.submit(form);
        return new SubmitResponse(saved.getId(), saved.getStatus());
    }

    // ---------------- 社区核验 / 派单 ----------------

    public record VerifyRequest(Boolean eligible, String remark) {
    }

    @PostMapping("/applications/{id}/verify")
    @PreAuthorize("hasRole('COMMUNITY')")
    public Application verify(@PathVariable Long id, @RequestBody VerifyRequest req) {
        if (req.eligible() == null) {
            throw new com.eldercare.common.ApiException("请给出核验结论");
        }
        return workflow.verify(id, req.eligible(), req.remark());
    }

    public record AssignRequest(Long assessorId) {
    }

    @PostMapping("/applications/{id}/assign")
    @PreAuthorize("hasRole('COMMUNITY')")
    public Application assign(@PathVariable Long id, @RequestBody AssignRequest req) {
        return workflow.assign(id, req.assessorId());
    }

    // ---------------- 入户评估 ----------------

    @PostMapping("/applications/{id}/assessment")
    @PreAuthorize("hasRole('ASSESSOR')")
    public Assessment assessment(@PathVariable Long id, @RequestBody Assessment form) {
        return workflow.submitAssessment(id, form);
    }

    // ---------------- 方案确认 / 社区复核 ----------------

    public record RemovedItemRequest(Long itemId, String familyReason) {
    }

    public record FamilyPlanRequest(String signer, List<RemovedItemRequest> removals, List<PlanItem> addedItems) {
    }

    @PostMapping("/applications/{id}/plan/family-confirm")
    @PreAuthorize("hasRole('FAMILY')")
    public PlanConfirmation familyConfirmPlan(@PathVariable Long id, @RequestBody FamilyPlanRequest req) {
        List<com.eldercare.service.WorkflowService.RemovedItem> removals = req.removals() == null
                ? List.of()
                : req.removals().stream()
                        .map(r -> new com.eldercare.service.WorkflowService.RemovedItem(r.itemId(), r.familyReason()))
                        .toList();
        return workflow.familyConfirmPlan(id, req.signer(), removals, req.addedItems());
    }

    public record CommunityPlanRequest(Boolean approved, String remark, Map<Long, BigDecimal> itemCaps) {
    }

    @PostMapping("/applications/{id}/plan/community-review")
    @PreAuthorize("hasRole('COMMUNITY')")
    public PlanConfirmation communityReviewPlan(@PathVariable Long id, @RequestBody CommunityPlanRequest req) {
        if (req.approved() == null) {
            throw new com.eldercare.common.ApiException("请给出复核结论");
        }
        return workflow.communityReviewPlan(id, req.approved(), req.remark(), req.itemCaps());
    }

    // ---------------- 施工排期 / 开工 / 变更 ----------------

    @PostMapping("/applications/{id}/schedule")
    @PreAuthorize("hasRole('TEAM')")
    public ConstructionSchedule accept(@PathVariable Long id, @RequestBody ConstructionSchedule form) {
        return workflow.teamAccept(id, form);
    }

    @PostMapping("/applications/{id}/start")
    @PreAuthorize("hasRole('TEAM')")
    public Map<String, Object> start(@PathVariable Long id) {
        workflow.startConstruction(id);
        return Map.of("status", "IN_CONSTRUCTION");
    }

    @PostMapping("/applications/{id}/changes")
    @PreAuthorize("hasRole('TEAM')")
    public ConstructionChange submitChange(@PathVariable Long id, @RequestBody ConstructionChange form) {
        return workflow.submitChange(id, form);
    }

    public record FamilyChangeRequest(Boolean agree, String opinion) {
    }

    @PostMapping("/changes/{changeId}/family")
    @PreAuthorize("hasRole('FAMILY')")
    public ConstructionChange familyChange(@PathVariable Long changeId, @RequestBody FamilyChangeRequest req) {
        if (req.agree() == null) {
            throw new com.eldercare.common.ApiException("请选择是否同意变更");
        }
        return workflow.familyConfirmChange(changeId, req.agree(), req.opinion());
    }

    public record CommunityChangeRequest(Boolean approved, String remark) {
    }

    @PostMapping("/changes/{changeId}/community")
    @PreAuthorize("hasRole('COMMUNITY')")
    public ConstructionChange communityChange(@PathVariable Long changeId, @RequestBody CommunityChangeRequest req) {
        if (req.approved() == null) {
            throw new com.eldercare.common.ApiException("请给出复核结论");
        }
        return workflow.communityReviewChange(changeId, req.approved(), req.remark());
    }

    // ---------------- 竣工 / 街道审核 / 质保 ----------------

    @PostMapping("/applications/{id}/complete")
    @PreAuthorize("hasRole('TEAM')")
    public Completion complete(@PathVariable Long id, @RequestBody Completion form) {
        return workflow.complete(id, form);
    }

    public record SubsidyReviewRequest(String conclusion, BigDecimal approvedSubsidy, String remark) {
    }

    @PostMapping("/applications/{id}/subsidy-review")
    @PreAuthorize("hasRole('STREET')")
    public SubsidyReview subsidyReview(@PathVariable Long id, @RequestBody SubsidyReviewRequest req) {
        if (!"APPROVED".equals(req.conclusion()) && !"REJECTED".equals(req.conclusion())) {
            throw new com.eldercare.common.ApiException("审核结论必须为 APPROVED 或 REJECTED");
        }
        return workflow.streetReview(id, req.conclusion(), req.approvedSubsidy(), req.remark());
    }

    @PostMapping("/applications/{id}/warranty")
    @PreAuthorize("hasRole('TEAM')")
    public WarrantyVisit warranty(@PathVariable Long id, @RequestBody WarrantyVisit form) {
        return workflow.warrantyVisit(id, form);
    }

    /** 当前登录用户（前端刷新页面后恢复会话） */
    @GetMapping("/me")
    public AuthUser me() {
        return CurrentUser.get();
    }
}
