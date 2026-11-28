package com.cbooking.campusbooking.auth.controller;

import com.cbooking.campusbooking.auth.pojo.dto.LoginRequest;
import com.cbooking.campusbooking.auth.pojo.dto.RegisterRequest;
import com.cbooking.campusbooking.auth.pojo.vo.LoginResponse;
import com.cbooking.campusbooking.auth.pojo.vo.UserVO;
import com.cbooking.campusbooking.auth.service.AuthService;
import com.cbooking.campusbooking.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    AuthService authService;
    // 登录
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        return ApiResponse.success(authService.login(loginRequest));
    }
    //注册
    @PostMapping("/register")
    public ApiResponse<UserVO> register(@RequestBody RegisterRequest registerRequest) {
        return ApiResponse.success(authService.register(registerRequest));
    }
}
