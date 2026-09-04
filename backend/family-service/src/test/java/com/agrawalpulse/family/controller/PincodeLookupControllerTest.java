package com.agrawalpulse.family.controller;

import com.agrawalpulse.common.security.SecurityConfig;
import com.agrawalpulse.family.entity.Pincode;
import com.agrawalpulse.family.repository.PincodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Controller-slice tests, same shape as FamilyControllerTest: only the web/security layer is
// loaded, PincodeRepository is mocked (no more external call/upstream-failure case to cover now
// that this is a plain DB lookup - see V4__pincode_reference_data.sql).
@WebMvcTest(PincodeLookupController.class)
@Import({SecurityConfig.class, PincodeLookupControllerTest.JwtDecoderTestConfig.class})
class PincodeLookupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PincodeRepository pincodeRepository;

    @TestConfiguration
    static class JwtDecoderTestConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new UnsupportedOperationException("not used - tests inject auth via jwt() post-processor");
            };
        }
    }

    @Test
    void lookup_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/families/pincode/452001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lookup_authenticatedWithNoSpecificPermission_stillSucceeds() throws Exception {
        // No @PreAuthorize on this endpoint by design - any authenticated user may look up a PIN
        // code, same bar as ChapterController's list/get.
        when(pincodeRepository.findById("452001"))
                .thenReturn(Optional.of(new Pincode("452001", "Indore", "Madhya Pradesh", "India")));

        mockMvc.perform(get("/api/v1/families/pincode/452001").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.district").value("Indore"))
                .andExpect(jsonPath("$.state").value("Madhya Pradesh"))
                .andExpect(jsonPath("$.country").value("India"));
    }

    @Test
    void lookup_rejectsAMalformedPincode_with400() throws Exception {
        mockMvc.perform(get("/api/v1/families/pincode/12AB56").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lookup_returns404_whenNoMatchingRow() throws Exception {
        when(pincodeRepository.findById("000000")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/families/pincode/000000").with(jwt()))
                .andExpect(status().isNotFound());
    }
}
