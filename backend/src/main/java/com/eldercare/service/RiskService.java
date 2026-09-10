package com.eldercare.service;

import com.eldercare.common.ApiException;
import com.eldercare.domain.Application;
import com.eldercare.domain.Assessment;

import java.util.ArrayList;
import java.util.List;

/**
 * 入户评估风险分级引擎。
 * 五个现场维度加权评分（满分 15）：
 * 行动能力 0-4、卫生间湿滑 0-3、床边起身难度 0-3、夜间照明 0-2、紧急呼叫条件 0-3。
 * 总分 >=10 高风险，5-9 中风险，<=4 低风险。
 */
public final class RiskService {

    public record RiskResult(int mobility, int wetness, int bed, int lighting, int emergency,
                             int total, String level, List<String> factors, String careRecommendation) {
    }

    private RiskService() {
    }

    public static RiskResult evaluate(Application app, Assessment a) {
        List<String> factors = new ArrayList<>();

        int mobility = scoreMobility(a.getMobilityObserved(), app.getMobility());
        switch (mobility) {
            case 4 -> factors.add("行动能力：卧床，完全依赖照护（4分）");
            case 3 -> factors.add("行动能力：依赖轮椅，转移困难（3分）");
            case 2 -> factors.add("行动能力：需搀扶行走，平衡能力差（2分）");
            case 1 -> factors.add("行动能力：拄拐慢行，下肢力量弱（1分）");
            default -> { }
        }

        int wetness = scoreWetness(a.getWetness());
        switch (wetness) {
            case 3 -> factors.add("卫生间地面积水、打滑严重（3分）");
            case 2 -> factors.add("卫生间地面较湿、无有效防滑（2分）");
            case 1 -> factors.add("卫生间洗漱后地面偏湿（1分）");
            default -> { }
        }

        int bed = clamp(a.getBedDifficultyScore(), 0, 3);
        if (bed >= 3) {
            factors.add("床边起身：无法独立完成，需他人搀扶（3分）");
        } else if (bed == 2) {
            factors.add("床边起身：明显困难、耗时长，夜间风险高（2分）");
        } else if (bed == 1) {
            factors.add("床边起身：略有困难（1分）");
        }

        int lighting = scoreLighting(a.getNightLighting());
        if (lighting == 2) {
            factors.add("夜间照明：昏暗，床到卫生间动线无灯（2分）");
        } else if (lighting == 1) {
            factors.add("夜间照明：一般，局部动线看不清（1分）");
        }

        int emergency = scoreEmergency(a.getEmergencyCondition(),
                Boolean.TRUE.equals(app.getLivingAlone()));
        if (emergency >= 3) {
            factors.add("紧急呼叫：缺乏有效呼救手段，意外后无法及时求助（3分）");
        } else if (emergency == 2) {
            factors.add("紧急呼叫：仅能大声呼救/邻里距离远，独居响应无保障（2分）");
        } else if (emergency == 1) {
            factors.add("紧急呼叫：有手机但不随身或操作困难（1分）");
        }

        if (app.getFallHistory() != null && app.getFallHistory().contains("有")) {
            factors.add("既往跌倒史：" + app.getFallHistory());
        }

        int total = mobility + wetness + bed + lighting + emergency;
        String level = total >= 10 ? "高" : total >= 5 ? "中" : "低";

        String care = null;
        if ("高".equals(level)) {
            List<String> tips = new ArrayList<>();
            tips.add("高风险家庭，列入优先排期，压缩开工等待时间");
            if (mobility >= 3) {
                tips.add("施工期间需家属全程陪同；坐便拆改、卫生间湿作业当天安排临时照护，防止老人使用临时设施发生意外");
            } else {
                tips.add("施工期间建议家属在场陪同，关键拆改工序安排临时照护或日间托管");
            }
            if (emergency >= 2) {
                tips.add("紧急呼叫装置优先于其他项目先安装并当日联网调试");
            }
            tips.add("为老人安排远离噪声/粉尘的临时休息区，午休时段停工");
            care = String.join("；", tips) + "。";
        } else if ("中".equals(level)) {
            care = "中风险家庭：建议施工当日有家属或邻里照看，材料堆放与湿作业区域设置临时警示。";
        }
        return new RiskResult(mobility, wetness, bed, lighting, emergency, total, level, factors, care);
    }

    private static int scoreMobility(String observed, String declared) {
        String m = observed != null && !observed.isBlank() ? observed : declared;
        if (m == null) return 0;
        if (m.contains("卧床")) return 4;
        if (m.contains("轮椅")) return 3;
        if (m.contains("搀扶")) return 2;
        if (m.contains("拐")) return 1;
        return 0; // 独立
    }

    private static int scoreWetness(String w) {
        if (w == null) return 0;
        return switch (w) {
            case "积水" -> 3;
            case "较湿" -> 2;
            case "一般" -> 1;
            default -> 0; // 干燥
        };
    }

    private static int scoreLighting(String l) {
        if ("昏暗".equals(l)) return 2;
        if ("一般".equals(l)) return 1;
        return 0;
    }

    /** 从评估师记录的紧急呼叫条件文本中推断评分；评估师也可直接传 emergencyScore 覆盖 */
    public static int scoreEmergency(String condition, boolean livingAlone) {
        if (condition == null || condition.isBlank()) {
            return livingAlone ? 2 : 1;
        }
        String c = condition;
        boolean hasDevice = c.contains("呼叫器") || c.contains("呼叫按钮") || c.contains("同住")
                || c.contains("陪同") || c.contains("随身") && c.contains("手机");
        boolean noMeans = c.contains("无手机") || c.contains("没有手机") || c.contains("无法呼叫")
                || c.contains("无呼叫") || c.contains("联系不上");
        if (noMeans) return 3;
        if (hasDevice) return 0;
        if (c.contains("手机")) return 1;
        if (c.contains("邻居") || c.contains("独居") || c.contains("独处")) return 2;
        return livingAlone ? 2 : 1;
    }

    private static int clamp(Integer v, int min, int max) {
        if (v == null) return 0;
        return Math.max(min, Math.min(max, v));
    }

    public static void requireLevel(String level) {
        if (!"高".equals(level) && !"中".equals(level) && !"低".equals(level)) {
            throw new ApiException("风险等级必须为 高/中/低");
        }
    }
}
