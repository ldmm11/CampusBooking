package com.cbooking.campusbooking.auth;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthWebTest.Users.class)
class AuthWebTest {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;

    @Test void meAndAuthenticationErrors() throws Exception {
        mvc.perform(get("/me")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(2001));
        mvc.perform(get("/me").header("Authorization", "Bearer broken"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(2001));
        mvc.perform(get("/me").header("Authorization", "Bearer " + jwt.issue(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.name").value("alice"));
        mvc.perform(get("/me").header("Authorization", "Bearer " + jwt.issue(2)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(2002));
        mvc.perform(get("/me").header("Authorization", "Bearer " + jwt.issue(3)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(2001));
        mvc.perform(get("/admin/users").header("Authorization", "Bearer " + jwt.issue(1)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(2002));
    }

    @Test void tamperingIsRejected() throws Exception {
        String token = jwt.issue(1);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");
        mvc.perform(get("/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(2001));
    }

    @TestConfiguration
    static class Users {
        @Bean AuthUserLookup authUserLookup() {
            return id -> switch ((int) id) {
                case 1 -> Optional.of(new AuthUser("1", "alice", "Alice", "STUDENT", "ENABLED", null,
                        OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(8))));
                case 2 -> Optional.of(new AuthUser("2", "bob", "Bob", "STUDENT", "DISABLED", null, null));
                default -> Optional.empty();
            };
        }
    }
}
