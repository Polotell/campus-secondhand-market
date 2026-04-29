package com.campus.market.exception;

import com.campus.market.common.Result;
import com.campus.market.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 使用 {@code @RestControllerAdvice} 拦截整个应用中 Controller 抛出的异常，
 * 将其转换为统一的 {@link Result} 结构返回，避免散落在 Controller 的 try-catch。
 * <p>
 * 处理优先级：精确类型 &gt; 通用异常。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常（Service 层手动抛出） */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e, HttpServletRequest req) {
        log.warn("业务异常 uri={} code={} msg={}", req.getRequestURI(), e.getCode(), e.getMsg());
        return Result.fail(e.getCode(), e.getMsg());
    }

    /** @RequestBody @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleBodyValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }

    /** form 表单 @Valid 校验失败 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }

    /** @RequestParam 上 @Validated 校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraint(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }

    /** 缺少请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissing(MissingServletRequestParameterException e) {
        return Result.fail(ResultCode.BAD_REQUEST, "缺少参数：" + e.getParameterName());
    }

    /** 缺少 multipart 表单中的 part（例如 /file/upload 没带 file 字段） */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public Result<Void> handleMissingPart(MissingServletRequestPartException e) {
        return Result.fail(ResultCode.BAD_REQUEST, "缺少文件字段：" + e.getRequestPartName());
    }

    /** 请求参数类型不匹配 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.fail(ResultCode.BAD_REQUEST, "参数类型错误：" + e.getName());
    }

    /** JSON 解析失败 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleJson(HttpMessageNotReadableException e) {
        return Result.fail(ResultCode.BAD_REQUEST, "请求体格式错误");
    }

    /** 文件上传超限 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleUpload(MaxUploadSizeExceededException e) {
        return Result.fail(ResultCode.BAD_REQUEST, "上传文件过大，单文件最大 10MB");
    }

    /**
     * Multipart 解析失败：如 Content-Type 不是 multipart/form-data、请求体为空、boundary 缺失。
     * 兜底返回 400 而非 500，对前端更友好，避免暴露 FileUploadException 细节。
     */
    @ExceptionHandler(MultipartException.class)
    public Result<Void> handleMultipart(MultipartException e) {
        log.warn("Multipart 解析失败：{}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST, "请使用 multipart/form-data 上传文件，并确保字段名为 file");
    }

    /**
     * 404 —— 需配合 {@code spring.mvc.throw-exception-if-no-handler-found=true}
     * 和 {@code spring.web.resources.add-mappings=false} 才能进到这里。
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNotFound(NoHandlerFoundException e) {
        return Result.fail(ResultCode.NOT_FOUND, "接口不存在：" + e.getRequestURL());
    }

    /** 兜底：未知异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception e, HttpServletRequest req) {
        log.error("未捕获异常 uri={}", req.getRequestURI(), e);
        return Result.fail(ResultCode.INTERNAL_ERROR, "服务器开小差了：" + e.getMessage());
    }
}
