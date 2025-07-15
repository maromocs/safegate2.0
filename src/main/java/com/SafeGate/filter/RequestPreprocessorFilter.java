package com.SafeGate.filter;

import jakarta.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
@Order(1) // Run this filter first
public class RequestPreprocessorFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestPreprocessorFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Log the DECODED payload for better readability
        String normalizedPayload = normalizeRequest(httpRequest);
        logger.info("Normalized payload: {}", normalizedPayload);

        chain.doFilter(request, response);
    }

    private String normalizeRequest(HttpServletRequest request) {
        StringBuilder payload = new StringBuilder();

        payload.append("METHOD=").append(request.getMethod());
        payload.append(" PATH=").append(request.getRequestURI());

        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            // Decode the query string to make logs readable
            String decodedQuery = URLDecoder.decode(queryString, StandardCharsets.UTF_8);
            payload.append(" QUERY=").append(decodedQuery);
        }

        payload.append(" HEADERS=");
        var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            payload.append(headerName).append("=").append(request.getHeader(headerName)).append(";");
        }

        return payload.toString();
    }
}