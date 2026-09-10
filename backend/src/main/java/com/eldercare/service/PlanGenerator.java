package com.eldercare.service;

import com.eldercare.common.ApiException;
import com.eldercare.domain.Application;
import com.eldercare.domain.Assessment;
import com.eldercare.domain.PlanItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 方案生成引擎：评估结果直接决定改造项目、安装位置与工艺。
 * 补贴规则：按项目设置补贴上限，总额另设封顶；非目录内项目补贴上限为 0。
 */
public final class PlanGenerator {

    public static final BigDecimal TOTAL_SUBSIDY_CAP = new BigDecimal("5000.00");

    /** 标准改造项目目录：类别 -> [名称, 单价, 单项补贴上限, 单位] */
    private record Catalog(String category, String name, String price, String cap, String unit) {
    }

    private static final List<Catalog> CATALOG = List.of(
            new Catalog("扶手", "坐便器旁 L 形安全扶手", "280.00", "280.00", "套"),
            new Catalog("扶手", "淋浴区一字扶手", "220.00", "220.00", "套"),
            new Catalog("扶手", "入户玄关扶手", "260.00", "200.00", "套"),
            new Catalog("防滑", "卫生间地面防滑处理（防滑剂）", "900.00", "800.00", "间"),
            new Catalog("防滑", "淋浴区防滑垫", "120.00", "120.00", "块"),
            new Catalog("坐便", "蹲改坐坐便器+加装扶手", "2600.00", "2000.00", "套"),
            new Catalog("床边护栏", "床边起身辅助护栏", "480.00", "400.00", "套"),
            new Catalog("感应夜灯", "卧室至卫生间感应夜灯", "150.00", "150.00", "套"),
            new Catalog("紧急呼叫", "床头+卫生间紧急呼叫按钮", "1200.00", "1000.00", "套"),
            new Catalog("坡道", "门口无障碍坡道（门槛过渡）", "680.00", "500.00", "处")
    );

    private PlanGenerator() {
    }

    public static List<PlanItem> generate(Application app, Assessment a) {
        List<PlanItem> items = new ArrayList<>();
        String wallFix = wallFixNote(a.getWallMaterial());
        boolean highRisk = "高".equals(a.getFallRiskLevel());
        boolean midRisk = "中".equals(a.getFallRiskLevel());
        boolean needSupport = highRisk || midRisk
                || "拐杖".equals(app.getMobility()) || "轮椅".equals(app.getMobility())
                || "卧床".equals(app.getMobility());
        String bathroom = nz(app.getBathroomStatus());
        String bedDiff = nz(a.getBedTransferDifficulty());
        String fall = nz(app.getFallHistory());

        // 门槛高差 -> 坡道过渡
        if (a.getThresholdHeight() != null && a.getThresholdHeight().compareTo(new BigDecimal("2")) >= 0) {
            items.add(build(catalog("坡道"),
                    "入户/卫生间门槛高差 " + a.getThresholdHeight() + "cm，轮椅及抬脚困难易绊跌",
                    "成品坡道+膨胀螺栓固定，铺装 1 处，几乎无噪音"));
        }

        // 蹲便 -> 坐便改造
        if (bathing(bathroom, "蹲")) {
            items.add(build(catalog("坐便"),
                    "卫生间为蹲便器，老人" + app.getMobility() + "行动，下蹲/起身困难且跌倒风险" + a.getFallRiskLevel(),
                    "拆除旧蹲便、改管道、安装坐便器，约 1 天，有切割噪音与粉尘"));
        }

        // 扶手：按评估结论设置位置；安装工艺随墙体材质变化
        if (needSupport) {
            String grabReason = "老人" + app.getMobility() + "行动、跌倒风险" + a.getFallRiskLevel()
                    + (fall.isEmpty() ? "" : "，有跌倒史（" + truncate(fall) + "）");
            items.add(build(catalog("扶手", "坐便器旁 L 形安全扶手"),
                    grabReason + "，坐便起身需借力",
                    "安装于坐便器侧前方承重墙体；" + wallFix));
            items.add(build(catalog("扶手", "淋浴区一字扶手"),
                    grabReason + "，淋浴湿滑环境易滑倒",
                    "安装于淋浴位侧墙，离地 70-80cm；" + wallFix));
        }

        // 卫生间防滑（现状描述或现场湿滑分级任一命中即处理）
        String wet = nz(a.getWetness());
        boolean wetFloor = wet.contains("积水") || wet.contains("较湿");
        if (bathing(bathroom, "瓷砖") || bathing(bathroom, "滑") || wetFloor || bathroom.isEmpty()) {
            String why = wetFloor
                    ? "现场判定卫生间地面" + wet + "，洗漱后湿滑明显（尺寸 "
                            + nz(a.getBathroomWidth()) + "×" + nz(a.getBathroomDepth()) + "cm）"
                    : "卫生间地面为光面瓷砖、洗澡后湿滑（尺寸 "
                            + nz(a.getBathroomWidth()) + "×" + nz(a.getBathroomDepth()) + "cm）";
            items.add(build(catalog("防滑", "卫生间地面防滑处理（防滑剂）"),
                    why,
                    "瓷砖面渗透防滑剂，不砸砖、约半天，基本无噪音"));
        }
        items.add(build(catalog("防滑", "淋浴区防滑垫"),
                "淋浴站立时脚下需要即时防滑缓冲",
                "成品铺设，无施工影响"));

        // 床边护栏：床边起身困难点直接驱动
        if (!bedDiff.isBlank() || needSupport) {
            items.add(build(catalog("床边护栏"),
                    bedDiff.isBlank()
                            ? "老人起立需借力，加装护栏辅助床边转身与起身"
                            : "评估记录床边起身困难点：" + truncate(bedDiff),
                    "成品夹具式护栏，无需打孔，约 30 分钟"));
        }

        // 夜间照明不足 -> 感应夜灯
        if (!"充足".equals(a.getNightLighting())) {
            items.add(build(catalog("感应夜灯"),
                    "夜间照明" + nz(a.getNightLighting(), "一般") + "，老人起夜频次高，动线昏暗易跌倒",
                    "沿床—通道—卫生间布置人体感应灯，插电/免布线，约 1 小时"));
        }

        // 独居/高危/行动极差 -> 紧急呼叫
        if (Boolean.TRUE.equals(app.getLivingAlone()) || highRisk
                || "轮椅".equals(app.getMobility()) || "卧床".equals(app.getMobility())) {
            String why = Boolean.TRUE.equals(app.getLivingAlone()) ? "老人独居，突发意外无法及时呼救"
                    : "跌倒风险高/行动能力差，跌倒后难自主起身呼救";
            items.add(build(catalog("紧急呼叫"),
                    why + "；夜间照明" + nz(a.getNightLighting(), "一般"),
                    "床头及卫生间各一个呼叫按钮，联网家属/社区，免布线安装约 1 小时"));
        }
        return items;
    }

    /** 按当前方案项目重算费用、补贴、自付 */
    public static Cost calc(List<PlanItem> activeItems) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal subsidy = BigDecimal.ZERO;
        for (PlanItem it : activeItems) {
            BigDecimal line = lineAmount(it);
            total = total.add(line);
            // subsidyCap 为「该项单价补贴上限」，补贴额 = 上限 × 数量
            BigDecimal cap = it.getSubsidyCap() == null ? BigDecimal.ZERO : it.getSubsidyCap();
            subsidy = subsidy.add(cap.multiply(BigDecimal.valueOf(it.getQuantity())));
        }
        if (subsidy.compareTo(TOTAL_SUBSIDY_CAP) > 0) {
            subsidy = TOTAL_SUBSIDY_CAP;
        }
        if (subsidy.compareTo(total) > 0) {
            subsidy = total;
        }
        return new Cost(total, subsidy, total.subtract(subsidy));
    }

    /** 单行总额：有材料/人工拆分时用拆分值，否则回退单价×数量 */
    public static BigDecimal lineAmount(PlanItem it) {
        if (it.getMaterialFee() != null || it.getLaborFee() != null) {
            BigDecimal m = it.getMaterialFee() == null ? BigDecimal.ZERO : it.getMaterialFee();
            BigDecimal l = it.getLaborFee() == null ? BigDecimal.ZERO : it.getLaborFee();
            return m.add(l);
        }
        return it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity()));
    }

    /** 方案材料费/人工费汇总 */
    public static MaterialLabor splitCost(List<PlanItem> activeItems) {
        BigDecimal material = BigDecimal.ZERO;
        BigDecimal labor = BigDecimal.ZERO;
        for (PlanItem it : activeItems) {
            if (it.getMaterialFee() != null) {
                material = material.add(it.getMaterialFee());
                labor = labor.add(it.getLaborFee() == null ? BigDecimal.ZERO : it.getLaborFee());
            } else {
                // 无拆分明细时按行金额 80% 材料 / 20% 人工兜底
                BigDecimal line = lineAmount(it);
                BigDecimal m = line.multiply(new BigDecimal("0.8")).setScale(2, java.math.RoundingMode.HALF_UP);
                material = material.add(m);
                labor = labor.add(line.subtract(m));
            }
        }
        return new MaterialLabor(material, labor);
    }

    public record MaterialLabor(BigDecimal material, BigDecimal labor) {
    }

    public record Cost(BigDecimal total, BigDecimal subsidy, BigDecimal selfPay) {
    }

    /** 家属新增项目：目录内类别给标准补贴上限，其余类别补贴为 0（需社区复核） */
    public static BigDecimal subsidyCapFor(String category, BigDecimal price) {
        return CATALOG.stream()
                .filter(c -> c.category().equals(category))
                .map(c -> new BigDecimal(c.cap()))
                .findFirst()
                .orElse(BigDecimal.ZERO).min(price);
    }

    private static String wallFixNote(String wall) {
        String w = nz(wall);
        if (w.contains("轻质") || w.contains("空心")) {
            return "该墙体为" + w + "，不能直接膨胀螺栓，采用背板加固/穿墙对穿螺栓工艺，工期增加约 1 小时";
        }
        if (w.contains("瓷砖")) {
            return "瓷砖墙面先定位钻孔、避免空鼓开裂，配装饰盖";
        }
        return w.contains("实心") ? "实心砖墙可直接膨胀螺栓固定" : "默认膨胀螺栓固定，打孔当天约 30 分钟噪音";
    }

    private static boolean bathing(String text, String key) {
        return text.contains(key);
    }

    private static String nz(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static String nz(Object v, String dft) {
        String s = nz(v);
        return s.isEmpty() ? dft : s;
    }

    private static String truncate(String s) {
        return s.length() > 60 ? s.substring(0, 60) + "…" : s;
    }

    private static Catalog catalog(String category) {
        return CATALOG.stream().filter(c -> c.category().equals(category)).findFirst()
                .orElseThrow(() -> new ApiException("未知项目类别: " + category));
    }

    private static Catalog catalog(String category, String name) {
        return CATALOG.stream().filter(c -> c.category().equals(category) && c.name().equals(name))
                .findFirst().orElseThrow(() -> new ApiException("未知项目: " + name));
    }

    private static PlanItem build(Catalog c, String reason, String impact) {
        PlanItem p = new PlanItem();
        p.setCategory(c.category());
        p.setName(c.name());
        p.setSpec("标准适老规格，符合 GB 50763 无障碍设计要求");
        p.setUnit(c.unit());
        p.setQuantity(1);
        p.setUnitPrice(new BigDecimal(c.price()));
        p.setSubsidyCap(new BigDecimal(c.cap()));
        // 行级费用拆分：约 80% 材料、20% 人工（演示口径，人工=差额避免尾差）
        BigDecimal line = new BigDecimal(c.price());
        BigDecimal material = line.multiply(new BigDecimal("0.8")).setScale(2, java.math.RoundingMode.HALF_UP);
        p.setMaterialFee(material);
        p.setLaborFee(line.subtract(material));
        p.setSource("ASSESSMENT");
        p.setReason(reason);
        p.setConstructionImpact(impact);
        p.setStatus("PROPOSED");
        return p;
    }
}
