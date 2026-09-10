package com.eldercare.web;

import com.eldercare.service.StatusMeta;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meta")
public class MetaController {

    @GetMapping
    public Map<String, Object> meta() {
        return Map.of(
                "statusLabels", StatusMeta.LABELS,
                "changeReasons", StatusMeta.CHANGE_REASONS,
                "mobility", List.of("独立", "拐杖", "轮椅", "卧床"),
                "wallMaterial", List.of("实心砖墙", "空心砖墙", "轻质隔墙", "瓷砖墙面"),
                "nightLighting", List.of("充足", "一般", "昏暗"),
                "ownership", List.of("自有产权", "租赁", "公房", "子女房产"),
                "planCategories", List.of("扶手", "防滑", "坐便", "床边护栏", "感应夜灯", "紧急呼叫", "坡道", "其他")
        );
    }
}
