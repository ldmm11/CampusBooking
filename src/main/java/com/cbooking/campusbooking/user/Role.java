package com.cbooking.campusbooking.user;

import lombok.Data;

// 角色枚举,int->String转换
public enum Role {
    STUDENT(0),
    SUPER_ADMIN(1),
    VENUE_ADMIN(2),
    GUEST(3);
    private final int code;
    Role(int code){
        this.code=code;
    }
    public int getCode(){
        return code;
    }

    public static Role fromCode(int code){
        if(code==0){
            return STUDENT;//默认学生
        }
        for(Role role : Role.values()){
            if(role.getCode()==code){
                return role;
            }
        }
        return null;
    }
}
