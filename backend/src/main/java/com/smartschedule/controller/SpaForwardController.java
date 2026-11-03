package com.smartschedule.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * SPA 回退：不含点号的前端路由统一返回 index.html（契约 §6）。
 * static/index.html 尚未打包时不返回 500，而是给一段可读的提示页。
 */
@Controller
public class SpaForwardController {

    private static final String INDEX_LOCATION = "static/index.html";

    private static final String MISSING_PAGE =
            "<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">"
            + "<title>SmartSchedule</title></head>"
            + "<body style=\"font-family:system-ui,-apple-system,Segoe UI,sans-serif;padding:48px;line-height:1.8\">"
            + "<h2>前端资源尚未打包</h2>"
            + "<p>先执行 <code>npm run build</code>，把 <code>frontend/dist/*</code> 复制到 "
            + "<code>backend/src/main/resources/static/</code>，再 <code>mvn -DskipTests package</code>。</p>"
            + "<p>后端接口正常：<code>GET /api/health</code>。</p>"
            + "</body></html>";

    @GetMapping({
            "/", "/index.html",
            "/today", "/taskbooks", "/taskbooks/**",
            "/dashboard", "/goals", "/courses", "/ai-chat", "/points",
            "/login", "/register"
    })
    public void index(HttpServletResponse response) throws IOException {
        ClassPathResource resource = new ClassPathResource(INDEX_LOCATION);
        response.setContentType("text/html;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        if (!resource.exists()) {
            response.getOutputStream().write(MISSING_PAGE.getBytes(StandardCharsets.UTF_8));
            return;
        }
        InputStream in = resource.getInputStream();
        try {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, read);
            }
        } finally {
            in.close();
        }
    }
}
