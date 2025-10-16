package com.SafeGate.filter;

import com.SafeGate.entity.BlockedRequest;
import com.SafeGate.enums.LLMMode;
import com.SafeGate.model.LLMConfig;
import com.SafeGate.model.SignatureRule;
import com.SafeGate.repository.BlockedRequestRepository;
import com.SafeGate.service.LLMService;
import com.SafeGate.service.SignatureRulesEngine;
import com.SafeGate.service.WafTestModeService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Component
@Order(2)
public class WafFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(WafFilter.class);

    @Autowired
    private SignatureRulesEngine signatureRulesEngine;

    @Autowired
    private BlockedRequestRepository blockedRequestRepository;

    @Autowired
    private WafTestModeService testModeService;
    
    @Autowired
    private LLMService llmService;

    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api/logs",
            "/api/rules",
            "/api/tests",
            "/api/llm",
            "/actuator"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Short-circuit CORS preflight
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Exclude static resources and known API exclusions
        String uri = httpRequest.getRequestURI();
        if (isExcluded(uri) || isStaticResource(uri)) {
            chain.doFilter(request, response);
            return;
        }

        String normalizedPayload = normalizeRequest(httpRequest);
        Optional<SignatureRule> matchingRule = signatureRulesEngine.getMatchingRule(normalizedPayload);

        if (matchingRule.isPresent()) {
            // Request is BLOCKED
            SignatureRule rule = matchingRule.get();
            long blockedRequestId = -1; // Default value, used when not saving

            if (testModeService.isTestModeEnabled()) {
                testModeService.recordBlockedRequest(rule.getName());
                logger.warn("BLOCKED (TEST MODE) - Rule: {} | IP: {}", rule.getName(), getClientIpAddress(httpRequest));
            } else {
                BlockedRequest blockedRequest = new BlockedRequest(
                    getClientIpAddress(httpRequest),
                    rule.getName(),
                    normalizedPayload,
                    rule.getRuleId()
                );
                blockedRequest.setRequestMethod(httpRequest.getMethod());
                blockedRequest.setRequestUri(httpRequest.getRequestURI());
                blockedRequest.setUserAgent(httpRequest.getHeader("User-Agent"));

                try {
                    BlockedRequest savedRequest = blockedRequestRepository.save(blockedRequest);
                    blockedRequestId = savedRequest.getId();
                    logger.warn("BLOCKED & SAVED - Rule: {} | IP: {} | ID: {}",
                               rule.getName(), getClientIpAddress(httpRequest), blockedRequestId);
                } catch (Exception e) {
                    logger.error("Failed to save blocked request", e);
                }
            }

            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(String.format(
                "{\"error\":\"Blocked by WAF\",\"rule\":\"%s\",\"id\":%d}",
                rule.getName(), blockedRequestId
            ));
            return; // End the filter chain here
        }

        // If request is not blocked by rules, check with LLM
        // During dataset tests, avoid invoking LLM on the test harness path to allow batch processing later
        if (testModeService.isTestModeEnabled() && httpRequest.getRequestURI().startsWith("/api/test/test-harness")) {
            if (testModeService.isTestModeEnabled()) {
                testModeService.recordPassedRequest();
            }
            chain.doFilter(request, response);
            return;
        }

        LLMMode llmMode = llmService.getConfig().map(LLMConfig::getLlmMode).orElse(LLMMode.DISABLED);
        boolean llmCheckActive = (testModeService.isTestModeEnabled() && (llmMode == LLMMode.TEST_ONLY || llmMode == LLMMode.NORMAL_AND_TEST)) ||
                               (!testModeService.isTestModeEnabled() && (llmMode == LLMMode.NORMAL_ONLY || llmMode == LLMMode.NORMAL_AND_TEST));

        if (llmCheckActive) {
            Map llmRes = llmService.analyzeSingle(normalizedPayload);
            Object malObj = llmRes.get("is_malicious");
            boolean mal = (malObj instanceof Boolean) ? (Boolean) malObj : false;
            String category = String.valueOf(llmRes.getOrDefault("category", "OTHER"));
            String reason = String.valueOf(llmRes.getOrDefault("reason", ""));
            if (mal) {
                // LLM identified as malicious
                long blockedRequestId = -1;
                String ruleName = "LLM:" + category;
                String ruleId = "LLM-" + category;
                
                if (testModeService.isTestModeEnabled()) {
                    testModeService.recordBlockedRequest(ruleName);
                    logger.warn("BLOCKED (TEST MODE) - Rule: {} | IP: {} | Reason: {}", ruleName, getClientIpAddress(httpRequest), reason);
                } else {
                    BlockedRequest blockedRequest = new BlockedRequest(
                        getClientIpAddress(httpRequest),
                        ruleName + (reason.isEmpty() ? "" : " - " + reason),
                        normalizedPayload,
                        ruleId
                    );
                    blockedRequest.setRequestMethod(httpRequest.getMethod());
                    blockedRequest.setRequestUri(httpRequest.getRequestURI());
                    blockedRequest.setUserAgent(httpRequest.getHeader("User-Agent"));
                    
                    try {
                        BlockedRequest savedRequest = blockedRequestRepository.save(blockedRequest);
                        blockedRequestId = savedRequest.getId();
                        logger.warn("BLOCKED & SAVED - Rule: {} | IP: {} | ID: {}", 
                                   ruleName, getClientIpAddress(httpRequest), blockedRequestId);
                    } catch (Exception e) {
                        logger.error("Failed to save blocked request", e);
                    }
                }
                
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write(String.format(
                    "{\"error\":\"Blocked by WAF\",\"rule\":\"%s\",\"category\":\"%s\",\"reason\":\"%s\",\"id\":%d}",
                    "LLM", category, escapeJson(reason), blockedRequestId
                ));
                return; // End the filter chain here
            }
        }

        // Request is PASSED
        if (testModeService.isTestModeEnabled()) {
            testModeService.recordPassedRequest();
        }
        chain.doFilter(request, response);
    }
    
    private boolean isExcluded(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean isStaticResource(String path) {
        if (path == null) return false;
        // Exact static paths
        if ("/".equals(path) || "/index.html".equals(path) || "/favicon.ico".equals(path)) {
            return true;
        }
        // Treat any non-API HTML page as static (e.g., /logs.html, /llm.html, /rules.html, /testing.html)
        if (!path.startsWith("/api/") && path.endsWith(".html")) {
            return true;
        }
        // Common static prefixes
        String[] prefixes = new String[] { "/static", "/assets", "/css", "/js", "/images", "/img", "/fonts" };
        for (String p : prefixes) {
            if (path.startsWith(p)) return true;
        }
        return false;
    }

    private String normalizeRequest(HttpServletRequest request) {
        StringBuilder payload = new StringBuilder();
        payload.append("METHOD=").append(request.getMethod());
        payload.append(" PATH=").append(request.getRequestURI());

        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            String decodedQuery = URLDecoder.decode(queryString, StandardCharsets.UTF_8);
            payload.append(" QUERY=").append(decodedQuery);
        }

        return payload.toString();
    }
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}