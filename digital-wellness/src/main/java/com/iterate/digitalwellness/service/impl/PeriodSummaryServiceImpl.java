package com.iterate.digitalwellness.service.impl;

import com.iterate.digitalwellness.entity.DailyActivity;
import com.iterate.digitalwellness.entity.PeriodSummary;
import com.iterate.digitalwellness.entity.PhoneUsage;
import com.iterate.digitalwellness.repository.DailyActivityRepository;
import com.iterate.digitalwellness.repository.PeriodSummaryRepository;
import com.iterate.digitalwellness.repository.PhoneUsageRepository;
import com.iterate.digitalwellness.service.PeriodSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PeriodSummaryServiceImpl implements PeriodSummaryService {
    @Autowired
    private PeriodSummaryRepository periodSummaryRepository;
    @Autowired
    private DailyActivityRepository dailyActivityRepository;
    @Autowired
    private PhoneUsageRepository phoneUsageRepository;
    @Autowired
    private AIServiceImpl aiService;

    @Override
    public PeriodSummary save(PeriodSummary periodSummary) {
        return periodSummaryRepository.save(periodSummary);
    }

    @Override
    public List<PeriodSummary> findAll() {
        return periodSummaryRepository.findAll();
    }

    @Override
    public PeriodSummary findById(Long id) {
        return periodSummaryRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteById(Long id) {
        periodSummaryRepository.deleteById(id);
    }

    @Override
    public PeriodSummary generateSummary(LocalDate startDate, LocalDate endDate, String periodType) {
        // 获取每日活动数据
        List<DailyActivity> dailyActivities = dailyActivityRepository.findByDateBetween(startDate, endDate);
        // 获取手机使用数据
        List<PhoneUsage> phoneUsages = phoneUsageRepository.findByDateBetween(startDate, endDate);

        // 准备 AI 输入数据
        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append(periodType).append("总结 (").append(startDate).append(" 至 " ).append(endDate).append(")\n\n");

        // 每日活动数据
        if (!dailyActivities.isEmpty()) {
            dataBuilder.append("每日活动：\n");
            for (DailyActivity activity : dailyActivities) {
                dataBuilder.append(activity.getDate()).append("：").append(activity.getActivity()).append("\n");
            }
            dataBuilder.append("\n");
        }

        // 手机使用数据
        if (!phoneUsages.isEmpty()) {
            dataBuilder.append("手机使用情况：\n");
            long totalUsageTime = 0;
            for (PhoneUsage usage : phoneUsages) {
                dataBuilder.append(usage.getDate()).append("：").append(usage.getUsageTime()).append("分钟\n");
                totalUsageTime += usage.getUsageTime();
            }
            dataBuilder.append("总使用时间：").append(totalUsageTime).append("分钟\n");
            dataBuilder.append("平均使用时间：").append(totalUsageTime / phoneUsages.size()).append("分钟\n");
        }

        // 使用 AI 生成总结
        String aiSummary = aiService.generateSummary(dataBuilder.toString());

        // 创建并保存周期总结
        PeriodSummary periodSummary = new PeriodSummary();
        periodSummary.setPeriod(periodType + " (" + startDate + " 至 " + endDate + ")");
        periodSummary.setSummary(aiSummary);

        return periodSummaryRepository.save(periodSummary);
    }
}