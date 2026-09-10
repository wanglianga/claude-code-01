package com.eldercare.service;

import com.eldercare.common.ApiException;
import com.eldercare.common.CurrentUser;
import com.eldercare.domain.*;
import com.eldercare.repo.*;
import com.eldercare.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowService {

    private final ApplicationRepository applications;
    private final AssessmentRepository assessments;
    private final PlanItemRepository planItems;
    private final PlanConfirmationRepository confirmations;
    private final ConstructionScheduleRepository schedules;
    private final ConstructionChangeRepository changes;
    private final CompletionRepository completions;
    private final SubsidyReviewRepository reviews;
    private final SettlementRepository settlements;
    private final WarrantyVisitRepository visits;
    private final WorkflowLogRepository logs;
    private final UserRepository users;

    public WorkflowService(ApplicationRepository applications, AssessmentRepository assessments,
                           PlanItemRepository planItems, PlanConfirmationRepository confirmations,
                           ConstructionScheduleRepository schedules, ConstructionChangeRepository changes,
                           CompletionRepository completions, SubsidyReviewRepository reviews,
                           SettlementRepository settlements, WarrantyVisitRepository visits,
                           WorkflowLogRepository logs, UserRepository users) {
        this.applications = applications;
        this.assessments = assessments;
        this.planItems = planItems;
        this.confirmations = confirmations;
        this.schedules = schedules;
        this.changes = changes;
        this.completions = completions;
        this.reviews = reviews;
        this.settlements = settlements;
        this.visits = visits;
        this.logs = logs;
        this.users = users;
    }

    // ---------------- 申请 ----------------

    @Transactional
    public Application submit(Application form) {
        AuthUser me = CurrentUser.get();
        form.setId(null);
        form.setStatus("SUBMITTED");
        form.setApplicantUserId(me.id());
        if (form.getApplicantName() == null || form.getApplicantName().isBlank()) {
            form.setApplicantName(me.realName());
        }
        Application saved = applications.save(form);
        log(saved, "提交申请", null, "SUBMITTED", "老人/家属在线提交适老化改造申请");
        return saved;
    }

    /** 社区核验补贴资格与房屋信息 */
    @Transactional
    public Application verify(Long id, Boolean eligible, String remark) {
        Application app = mustGet(id);
        mustStatus(app, "SUBMITTED");
        app.setSubsidyEligible(eligible);
        app.setVerifyRemark(remark);
        app.setVerifiedAt(LocalDateTime.now());
        if (Boolean.FALSE.equals(eligible)) {
            app.setStatus("VERIFY_REJECTED");
            log(app, "核验不通过", "SUBMITTED", "VERIFY_REJECTED", remark);
        } else {
            app.setStatus("VERIFIED");
            log(app, "社区核验通过", "SUBMITTED", "VERIFIED", "补贴资格与房屋信息核验通过");
        }
        return app;
    }

    /** 社区派单给评估师 */
    @Transactional
    public Application assign(Long id, Long assessorId) {
        Application app = mustGet(id);
        mustStatus(app, "VERIFIED");
        User assessor = users.findById(assessorId)
                .orElseThrow(() -> new ApiException("评估师不存在"));
        if (!"ASSESSOR".equals(assessor.getRole())) {
            throw new ApiException("被指派人不是评估师角色");
        }
        app.setAssessorId(assessorId);
        app.setAssignedAt(LocalDateTime.now());
        app.setStatus("ASSIGNED");
        log(app, "派单评估", "VERIFIED", "ASSIGNED",
                "已派单给评估师：" + assessor.getRealName());
        return app;
    }

    // ---------------- 入户评估 + 方案生成 ----------------

    @Transactional
    public Assessment submitAssessment(Long id, Assessment form) {
        Application app = mustGet(id);
        mustStatus(app, "ASSIGNED");
        if (!app.getAssessorId().equals(CurrentUser.get().id())) {
            throw new ApiException("该评估任务未派给当前评估师");
        }
        form.setId(null);
        form.setApplicationId(id);
        form.setAssessorId(CurrentUser.get().id());
        form.setAssessedAt(LocalDateTime.now());
        if (form.getFallRiskLevel() == null) {
            form.setFallRiskLevel(deriveRisk(app, form));
        }
        Assessment saved = assessments.save(form);

        // 评估结果直接驱动改造方案
        List<PlanItem> generated = PlanGenerator.generate(app, saved);
        generated.forEach(it -> it.setApplicationId(id));
        planItems.saveAll(generated);

        app.setStatus("PLAN_REVIEW");
        log(app, "入户评估完成", "ASSIGNED", "PLAN_REVIEW",
                "评估师入户采集完成，系统生成 " + generated.size() + " 项改造建议，跌倒风险："
                        + saved.getFallRiskLevel());
        return saved;
    }

    private String deriveRisk(Application app, Assessment a) {
        int score = 0;
        if (a.getThresholdHeight() != null && a.getThresholdHeight().compareTo(new BigDecimal("3")) >= 0) score++;
        if ("昏暗".equals(a.getNightLighting())) score++;
        if (a.getBedTransferDifficulty() != null && !a.getBedTransferDifficulty().isBlank()) score++;
        if (app.getFallHistory() != null && app.getFallHistory().contains("有")) score += 2;
        if ("拐杖".equals(app.getMobility()) || "轮椅".equals(app.getMobility())) score += 2;
        if ("卧床".equals(app.getMobility())) score += 3;
        return score >= 4 ? "高" : score >= 2 ? "中" : "低";
    }

    // ---------------- 家属确认方案 / 社区复核补贴 ----------------

    @Transactional
    public PlanConfirmation familyConfirmPlan(Long id, String signer, List<Long> removedItemIds,
                                              List<PlanItem> addedItems) {
        Application app = mustGet(id);
        mustStatus(app, "PLAN_REVIEW");
        checkFamilyOwnership(app);

        if (removedItemIds != null) {
            for (Long itemId : removedItemIds) {
                PlanItem item = planItems.findById(itemId)
                        .orElseThrow(() -> new ApiException("改造项目不存在: " + itemId));
                if (!item.getApplicationId().equals(id)) {
                    throw new ApiException("项目不属于该申请");
                }
                item.setStatus("REMOVED");
            }
        }
        if (addedItems != null) {
            for (PlanItem add : addedItems) {
                if (add.getName() == null || add.getName().isBlank()
                        || add.getUnitPrice() == null || add.getCategory() == null) {
                    throw new ApiException("新增项目必须填写类别、名称、单价");
                }
                add.setId(null);
                add.setApplicationId(id);
                add.setQuantity(add.getQuantity() == null ? 1 : add.getQuantity());
                add.setSubsidyCap(PlanGenerator.subsidyCapFor(add.getCategory(), add.getUnitPrice()));
                add.setStatus("ADDED");
                planItems.save(add);
            }
        }

        List<PlanItem> active = planItems.findByApplicationIdAndStatusNot(id, "REMOVED");
        if (active.isEmpty()) {
            throw new ApiException("至少保留一个改造项目，不能全部删除");
        }
        PlanGenerator.Cost cost = PlanGenerator.calc(active);
        int round = confirmations.findByApplicationIdOrderByRoundNoDesc(id).stream()
                .findFirst().map(PlanConfirmation::getRoundNo).orElse(0) + 1;

        PlanConfirmation pc = new PlanConfirmation();
        pc.setApplicationId(id);
        pc.setRoundNo(round);
        pc.setTotalCost(cost.total());
        pc.setSubsidyAmount(cost.subsidy());
        pc.setSelfPay(cost.selfPay());
        pc.setFamilyConfirmed(true);
        pc.setFamilyConfirmedAt(LocalDateTime.now());
        pc.setFamilySigner(signer == null || signer.isBlank() ? CurrentUser.get().realName() : signer);
        confirmations.save(pc);

        app.setStatus("PLAN_FAMILY_CONFIRMED");
        log(app, "家属确认方案（第" + round + "轮）", "PLAN_REVIEW", "PLAN_FAMILY_CONFIRMED",
                "家属签字：" + pc.getFamilySigner() + "，合计 " + cost.total()
                        + " 元，拟补贴 " + cost.subsidy() + " 元，自付 " + cost.selfPay() + " 元");
        return pc;
    }

    /**
     * 社区复核：家属增删后重新确认补贴规则。
     * itemCaps 可对项目补贴上限进行核定（特别是家属新增、系统默认补贴为 0 的项目）。
     */
    @Transactional
    public PlanConfirmation communityReviewPlan(Long id, Boolean approved, String remark,
                                                Map<Long, BigDecimal> itemCaps) {
        Application app = mustGet(id);
        mustStatus(app, "PLAN_FAMILY_CONFIRMED");
        PlanConfirmation pc = confirmations.findFirstByApplicationIdOrderByRoundNoDesc(id)
                .orElseThrow(() -> new ApiException("不存在方案确认单"));

        if (Boolean.TRUE.equals(approved) && itemCaps != null) {
            for (Map.Entry<Long, BigDecimal> e : itemCaps.entrySet()) {
                PlanItem item = planItems.findById(e.getKey())
                        .orElseThrow(() -> new ApiException("项目不存在: " + e.getKey()));
                if (!item.getApplicationId().equals(id)) {
                    throw new ApiException("项目不属于该申请");
                }
                BigDecimal cap = e.getValue() == null ? BigDecimal.ZERO : e.getValue();
                item.setSubsidyCap(cap.min(item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()))));
            }
            PlanGenerator.Cost cost = PlanGenerator.calc(
                    planItems.findByApplicationIdAndStatusNot(id, "REMOVED"));
            pc.setTotalCost(cost.total());
            pc.setSubsidyAmount(cost.subsidy());
            pc.setSelfPay(cost.selfPay());
        }

        pc.setCommunityApproved(approved);
        pc.setCommunityRemark(remark);
        pc.setCommunityReviewedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(approved)) {
            app.setStatus("PLAN_APPROVED");
            log(app, "社区复核通过", "PLAN_FAMILY_CONFIRMED", "PLAN_APPROVED",
                    "补贴规则复核通过，最终补贴 " + pc.getSubsidyAmount() + " 元"
                            + (remark == null ? "" : "；" + remark));
        } else {
            // 退回家属重新调整（增删项目后重新走确认）
            app.setStatus("PLAN_REVIEW");
            log(app, "社区复核退回", "PLAN_FAMILY_CONFIRMED", "PLAN_REVIEW",
                    "方案不符合补贴规则，退回家属调整：" + remark);
        }
        return pc;
    }

    // ---------------- 施工排期与变更 ----------------

    @Transactional
    public ConstructionSchedule teamAccept(Long id, ConstructionSchedule form) {
        Application app = mustGet(id);
        mustStatus(app, "PLAN_APPROVED");
        ConstructionSchedule s = schedules.findByApplicationId(id).orElseGet(ConstructionSchedule::new);
        s.setApplicationId(id);
        s.setTeamId(CurrentUser.get().id());
        s.setScheduledStart(form.getScheduledStart());
        s.setScheduledEnd(form.getScheduledEnd());
        s.setBuildingAccess(form.getBuildingAccess());
        s.setElevatorPlan(form.getElevatorPlan());
        s.setMaterialArrival(form.getMaterialArrival());
        s.setElderSchedule(form.getElderSchedule());
        s.setNoiseRestriction(form.getNoiseRestriction());
        s.setRemark(form.getRemark());
        ConstructionSchedule saved = schedules.save(s);

        app.setStatus("SCHEDULED");
        log(app, "施工队接单排期", "PLAN_APPROVED", "SCHEDULED",
                "计划 " + s.getScheduledStart() + " 至 " + s.getScheduledEnd() + " 上门施工");
        return saved;
    }

    @Transactional
    public void startConstruction(Long id) {
        Application app = mustGet(id);
        mustStatus(app, "SCHEDULED");
        app.setStatus("IN_CONSTRUCTION");
        log(app, "开工", "SCHEDULED", "IN_CONSTRUCTION", "施工队按排期上门，开始施工");
    }

    @Transactional
    public ConstructionChange submitChange(Long id, ConstructionChange form) {
        Application app = mustGet(id);
        mustStatus(app, "IN_CONSTRUCTION");
        form.setId(null);
        form.setApplicationId(id);
        form.setStatus("SUBMITTED");
        if (form.getCostDelta() == null) {
            form.setCostDelta(BigDecimal.ZERO);
        }
        ConstructionChange saved = changes.save(form);
        app.setStatus("CHANGE_PENDING_FAMILY");
        log(app, "提交现场变更", "IN_CONSTRUCTION", "CHANGE_PENDING_FAMILY",
                changeReason(saved.getReasonType()) + "：" + saved.getDescription()
                        + "，费用变化 " + saved.getCostDelta() + " 元");
        return saved;
    }

    @Transactional
    public ConstructionChange familyConfirmChange(Long changeId, Boolean agree, String opinion) {
        ConstructionChange ch = mustChange(changeId);
        Application app = mustGet(ch.getApplicationId());
        mustStatus(app, "CHANGE_PENDING_FAMILY");
        checkFamilyOwnership(app);
        ch.setFamilyOpinion(opinion);
        ch.setFamilyConfirmedAt(LocalDateTime.now());
        if (Boolean.FALSE.equals(agree)) {
            ch.setStatus("REJECTED");
            app.setStatus("IN_CONSTRUCTION");
            log(app, "家属驳回变更", "CHANGE_PENDING_FAMILY", "IN_CONSTRUCTION",
                    "变更#" + ch.getId() + " 家属不同意：" + opinion);
        } else {
            ch.setStatus("FAMILY_CONFIRMED");
            app.setStatus("CHANGE_PENDING_COMMUNITY");
            log(app, "家属同意变更", "CHANGE_PENDING_FAMILY", "CHANGE_PENDING_COMMUNITY",
                    "变更#" + ch.getId() + " 家属意见：" + opinion);
        }
        return ch;
    }

    @Transactional
    public ConstructionChange communityReviewChange(Long changeId, Boolean approved, String remark) {
        ConstructionChange ch = mustChange(changeId);
        Application app = mustGet(ch.getApplicationId());
        mustStatus(app, "CHANGE_PENDING_COMMUNITY");
        ch.setCommunityRemark(remark);
        ch.setCommunityReviewedAt(LocalDateTime.now());
        if (Boolean.TRUE.equals(approved)) {
            ch.setStatus("COMMUNITY_APPROVED");
            app.setStatus("IN_CONSTRUCTION");
            log(app, "社区核准变更", "CHANGE_PENDING_COMMUNITY", "IN_CONSTRUCTION",
                    "变更#" + ch.getId() + " 核准通过" + costNote(ch) + "；" + nz(remark));
        } else {
            ch.setStatus("REJECTED");
            app.setStatus("IN_CONSTRUCTION");
            log(app, "社区驳回变更", "CHANGE_PENDING_COMMUNITY", "IN_CONSTRUCTION",
                    "变更#" + ch.getId() + " 不符合补贴/施工规则：" + nz(remark));
        }
        return ch;
    }

    // ---------------- 竣工验收 / 街道补贴审核 / 结算 / 质保 ----------------

    @Transactional
    public Completion complete(Long id, Completion form) {
        Application app = mustGet(id);
        mustStatus(app, "IN_CONSTRUCTION");
        form.setId(null);
        form.setApplicationId(id);
        form.setSignedAt(LocalDateTime.now());
        form.setCompletedAt(LocalDateTime.now());
        Completion saved = completions.save(form);
        app.setStatus("COMPLETED");
        log(app, "竣工验收", "IN_CONSTRUCTION", "COMPLETED",
                "前后对比照片、老人试用记录、家属签字（" + nz(form.getFamilySigner())
                        + "）、材料费用明细已提交，合计 " + form.getTotalCost() + " 元，进入街道补贴审核");
        return saved;
    }

    /** 街道审核：结论回写施工队结算 */
    @Transactional
    public SubsidyReview streetReview(Long id, String conclusion, BigDecimal approvedSubsidy,
                                      String remark) {
        Application app = mustGet(id);
        mustStatus(app, "COMPLETED");
        Completion completion = completions.findByApplicationId(id)
                .orElseThrow(() -> new ApiException("缺少竣工验收材料"));

        BigDecimal total = completion.getTotalCost() == null ? BigDecimal.ZERO : completion.getTotalCost();
        BigDecimal subsidy = "APPROVED".equals(conclusion)
                ? (approvedSubsidy == null ? BigDecimal.ZERO : approvedSubsidy.min(total))
                : BigDecimal.ZERO;

        SubsidyReview review = reviews.findByApplicationId(id).orElseGet(SubsidyReview::new);
        review.setApplicationId(id);
        review.setReviewerId(CurrentUser.get().id());
        review.setConclusion("REJECTED".equals(conclusion) ? "REJECTED" : "APPROVED");
        review.setApprovedSubsidy(subsidy);
        review.setSelfPay(total.subtract(subsidy));
        review.setRemark(remark);
        review.setReviewedAt(LocalDateTime.now());
        reviews.save(review);

        // 回写施工队结算单：总额 = 补贴（街道拨付）+ 家属自付
        Settlement st = settlements.findByApplicationId(id).orElseGet(Settlement::new);
        st.setApplicationId(id);
        st.setTotalAmount(total);
        st.setSubsidyAmount(subsidy);
        st.setFamilyPayAmount(total.subtract(subsidy));
        st.setTeamPayAmount(total);
        st.setStatus("SETTLED");
        st.setSettledAt(LocalDateTime.now());
        settlements.save(st);

        app.setStatus("SETTLED");
        log(app, "街道补贴审核完成", "COMPLETED", "SETTLED",
                "审核结论：" + ("REJECTED".equals(conclusion) ? "不予补贴" : "准予补贴")
                        + "，核准补贴 " + subsidy + " 元，自付 " + total.subtract(subsidy)
                        + " 元；已回写施工队结算单，待质保回访");
        return review;
    }

    @Transactional
    public WarrantyVisit warrantyVisit(Long id, WarrantyVisit form) {
        Application app = mustGet(id);
        if (!"SETTLED".equals(app.getStatus()) && !"VISITED".equals(app.getStatus())) {
            throw new ApiException("当前状态（" + app.getStatus() + "）不能登记质保回访");
        }
        form.setId(null);
        form.setApplicationId(id);
        if (form.getVisitTime() == null) {
            form.setVisitTime(LocalDateTime.now());
        }
        WarrantyVisit saved = visits.save(form);
        app.setStatus("VISITED");
        log(app, "质保回访", "SETTLED".equals(app.getStatus()) ? "SETTLED" : "VISITED", "VISITED",
                "回访结论：" + ("ISSUES".equals(form.getResult()) ? "存在问题，已登记跟进" : "老人满意")
                        + "；" + nz(form.getContent()));
        return saved;
    }

    // ---------------- 查询 ----------------

    @Transactional(readOnly = true)
    public Map<String, Object> detail(Long id) {
        Application app = mustGet(id);
        AuthUser me = CurrentUser.get();
        if ("FAMILY".equals(me.role()) && !me.id().equals(app.getApplicantUserId())) {
            throw new ApiException("只能查看本家庭的申请");
        }
        Map<String, Object> d = new java.util.LinkedHashMap<>();
        d.put("application", app);
        assessments.findByApplicationId(id).ifPresent(a -> d.put("assessment", a));
        d.put("planItems", planItems.findByApplicationId(id));
        d.put("confirmations", confirmations.findByApplicationIdOrderByRoundNoDesc(id));
        schedules.findByApplicationId(id).ifPresent(s -> d.put("schedule", s));
        d.put("changes", changes.findByApplicationIdOrderByCreatedAtDesc(id));
        completions.findByApplicationId(id).ifPresent(c -> d.put("completion", c));
        reviews.findByApplicationId(id).ifPresent(r -> d.put("subsidyReview", r));
        settlements.findByApplicationId(id).ifPresent(s -> d.put("settlement", s));
        d.put("warrantyVisits", visits.findByApplicationIdOrderByVisitTimeDesc(id));
        d.put("logs", logs.findByApplicationIdOrderByCreatedAtAsc(id));
        return d;
    }

    // ---------------- 辅助 ----------------

    private Application mustGet(Long id) {
        return applications.findById(id).orElseThrow(() -> new ApiException("申请不存在: " + id));
    }

    private ConstructionChange mustChange(Long id) {
        return changes.findById(id).orElseThrow(() -> new ApiException("变更单不存在: " + id));
    }

    private void mustStatus(Application app, String... allowed) {
        for (String s : allowed) {
            if (s.equals(app.getStatus())) {
                return;
            }
        }
        throw new ApiException("当前状态【" + StatusMeta.label(app.getStatus())
                + "】不允许执行该操作");
    }

    private void checkFamilyOwnership(Application app) {
        AuthUser me = CurrentUser.get();
        if ("FAMILY".equals(me.role()) && !me.id().equals(app.getApplicantUserId())) {
            throw new ApiException("只能操作本家庭提交的申请");
        }
    }

    private void log(Application app, String action, String from, String to, String note) {
        AuthUser me = CurrentUser.getOrNull();
        WorkflowLog l = new WorkflowLog();
        l.setApplicationId(app.getId());
        l.setAction(action);
        l.setFromStatus(from);
        l.setToStatus(to);
        l.setNote(note);
        if (me != null) {
            l.setOperatorId(me.id());
            l.setOperatorName(me.realName());
        }
        logs.save(l);
    }

    private String changeReason(String t) {
        return switch (t) {
            case "WALL_UNDRILLABLE" -> "墙体无法打孔";
            case "PIPE_BLOCK" -> "管线占位";
            case "HOSPITAL" -> "老人临时住院";
            case "FAMILY_CHANGE" -> "家属意见变化";
            case "MODEL_MISMATCH" -> "材料型号不匹配";
            default -> t;
        };
    }

    private String costNote(ConstructionChange ch) {
        return ch.getCostDelta() == null || ch.getCostDelta().signum() == 0
                ? "" : "，核定费用变化 " + ch.getCostDelta() + " 元";
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
