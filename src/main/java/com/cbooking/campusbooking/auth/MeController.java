package com.cbooking.campusbooking.auth;

import com.cbooking.campusbooking.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {
    @GetMapping("/me")
    public ApiResponse<AuthUser> me(HttpServletRequest request) {
        return ApiResponse.success((AuthUser) request.getAttribute(AuthConfiguration.CURRENT_USER));
    }
}
