package com.campus.market.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果封装（【强制】所有 Controller 返回值必须使用此类）
 * <p>
 * 设计理由：
 * <ul>
 *   <li>前后端约定统一结构 {@code {code, message, data}}，前端 Axios 可集中处理成功/失败。</li>
 *   <li>业务状态码（code）与 HTTP 状态码解耦，HTTP 语义保留给网络层。</li>
 * </ul>
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码，0=成功，详见 {@link ResultCode} */
    private Integer code;
    /** 提示信息 */
    private String message;
    /** 业务数据 */
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(ResultCode.SUCCESS.getMessage());
        r.setData(data);
        return r;
    }

    public static <T> Result<T> success(T data, String message) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(message);
        r.setData(data);
        return r;
    }

    public static <T> Result<T> fail(ResultCode code) {
        return fail(code.getCode(), code.getMessage());
    }

    public static <T> Result<T> fail(ResultCode code, String message) {
        return fail(code.getCode(), message);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
