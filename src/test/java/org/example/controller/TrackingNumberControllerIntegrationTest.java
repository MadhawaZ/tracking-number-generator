package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TrackingNumberControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Pattern TRACKING_NUMBER_PATTERN = Pattern.compile("^[A-Z0-9]{1,16}$");

    @Test
    @WithMockUser
    void getNextTrackingNumber_WithValidParams_ShouldReturnTrackingNumber() throws Exception {
        MvcResult result = mockMvc.perform(get("/next-tracking-number")
                .param("origin_country_id", "MY")
                .param("destination_country_id", "ID")
                .param("weight", "1.234")
                .param("customer_id", "de619854-b59b-425e-9db4-943979e1bd49"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.tracking_number").exists())
                .andExpect(jsonPath("$.created_at").exists())
                .andExpect(jsonPath("$.origin_country_id").value("MY"))
                .andExpect(jsonPath("$.destination_country_id").value("ID"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
        
        String trackingNumber = (String) response.get("tracking_number");
        assertNotNull(trackingNumber);
        assertTrue(TRACKING_NUMBER_PATTERN.matcher(trackingNumber).matches());
        assertEquals(16, trackingNumber.length());
    }

    @Test
    @WithMockUser
    void getNextTrackingNumber_WithInvalidCountryCode_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/next-tracking-number")
                .param("origin_country_id", "INVALID")
                .param("destination_country_id", "ID")
                .param("weight", "1.234")
                .param("customer_id", "de619854-b59b-425e-9db4-943979e1bd49"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getNextTrackingNumber_WithInvalidWeight_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/next-tracking-number")
                .param("origin_country_id", "MY")
                .param("destination_country_id", "ID")
                .param("weight", "-1.0")
                .param("customer_id", "de619854-b59b-425e-9db4-943979e1bd49"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getNextTrackingNumber_WithInvalidCustomerId_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/next-tracking-number")
                .param("origin_country_id", "MY")
                .param("destination_country_id", "ID")
                .param("weight", "1.234")
                .param("customer_id", "invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getNextTrackingNumber_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/next-tracking-number")
                .param("origin_country_id", "MY")
                .param("destination_country_id", "ID")
                .param("weight", "1.234")
                .param("customer_id", "de619854-b59b-425e-9db4-943979e1bd49"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getNextTrackingNumber_WithMissingParams_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/next-tracking-number")
                .param("origin_country_id", "MY"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getNextTrackingNumber_MultipleRequests_ShouldGenerateUniqueNumbers() throws Exception {
        String trackingNumber1 = getTrackingNumberFromResponse(
            mockMvc.perform(get("/next-tracking-number")
                .param("origin_country_id", "MY")
                .param("destination_country_id", "ID")
                .param("weight", "1.234")
                .param("customer_id", "de619854-b59b-425e-9db4-943979e1bd49"))
                .andExpect(status().isOk())
                .andReturn()
        );

        String trackingNumber2 = getTrackingNumberFromResponse(
            mockMvc.perform(get("/next-tracking-number")
                .param("origin_country_id", "MY")
                .param("destination_country_id", "ID")
                .param("weight", "1.234")
                .param("customer_id", "de619854-b59b-425e-9db4-943979e1bd49"))
                .andExpect(status().isOk())
                .andReturn()
        );

        assertNotEquals(trackingNumber1, trackingNumber2, "Consecutive requests should generate different tracking numbers");
    }

    private String getTrackingNumberFromResponse(MvcResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
        return (String) response.get("tracking_number");
    }
}
