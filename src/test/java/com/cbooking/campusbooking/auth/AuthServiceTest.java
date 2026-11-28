package com.cbooking.campusbooking.auth;

import com.cbooking.campusbooking.auth.pojo.dto.LoginRequest;
import com.cbooking.campusbooking.auth.pojo.dto.RegisterRequest;
import com.cbooking.campusbooking.auth.pojo.vo.UserVO;
import com.cbooking.campusbooking.auth.service.AuthService;
import com.cbooking.campusbooking.auth.service.JwtService;
import com.cbooking.campusbooking.common.BusinessException;
import com.cbooking.campusbooking.common.ErrorCode;
import com.cbooking.campusbooking.user.mapper.UserMapper;
import com.cbooking.campusbooking.user.pojo.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserMapper userMapper;      // 造假对象：数据库不参与
    private JwtService jwt;             // 造假对象：token 不真的签
    private PasswordEncoder encoder;    // 真对象：BCrypt 纯算法
    private AuthService service;

    @BeforeEach
    void setUp() {                      // 每个测试方法执行前都跑一遍，保证互不污染
        userMapper = mock(UserMapper.class);
        jwt = mock(JwtService.class);
        encoder = new BCryptPasswordEncoder();
        service = new AuthService(userMapper, encoder, jwt);
    }

    @Test
    void registerSuccess() {
        when(userMapper.selectOne(any())).thenReturn(null);   // 库里没人，允许注册
        UserVO vo = service.register(new RegisterRequest("luodm", "12345678", null));



        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).insert(captor.capture());          // 抓住传给 insert 的那个对象
        UserEntity saved = captor.getValue();
        assertEquals("某某", saved.getNickname());            // 省略昵称 -> 默认值
        assertEquals(0, saved.getRole());                     // 默认学生
        assertNotEquals("12345678", saved.getPassword());     // 明文绝不落库
        assertTrue(saved.getPassword().startsWith("$2"));     // BCrypt 哈希的特征前缀
    }

    @Test
    void registerDuplicateNameRejected() {
        when(userMapper.selectOne(any())).thenReturn(new UserEntity());  // 库里已有人
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.register(new RegisterRequest("luodm", "12345678", null)));
        assertEquals(ErrorCode.ACCOUNT_EXISTS, ex.errorCode());
    }

    @Test
    void registerInvalidNameRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.register(new RegisterRequest("ab", "12345678", null)));  // 少于 3 位
        assertEquals(ErrorCode.INVALID_PARAMETER, ex.errorCode());
    }

    @Test
    void loginSuccess() {
        UserEntity user = new UserEntity();
        user.setId(42L);
        user.setName("luodm");
        user.setPassword(encoder.encode("12345678"));         // 模拟库里存的哈希
        when(userMapper.selectOne(any())).thenReturn(user);
        when(jwt.issue(42L)).thenReturn("fake-token");
        when(jwt.expiresIn()).thenReturn(7200L);

        var resp = service.login(new LoginRequest("luodm", "12345678"));

        assertEquals("fake-token", resp.accessToken());
        assertEquals("Bearer", resp.tokenType());
        assertEquals(7200L, resp.expiresIn());
    }

    @Test
    void loginWrongPasswordRejected() {
        UserEntity user = new UserEntity();
        user.setId(42L);
        user.setPassword(encoder.encode("12345678"));
        when(userMapper.selectOne(any())).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.login(new LoginRequest("luodm", "wrong-pass")));
        assertEquals(ErrorCode.UNAUTHENTICATED, ex.errorCode());
    }

    @Test
    void loginUnknownUserSameError() {
        when(userMapper.selectOne(any())).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.login(new LoginRequest("nobody", "12345678")));
        assertEquals(ErrorCode.UNAUTHENTICATED, ex.errorCode());   // 和密码错误同一个码
    }
}
