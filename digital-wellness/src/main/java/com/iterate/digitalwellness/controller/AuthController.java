package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.entity.User;
import com.iterate.digitalwellness.repository.UserRepository;
import com.iterate.digitalwellness.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        return userRepository.save(user);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody User user) {
        User existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser != null) {
            // 修复：暂时使用明文比对（测试阶段），后续可改为密码加密
            if (user.getPassword().equals(existingUser.getPassword())) {
                // 生成 JWT Token
                String token = jwtUtil.generateToken(existingUser.getUsername(), existingUser.getRole());
                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                response.put("username", existingUser.getUsername());
                response.put("role", existingUser.getRole());
                return ResponseEntity.ok(response);
            }
        }
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Login failed");
        return ResponseEntity.badRequest().body(errorResponse);
    }
}