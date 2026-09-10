package com.eldercare.service;

import java.util.Map;

/** 流程状态中文标签与步骤序号，供前端时间线/标签使用 */
public final class StatusMeta {

    public static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("SUBMITTED", "待社区核验"),
            Map.entry("VERIFY_REJECTED", "核验不通过"),
            Map.entry("VERIFIED", "已核验待派单"),
            Map.entry("ASSIGNED", "已派单待评估"),
            Map.entry("PLAN_REVIEW", "方案待家属确认"),
            Map.entry("PLAN_FAMILY_CONFIRMED", "家属已确认待社区复核"),
            Map.entry("PLAN_APPROVED", "方案已核准待接单"),
            Map.entry("SCHEDULED", "已排期待施工"),
            Map.entry("IN_CONSTRUCTION", "施工中"),
            Map.entry("CHANGE_PENDING_FAMILY", "变更待家属确认"),
            Map.entry("CHANGE_PENDING_COMMUNITY", "变更待社区复核"),
            Map.entry("COMPLETED", "竣工待街道审核"),
            Map.entry("SETTLED", "补贴已审核已结算"),
            Map.entry("VISITED", "质保回访完成")
    );

    public static final Map<String, String> CHANGE_REASONS = Map.of(
            "WALL_UNDRILLABLE", "墙体无法打孔",
            "PIPE_BLOCK", "管线占位",
            "HOSPITAL", "老人临时住院",
            "FAMILY_CHANGE", "家属意见变化",
            "MODEL_MISMATCH", "材料型号不匹配"
    );

    private StatusMeta() {
    }

    public static String label(String status) {
        return LABELS.getOrDefault(status, status);
    }
}
