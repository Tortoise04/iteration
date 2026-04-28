package com.iterate.digitalwellness.util;

import com.iterate.digitalwellness.entity.User;
import com.iterate.digitalwellness.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 安全工具类，用于获取当前登录用户的ID
 */
@Component
public class SecurityUtils {

    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);

    private static SecurityUtils instance;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @PostConstruct
    public void init() {
        instance = this;
    }

    /**
     * 获取当前登录用户的ID
     * 从请求头的Authorization中解析JWT Token，获取用户ID
     *
     * @return 当前用户ID，如果未登录则返回null
     */
    public static Long getCurrentUserId() {
        if (instance == null) {
            logger.warn("SecurityUtils未初始化");
            return null;
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            logger.warn("无法获取请求上下文");
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String username = instance.jwtUtil.getUsernameFromToken(token);
                User user = instance.userRepository.findByUsername(username);
                if (user != null) {
                    return user.getId();
                }
            } catch (Exception e) {
                logger.warn("Token解析失败: {}", e.getMessage());
            }
        }

        return null;
    }
}
