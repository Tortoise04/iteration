package com.smartschedule.service;

import com.smartschedule.entity.PointsRecord;
import com.smartschedule.entity.User;
import com.smartschedule.repository.PointsRecordRepository;
import com.smartschedule.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsService {

    private final PointsRecordRepository pointsRepo;
    private final UserRepository userRepo;

    public int calculateTaskPoints(String category, int durationMinutes) {
        double rate;
        switch (category != null ? category : "OTHER") {
            case "STUDY": rate = 0.15; break;
            case "WORK": rate = 0.12; break;
            case "EXERCISE": rate = 0.20; break;
            case "HOBBY": rate = 0.10; break;
            default: rate = 0.08; break;
        }
        return (int) Math.ceil(rate * durationMinutes);
    }

    @Transactional
    public void awardPoints(Long userId, Long taskId, int points, String reason) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setTotalPoints(user.getTotalPoints() + points);

        int newLevel = (user.getTotalPoints() / 200) + 1;
        user.setLevel(Math.min(newLevel, 10));
        userRepo.save(user);

        pointsRepo.save(PointsRecord.builder()
                .user(user).taskId(taskId).points(points).reason(reason).build());

        log.info("Points awarded: user={}, points=+{}, total={}, level={}",
                userId, points, user.getTotalPoints(), user.getLevel());
    }

    public List<PointsRecord> getUserPointsHistory(Long userId) {
        return pointsRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
