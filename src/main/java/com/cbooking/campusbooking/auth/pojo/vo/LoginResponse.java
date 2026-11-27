package com.cbooking.campusbooking.auth.pojo.vo;

public record LoginResponse(String accessToken,String tokenType,long expiresIn,UserVO user) {
}
