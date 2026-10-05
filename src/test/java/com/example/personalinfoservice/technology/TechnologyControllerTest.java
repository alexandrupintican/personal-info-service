package com.example.personalinfoservice.technology;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.personalinfoservice.common.security.SecurityConfig;
import com.example.personalinfoservice.technology.dto.TechnologyResponse;

/**
 * Web-slice test covering the happy path and the 500 path (mirroring the
 * error handling the Hono placeholder used to do ad hoc), now produced by
 * the shared {@code GlobalExceptionHandler} that every future controller
 * will also go through. {@code SecurityConfig} is imported explicitly
 * because {@code @WebMvcTest} does not pick up user {@code @Configuration}
 * classes automatically — without it, Spring Security's default lockdown
 * (active because spring-boot-starter-security-oauth2-client is on the
 * classpath) would 401 every request here, unlike the real application.
 */
@WebMvcTest(TechnologyController.class)
@Import(SecurityConfig.class)
class TechnologyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TechnologyService technologyService;

    @Test
    void getTechnologies_returnsJsonArrayOfTechnologies() throws Exception {
        given(technologyService.getAllTechnologies()).willReturn(List.of(
                new TechnologyResponse("Vue.js", "stack", 5),
                new TechnologyResponse("TypeScript", "stack", 5),
                new TechnologyResponse("Docker", "in_progress", 2)
        ));

        mockMvc.perform(get("/api/v1/technologies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Vue.js"))
                .andExpect(jsonPath("$[0].category").value("stack"))
                .andExpect(jsonPath("$[0].confidence").value(5))
                .andExpect(jsonPath("$[2].name").value("Docker"))
                .andExpect(jsonPath("$[2].category").value("in_progress"));
    }

    @Test
    void getTechnologies_whenServiceThrows_returnsStandardErrorEnvelopeWith500() throws Exception {
        given(technologyService.getAllTechnologies()).willThrow(new RuntimeException("db unreachable"));

        mockMvc.perform(get("/api/v1/technologies"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/technologies"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
