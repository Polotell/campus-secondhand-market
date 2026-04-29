package com.campus.market.exception;

import com.campus.market.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常（【强制】Service 层抛错统一使用此异常）
 * <p>
 * 一律不要抛 {@link RuntimeException} 或自己定义一堆 XXException；
 * 通过 {@link ResultCode} + 可选 msg 组合即可表达所有业务错误。
 * 被 {@code GlobalExceptionHandler} 统一捕获并转成 {@code Result.fail()}。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final String  msg;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
        this.msg  = resultCode.getMessage();
    }

    public BusinessException(ResultCode resultCode, String msg) {
        super(msg);
        this.code = resultCode.getCode();
        this.msg  = msg;
    }

    public BusinessException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg  = msg;
    }

    public static BusinessException of(ResultCode code) {
        return new BusinessException(code);
    }

    public static BusinessException of(ResultCode code, String msg) {
        return new BusinessException(code, msg);
    }
}
