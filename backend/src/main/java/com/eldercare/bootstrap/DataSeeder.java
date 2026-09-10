package com.eldercare.bootstrap;

import com.eldercare.domain.*;
import com.eldercare.repo.*;
import com.eldercare.service.PlanGenerator;
import com.eldercare.service.RiskService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 初始化演示账号与覆盖全流程节点的演示工单（仅在用户表为空时执行）。
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
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
    private final PlanItemRejectionRepository rejections;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, ApplicationRepository applications,
                      AssessmentRepository assessments, PlanItemRepository planItems,
                      PlanConfirmationRepository confirmations, ConstructionScheduleRepository schedules,
                      ConstructionChangeRepository changes, CompletionRepository completions,
                      SubsidyReviewRepository reviews, SettlementRepository settlements,
                      WarrantyVisitRepository visits, WorkflowLogRepository logs,
                      PlanItemRejectionRepository rejections, PasswordEncoder encoder) {
        this.users = users;
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
        this.rejections = rejections;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        String pwd = encoder.encode("123456");
        User family1 = user("zhangwei", pwd, "张伟", "FAMILY", "13800000001", "幸福里社区-家属");
        User family2 = user("chenmin", pwd, "陈敏", "FAMILY", "13800000002", "和平街社区-家属");
        User community = user("wangli", pwd, "王丽", "COMMUNITY", "13900000001", "幸福里社区居委会");
        User assessor = user("lizhigu", pwd, "李智固", "ASSESSOR", "13700000001", "区适老化评估中心");
        user("zhaomin", pwd, "赵敏", "ASSESSOR", "13700000002", "区适老化评估中心");
        User team = user("chenjianguo", pwd, "陈建国", "TEAM", "13600000001", "安馨适老施工队");
        User street = user("sunjiwei", pwd, "孙纪伟", "STREET", "13500000001", "和平街道办事处");
        users.flush();

        // 1. 刚提交待核验
        Application a1 = app(family1, "王秀兰", 82, "3楼无电梯，夜间起夜两次", "SUBMITTED");
        a1.setFloor(3);
        a1.setHasElevator(false);
        a1.setLivingAlone(true);
        a1.setMobility("拐杖");
        a1.setFallHistory("近半年有1次卧室门口滑倒");
        a1.setBathroomStatus("老式蹲便、光面瓷砖地、无扶手");
        a1.setBedroomStatus("床较矮，起身需扶床头柜");
        a1.setHouseOwnership("自有产权");
        a1.setExpectedItems("卫生间扶手、防滑、夜灯");
        applications.save(a1);
        timeline(a1, "提交申请", null, "SUBMITTED", community, "家属代老人在线提交申请");

        // 2. 已核验待派单
        Application a2 = app(family1, "李桂芳", 76, "腿脚不便希望加装扶手", "VERIFIED");
        a2.setFloor(2);
        a2.setMobility("拐杖");
        a2.setFallHistory("无");
        a2.setBathroomStatus("坐便、无扶手、地面湿滑");
        a2.setBedroomStatus("正常");
        a2.setHouseOwnership("公房");
        a2.setSubsidyEligible(true);
        a2.setVerifyRemark("户籍与房屋信息一致，属高龄补贴对象");
        a2.setVerifiedAt(LocalDateTime.now().minusDays(3));
        applications.save(a2);
        timeline(a2, "提交申请", null, "SUBMITTED", family1, null);
        timeline(a2, "社区核验通过", "SUBMITTED", "VERIFIED", community, "补贴资格与房屋信息核验通过");

        // 3. 已派单待评估
        Application a3 = app(family2, "赵德山", 79, "坐改蹲+扶手，尽快安排评估", "ASSIGNED");
        a3.setFloor(1);
        a3.setMobility("轮椅");
        a3.setFallHistory("有2次跌倒");
        a3.setBathroomStatus("蹲便器，门洞窄，轮椅无法进入");
        a3.setBedroomStatus("床边无支撑");
        a3.setHouseOwnership("自有产权");
        a3.setSubsidyEligible(true);
        a3.setAssessorId(assessor.getId());
        a3.setVerifiedAt(LocalDateTime.now().minusDays(2));
        a3.setAssignedAt(LocalDateTime.now().minusDays(1));
        applications.save(a3);
        timeline(a3, "社区核验通过", "SUBMITTED", "VERIFIED", community, null);
        timeline(a3, "派单评估", "VERIFIED", "ASSIGNED", community, "派单给评估师李智固");

        // 4. 已评估，方案待家属确认
        Application a4 = assessedApp(family1, community, assessor, "孙玉梅", 85,
                "PLAN_REVIEW", "中", null);

        // 5. 家属已确认（增删过项目），待社区复核
        Application a5 = assessedApp(family2, community, assessor, "周福海", 81,
                "PLAN_FAMILY_CONFIRMED", "高", null);
        // 家属拒绝了高风险相关的紧急呼叫项目：保留评估师说明与家属拒绝原因，街道审核可查
        List<PlanItem> items5 = planItems.findByApplicationId(a5.getId());
        PlanItem rejected = items5.stream()
                .filter(i -> i.getName().contains("紧急呼叫")).findFirst()
                .orElseGet(() -> items5.get(0));
        rejected.setStatus("REMOVED");
        planItems.save(rejected);
        PlanItemRejection rejection = new PlanItemRejection();
        rejection.setApplicationId(a5.getId());
        rejection.setPlanItemId(rejected.getId());
        rejection.setItemName(rejected.getName());
        rejection.setCategory(rejected.getCategory());
        rejection.setAssessorNote(rejected.getReason());
        rejection.setFamilyReason("老人抵触电子设备、不愿随身佩戴；家属认为与对门邻居关系好，白天可相互照应，暂不安装");
        rejection.setRiskLevel("高");
        rejections.save(rejection);
        // 家属同时新增“其他”类项目（默认补贴 0，等社区核定）
        PlanItem added = new PlanItem();
        added.setApplicationId(a5.getId());
        added.setCategory("其他");
        added.setName("厨房操作台圆角防撞处理");
        added.setReason("家属提出老人常在厨房活动，担心磕碰");
        added.setSpec("定制软包护角");
        added.setUnit("米");
        added.setQuantity(2);
        added.setUnitPrice(new BigDecimal("180.00"));
        added.setSubsidyCap(BigDecimal.ZERO);
        added.setConstructionImpact("现场量尺后安装，无噪音");
        added.setStatus("ADDED");
        planItems.save(added);
        PlanGenerator.Cost c5 = PlanGenerator.calc(
                planItems.findByApplicationIdAndStatusNot(a5.getId(), "REMOVED"));
        PlanConfirmation pc5 = confirmation(a5.getId(), 1, c5, true);
        pc5.setFamilySigner("周福海之子 周明");
        confirmations.save(pc5);
        timeline(a5, "家属确认方案（第1轮）", "PLAN_REVIEW", "PLAN_FAMILY_CONFIRMED", family2,
                "高风险家庭拒绝紧急呼叫1项（原因已登记，街道审核可查），新增厨房防撞处理1项，待社区重新核定补贴");

        // 6. 方案核准，待施工队接单
        Application a6 = assessedApp(family1, community, assessor, "吴兰英", 78,
                "PLAN_APPROVED", "中", null);
        PlanGenerator.Cost c6 = PlanGenerator.calc(
                planItems.findByApplicationIdAndStatusNot(a6.getId(), "REMOVED"));
        PlanConfirmation pc6 = confirmation(a6.getId(), 1, c6, true);
        pc6.setFamilySigner("吴兰英");
        pc6.setCommunityApproved(true);
        pc6.setCommunityRemark("项目均在补贴目录内，按规则补贴");
        pc6.setCommunityReviewedAt(LocalDateTime.now().minusDays(1));
        confirmations.save(pc6);
        timeline(a6, "家属确认方案（第1轮）", "PLAN_REVIEW", "PLAN_FAMILY_CONFIRMED", family1,
                "家属无增删，签字确认");
        timeline(a6, "社区复核通过", "PLAN_FAMILY_CONFIRMED", "PLAN_APPROVED", community,
                "补贴 " + c6.subsidy() + " 元，自付 " + c6.selfPay() + " 元");

        // 7. 已排期待开工
        Application a7 = plannedApp(family1, community, assessor, team, "郑长发", 83,
                "SCHEDULED", "高");
        timeline(a7, "施工队接单排期", "PLAN_APPROVED", "SCHEDULED", team,
                "计划明天上门，避开午休时段使用电锤");

        // 8. 施工中
        Application a8 = plannedApp(family2, community, assessor, team, "冯淑珍", 80,
                "IN_CONSTRUCTION", "中");
        timeline(a8, "开工", "SCHEDULED", "IN_CONSTRUCTION", team, "扶手与防滑工序进行中");

        // 9. 现场变更待家属确认
        Application a9 = plannedApp(family1, community, assessor, team, "钱国栋", 84,
                "CHANGE_PENDING_FAMILY", "高");
        ConstructionChange ch = new ConstructionChange();
        ch.setApplicationId(a9.getId());
        ch.setReasonType("WALL_UNDRILLABLE");
        ch.setDescription("卫生间淋浴位墙体为轻质隔墙，原方案膨胀螺栓无法承重，"
                + "拟改为背板穿墙对穿固定并增加不锈钢背板，材料与人工增加 320 元");
        ch.setCostDelta(new BigDecimal("320.00"));
        ch.setStatus("SUBMITTED");
        changes.save(ch);
        timeline(a9, "提交现场变更", "IN_CONSTRUCTION", "CHANGE_PENDING_FAMILY", team,
                "墙体无法打孔，变更安装工艺，费用 +320 元");

        // 10. 竣工待街道审核
        Application a10 = plannedApp(family2, community, assessor, team, "马桂芳", 77,
                "COMPLETED", "中");
        Completion cp = new Completion();
        cp.setApplicationId(a10.getId());
        cp.setBeforePhotos("卫生间蹲便旧照、卧室无护栏旧照（现场档案 WP-2026-010-B1.jpg 等2张）");
        cp.setAfterPhotos("坐便+L形扶手完工照、感应夜灯夜间实拍、床边护栏照（3张）");
        cp.setTrialRecord("老人现场试用：扶手起身顺畅，夜灯人到即亮，坐便如厕无需下蹲，试用15分钟无不适");
        cp.setFamilySigner("马桂芳之女 陈敏");
        cp.setMaterialsDetail("L形扶手1套、一字扶手1套、防滑剂1间用量、感应夜灯3只、坐便1套");
        cp.setCostDetail("产品费 3120 元 + 施工人工 980 元，合计 4100 元");
        cp.setTotalCost(new BigDecimal("4100.00"));
        cp.setSignedAt(LocalDateTime.now().minusHours(5));
        cp.setCompletedAt(LocalDateTime.now().minusHours(5));
        completions.save(cp);
        timeline(a10, "竣工验收", "IN_CONSTRUCTION", "COMPLETED", team,
                "前后照片、试用记录、家属签字、材料费用明细齐备，报审");

        // 11. 已审核结算，待质保回访
        Application a11 = plannedApp(family1, community, assessor, team, "刘德海", 86,
                "SETTLED", "高");
        settled(a11, street, team, new BigDecimal("4860.00"), new BigDecimal("4200.00"));

        // 12. 质保回访完成（全流程闭环）
        Application a12 = plannedApp(family2, community, assessor, team, "黄秀珍", 74,
                "VISITED", "低");
        settled(a12, street, team, new BigDecimal("2680.00"), new BigDecimal("2300.00"));
        WarrantyVisit wv = new WarrantyVisit();
        wv.setApplicationId(a12.getId());
        wv.setVisitTime(LocalDateTime.now().minusDays(2));
        wv.setResult("SATISFIED");
        wv.setContent("完工30天回访：扶手无松动、夜灯工作正常，老人对如厕改造非常满意");
        wv.setNextVisitDate(LocalDate.now().plusMonths(11));
        visits.save(wv);
        timeline(a12, "质保回访", "SETTLED", "VISITED", team, "老人满意，纳入年度质保跟踪");
    }

    // ---------- 组装辅助 ----------

    private Application assessedApp(User family, User community, User assessor, String elder,
                                    int age, String status, String risk, String unused) {
        Application app = app(family, elder, age, "上门评估采集", status);
        app.setFloor(4);
        app.setHasElevator(false);
        app.setLivingAlone("高".equals(risk));
        app.setMobility("高".equals(risk) ? "轮椅" : "拐杖");
        app.setFallHistory("高".equals(risk) ? "近一年跌倒3次" : "半年前滑倒1次");
        app.setBathroomStatus("蹲便、瓷砖地面湿滑、无任何扶手");
        app.setBedroomStatus("床边起身困难，夜间摸黑起夜");
        app.setHouseOwnership("自有产权");
        app.setExpectedItems("扶手、防滑、夜灯、坐便改造");
        app.setSubsidyEligible(true);
        app.setAssessorId(assessor.getId());
        app.setVerifiedAt(LocalDateTime.now().minusDays(6));
        app.setAssignedAt(LocalDateTime.now().minusDays(5));
        applications.save(app);

        Assessment a = new Assessment();
        a.setApplicationId(app.getId());
        a.setAssessorId(assessor.getId());
        a.setThresholdHeight(new BigDecimal("3.50"));
        a.setBathroomWidth(new BigDecimal("150"));
        a.setBathroomDepth(new BigDecimal("180"));
        a.setWallMaterial("高".equals(risk) ? "轻质隔墙" : "实心砖墙");
        a.setNightLighting("昏暗");
        a.setBedTransferDifficulty("床高偏低，老人上肢力量弱，独立起身需 20 秒以上");
        a.setTrialActions("坐位→站立需扶持；行走 5 米需中途停顿；试蹲无法自主起立");
        // 五维度现场记录
        a.setMobilityObserved("高".equals(risk) ? "轮椅" : "需搀扶");
        a.setWetness("高".equals(risk) ? "积水" : "较湿");
        a.setBedDifficultyScore("高".equals(risk) ? 3 : 2);
        a.setEmergencyCondition("高".equals(risk) ? "独居，无手机，邻居距离远，无法自主呼叫" : "有手机但常忘带");
        applyRisk(app, a);
        a.setSummary("五维度综合评分 " + a.getRiskScore() + "/15，平台判定风险【" + risk
                + "】；建议优先卫生间与床边改造、完善夜间动线照明与呼叫。");
        a.setAssessedAt(LocalDateTime.now().minusDays(4));
        assessments.save(a);

        List<PlanItem> generated = PlanGenerator.generate(app, a);
        generated.forEach(it -> it.setApplicationId(app.getId()));
        planItems.saveAll(generated);

        timeline(app, "社区核验通过", "SUBMITTED", "VERIFIED", community, null);
        timeline(app, "派单评估", "VERIFIED", "ASSIGNED", community, "派单给评估师李智固");
        timeline(app, "入户评估完成", "ASSIGNED", "PLAN_REVIEW", assessor,
                "五维度评分 " + a.getRiskScore() + "/15，风险等级【" + risk + "】，生成 "
                        + generated.size() + " 项改造建议"
                        + ("高".equals(risk) ? "；高风险将优先排期并安排施工陪同/临时照护" : ""));
        return app;
    }

    private Application plannedApp(User family, User community, User assessor, User team,
                                   String elder, int age, String status, String risk) {
        Application app = assessedAppSilent(family, community, assessor, elder, age, risk);
        PlanGenerator.Cost cost = PlanGenerator.calc(
                planItems.findByApplicationIdAndStatusNot(app.getId(), "REMOVED"));
        PlanConfirmation pc = confirmation(app.getId(), 1, cost, true);
        pc.setFamilySigner(family.getRealName());
        pc.setCommunityApproved(true);
        pc.setCommunityRemark("演示数据：补贴规则复核通过");
        pc.setCommunityReviewedAt(LocalDateTime.now().minusDays(3));
        confirmations.save(pc);
        timeline(app, "社区复核通过", "PLAN_FAMILY_CONFIRMED", "PLAN_APPROVED", community,
                "补贴 " + cost.subsidy() + " 元");

        ConstructionSchedule s = new ConstructionSchedule();
        s.setApplicationId(app.getId());
        s.setTeamId(team.getId());
        s.setScheduledStart(LocalDate.now().minusDays(2));
        s.setScheduledEnd(LocalDate.now().minusDays(1));
        s.setBuildingAccess("单元门洞宽 0.9m，材料经单元门搬运，提前与物业报备");
        s.setElevatorPlan("无电梯，4 楼以下错峰步行搬运，避开早高峰 7:30-8:30");
        s.setMaterialArrival("扶手、坐便、防滑剂已到社区暂存点，夜灯随车携带");
        s.setElderSchedule("老人午休 12:30-14:30，期间仅安排无噪音工序");
        s.setNoiseRestriction("邻里要求电锤作业限 9:00-11:30、15:00-17:30");
        if ("高".equals(risk)) {
            s.setPriority(1);
            s.setCareRequired("COMPANION");
            s.setCareArrangement("高风险优先排期；施工两天由家属轮班全程陪同，坐便拆改当天联系社区日间照护中心临时照护，联系电话已同步施工群");
        } else {
            s.setPriority(0);
            s.setCareRequired("NONE");
        }
        schedules.save(s);
        timeline(app, "施工队接单排期", "PLAN_APPROVED", "SCHEDULED", team,
                ("高".equals(risk) ? "高风险家庭【优先排期】，已落实施工陪同/临时照护安排；" : "")
                        + "结合楼栋通行、电梯、材料、老人作息与噪音限制排期");

        if (!"PLAN_APPROVED".equals(status) && !"SCHEDULED".equals(status)) {
            app.setStatus("IN_CONSTRUCTION");
            timeline(app, "开工", "SCHEDULED", "IN_CONSTRUCTION", team, null);
            applications.save(app);
        }
        app.setStatus(status);
        applications.save(app);
        return app;
    }

    private Application assessedAppSilent(User family, User community, User assessor,
                                          String elder, int age, String risk) {
        Application app = app(family, elder, age, "演示工单", "PLAN_REVIEW");
        app.setFloor(4);
        app.setHasElevator(false);
        app.setLivingAlone("高".equals(risk));
        app.setMobility("高".equals(risk) ? "轮椅" : "拐杖");
        app.setFallHistory("有跌倒史");
        app.setBathroomStatus("蹲便、瓷砖湿滑、无扶手");
        app.setBedroomStatus("床边无支撑、夜灯昏暗");
        app.setHouseOwnership("自有产权");
        app.setExpectedItems("扶手/防滑/坐便/夜灯");
        app.setSubsidyEligible(true);
        app.setAssessorId(assessor.getId());
        app.setVerifiedAt(LocalDateTime.now().minusDays(8));
        app.setAssignedAt(LocalDateTime.now().minusDays(7));
        applications.save(app);

        Assessment a = new Assessment();
        a.setApplicationId(app.getId());
        a.setAssessorId(assessor.getId());
        a.setThresholdHeight(new BigDecimal("3.00"));
        a.setBathroomWidth(new BigDecimal("160"));
        a.setBathroomDepth(new BigDecimal("200"));
        a.setWallMaterial("实心砖墙");
        a.setNightLighting("昏暗");
        a.setBedTransferDifficulty("起身困难需搀扶");
        a.setTrialActions("扶持下可短距离行走");
        a.setMobilityObserved("高".equals(risk) ? "轮椅" : "拄拐");
        a.setWetness("高".equals(risk) ? "较湿" : "一般");
        a.setBedDifficultyScore("高".equals(risk) ? 3 : 1);
        a.setEmergencyCondition("高".equals(risk) ? "独居，老人不会用智能手机，紧急情况下只能敲邻居门" : "有子女同住，配老人机");
        applyRisk(app, a);
        a.setSummary("五维度综合评分 " + a.getRiskScore() + "/15，平台判定风险【" + risk
                + "】；建议卫生间与床边适老化改造、加装夜灯与呼叫装置。");
        a.setAssessedAt(LocalDateTime.now().minusDays(6));
        assessments.save(a);

        List<PlanItem> generated = PlanGenerator.generate(app, a);
        generated.forEach(it -> it.setApplicationId(app.getId()));
        planItems.saveAll(generated);

        timeline(app, "社区核验通过", "SUBMITTED", "VERIFIED", community, null);
        timeline(app, "派单评估", "VERIFIED", "ASSIGNED", community, "派单给评估师李智固");
        timeline(app, "入户评估完成", "ASSIGNED", "PLAN_REVIEW", assessor,
                "系统生成 " + generated.size() + " 项改造建议，跌倒风险：" + risk);
        return app;
    }

    private void settled(Application app, User street, User team, BigDecimal total, BigDecimal subsidy) {
        Completion cp = new Completion();
        cp.setApplicationId(app.getId());
        cp.setBeforePhotos("档案照片：改造前卫生间、卧室（2张）");
        cp.setAfterPhotos("档案照片：改造后扶手、夜灯、坐便/护栏（3张）");
        cp.setTrialRecord("老人试用全部新增设施，动作安全顺畅");
        cp.setFamilySigner("家属现场签字");
        cp.setMaterialsDetail("扶手/防滑/坐便/护栏/夜灯/呼叫按钮等明细见竣工单");
        cp.setCostDetail("材料 + 人工合计 " + total + " 元");
        cp.setTotalCost(total);
        cp.setSignedAt(LocalDateTime.now().minusDays(4));
        cp.setCompletedAt(LocalDateTime.now().minusDays(4));
        completions.save(cp);
        timeline(app, "竣工验收", "IN_CONSTRUCTION", "COMPLETED", team,
                "前后对比照片、老人试用记录、家属签字、材料费用明细齐备，合计 " + total + " 元");

        SubsidyReview r = new SubsidyReview();
        r.setApplicationId(app.getId());
        r.setReviewerId(street.getId());
        r.setConclusion("APPROVED");
        r.setApprovedSubsidy(subsidy);
        r.setSelfPay(total.subtract(subsidy));
        r.setRemark("材料齐备、前后对比清晰、家属签字完整，准予补贴");
        r.setReviewedAt(LocalDateTime.now().minusDays(3));
        reviews.save(r);

        Settlement st = new Settlement();
        st.setApplicationId(app.getId());
        st.setTotalAmount(total);
        st.setSubsidyAmount(subsidy);
        st.setFamilyPayAmount(total.subtract(subsidy));
        st.setTeamPayAmount(total);
        st.setStatus("SETTLED");
        st.setSettledAt(LocalDateTime.now().minusDays(3));
        settlements.save(st);

        app.setStatus("SETTLED");
        applications.save(app);
        timeline(app, "街道补贴审核完成", "COMPLETED", "SETTLED", street,
                "核准补贴 " + subsidy + " 元，已回写施工队结算");
    }

    /** 用平台统一评分引擎回填五维度分值/总分/等级/风险因子/照护建议 */
    private void applyRisk(Application app, Assessment a) {
        RiskService.RiskResult r = RiskService.evaluate(app, a);
        a.setMobilityScore(r.mobility());
        a.setWetnessScore(r.wetness());
        a.setBedDifficultyScore(r.bed());
        a.setLightingScore(r.lighting());
        a.setEmergencyScore(r.emergency());
        a.setRiskScore(r.total());
        a.setFallRiskLevel(r.level());
        a.setRiskFactors(String.join("；", r.factors()));
        a.setCareRecommendation(r.careRecommendation());
    }

    private PlanConfirmation confirmation(Long appId, int round, PlanGenerator.Cost c,
                                           Boolean family) {        PlanConfirmation pc = new PlanConfirmation();
        pc.setApplicationId(appId);
        pc.setRoundNo(round);
        pc.setTotalCost(c.total());
        pc.setSubsidyAmount(c.subsidy());
        pc.setSelfPay(c.selfPay());
        pc.setFamilyConfirmed(family);
        pc.setFamilyConfirmedAt(LocalDateTime.now().minusDays(2));
        return pc;
    }

    private void timeline(Application app, String action, String from, String to, User operator, String note) {
        WorkflowLog l = new WorkflowLog();
        l.setApplicationId(app.getId());
        l.setAction(action);
        l.setFromStatus(from);
        l.setToStatus(to);
        if (operator != null) {
            l.setOperatorId(operator.getId());
            l.setOperatorName(operator.getRealName());
        }
        l.setNote(note);
        logs.save(l);
    }

    private User user(String username, String pwd, String name, String role, String phone, String org) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(pwd);
        u.setRealName(name);
        u.setRole(role);
        u.setPhone(phone);
        u.setOrganization(org);
        u.setEnabled(true);
        return users.save(u);
    }

    private Application app(User family, String elder, int age, String expected, String status) {
        Application a = new Application();
        a.setApplicantUserId(family.getId());
        a.setApplicantName(family.getRealName());
        a.setElderName(elder);
        a.setElderAge(age);
        a.setIdCard("演示证件号（已脱敏）");
        a.setPhone(family.getPhone());
        a.setAddress("幸福里社区 " + (6 + (int) (Math.random() * 20)) + " 栋 "
                + (100 + (int) (Math.random() * 800)) + " 室");
        a.setCommunity("幸福里社区");
        a.setExpectedItems(expected);
        a.setStatus(status);
        return a;
    }
}
