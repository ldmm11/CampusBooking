package com.cbooking.campusbooking.auth.controller;

import com.cbooking.campusbooking.auth.AuthConfiguration;
import com.cbooking.campusbooking.auth.AuthUser;
import com.cbooking.campusbooking.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {
    @GetMapping("/api/v1/me")
    public ApiResponse<AuthUser> me(HttpServletRequest request) {
        return ApiResponse.success((AuthUser) request.getAttribute(AuthConfiguration.CURRENT_USER));
    }
}
