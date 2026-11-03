package com.smartschedule.controller;

import com.smartschedule.entity.Course;
import com.smartschedule.entity.User;
import com.smartschedule.dto.CourseRequest;
import com.smartschedule.exception.BusinessException;
import com.smartschedule.repository.CourseRepository;
import com.smartschedule.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import javax.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseRepository courseRepo;
    private final UserRepository userRepo;

    private static final DateTimeFormatter[] TIME_FORMATTERS = {
        DateTimeFormatter.ofPattern("HH:mm"),
        DateTimeFormatter.ofPattern("H:mm"),
        DateTimeFormatter.ofPattern("HH:mm:ss"),
        DateTimeFormatter.ISO_LOCAL_TIME
    };

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            throw new BusinessException("时间不能为空");
        }
        String trimmed = timeStr.trim();

        for (DateTimeFormatter fmt : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(trimmed, fmt);
            } catch (DateTimeParseException ignored) { }
        }

        // 处理完整日期时间字符串（如 axios 序列化 Date 产生的 ISO 字符串）
        try {
            if (trimmed.contains("T")) {
                String afterT = trimmed.substring(trimmed.indexOf('T') + 1);
                int tzIdx = afterT.indexOf('+');
                if (tzIdx < 0) tzIdx = afterT.indexOf('Z');
                if (tzIdx < 0) tzIdx = afterT.indexOf('-', 3);
                if (tzIdx > 0) afterT = afterT.substring(0, tzIdx);
                int dotIdx = afterT.indexOf('.');
                if (dotIdx > 0) afterT = afterT.substring(0, dotIdx);
                return LocalTime.parse(afterT, DateTimeFormatter.ofPattern("HH:mm:ss"));
            }
        } catch (Exception ignored) { }

        throw new BusinessException("时间格式错误: " + timeStr + "，请使用 HH:mm 格式");
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Course> list(@AuthenticationPrincipal User user) {
        return courseRepo.findByUserIdAndIsActiveTrue(user.getId());
    }

    @PostMapping
    @Transactional
    public Course create(@AuthenticationPrincipal User user, @Valid @RequestBody CourseRequest req) {
        User managedUser = userRepo.getReferenceById(user.getId());
        Course course = Course.builder()
                .user(managedUser)
                .name(req.getName())
                .dayOfWeek(req.getDayOfWeek())
                .startTime(parseTime(req.getStartTime()))
                .endTime(parseTime(req.getEndTime()))
                .location(req.getLocation())
                .isActive(true)
                .build();
        return courseRepo.save(course);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Course course = courseRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("课程不存在"));
        if (!course.getUser().getId().equals(user.getId())) {
            throw new BusinessException("无权操作");
        }
        course.setIsActive(false);
        courseRepo.save(course);

        Map<String, String> result = new LinkedHashMap<>();
        result.put("message", "已删除");
        return ResponseEntity.ok(result);
    }
}
