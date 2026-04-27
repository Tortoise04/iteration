package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.dto.AppUsageDetailDTO;
import com.iterate.digitalwellness.dto.PhoneUsageCreateDTO;
import com.iterate.digitalwellness.dto.PhoneUsageDTO;
import com.iterate.digitalwellness.entity.AppUsageDetail;
import com.iterate.digitalwellness.entity.AppPreset;
import com.iterate.digitalwellness.entity.PhoneUsage;
import com.iterate.digitalwellness.entity.User;
import com.iterate.digitalwellness.repository.AppPresetRepository;
import com.iterate.digitalwellness.repository.AppUsageDetailRepository;
import com.iterate.digitalwellness.repository.UserRepository;
import com.iterate.digitalwellness.service.PhoneUsageService;
import com.iterate.digitalwellness.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/phone-usage")
public class PhoneUsageController {

    private static final Logger logger = LoggerFactory.getLogger(PhoneUsageController.class);

    @Autowired
    private PhoneUsageService phoneUsageService;

    @Autowired
    private AppPresetRepository appPresetRepository;

    @Autowired
    private AppUsageDetailRepository appUsageDetailRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    /**
     * 从 Authorization header 提取用户 ID
     */
    private Long extractUserId(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String username = jwtUtil.getUsernameFromToken(token);
                User user = userRepository.findByUsername(username);
                if (user != null) {
                    return user.getId();
                }
            } catch (Exception e) {
                logger.warn("Token 解析失败: {}", e.getMessage());
            }
        }
        // 开发阶段：如果没有token，返回第一个用户的ID，方便测试
        List<User> users = userRepository.findAll();
        if (!users.isEmpty()) {
            logger.warn("使用默认用户ID: {}", users.get(0).getId());
            return users.get(0).getId();
        }
        return null;
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody PhoneUsageCreateDTO dto,
                                   @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "未授权");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            logger.info("创建手机使用记录: {}, userId: {}", dto.getDate(), userId);

            PhoneUsage phoneUsage = new PhoneUsage();
            phoneUsage.setDate(dto.getDate());
            phoneUsage.setUsageTime(dto.getUsageTime());
            phoneUsage.setCreatedAt(LocalDateTime.now());

            User user = new User();
            user.setId(userId);
            phoneUsage.setUser(user);

            PhoneUsage saved = phoneUsageService.save(phoneUsage);

            // 保存应用详情
            if (dto.getAppDetails() != null && !dto.getAppDetails().isEmpty()) {
                for (AppUsageDetailDTO detailDTO : dto.getAppDetails()) {
                    AppUsageDetail detail = new AppUsageDetail();
                    detail.setPhoneUsage(saved);
                    detail.setAppName(detailDTO.getAppName());
                    detail.setUsageTime(detailDTO.getUsageTime());
                    detail.setCreatedAt(LocalDateTime.now());

                    if (detailDTO.getAppPresetId() != null) {
                        Optional<AppPreset> preset = appPresetRepository.findById(detailDTO.getAppPresetId());
                        preset.ifPresent(detail::setAppPreset);
                    }

                    appUsageDetailRepository.save(detail);
                }
            }

            logger.info("记录创建成功, ID: {}", saved.getId());
            return ResponseEntity.ok(convertToDTO(saved));
        } catch (Exception e) {
            logger.error("创建手机使用记录失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "创建失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @GetMapping
    public ResponseEntity<?> findAll(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "未授权");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            logger.info("查询所有手机使用记录, userId: {}", userId);
            List<PhoneUsage> records = phoneUsageService.findByUserId(userId);
            logger.info("查询到 {} 条记录", records.size());

            List<PhoneUsageDTO> dtos = records.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("查询手机使用记录失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "查询失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id,
                                       @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "未授权");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            PhoneUsage record = phoneUsageService.findById(id);
            if (record == null || !record.getUser().getId().equals(userId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "记录不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(response);
            }
            return ResponseEntity.ok(convertToDTO(record));
        } catch (Exception e) {
            logger.error("查询手机使用记录失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "查询失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PhoneUsageCreateDTO dto,
                                     @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "未授权");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            logger.info("更新手机使用记录 ID: {}", id);
            PhoneUsage existing = phoneUsageService.findById(id);
            if (existing == null || !existing.getUser().getId().equals(userId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "记录不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(response);
            }

            if (dto.getDate() != null) existing.setDate(dto.getDate());
            if (dto.getUsageTime() != null) existing.setUsageTime(dto.getUsageTime());
            existing.setUpdatedAt(LocalDateTime.now());

            PhoneUsage saved = phoneUsageService.save(existing);

            // 更新应用详情：先删除旧的，再添加新的
            List<AppUsageDetail> oldDetails = appUsageDetailRepository.findByPhoneUsageId(saved.getId());
            appUsageDetailRepository.deleteAll(oldDetails);

            if (dto.getAppDetails() != null) {
                for (AppUsageDetailDTO detailDTO : dto.getAppDetails()) {
                    AppUsageDetail detail = new AppUsageDetail();
                    detail.setPhoneUsage(saved);
                    detail.setAppName(detailDTO.getAppName());
                    detail.setUsageTime(detailDTO.getUsageTime());
                    detail.setCreatedAt(LocalDateTime.now());

                    if (detailDTO.getAppPresetId() != null) {
                        Optional<AppPreset> preset = appPresetRepository.findById(detailDTO.getAppPresetId());
                        preset.ifPresent(detail::setAppPreset);
                    }

                    appUsageDetailRepository.save(detail);
                }
            }

            return ResponseEntity.ok(convertToDTO(saved));
        } catch (Exception e) {
            logger.error("更新手机使用记录失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "更新失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(@PathVariable Long id,
                                         @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "未授权");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            logger.info("删除手机使用记录 ID: {}", id);
            PhoneUsage existing = phoneUsageService.findById(id);
            if (existing == null || !existing.getUser().getId().equals(userId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "记录不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(response);
            }

            // 先删除应用详情
            List<AppUsageDetail> details = appUsageDetailRepository.findByPhoneUsageId(id);
            appUsageDetailRepository.deleteAll(details);

            phoneUsageService.deleteById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除手机使用记录失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "删除失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<?> findByDateBetween(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "未授权");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            List<PhoneUsage> records = phoneUsageService.findByUserIdAndDateBetween(userId, startDate, endDate);

            List<PhoneUsageDTO> dtos = records.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("按日期查询手机使用记录失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "查询失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    private PhoneUsageDTO convertToDTO(PhoneUsage phoneUsage) {
        PhoneUsageDTO dto = new PhoneUsageDTO();
        dto.setId(phoneUsage.getId());
        dto.setUserId(phoneUsage.getUserId());
        dto.setDate(phoneUsage.getDate());
        dto.setUsageTime(phoneUsage.getUsageTime());

        List<AppUsageDetail> details = appUsageDetailRepository.findByPhoneUsageId(phoneUsage.getId());
        List<AppUsageDetailDTO> detailDTOs = details.stream().map(detail -> {
            AppUsageDetailDTO detailDTO = new AppUsageDetailDTO();
            detailDTO.setId(detail.getId());
            detailDTO.setPhoneUsageId(phoneUsage.getId());
            detailDTO.setAppName(detail.getAppName());
            detailDTO.setUsageTime(detail.getUsageTime());
            if (detail.getAppPreset() != null) {
                detailDTO.setAppPresetId(detail.getAppPreset().getId());
            }
            return detailDTO;
        }).collect(Collectors.toList());

        dto.setAppDetails(detailDTOs);
        return dto;
    }
}
