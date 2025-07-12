package com.shopper.interceptor;

import com.shopper.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        String url = request.getRequestURI();
        String userInfo = getUserInfo();
        
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTRIBUTE, startTime);
        
        log.info("🔄 {} {} {} - Request started", method, url, userInfo);
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String method = request.getMethod();
        String url = request.getRequestURI();
        String userInfo = getUserInfo();
        
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
        
        int statusCode = response.getStatus();
        
        if (ex != null) {
            log.info("❌ {} {} {} - {} ({}ms) [{}]", method, url, userInfo, statusCode, duration, ex.getMessage());
        } else {
            log.info("✅ {} {} {} - {} ({}ms)", method, url, userInfo, statusCode, duration);
        }
    }

    private String getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            
            if (authentication.getPrincipal() instanceof User user) {
                return String.format("[User: %s]", user.getUsername());
            } else if (authentication.getName() != null) {
                return String.format("[User: %s]", authentication.getName());
            }
        }
        
        return "[Anonymous]";
    }
}