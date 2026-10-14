package org.wymix.framework.web.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.wymix.framework.common.consts.SystemConstant;
import org.wymix.framework.common.exception.BusinessException;
import org.wymix.framework.common.exception.LoginException;
import org.wymix.framework.common.result.ResultVO;
import org.wymix.framework.utils.StringUtils;
import org.wymix.framework.web.i18n.MessageResourceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.Objects;

@ControllerAdvice
@RestControllerAdvice
public class ControllerResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger log = LoggerFactory.getLogger(ControllerResponseAdvice.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !StringUtils.containsAny(returnType.getDeclaringClass().getName(), "springdoc", "actuator");
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ResultVO) {
            return body;
        }
        if (body instanceof String) {
            Objects.requireNonNull(((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getResponse()).setContentType("application/json");
        }
        return ResultVO.of(body);
    }

    @ExceptionHandler({BusinessException.class, LoginException.class, MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ResultVO> handleBusinessException(Exception ex) {
        return createResponseEntity(ex);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResultVO> handleInvalidFormatException(HttpMessageNotReadableException ex) {
        log.error(ex.getMessage(), ex);
        return new ResponseEntity<>(ResultVO.of(SystemConstant.ERROR_CODE_VALIDATION_MESSAGE_NOT_READABLE, MessageResourceHelper.generateMessages(SystemConstant.SYSTEM_VALID_EXCEPTION)), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ResultVO> handleSQLException(SQLException ex) {
        return createSQLExceptionResponse(ex);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ResultVO> handleBindError(BindException ex) {
        log.error(ex.getMessage(), ex);
        return new ResponseEntity<>(ResultVO.of(SystemConstant.ERROR_CODE_DATA_VALIDATION, MessageResourceHelper.generateMessages(SystemConstant.SYSTEM_VALID_EXCEPTION)), HttpStatus.BAD_REQUEST);
    }

    /**
     * handleUnexpectedServerError
     *
     * @param ex RuntimeException
     * @return ResponseEntity<ErrorMessage>
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResultVO> handleUnexpectedServerError(RuntimeException ex) {
        log.error(ex.getMessage(), ex);
        // 应对HystrixRuntimeException的情况
        Throwable t = ex.getCause();
        while (t != null) {
            if (t instanceof BusinessException) {
                return handleBusinessException((BusinessException) t);
            }
            t = t.getCause();
        }
        return createResponseEntity(ex);
    }

    private ResponseEntity<ResultVO> createSQLExceptionResponse(SQLException ex) {
        log.error(ex.getMessage(), ex);
        if (ex.getMessage().contains("23503") || ex.getMessage().contains("23000")) {
            return new ResponseEntity<>(ResultVO.of(SystemConstant.ERROR_CODE_DATABASE_CONSTRAINT_VIOLATION, ""), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(
                ResultVO.of(SystemConstant.ERROR_CODE_DATABASE_SQL, ""),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ResultVO> createResponseEntity(Exception ex) {
        log.error(ex.getMessage(), ex);
        if (ex instanceof LoginException) {
            ResultVO resultVO = ResultVO.of(401, MessageResourceHelper.generateMessages(ex.getMessage()));
            return new ResponseEntity<>(resultVO, HttpStatus.UNAUTHORIZED);
        }
        if (ex instanceof BusinessException) {
            ResultVO resultVO;
            if (((BusinessException) ex).getParams() != null) {
                resultVO = ResultVO.of(((BusinessException) ex).getStatusCode(), MessageResourceHelper.generateMessages(((BusinessException) ex).getErrorMessage(), ((BusinessException) ex).getParams()));
            } else {
                resultVO = ResultVO.of(((BusinessException) ex).getStatusCode(), MessageResourceHelper.generateMessages(((BusinessException) ex).getErrorMessage()));
            }
            if (((BusinessException) ex).getFieldErrors() != null) {
                ((BusinessException) ex).getFieldErrors().forEach((k, v) -> resultVO.addInvalidMessage(k, MessageResourceHelper.generateMessages(v)));
            }
            return new ResponseEntity<>(resultVO, HttpStatus.BAD_REQUEST);
        }
        if (ex instanceof MethodArgumentNotValidException) {
            ResultVO resultVO = ResultVO.of(SystemConstant.ERROR_CODE_DATA_VALIDATION, "");
            ((MethodArgumentNotValidException) ex).getBindingResult().getAllErrors().forEach(s -> {
                if (s instanceof FieldError) {
                    resultVO.addInvalidMessage(((FieldError) s).getField(), MessageResourceHelper.generateMessages(s.getDefaultMessage()));
                } else {
                    resultVO.addInvalidMessage(s.getCode(), MessageResourceHelper.generateMessages(s.getDefaultMessage()));
                }
            });
            return new ResponseEntity<>(resultVO, HttpStatus.BAD_REQUEST);
        }

        if (ex instanceof ConstraintViolationException) {
            ResultVO resultVO = ResultVO.of(SystemConstant.ERROR_CODE_DATA_VALIDATION, "");
            ((ConstraintViolationException) ex).getConstraintViolations().forEach(s -> {
                resultVO.addInvalidMessage(catListErrorPath(s.getPropertyPath().toString()), MessageResourceHelper.generateMessages(s.getMessage()));
            });
            return new ResponseEntity<>(resultVO, HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(ResultVO.of(SystemConstant.SYSTEM_INTERNAL_ERROR_CODE, MessageResourceHelper.generateMessages(SystemConstant.SYSTEM_INTERNAL_ERROR)), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String catListErrorPath(String s) {
        if (s.contains(".")) {
            return StringUtils.substring(s, s.indexOf('.') + 1);
        }
        return s;
    }
}
