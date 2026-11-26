package com.cbooking.campusbooking.auth;

import com.cbooking.campusbooking.common.BusinessException;
import com.cbooking.campusbooking.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AuthConfiguration implements WebMvcConfigurer {
    public static final String CURRENT_USER = AuthConfiguration.class.getName() + ".user";
    private final JwtService jwt;
    private final ObjectProvider<AuthUserLookup> users;
    private final ObjectProvider<HandlerExceptionResolver> errors;

    public AuthConfiguration(JwtService jwt, ObjectProvider<AuthUserLookup> users,
                             @Qualifier("handlerExceptionResolver") ObjectProvider<HandlerExceptionResolver> errors) {
        this.jwt = jwt;
        this.users = users;
        this.errors = errors;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                try {
                    String authorization = request.getHeader("Authorization");
                    if (authorization == null || !authorization.matches("Bearer [^ ]+"))
                        throw new BusinessException(ErrorCode.UNAUTHENTICATED);
                    long id = jwt.verify(authorization.substring(7));
                    AuthUserLookup lookup = users.getIfAvailable();
                    AuthUser user = lookup == null ? null : lookup.findById(id).orElse(null);
                    if (user == null) throw new BusinessException(ErrorCode.UNAUTHENTICATED);
                    if (!user.enabled()) throw new BusinessException(ErrorCode.FORBIDDEN);
                    if (request.getRequestURI().startsWith("/admin/") && !"ADMIN".equals(user.role()))
                        throw new BusinessException(ErrorCode.FORBIDDEN);
                    request.setAttribute(CURRENT_USER, user);
                    return true;
                } catch (BusinessException ex) {
                    errors.getObject().resolveException(request, response, handler, ex);
                    return false;
                }
            }
        }).addPathPatterns("/me", "/me/**", "/admin", "/admin/**", "/bookings", "/bookings/**");
    }
}
