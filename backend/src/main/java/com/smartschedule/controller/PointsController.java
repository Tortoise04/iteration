package com.smartschedule.controller;

import com.smartschedule.entity.PointsRecord;
import com.smartschedule.entity.User;
import com.smartschedule.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;

    @GetMapping
    @Transactional(readOnly = true)
    public Map<String, Object> myPoints(@AuthenticationPrincipal User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalPoints", user.getTotalPoints());
        result.put("level", user.getLevel());
        result.put("levelName", getLevelName(user.getLevel()));
        return result;
    }

    @GetMapping("/history")
    @Transactional(readOnly = true)
    public List<PointsRecord> history(@AuthenticationPrincipal User user) {
        return pointsService.getUserPointsHistory(user.getId());
    }

    private String getLevelName(int level) {
        if (level >= 10) return "时间之神";
        if (level >= 8) return "日程领主";
        if (level >= 6) return "时间管理大师";
        if (level >= 5) return "效率达人";
        if (level >= 4) return "自律者";
        if (level >= 3) return "践行者";
        if (level >= 2) return "探索者";
        return "初学者";
    }
}
