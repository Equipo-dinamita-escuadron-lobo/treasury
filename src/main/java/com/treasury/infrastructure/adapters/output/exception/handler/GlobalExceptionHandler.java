package com.treasury.infrastructure.adapters.output.exception.handler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.treasury.infrastructure.adapters.output.exception.dto.ErrorResponseDto;

import jakarta.persistence.EntityNotFoundException;

/**
 * Global Exception Handler to manage various exception types.
 */

@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handles exceptions.
   * Logs the error message and returns a response for this specific exception.
   *
   * @param ex The Exception instance.
   * @return Response entity containing error details.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
      Map<String, String> errors = new HashMap<>();
      ex.getBindingResult().getAllErrors().forEach((error) -> {
          String fieldName = ((FieldError) error).getField();
          String errorMessage = error.getDefaultMessage();
          errors.put(fieldName, errorMessage);
      });
      return ResponseEntity.badRequest().body(errors);
  }

  /**
   * Handles exceptions.
   * Logs the error message and returns a response for this specific exception.
   *
   * @param e The HttpRequestMethodNotSupportedException instance.
   * @return Response entity containing error details.
   */
   @ExceptionHandler(NoResourceFoundException.class)
   public ResponseEntity<ErrorResponseDto<Object>> handleNoResourceFoundException(NoResourceFoundException e) {
     return ErrorResponseDto.builder()
         .errorCode(HttpStatus.NOT_FOUND.value())
         .message("Resource not found")
         .build()
         .of();
   }

  /**
   * Handles exceptions.
   * Logs the error message and returns a response for this specific exception.
   *
   * @param e The BusinessRuleException instance.
   * @return Response entity containing error details.
   */
  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ErrorResponseDto<Object>> handleBusinessRuleException(BusinessRuleException e) {
    return ErrorResponseDto.builder()
        .errorCode(e.getStatus())
        .message(e.getMessage())
        .build()
        .of();
  }


  /**
   * Handles MissingServletRequestParameterException.
   * Logs the error and returns a response entity with error details.
   *
   * @param ex The MissingServletRequestParameterException instance.
   * @return Response entity containing error details.
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponseDto<Object>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
    return ErrorResponseDto.builder()
        .errorCode(HttpStatus.BAD_REQUEST.value())
        .message("Missing parameter")
        .build()
        .of();
  }


  /**
   * Handles RuntimeException.
   * Logs the error and returns a response entity with error details.
   *
   * @param ex The RuntimeException instance.
   * @return Response entity containing error details.
   */
  @ExceptionHandler(RuntimeException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<ErrorResponseDto<Object>> handleRuntimeException(RuntimeException ex) {
    return ErrorResponseDto.builder()
        .errorCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .message("Internal server error")
        .build()
        .of();

  }


  /**
   * Handles `MethodArgumentTypeMismatchException` by returning a response with a 400 BAD REQUEST status.
   *
   * @param ex the exception thrown when a method argument type mismatch occurs.
   * @return a response with the error details.
   */

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<ErrorResponseDto<Object>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
    return ErrorResponseDto.builder()
        .errorCode(HttpStatus.BAD_REQUEST.value())
        .message("Method argument type mismatch")
        .build()
        .of();
  }

  /**
   * Handles `EntityNotFoundException` by returning a response with a 404 NOT FOUND status.
   *
   * @param ex the exception thrown when an entity is not found.
   * @return a response with the error details.
   */
  @ExceptionHandler(EntityNotFoundException.class)
  @ResponseBody
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<ErrorResponseDto<Object>> handleEntityNotFoundException(EntityNotFoundException ex) {
    return ErrorResponseDto.builder()
        .errorCode(HttpStatus.NOT_FOUND.value())
        .message(ex.getMessage())
        .build()
        .of();
  }


}
