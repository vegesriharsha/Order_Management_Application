package com.example.eventdriven.filter;

import com.example.eventdriven.logging.LoggingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    @Mock
    private LoggingService loggingService;

    @Mock
    private FilterChain filterChain;

    private RequestLoggingFilter requestLoggingFilter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        requestLoggingFilter = new RequestLoggingFilter(loggingService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        // Setup request
        request.setMethod("GET");
        request.setRequestURI("/api/orders");
        request.setQueryString("status=CREATED");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "Test Agent");
    }

    @Test
    void doFilterInternal_shouldLogRequestAndResponse() throws ServletException, IOException {
        // Act
        requestLoggingFilter.doFilterInternal(request, response, filterChain);

        // Assert
        // Verify filter chain was called
        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), any(ContentCachingResponseWrapper.class));

        // Verify request logging
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> requestDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(loggingService).logInfo(eq("HTTP Request"), requestDataCaptor.capture());

        Map<String, Object> requestData = requestDataCaptor.getValue();
        assertNotNull(requestData.get("requestId"));
        assertEquals("GET", requestData.get("method"));
        assertEquals("/api/orders", requestData.get("uri"));
        assertEquals("status=CREATED", requestData.get("queryString"));
        assertEquals("127.0.0.1", requestData.get("clientIp"));
        assertEquals("Test Agent", requestData.get("userAgent"));

        // Verify response logging
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> responseDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(loggingService).logInfo(eq("HTTP Response"), responseDataCaptor.capture());

        Map<String, Object> responseData = responseDataCaptor.getValue();
        assertNotNull(responseData.get("requestId"));
        assertEquals(200, responseData.get("status"));
        assertTrue((Long) responseData.get("duration") >= 0);
    }

    @Test
    void doFilterInternal_shouldHandleExceptionInFilterChain() throws ServletException, IOException {
        // Arrange
        doThrow(new RuntimeException("Test exception"))
                .when(filterChain).doFilter(any(), any());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                requestLoggingFilter.doFilterInternal(request, response, filterChain));

        // Verify request was still logged
        verify(loggingService).logInfo(eq("HTTP Request"), any());
    }

    // Create a test-only subclass to expose the protected method
    static class TestableRequestLoggingFilter extends RequestLoggingFilter {
        public TestableRequestLoggingFilter(LoggingService loggingService) {
            super(loggingService);
        }
        
        @Override
        public boolean shouldNotFilter(HttpServletRequest request) {
            return super.shouldNotFilter(request);
        }
    }
    
    @Test
    void shouldNotFilter_shouldReturnFalse() {
        // Create a testable version that exposes the protected method
        TestableRequestLoggingFilter testableFilter = new TestableRequestLoggingFilter(loggingService);
        
        // This filter should process all requests
        assertFalse(testableFilter.shouldNotFilter(request));
    }
}
