package com.quanxiaoha.framework.biz.context.filter;

import com.quanxiaoha.framework.biz.context.holer.LoginUserContextHolder;
import com.quanxiaoha.framework.common.constant.GlobalConstants;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component

public class HeaderUserId2ContextFilter extends OncePerRequestFilter {

    @Resource
    LoginUserContextHolder loginUserContexHolder;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(GlobalConstants.USER_ID);

        if(StringUtils.isBlank(userId)){
            filterChain.doFilter(request,response);
            return;
        }

        loginUserContexHolder.setUserId(userId);
        try {
            filterChain.doFilter(request,response);
        }finally {
            loginUserContexHolder.remove();
        }

    }
}

