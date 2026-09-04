package com.travelsky.base.security.handler;

import com.travelsky.base.security.context.JwtTokenBlackList;
import com.travelsky.base.security.helper.JwtTokenHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class InvalidJwtTokenLogoutHandler implements LogoutHandler {

    @Autowired
    private JwtTokenHelper jwtTokenHelper;

    @Autowired
    private JwtTokenBlackList jwtTokenBlackList;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        jwtTokenBlackList.add(request.getHeader("Authorization"), jwtTokenHelper.getExpirationTime());
    }
}
