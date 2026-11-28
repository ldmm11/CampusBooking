package com.cbooking.campusbooking.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cbooking.campusbooking.auth.pojo.dto.LoginRequest;
import com.cbooking.campusbooking.auth.pojo.dto.RegisterRequest;
import com.cbooking.campusbooking.auth.pojo.vo.LoginResponse;
import com.cbooking.campusbooking.auth.pojo.vo.UserVO;
import com.cbooking.campusbooking.common.BusinessException;
import com.cbooking.campusbooking.common.ErrorCode;
import com.cbooking.campusbooking.user.Role;
import com.cbooking.campusbooking.user.mapper.UserMapper;
import com.cbooking.campusbooking.user.pojo.UserEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 注册与登录的业务逻辑。
 *
 * 注册：格式校验 -> 查重 -> BCrypt 加密 -> 插库 -> 返回 UserVO（不发 token）
 * 登录：查用户 -> 验密码 -> jwt.issue(userId) 签发 token -> 返回 LoginResponse
 *
 * 安全规则：
 *   1. "用户不存在"和"密码错误"抛同一个 UNAUTHENTICATED，不给攻击者探测用户名的机会；
 *   2. 密码只存 BCrypt 哈希，明文和 token 都不进日志。
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserMapper userMapper, PasswordEncoder encoder, JwtService jwt) {
        this.userMapper = userMapper;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /** 注册：成功返回用户视图，不发 token（api.md：注册成功后仍需登录）。 */
    public UserVO register(RegisterRequest req) {
        validateName(req.name());
        validatePassword(req.password());

        // nickname 可省略，默认"某某"（api.md 5.1）
        String nickname = (req.nickname() == null || req.nickname().isBlank())
                ? "某某" : req.nickname();
        if (nickname.length() > 16) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }

        // 查重：t_user 上有 uk_name 唯一索引，这里先查一次是为了返回友好错误码
        UserEntity exists = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getName, req.name()));
        if (exists != null) {
            throw new BusinessException(ErrorCode.ACCOUNT_EXISTS);
        }

        UserEntity user = new UserEntity();
        user.setName(req.name());
        user.setPassword(encoder.encode(req.password()));   // BCrypt 加密，永不存明文
        user.setNickname(nickname);
        user.setRole(Role.STUDENT.getCode());               // 注册用户默认学生
        user.setCreateAt(LocalDateTime.now());
        user.setUpdateAt(LocalDateTime.now());
        userMapper.insert(user);                            // 自增 id 由 MyBatis-Plus 回填到 user 对象
        return toVO(user);
    }

    /** 登录：验密码通过后签发 JWT。这是整个项目唯一的 JWT 签发点。 */
    public LoginResponse login(LoginRequest req) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getName, req.name()));

        // 用户不存在 / 密码错误 -> 同一个错误码（安全规则 1）
        if (user == null || !encoder.matches(req.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }

        // 【JWT 连接点】拿数据库里的 userId 签发通行证
        String accessToken = jwt.issue(user.getId());

        return new LoginResponse(accessToken, "Bearer", jwt.expiresIn(), toVO(user));
    }

    /** Entity -> VO：去密码、role int 转枚举名。 */
    private UserVO toVO(UserEntity user) {
        Role role = Role.fromCode(user.getRole());
        return new UserVO(
                user.getId().intValue(),
                user.getName(),
                user.getNickname(),
                role == null ? Role.STUDENT.name() : role.name());
    }

    /** name：3~16 位 ASCII 字母、数字、下划线（api.md 5.1）。 */
    private void validateName(String name) {
        if (name == null || !name.matches("^[A-Za-z0-9_]{3,16}$")) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
    }

    /** password：8~64 字符（api.md 5.1）。 */
    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 64) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        }
    }
}
