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
    private final PlanItemRejectionRepository rejections;
    private final ChangeItemRepository changeItems;

    public WorkflowService(ApplicationRepository applications, AssessmentRepository assessments,
                           PlanItemRepository planItems, PlanConfirmationRepository confirmations,
                           ConstructionScheduleRepository schedules, ConstructionChangeRepository changes,
                           CompletionRepository completions, SubsidyReviewRepository reviews,
                           SettlementRepository settlements, WarrantyVisitRepository visits,
                           WorkflowLogRepository logs, UserRepository users,
                           PlanItemRejectionRepository rejections,
                           ChangeItemRepository changeItems) {
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
        this.rejections = rejections;
        this.changeItems = changeItems;
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

        // 平台风险分级：五维度现场记录 → 综合评分与等级
        RiskService.RiskResult risk = RiskService.evaluate(app, form);
        form.setMobilityScore(risk.mobility());
        form.setWetnessScore(risk.wetness());
        form.setBedDifficultyScore(risk.bed());
        form.setLightingScore(risk.lighting());
        form.setEmergencyScore(risk.emergency());
        form.setRiskScore(risk.total());
        form.setFallRiskLevel(risk.level());
        form.setRiskFactors(String.join("；", risk.factors()));
        form.setCareRecommendation(risk.careRecommendation());

        form.setId(null);
        form.setApplicationId(id);
        form.setAssessorId(CurrentUser.get().id());
        form.setAssessedAt(LocalDateTime.now());
        Assessment saved = assessments.save(form);

        // 评估结果直接驱动改造方案
        List<PlanItem> generated = PlanGenerator.generate(app, saved);
        generated.forEach(it -> it.setApplicationId(id));
        planItems.saveAll(generated);

        app.setStatus("PLAN_REVIEW");
        log(app, "入户评估完成", "ASSIGNED", "PLAN_REVIEW",
                "评估师入户采集完成，系统五维度综合评分 " + risk.total() + "/15，风险等级【"
                        + risk.level() + "】，生成 " + generated.size() + " 项改造建议"
                        + ("高".equals(risk.level()) ? "；高风险家庭将优先排期并提示施工陪同/临时照护" : ""));
        return saved;
    }

    // ---------------- 家属确认方案 / 社区复核补贴 ----------------

    @Transactional
    public PlanConfirmation familyConfirmPlan(Long id, String signer, List<RemovedItem> removals,
                                              List<PlanItem> addedItems) {
        Application app = mustGet(id);
        mustStatus(app, "PLAN_REVIEW");
        checkFamilyOwnership(app);

        Assessment assessment = assessments.findByApplicationId(id).orElse(null);
        String riskLevel = assessment == null ? null : assessment.getFallRiskLevel();

        if (removals != null) {
            for (RemovedItem r : removals) {
                PlanItem item = planItems.findById(r.itemId())
                        .orElseThrow(() -> new ApiException("改造项目不存在: " + r.itemId()));
                if (!item.getApplicationId().equals(id)) {
                    throw new ApiException("项目不属于该申请");
                }
                String reason = r.familyReason() == null ? "" : r.familyReason().trim();
                if (reason.isEmpty()) {
                    throw new ApiException("删除项目【" + item.getName() + "】必须填写原因，该说明将随风险评估提交街道备查");
                }
                item.setStatus("REMOVED");

                // 保留评估师说明 + 家属拒绝原因，街道审核补贴时可查
                PlanItemRejection rec = new PlanItemRejection();
                rec.setApplicationId(id);
                rec.setPlanItemId(item.getId());
                rec.setItemName(item.getName());
                rec.setCategory(item.getCategory());
                rec.setAssessorNote(item.getReason());
                rec.setFamilyReason(reason);
                rec.setRiskLevel(riskLevel);
                rejections.save(rec);
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
                add.setSubsidyCap(PlanGenerator.subsidyCapFor(add.getCategory(), add.getUnitPrice())
                        .min(add.getUnitPrice()));
                add.setSource("FAMILY");
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
        int rejectionCount = removals == null ? 0 : removals.size();
        log(app, "家属确认方案（第" + round + "轮）", "PLAN_REVIEW", "PLAN_FAMILY_CONFIRMED",
                "家属签字：" + pc.getFamilySigner() + "，合计 " + cost.total()
                        + " 元，拟补贴 " + cost.subsidy() + " 元，自付 " + cost.selfPay() + " 元"
                        + (riskLevel != null ? "；评估风险等级【" + riskLevel + "】" : "")
                        + (rejectionCount > 0 ? "；家属拒绝 " + rejectionCount + " 项（已记录原因，街道审核可查）" : ""));
        return pc;
    }

    /** 家属删除项目：项目 + 必填的拒绝原因 */
    public record RemovedItem(Long itemId, String familyReason) {
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
        Assessment assessment = assessments.findByApplicationId(id).orElse(null);
        boolean highRisk = assessment != null && "高".equals(assessment.getFallRiskLevel());

        if (highRisk) {
            String care = form.getCareRequired();
            if (care == null || "NONE".equals(care)) {
                throw new ApiException("高风险家庭必须明确施工期间的家属陪同或临时照护安排后才能接单");
            }
            if (form.getCareArrangement() == null || form.getCareArrangement().isBlank()) {
                throw new ApiException("请填写陪同/临时照护的具体安排（由谁陪同/照护、时间段、联系人）");
            }
        }

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
        // 高风险家庭优先排期
        s.setPriority(highRisk ? 1 : 0);
        s.setCareRequired(form.getCareRequired() == null ? "NONE" : form.getCareRequired());
        s.setCareArrangement(form.getCareArrangement());
        ConstructionSchedule saved = schedules.save(s);

        app.setStatus("SCHEDULED");
        String careNote = highRisk
                ? "；高风险【优先排期】，照护安排：" + ("TEMP_CARE".equals(saved.getCareRequired())
                        ? "临时照护" : "家属陪同") + "（" + saved.getCareArrangement() + "）"
                : "";
        log(app, "施工队接单排期", "PLAN_APPROVED", "SCHEDULED",
                (highRisk ? "高风险家庭优先排期，" : "")
                        + "计划 " + s.getScheduledStart() + " 至 " + s.getScheduledEnd()
                        + " 上门施工" + careNote);
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

        // 施工队必须上传现场照片、写明变更说明与新材料需求，禁止仅凭口头变更加价
        if (form.getSitePhotos() == null || form.getSitePhotos().isBlank()) {
            throw new ApiException("请上传现场照片或填写照片档案编号");
        }
        if (form.getDescription() == null || form.getDescription().isBlank()) {
            throw new ApiException("请填写变更说明");
        }
        if (form.getMaterialRequirements() == null || form.getMaterialRequirements().isBlank()) {
            throw new ApiException("请填写新的材料需求（无新材料也需注明“不涉及新材料”）");
        }
        if (form.getReasonType() == null) {
            throw new ApiException("请选择变更原因类型");
        }

        form.setId(null);
        form.setApplicationId(id);
        form.setStatus("SUBMITTED");

        BigDecimal materialDelta = BigDecimal.ZERO;
        BigDecimal laborDelta = BigDecimal.ZERO;
        if (form.getItems() != null) {
            for (ChangeItem ci : form.getItems()) {
                if (ci.getName() == null || ci.getName().isBlank()) {
                    throw new ApiException("变更明细中存在未命名项目");
                }
                if (ci.getMaterialFee() == null) ci.setMaterialFee(BigDecimal.ZERO);
                if (ci.getLaborFee() == null) ci.setLaborFee(BigDecimal.ZERO);
                if (ci.getQuantity() == null || ci.getQuantity() <= 0) ci.setQuantity(1);
                materialDelta = materialDelta.add(ci.getMaterialFee());
                laborDelta = laborDelta.add(ci.getLaborFee());
            }
        }
        // 若团队还填了汇总拆分字段，以明细为准；只给汇总时使用汇总
        if (form.getMaterialFeeDelta() != null && form.getMaterialFeeDelta().signum() > 0 && materialDelta.signum() == 0) {
            materialDelta = form.getMaterialFeeDelta();
        }
        if (form.getLaborFeeDelta() != null && form.getLaborFeeDelta().signum() > 0 && laborDelta.signum() == 0) {
            laborDelta = form.getLaborFeeDelta();
        }
        form.setMaterialFeeDelta(materialDelta);
        form.setLaborFeeDelta(laborDelta);
        form.setCostDelta(materialDelta.add(laborDelta));

        ConstructionChange saved = changes.save(form);

        if (form.getItems() != null) {
            for (ChangeItem ci : form.getItems()) {
                ci.setId(null);
                ci.setChangeId(saved.getId());
                ci.setApplicationId(id);
                if (ci.getItemType() == null) ci.setItemType("ADD");
                if (ci.getCategory() == null) ci.setCategory("其他");
                changeItems.save(ci);
            }
        }

        app.setStatus("CHANGE_PENDING_FAMILY");
        log(app, "提交现场变更", "IN_CONSTRUCTION", "CHANGE_PENDING_FAMILY",
                changeReason(saved.getReasonType()) + "：" + saved.getDescription()
                        + "；材料费变化 " + materialDelta + " 元，人工费变化 " + laborDelta
                        + " 元，须经家属确认、社区核定补贴后生效，未通过前仍按原方案计价");
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
            ch.setCommunityDecision("REJECTED");
            ch.setResolvedAt(LocalDateTime.now());
            app.setStatus("IN_CONSTRUCTION");
            log(app, "家属驳回变更", "CHANGE_PENDING_FAMILY", "IN_CONSTRUCTION",
                    "变更#" + ch.getId() + " 家属不同意，保留原方案继续施工：" + opinion);
        } else {
            ch.setStatus("FAMILY_CONFIRMED");
            app.setStatus("CHANGE_PENDING_COMMUNITY");
            log(app, "家属同意变更", "CHANGE_PENDING_FAMILY", "CHANGE_PENDING_COMMUNITY",
                    "变更#" + ch.getId() + " 家属意见：" + nz(opinion)
                            + "；待社区判断是否影响补贴资格并重算费用");
        }
        return ch;
    }

    /**
     * 社区复核变更并判断补贴影响。
     * decision: APPROVED 通过（重算清单与费用，新增项目并入方案）
     *           REJECTED 未通过（保留原方案，变更费用不予认可）
     *           COORDINATING 转社区协调（工单暂挂，施工队不得按变更加价施工）
     * eligibleItemIds：社区逐项核定可计入报销的变更明细
     */
    @Transactional
    public ConstructionChange communityReviewChange(Long changeId, String decision, String remark,
                                                    java.util.List<Long> eligibleItemIds) {
        ConstructionChange ch = mustChange(changeId);
        Application app = mustGet(ch.getApplicationId());
        mustStatus(app, "CHANGE_PENDING_COMMUNITY");
        ch.setCommunityRemark(remark);
        ch.setCommunityReviewedAt(LocalDateTime.now());
        ch.setCommunityDecision(decision);

        List<ChangeItem> cis = changeItems.findByChangeId(changeId);
        java.util.Set<Long> eligible = eligibleItemIds == null ? java.util.Set.of()
                : new java.util.HashSet<>(eligibleItemIds);

        if ("APPROVED".equals(decision)) {
            boolean subsidyAffected = !eligible.isEmpty();
            ch.setSubsidyAffected(subsidyAffected);

            // 新增/替代项目并入方案清单；可报销项按规则给补贴上限（新增可报销金额）
            BigDecimal addReimburse = BigDecimal.ZERO;
            for (ChangeItem ci : cis) {
                boolean ok = eligible.contains(ci.getId());
                ci.setSubsidyEligible(ok);
                changeItems.save(ci);
                if ("ADD".equals(ci.getItemType())) {
                    PlanItem pi = new PlanItem();
                    pi.setApplicationId(app.getId());
                    pi.setCategory(ci.getCategory());
                    pi.setName(ci.getName());
                    pi.setSpec(ci.getSpec());
                    pi.setUnit(ci.getUnit());
                    pi.setQuantity(ci.getQuantity());
                    BigDecimal line = ci.getMaterialFee().add(ci.getLaborFee());
                    // 单价 = 行金额/数量；材料/人工为行金额拆分
                    BigDecimal perUnit = line.divide(BigDecimal.valueOf(ci.getQuantity()),
                            2, java.math.RoundingMode.HALF_UP);
                    pi.setUnitPrice(perUnit);
                    pi.setMaterialFee(ci.getMaterialFee());
                    pi.setLaborFee(ci.getLaborFee());
                    // 社区核定可报销：单价上限按可报销单价计（受总额封顶约束），否则为 0
                    pi.setSubsidyCap(ok ? perUnit : BigDecimal.ZERO);
                    pi.setSource("CHANGE");
                    pi.setReason("施工变更并入（变更#" + ch.getId() + "）：" + nz(ci.getReason()));
                    pi.setConstructionImpact("变更核准后施工，材料：" + nz(ch.getMaterialRequirements()));
                    pi.setStatus("CHANGE_ADDED");
                    planItems.save(pi);
                    if (ok) addReimburse = addReimburse.add(line);
                }
            }

            // 重新生成全单材料费、人工费、可报销金额
            List<PlanItem> active = planItems.findByApplicationIdAndStatusNot(app.getId(), "REMOVED");
            BigDecimal newMaterial = PlanGenerator.splitCost(active).material();
            BigDecimal newLabor = PlanGenerator.splitCost(active).labor();
            PlanGenerator.Cost newCost = PlanGenerator.calc(active);

            ch.setStatus("COMMUNITY_APPROVED");
            ch.setNewMaterialFee(newMaterial);
            ch.setNewLaborFee(newLabor);
            ch.setNewTotalCost(newCost.total());
            ch.setNewSubsidyAmount(newCost.subsidy());
            // 本次新增可报销 = 本次勾选可报销的新增项金额（总额可报销仍受 5000 封顶约束）
            ch.setReimbursementDelta(addReimburse);
            ch.setResolvedAt(LocalDateTime.now());
            app.setStatus("IN_CONSTRUCTION");
            log(app, "社区核准变更并重算费用", "CHANGE_PENDING_COMMUNITY", "IN_CONSTRUCTION",
                    "变更#" + ch.getId() + " 核准通过：重算后材料费 " + newMaterial + " 元、人工费 "
                            + newLabor + " 元、总费用 " + newCost.total() + " 元、可报销 "
                            + newCost.subsidy() + " 元（本次新增可报销 " + addReimburse + " 元）；" + nz(remark));
        } else if ("COORDINATING".equals(decision)) {
            ch.setStatus("COORDINATING");
            ch.setCoordinationNote(remark);
            ch.setResolvedAt(LocalDateTime.now());
            app.setStatus("CHANGE_COORDINATING");
            log(app, "变更转社区协调", "CHANGE_PENDING_COMMUNITY", "CHANGE_COORDINATING",
                    "变更#" + ch.getId() + " 存在补贴/施工争议，转社区协调：" + nz(remark)
                            + "；协调期间施工队不得按变更内容加价施工");
        } else {
            ch.setStatus("REJECTED");
            ch.setSubsidyAffected(false);
            ch.setResolvedAt(LocalDateTime.now());
            app.setStatus("IN_CONSTRUCTION");
            log(app, "社区驳回变更", "CHANGE_PENDING_COMMUNITY", "IN_CONSTRUCTION",
                    "变更#" + ch.getId() + " 不予认可，保留原方案与原费用，继续施工：" + nz(remark));
        }
        return ch;
    }

    /** 社区协调完成后恢复施工（协调结果以 remark 记录） */
    @Transactional
    public ConstructionChange resolveCoordination(Long changeId, Boolean adopted, String remark) {
        ConstructionChange ch = mustChange(changeId);
        Application app = mustGet(ch.getApplicationId());
        mustStatus(app, "CHANGE_COORDINATING");
        ch.setCoordinationNote((ch.getCoordinationNote() == null ? "" : ch.getCoordinationNote() + "\n")
                + "协调结果：" + nz(remark));
        ch.setResolvedAt(LocalDateTime.now());
        if (Boolean.TRUE.equals(adopted)) {
            // 协调采纳：等价于核准（无新增报销，避免绕过补贴核定）
            ch.setStatus("COMMUNITY_APPROVED");
            ch.setCommunityDecision("APPROVED");
            ch.setSubsidyAffected(false);
            List<PlanItem> active = planItems.findByApplicationIdAndStatusNot(app.getId(), "REMOVED");
            ch.setNewMaterialFee(PlanGenerator.splitCost(active).material());
            ch.setNewLaborFee(PlanGenerator.splitCost(active).labor());
            ch.setNewTotalCost(PlanGenerator.calc(active).total());
            ch.setNewSubsidyAmount(PlanGenerator.calc(active).subsidy());
            ch.setReimbursementDelta(BigDecimal.ZERO);
            log(app, "社区协调完成（采纳变更，费用自理）", "CHANGE_COORDINATING", "IN_CONSTRUCTION",
                    "变更#" + ch.getId() + " 协调后采纳但不新增补贴：" + nz(remark));
        } else {
            ch.setStatus("REJECTED");
            ch.setCommunityDecision("REJECTED");
            log(app, "社区协调完成（维持原方案）", "CHANGE_COORDINATING", "IN_CONSTRUCTION",
                    "变更#" + ch.getId() + " 协调后维持原方案：" + nz(remark));
        }
        app.setStatus("IN_CONSTRUCTION");
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
        Map<Long, List<ChangeItem>> itemsByChange = new java.util.HashMap<>();
        for (ChangeItem ci : changeItems.findByApplicationId(id)) {
            itemsByChange.computeIfAbsent(ci.getChangeId(), k -> new java.util.ArrayList<>()).add(ci);
        }
        d.put("changeItems", itemsByChange);
        completions.findByApplicationId(id).ifPresent(c -> d.put("completion", c));
        reviews.findByApplicationId(id).ifPresent(r -> d.put("subsidyReview", r));
        settlements.findByApplicationId(id).ifPresent(s -> d.put("settlement", s));
        d.put("warrantyVisits", visits.findByApplicationIdOrderByVisitTimeDesc(id));
        d.put("rejections", rejections.findByApplicationIdOrderByCreatedAtDesc(id));
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
