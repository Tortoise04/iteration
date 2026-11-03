package com.smartschedule.controller;

import com.smartschedule.dto.*;
import com.smartschedule.entity.TaskBookItem;
import com.smartschedule.entity.User;
import com.smartschedule.exception.BusinessException;
import com.smartschedule.service.TaskBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * 任务书 REST 接口（契约 §3），一律 /api/taskbooks，全部需要 JWT。
 */
@RestController
@RequestMapping("/api/taskbooks")
@RequiredArgsConstructor
public class TaskBookController {

    private final TaskBookService taskBookService;

    /** POST /api/taskbooks/generate —— AI 生成整本任务书（DRAFT）。 */
    @PostMapping("/generate")
    public ResponseEntity<TaskBookDetail> generate(@AuthenticationPrincipal User user,
                                                   @RequestBody(required = false) TaskBookGenerateRequest req) {
        Long goalId = req == null ? null : req.getGoalId();
        LocalDate startDate = parseDate(req == null ? null : req.getStartDate());
        LocalDate endDate = parseDate(req == null ? null : req.getEndDate());
        return ResponseEntity.ok(taskBookService.generate(user.getId(), goalId, startDate, endDate));
    }

    /** GET /api/taskbooks —— 任务书列表。 */
    @GetMapping
    public ResponseEntity<List<TaskBookSummary>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(taskBookService.list(user.getId()));
    }

    /** GET /api/taskbooks/today —— 今日执行视图（只读）。 */
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> today(@AuthenticationPrincipal User user,
                                                     @RequestParam(required = false) String date) {
        return ResponseEntity.ok(taskBookService.today(user.getId(), parseDate(date)));
    }

    /** POST /api/taskbooks/reschedule —— 把所有逾期未完成条目顺延。 */
    @PostMapping("/reschedule")
    public ResponseEntity<Map<String, Object>> reschedule(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(taskBookService.rescheduleAll(user.getId()));
    }

    /** GET /api/taskbooks/{id} —— 任务书完整详情。 */
    @GetMapping("/{id}")
    public ResponseEntity<TaskBookDetail> detail(@AuthenticationPrincipal User user,
                                                 @PathVariable Long id) {
        return ResponseEntity.ok(taskBookService.detail(user.getId(), id));
    }

    /** PUT /api/taskbooks/{id} —— 改标题/目的/要求。 */
    @PutMapping("/{id}")
    public ResponseEntity<TaskBookDetail> update(@AuthenticationPrincipal User user,
                                                 @PathVariable Long id,
                                                 @RequestBody(required = false) TaskBookUpdateRequest req) {
        TaskBookUpdateRequest body = req == null ? new TaskBookUpdateRequest() : req;
        return ResponseEntity.ok(taskBookService.update(
                user.getId(), id, body.getTitle(), body.getPurpose(), body.getRequirements()));
    }

    /** PUT /api/taskbooks/{id}/status —— DRAFT / CONFIRMED / ARCHIVED。 */
    @PutMapping("/{id}/status")
    public ResponseEntity<TaskBookDetail> updateStatus(@AuthenticationPrincipal User user,
                                                       @PathVariable Long id,
                                                       @Valid @RequestBody StatusUpdateRequest req) {
        return ResponseEntity.ok(taskBookService.updateStatus(user.getId(), id, req.getStatus()));
    }

    /** PUT /api/taskbooks/items/{itemId} —— 改条目内容。 */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<TaskBookItem> updateItem(@AuthenticationPrincipal User user,
                                                    @PathVariable Long itemId,
                                                    @RequestBody(required = false) TaskBookItemUpdateRequest req) {
        return ResponseEntity.ok(taskBookService.updateItem(user.getId(), itemId, req));
    }

    /** PUT /api/taskbooks/items/{itemId}/status —— DONE / PENDING / DROPPED。 */
    @PutMapping("/items/{itemId}/status")
    public ResponseEntity<Map<String, Object>> updateItemStatus(@AuthenticationPrincipal User user,
                                                               @PathVariable Long itemId,
                                                               @Valid @RequestBody StatusUpdateRequest req) {
        return ResponseEntity.ok(taskBookService.updateItemStatus(user.getId(), itemId, req.getStatus()));
    }

    /** POST /api/taskbooks/items/{itemId}/defer —— 把这一条顺延到最近的可用日。 */
    @PostMapping("/items/{itemId}/defer")
    public ResponseEntity<Map<String, Object>> deferItem(@AuthenticationPrincipal User user,
                                                         @PathVariable Long itemId) {
        return ResponseEntity.ok(taskBookService.deferItem(user.getId(), itemId));
    }

    /** POST /api/taskbooks/{bookId}/items —— 新建一条条目（origin_date=planDate、PENDING、sort_order=当天最大+1）。 */
    @PostMapping("/{bookId}/items")
    public ResponseEntity<TaskBookItem> createItem(@AuthenticationPrincipal User user,
                                                   @PathVariable Long bookId,
                                                   @RequestBody TaskBookItemCreateRequest req) {
        return ResponseEntity.ok(taskBookService.createItem(user.getId(), bookId, req));
    }

    /** DELETE /api/taskbooks/items/{itemId} —— 删除一条条目。 */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Map<String, Object>> deleteItem(@AuthenticationPrincipal User user,
                                                          @PathVariable Long itemId) {
        return ResponseEntity.ok(taskBookService.deleteItem(user.getId(), itemId));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("日期格式不正确，请使用 yyyy-MM-dd 格式");
        }
    }
}
