package com.markethub.orders.web;

import com.markethub.orders.domain.exception.BadRequestException;
import com.markethub.orders.domain.exception.ConflictException;
import com.markethub.orders.domain.exception.NotFoundException;
import com.markethub.orders.domain.exception.PaymentDeclinedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), List.of());
    }

    @ExceptionHandler(ConflictException.class)      // also catches IllegalTransitionException
    public ResponseEntity<ApiError> conflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), List.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> concurrentUpdate(OptimisticLockingFailureException ex) {
        return build(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
            "The order was changed by another request. Please retry.", List.of());
    }

    @ExceptionHandler(PaymentDeclinedException.class)
    public ResponseEntity<ApiError> declined(PaymentDeclinedException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_DECLINED", ex.getMessage(), List.of());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> badRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException ex) {
        List<ApiError.FieldIssue> issues = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> new ApiError.FieldIssue(e.getField(), e.getDefaultMessage())).toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", issues);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class,
        MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> malformed(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request could not be understood", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> other(Exception ex) {
        if (ex instanceof ErrorResponse spring) {          // e.g. unknown URL (404), wrong method (405)
            HttpStatus status = HttpStatus.valueOf(spring.getStatusCode().value());
            return build(status, "REQUEST_REJECTED", status.getReasonPhrase(), List.of());
        }
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error", List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message,
                                           List<ApiError.FieldIssue> fieldErrors) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        return ResponseEntity.status(status).body(new ApiError(code, message, traceId, clock.instant(), fieldErrors));
    }
}
