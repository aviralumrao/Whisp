package com.example.whisp.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
    }

    @Test
    void testHandleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Message not found with id = 123");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFoundException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("Message not found with id = 123", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testHandleUnauthorizedException() {
        UnauthorizedException ex = new UnauthorizedException("Unauthorized: You can only edit your own messages");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorizedException(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Forbidden", response.getBody().getError());
        assertEquals("Unauthorized: You can only edit your own messages", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    void testHandleBadRequestException() {
        BadRequestException ex = new BadRequestException("Cannot edit message: The 1-minute edit window has expired");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadRequestException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertEquals("Cannot edit message: The 1-minute edit window has expired", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    void testHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input parameter");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Invalid input parameter", response.getBody().getMessage());
    }

    @Test
    void testHandleGlobalException() {
        Exception ex = new RuntimeException("Database connection error");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGlobalException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    void testHandleWsExceptions() {
        ResourceNotFoundException notFound = new ResourceNotFoundException("Not found");
        WsErrorResponse wsNotFound = exceptionHandler.handleWsResourceNotFoundException(notFound);
        assertEquals("NOT_FOUND", wsNotFound.getError());
        assertEquals("Not found", wsNotFound.getMessage());

        UnauthorizedException unauthorized = new UnauthorizedException("Unauthorized");
        WsErrorResponse wsUnauthorized = exceptionHandler.handleWsUnauthorizedException(unauthorized);
        assertEquals("UNAUTHORIZED", wsUnauthorized.getError());
        assertEquals("Unauthorized", wsUnauthorized.getMessage());

        BadRequestException badRequest = new BadRequestException("Bad request");
        WsErrorResponse wsBadRequest = exceptionHandler.handleWsBadRequestException(badRequest);
        assertEquals("BAD_REQUEST", wsBadRequest.getError());
        assertEquals("Bad request", wsBadRequest.getMessage());

        Exception generalEx = new RuntimeException("General error");
        WsErrorResponse wsGeneral = exceptionHandler.handleWsGeneralException(generalEx);
        assertEquals("INTERNAL_ERROR", wsGeneral.getError());
        assertEquals("General error", wsGeneral.getMessage());
    }
}
