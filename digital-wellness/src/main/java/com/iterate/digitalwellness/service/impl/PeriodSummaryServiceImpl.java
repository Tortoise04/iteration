package com.iterate.digitalwellness.service.impl;

import com.iterate.digitalwellness.ai.AIService;
import com.iterate.digitalwellness.entity.DailyActivity;
import com.iterate.digitalwellness.entity.Goal;
import com.iterate.digitalwellness.entity.PeriodSummary;
import com.iterate.digitalwellness.entity.PhoneUsage;
import com.iterate.digitalwellness.repository.DailyActivityRepository;
import com.iterate.digitalwellness.repository.GoalRepository;
import com.iterate.digitalwellness.repository.PeriodSummaryRepository;
import com.iterate.digitalwellness.repository.PhoneUsageRepository;
import com.iterate.digitalwellness.service.PeriodSummaryService;
import com.iterate.digitalwellness.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PeriodSummaryServiceImpl implements PeriodSummaryService {

    private static final Logger logger = LoggerFactory.getLogger(PeriodSummaryServiceImpl.class);

    @Autowired
    private PeriodSummaryRepository periodSummaryRepository;
    @Autowired
    private DailyActivityRepository dailyActivityRepository;
    @Autowired
    private PhoneUsageRepository phoneUsageRepository;
    @Autowired
    private GoalRepository goalRepository;
    @Autowired
    private AIService aiService;

    @Override
    public PeriodSummary save(PeriodSummary periodSummary) {
        // 设置当前用户ID
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null && periodSummary.getUserId() == null) {
            periodSummary.setUserId(userId);
        }
        return periodSummaryRepository.save(periodSummary);
    }

    @Override
    public List<PeriodSummary> findAll() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            return periodSummaryRepository.findByUserId(userId);
        }
        return periodSummaryRepository.findAll();
    }

    @Override
    public List<PeriodSummary> findByUserId(Long userId) {
        return periodSummaryRepository.findByUserId(userId);
    }

    @Override
    public PeriodSummary findById(Long id) {
        PeriodSummary summary = periodSummaryRepository.findById(id).orElse(null);
        // 校验数据归属
        Long userId = SecurityUtils.getCurrentUserId();
        if (summary != null && userId != null && summary.getUserId() != null && !summary.getUserId().equals(userId)) {
            return null;
        }
        return summary;
    }

    @Override
    public void deleteById(Long id) {
        PeriodSummary summary = periodSummaryRepository.findById(id).orElse(null);
        // 校验数据归属
        Long userId = SecurityUtils.getCurrentUserId();
        if (summary != null && userId != null && summary.getUserId() != null && !summary.getUserId().equals(userId)) {
            return;
        }
        periodSummaryRepository.deleteById(id);
    }

    @Override
    public PeriodSummary generateSummary(LocalDate startDate, LocalDate endDate, String periodType) {
        Long userId = SecurityUtils.getCurrentUserId();
        return generateSummary(userId, startDate, endDate, periodType);
    }

    @Override
    public PeriodSummary generateSummary(Long userId, LocalDate startDate, LocalDate endDate, String periodType) {
        logger.info("开始生成周期总结: userId={}, startDate={}, endDate={}, periodType={}", userId, startDate, endDate, periodType);

        // 如果userId为null，返回空数据，不查询其他用户的数据
        if (userId == null) {
            logger.error("用户未登录，无法生成总结");
            throw new RuntimeException("用户未登录，无法生成总结");
        }

        try {
            // 获取每日活动数据（按用户过滤）
            List<DailyActivity> dailyActivities = dailyActivityRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
            logger.info("查询到 {} 条每日活动", dailyActivities.size());

            // 获取手机使用数据（按用户过滤）
            List<PhoneUsage> phoneUsages = phoneUsageRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
            logger.info("查询到 {} 条手机使用记录", phoneUsages.size());

            // 获取目标数据（按用户过滤）
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            List<Goal> goals = goalRepository.findByUserId(userId).stream()
                    .filter(g -> g.getStartTime().isBefore(endDateTime) && g.getEndTime().isAfter(startDateTime))
                    .collect(Collectors.toList());
            logger.info("查询到 {} 个目标", goals.size());

            // 准备 AI 输入数据
            StringBuilder dataBuilder = new StringBuilder();
            String periodLabel = "WEEKLY".equals(periodType) ? "周" : "月";
            dataBuilder.append("# ").append(periodLabel).append("度总结\n\n");
            dataBuilder.append("**时间范围**：").append(startDate).append(" 至 ").append(endDate).append("\n\n");

            // 目标数据
            if (!goals.isEmpty()) {
                dataBuilder.append("## 目标完成情况\n\n");

                long completedCount = goals.stream().filter(g -> "COMPLETED".equals(g.getStatus())).count();
                long inProgressCount = goals.stream().filter(g -> "IN_PROGRESS".equals(g.getStatus())).count();
                long cancelledCount = goals.stream().filter(g -> "CANCELLED".equals(g.getStatus())).count();

                dataBuilder.append("**统计**：共 ").append(goals.size()).append(" 个目标\n");
                dataBuilder.append("- 已完成：").append(completedCount).append(" 个\n");
                dataBuilder.append("- 进行中：").append(inProgressCount).append(" 个\n");
                if (cancelledCount > 0) {
                    dataBuilder.append("- 已取消：").append(cancelledCount).append(" 个\n");
                }
                dataBuilder.append("\n**目标详情**：\n");

                for (Goal goal : goals) {
                    String statusIcon = "COMPLETED".equals(goal.getStatus()) ? "[V]" :
                            "CANCELLED".equals(goal.getStatus()) ? "[X]" : "[~]";
                    dataBuilder.append("- ").append(statusIcon).append(" **").append(goal.getGoal()).append("**");
                    if (goal.getDescription() != null && !goal.getDescription().isEmpty()) {
                        dataBuilder.append("：").append(goal.getDescription());
                    }
                    dataBuilder.append("\n");
                }
                dataBuilder.append("\n");
            }

            // 手机使用数据
            if (!phoneUsages.isEmpty()) {
                dataBuilder.append("## 手机使用情况\n\n");
                long totalUsageTime = 0;
                for (PhoneUsage usage : phoneUsages) {
                    totalUsageTime += usage.getUsageTime() != null ? usage.getUsageTime() : 0;
                }
                long avgUsageTime = phoneUsages.size() > 0 ? totalUsageTime / phoneUsages.size() : 0;

                dataBuilder.append("**统计**：\n");
                dataBuilder.append("- 记录天数：").append(phoneUsages.size()).append(" 天\n");
                dataBuilder.append("- 总使用时间：").append(totalUsageTime).append(" 分钟");
                if (totalUsageTime >= 60) {
                    dataBuilder.append("（约 ").append(totalUsageTime / 60).append(" 小时）\n");
                } else {
                    dataBuilder.append("\n");
                }
                dataBuilder.append("- 日均使用：").append(avgUsageTime).append(" 分钟\n\n");
                dataBuilder.append("**每日详情**：\n");
                for (PhoneUsage usage : phoneUsages) {
                    dataBuilder.append("- ").append(usage.getDate()).append("：").append(usage.getUsageTime()).append(" 分钟\n");
                }
                dataBuilder.append("\n");
            }

            // 每日活动数据
            if (!dailyActivities.isEmpty()) {
                dataBuilder.append("## 每日活动记录\n\n");
                dataBuilder.append("**统计**：共记录 ").append(dailyActivities.size()).append(" 项活动\n\n");

                int totalDuration = 0;
                for (DailyActivity activity : dailyActivities) {
                    if (activity.getDuration() != null) {
                        totalDuration += activity.getDuration();
                    }
                }
                if (totalDuration > 0) {
                    dataBuilder.append("- 活动总时长：").append(totalDuration).append(" 分钟");
                    if (totalDuration >= 60) {
                        dataBuilder.append("（约 ").append(totalDuration / 60).append(" 小时）\n\n");
                    } else {
                        dataBuilder.append("\n\n");
                    }
                }

                dataBuilder.append("**活动详情**：\n");
                for (DailyActivity activity : dailyActivities) {
                    dataBuilder.append("- **").append(activity.getDate()).append("**");
                    if (activity.getDuration() != null) {
                        dataBuilder.append("（").append(activity.getDuration()).append("分钟）");
                    }
                    dataBuilder.append("：").append(activity.getActivity());
                    if (activity.getLocation() != null && !activity.getLocation().isEmpty()) {
                        dataBuilder.append(" @").append(activity.getLocation());
                    }
                    dataBuilder.append("\n");
                }
                dataBuilder.append("\n");
            }

            // 如果没有任何数据
            if (dailyActivities.isEmpty() && phoneUsages.isEmpty() && goals.isEmpty()) {
                dataBuilder.append("该时间段内暂无任何数据记录。\n\n");
                dataBuilder.append("建议：\n");
                dataBuilder.append("- 开始记录您的手机使用情况\n");
                dataBuilder.append("- 添加每日活动记录\n");
                dataBuilder.append("- 设定新的目标\n");
            }

            String dataContent = dataBuilder.toString();
            logger.info("准备数据内容长度: {} 字符", dataContent.length());

            // 使用 AI 生成总结
            String aiSummary = aiService.generateSummary(dataContent);
            logger.info("AI 返回内容长度: {} 字符", aiSummary.length());
            logger.debug("AI 返回内容: {}", aiSummary);

            // 检查 AI 响应是否为错误
            if (aiSummary.startsWith("AI 服务未配置") ||
                aiSummary.startsWith("AI 调用失败") ||
                aiSummary.startsWith("AI 调用异常") ||
                aiSummary.startsWith("AI 调用超时") ||
                aiSummary.startsWith("AI 服务错误")) {
                logger.error("AI 服务调用失败: {}", aiSummary);
                throw new RuntimeException("AI 服务调用失败：" + aiSummary);
            }

            // 创建并保存周期总结
            PeriodSummary periodSummary = new PeriodSummary();
            periodSummary.setPeriod(periodLabel + "度总结 (" + startDate + " 至 " + endDate + ")");
            periodSummary.setPeriodType(periodType);
            periodSummary.setStartDate(startDate);
            periodSummary.setEndDate(endDate);
            periodSummary.setSummary(aiSummary);
            periodSummary.setUserId(userId);

            PeriodSummary saved = periodSummaryRepository.save(periodSummary);
            logger.info("周期总结保存成功, ID: {}", saved.getId());

            return saved;

        } catch (Exception e) {
            logger.error("生成周期总结时发生异常", e);
            throw new RuntimeException("生成周期总结失败：" + e.getMessage(), e);
        }
    }
}
