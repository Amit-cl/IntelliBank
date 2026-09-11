package com.intellibank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellibank.dto.AiChatRequest;
import com.intellibank.dto.AiChatResponse;
import com.intellibank.dto.AiSpendingInsightsResponse;
import com.intellibank.service.AiService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiService aiService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "rahul", roles = {"CUSTOMER"})
    @DisplayName("Should return 200 OK and AI chat reply")
    void testChat_Success() throws Exception {
        AiChatRequest request = new AiChatRequest("How much did I spend?", "1234567890");
        AiChatResponse response = new AiChatResponse("You spent ₹3,000 this month.", "llama-3.3-70b-versatile", LocalDateTime.now());

        when(aiService.chatWithAssistant(eq("rahul"), any(AiChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("You spent ₹3,000 this month."))
                .andExpect(jsonPath("$.modelUsed").value("llama-3.3-70b-versatile"));
    }

    @Test
    @WithMockUser(username = "rahul", roles = {"CUSTOMER"})
    @DisplayName("Should return 200 OK and spending insights")
    void testGetInsights_Success() throws Exception {
        AiSpendingInsightsResponse response = new AiSpendingInsightsResponse(
                "1234567890",
                "SAVINGS",
                new BigDecimal("50000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("2000.00"),
                new BigDecimal("8000.00"),
                "Dining & Food",
                "1) ₹2,000 spent on food. 2) Save 10% more.",
                List.of("[LOGIN] Successful login")
        );

        when(aiService.generateSpendingInsights(eq("rahul"), eq("1234567890"))).thenReturn(response);

        mockMvc.perform(get("/api/ai/insights/1234567890")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.currentBalance").value(50000.00))
                .andExpect(jsonPath("$.topSpendingCategory").value("Dining & Food"));
    }
}
