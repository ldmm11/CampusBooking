package com.cbooking.campusbooking.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(CommonWebTest.Probe.class)
class CommonWebTest {
    @Autowired MockMvc mvc;
    @Autowired Clock clock;

    @Test
    void successUsesServerTraceAndPreservesNullData() throws Exception {
        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Shanghai"));
        var first = mvc.perform(get("/probe").header("X-Trace-Id", "client"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("成功"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.traceId").value(matchesPattern("[a-f0-9]{32}")))
                .andReturn();
        var second = mvc.perform(get("/probe")).andReturn();
        assertThat(first.getRequest().getAttribute("traceId"))
                .isNotEqualTo(second.getRequest().getAttribute("traceId"));
        assertThat(org.slf4j.MDC.get("traceId")).isNull();
    }

    @Test
    void businessErrorUsesItsHttpStatus() throws Exception {
        mvc.perform(get("/probe/banned")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(3005))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void validationReturnsFieldDetails() throws Exception {
        mvc.perform(post("/probe").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.data.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.data.fieldErrors[0].message").value("不能为空"));
    }

    @Test
    void invalidJsonAndUnknownPropertiesAreRejected() throws Exception {
        for (String body : new String[]{"{", "{\"name\":\"A\",\"role\":\"ADMIN\"}"}) {
            mvc.perform(post("/probe").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(1001));
        }
    }

    @Test
    void missingResourceUsesContractCode() throws Exception {
        mvc.perform(get("/missing-path")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    void internalFailureReturnsOnlyPublicMessage() throws Exception {
        mvc.perform(get("/probe/failure")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(5000))
                .andExpect(jsonPath("$.message").value("服务端异常"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @TestComponent
    @RestController
    @RequestMapping("/probe")
    static class Probe {
        record Input(@NotBlank(message = "不能为空") String name) {}

        @GetMapping
        ApiResponse<Void> success() { return ApiResponse.success(null); }

        @PostMapping
        ApiResponse<Input> echo(@Valid @RequestBody Input input) { return ApiResponse.success(input); }

        @GetMapping("/banned")
        void banned() { throw new BusinessException(ErrorCode.BOOKING_BANNED); }

        @GetMapping("/failure")
        void fail() { throw new IllegalStateException("internal detail"); }
    }
}
