package com.smartschedule.controller;

import com.smartschedule.dto.ChatRequest;
import com.smartschedule.dto.ChatResponse;
import com.smartschedule.entity.*;
import com.smartschedule.repository.AiConversationRepository;
import com.smartschedule.repository.DailyTaskRepository;
import com.smartschedule.service.AiService;
import com.smartschedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiService aiService;
    private final ScheduleService scheduleService;
    private final DailyTaskRepository taskRepo;
    private final AiConversationRepository conversationRepo;

    @PostMapping("/chat")
    @Transactional
    public ResponseEntity<ChatResponse> chat(@AuthenticationPrincipal User user,
                                              @Valid @RequestBody ChatRequest req) {
        String sessionId = req.getSessionId() != null ? req.getSessionId() : UUID.randomUUID().toString();

        ChatResponse response = aiService.chat(user, sessionId, req.getMessage());
        response.setSessionId(sessionId);

        // 如果 AI 检测到排程意图，真正生成日程
        if (response.getAction() != null && "SCHEDULE_GENERATE".equals(response.getAction().getType())) {
            try {
                LocalDate targetDate = LocalDate.now();
                String msg = req.getMessage();
                if (msg != null && (msg.contains("明天") || msg.contains("明日"))) {
                    targetDate = LocalDate.now().plusDays(1);
                }

                DailyPlan plan = scheduleService.generateDailyPlan(user.getId(), targetDate);
                List<DailyTask> tasks = taskRepo.findByDailyPlanIdOrderBySortOrderAsc(plan.getId());

                Map<String, Object> actionData = new LinkedHashMap<>();
                actionData.put("plan", plan);
                actionData.put("tasks", tasks);
                actionData.put("date", targetDate.toString());
                response.getAction().setData(actionData);
                response.getAction().setType("SCHEDULE_GENERATED");
            } catch (Exception e) {
                response.getAction().setType("SCHEDULE_FAILED");
            }
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> sessions(@AuthenticationPrincipal User user) {
        List<String> sessionIds = conversationRepo.findDistinctSessionIdsByUserId(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (String sid : sessionIds) {
            List<AiConversation> msgs = conversationRepo
                    .findByUserIdAndSessionIdOrderByCreatedAtAsc(user.getId(), sid);
            String title = "新对话";
            if (!msgs.isEmpty()) {
                String first = msgs.get(0).getContent();
                title = first.length() > 20 ? first.substring(0, 20) + "..." : first;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sessionId", sid);
            item.put("title", title);
            item.put("messageCount", msgs.size());
            item.put("updatedAt", msgs.isEmpty() ? null : msgs.get(msgs.size() - 1).getCreatedAt());
            result.add(item);
        }
        return result;
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteSession(@AuthenticationPrincipal User user,
                                                              @PathVariable String sessionId) {
        List<AiConversation> conversations = conversationRepo
                .findByUserIdAndSessionIdOrderByCreatedAtAsc(user.getId(), sessionId);
        if (conversations.isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "会话不存在");
            return ResponseEntity.badRequest().body(err);
        }
        conversationRepo.deleteByUserIdAndSessionId(user.getId(), sessionId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "会话已删除");
        result.put("deletedCount", conversations.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    @Transactional(readOnly = true)
    public List<ChatResponse> history(@AuthenticationPrincipal User user,
                                       @RequestParam(required = false) String sessionId) {
        List<AiConversation> list;
        if (sessionId != null && !sessionId.isEmpty()) {
            list = conversationRepo.findByUserIdAndSessionIdOrderByCreatedAtAsc(user.getId(), sessionId);
        } else {
            list = conversationRepo.findByUserIdOrderByCreatedAtAsc(user.getId());
        }
        return list.stream()
            .map(c -> new ChatResponse(c.getRole(), c.getContent()))
            .collect(Collectors.toList());
    }
}
