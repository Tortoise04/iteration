package com.smartschedule.service;

import com.smartschedule.config.JwtUtil;
import com.smartschedule.dto.LoginRequest;
import com.smartschedule.dto.LoginResponse;
import com.smartschedule.dto.RegisterRequest;
import com.smartschedule.entity.User;
import com.smartschedule.exception.BusinessException;
import com.smartschedule.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试 — 注册、登录、获取当前用户。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    // ========== 注册 ==========

    @Test
    void testRegister_Success() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("123456");
        req.setNickname("新人");

        when(userRepo.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hashed_pw");
        when(userRepo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);  // 模拟数据库生成 ID
            return u;
        });
        when(jwtUtil.generateToken(eq(1L), eq("newuser"))).thenReturn("mock_jwt_token");

        LoginResponse resp = userService.register(req);

        assertNotNull(resp);
        assertEquals("newuser", resp.getUsername());
        assertEquals("mock_jwt_token", resp.getToken());
        assertEquals("新人", resp.getNickname());
        verify(userRepo).save(any(User.class));
    }

    @Test
    void testRegister_DuplicateUsername() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("exist");
        req.setPassword("123456");

        when(userRepo.existsByUsername("exist")).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.register(req));
        verify(userRepo, never()).save(any());
    }

    // ========== 登录 ==========

    @Test
    void testLogin_Success() {
        LoginRequest req = new LoginRequest();
        req.setUsername("user1");
        req.setPassword("correct");

        User user = User.builder()
                .id(1L).username("user1").password("hashed_pw")
                .nickname("小明").build();

        when(userRepo.findByUsername("user1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "hashed_pw")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "user1")).thenReturn("mock_jwt_token");

        LoginResponse resp = userService.login(req);

        assertNotNull(resp);
        assertEquals("user1", resp.getUsername());
        assertEquals("mock_jwt_token", resp.getToken());
    }

    @Test
    void testLogin_WrongPassword() {
        LoginRequest req = new LoginRequest();
        req.setUsername("user1");
        req.setPassword("wrong");

        User user = User.builder()
                .id(1L).username("user1").password("hashed_pw").build();

        when(userRepo.findByUsername("user1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed_pw")).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.login(req));
    }

    @Test
    void testLogin_UserNotFound() {
        LoginRequest req = new LoginRequest();
        req.setUsername("ghost");
        req.setPassword("any");

        when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> userService.login(req));
    }

    // ========== 获取当前用户 ==========

    @Test
    void testGetCurrentUser_Success() {
        User user = User.builder().id(1L).username("user1").build();
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getCurrentUser(1L);
        assertEquals("user1", result.getUsername());
    }

    @Test
    void testGetCurrentUser_NotFound() {
        when(userRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.getCurrentUser(999L));
    }
}
