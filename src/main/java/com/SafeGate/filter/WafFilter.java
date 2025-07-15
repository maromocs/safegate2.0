package com.SafeGate.filter;

import com.SafeGate.entity.BlockedRequest;
import com.SafeGate.model.SignatureRule;
import com.SafeGate.repository.BlockedRequestRepository;
import com.SafeGate.service.SignatureRulesEngine;
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
import java.util.Optional;

@Component
@Order(2)
public class WafFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(WafFilter.class);

    @Autowired
    private SignatureRulesEngine signatureRulesEngine;

    @Autowired
    private BlockedRequestRepository blockedRequestRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip filtering for API endpoints
        if (httpRequest.getRequestURI().startsWith("/api/logs") ||
            httpRequest.getRequestURI().startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }

        String normalizedPayload = normalizeRequest(httpRequest);
        Optional<SignatureRule> matchingRule = signatureRulesEngine.getMatchingRule(normalizedPayload);

        if (matchingRule.isPresent()) {
            SignatureRule rule = matchingRule.get();

            // Create and save blocked request
            BlockedRequest blockedRequest = new BlockedRequest(
                getClientIpAddress(httpRequest),
                rule.getName(),
                normalizedPayload,
                rule.getId()
            );

            blockedRequest.setRequestMethod(httpRequest.getMethod());
            blockedRequest.setRequestUri(httpRequest.getRequestURI());
            blockedRequest.setUserAgent(httpRequest.getHeader("User-Agent"));

            try {
                blockedRequestRepository.save(blockedRequest);
                logger.warn("BLOCKED & SAVED - Rule: {} | IP: {} | ID: {}",
                           rule.getName(), getClientIpAddress(httpRequest), blockedRequest.getId());
            } catch (Exception e) {
                logger.error("Failed to save blocked request", e);
            }

            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(String.format(
                "{\"error\":\"Blocked by WAF\",\"rule\":\"%s\",\"id\":%d}",
                rule.getName(), blockedRequest.getId()
            ));
            return;
        }

        chain.doFilter(request, response);
    }

    private String normalizeRequest(HttpServletRequest request) {
        StringBuilder payload = new StringBuilder();
        payload.append("METHOD=").append(request.getMethod());
        payload.append(" PATH=").append(request.getRequestURI());

        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            // Decode the query string before rule matching
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
}