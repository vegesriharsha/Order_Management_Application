package com.example.eventdriven.filter;

import com.example.eventdriven.logging.LoggingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Filter for logging HTTP requests and responses
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final LoggingService loggingService;

    public RequestLoggingFilter(LoggingService loggingService) {
        this.loggingService = loggingService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Create a unique ID for the request
        String requestId = UUID.randomUUID().toString();

        // Wrap request and response for multiple reads
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        // Add request ID to MDC for logging
        MDC.put("requestId", requestId);

        long startTime = System.currentTimeMillis();

        try {
            // Log request details
            logRequest(requestWrapper, requestId);

            // Continue with the request
            filterChain.doFilter(requestWrapper, responseWrapper);

            // Calculate request duration
            long duration = System.currentTimeMillis() - startTime;

            // Log response details
            logResponse(responseWrapper, requestId, duration);

        } finally {
            // Copy content to the original response
            responseWrapper.copyBodyToResponse();

            // Remove request ID from MDC
            MDC.remove("requestId");
        }
    }

    /**
     * Log request details
     *
     * @param request the HTTP request
     * @param requestId the unique request ID
     */
    private void logRequest(ContentCachingRequestWrapper request, String requestId) {
        Map<String, Object> data = new HashMap<>();
        data.put("requestId", requestId);
        data.put("method", request.getMethod());
        data.put("uri", request.getRequestURI());
        data.put("queryString", request.getQueryString());
        data.put("clientIp", request.getRemoteAddr());
        data.put("userAgent", request.getHeader("User-Agent"));

        logger.info("Received HTTP request: {} {}", request.getMethod(), request.getRequestURI());
        loggingService.logInfo("HTTP Request", data);
    }

    /**
     * Log response details
     *
     * @param response the HTTP response
     * @param requestId the unique request ID
     * @param duration the request duration in milliseconds
     */
    private void logResponse(ContentCachingResponseWrapper response, String requestId, long duration) {
        Map<String, Object> data = new HashMap<>();
        data.put("requestId", requestId);
        data.put("status", response.getStatus());
        data.put("duration", duration);

        String logMessage = String.format("HTTP Response: status=%d, duration=%dms",
                response.getStatus(), duration);

        logger.info(logMessage);
        loggingService.logInfo("HTTP Response", data);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // This filter should process all requests
        return false;
    }
}
