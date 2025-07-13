package com.SafeGate.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class RequestPreprocessorFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestPreprocessorFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Normalize incoming request into a single payload string
        String normalizedPayload = normalizeRequest(httpRequest);
        
        // Log the normalized payload
        logger.info("Normalized payload: {}", normalizedPayload);
        
        chain.doFilter(request, response);
    }
    
    private String normalizeRequest(HttpServletRequest request) {
        StringBuilder payload = new StringBuilder();
        
        // Method
        payload.append("METHOD=").append(request.getMethod());
        
        // Path
        payload.append(" PATH=").append(request.getRequestURI());
        
        // Query parameters
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            payload.append(" QUERY=").append(queryString);
        }
        
        // Headers (optional - you might want to include key headers)
        payload.append(" HEADERS=");
        var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            payload.append(headerName).append("=").append(request.getHeader(headerName)).append(";");
        }
        
        return payload.toString();
    }
}