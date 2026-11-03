package com.smartschedule.config;

import com.smartschedule.entity.User;
import com.smartschedule.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * 应用启动时初始化种子数据。
 * 确保 demo 用户始终存在且密码正确，不受 schema.sql 配置影响。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        java.util.Optional<User> existing = userRepo.findByUsername("demo");
        if (existing.isPresent()) {
            // 用户已存在：强制重置密码，确保与 BCryptPasswordEncoder 一致
            User user = existing.get();
            user.setPassword(passwordEncoder.encode("demo123"));
            user.setNickname("演示用户");
            user.setWakeUpTime(LocalTime.of(8, 0));
            user.setSleepTime(LocalTime.of(23, 0));
            user.setEnergyPeak("EVENING");
            user.setEnabled(true);
            userRepo.save(user);
            log.info("Demo user password reset: demo / demo123");
        } else {
            // 用户不存在：全新创建
            User demo = User.builder()
                    .username("demo")
                    .password(passwordEncoder.encode("demo123"))
                    .nickname("演示用户")
                    .wakeUpTime(LocalTime.of(8, 0))
                    .sleepTime(LocalTime.of(23, 0))
                    .energyPeak("EVENING")
                    .totalPoints(0)
                    .level(1)
                    .enabled(true)
                    .build();
            userRepo.save(demo);
            log.info("Demo user created: demo / demo123");
        }
    }
}
