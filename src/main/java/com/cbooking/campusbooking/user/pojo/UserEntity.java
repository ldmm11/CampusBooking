package com.cbooking.campusbooking.user.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class UserEntity {
    private Long id;
    private String name;
    private String password;
    private String nickname;
    private Integer role;
    private LocalDateTime bannedUntil;
    private LocalDateTime CreateAt;
    private LocalDateTime UpdateAt;
}
