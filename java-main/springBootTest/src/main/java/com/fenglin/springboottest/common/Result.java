package com.fenglin.springboottest.common;

import lombok.Data;

/**
 * 统一返回结果封装
 */
@Data
public class Result<T> {

    /** 状态码：200 成功，其他失败 */
    private Integer code;
    /** 提示信息 */
    private String message;
    /** 数据 */
    private T data;

    public static <T> Result<T> success(String message, T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMessage(message);
        r.setData(data);
        return r;
    }

    public static <T> Result<T> success(String message) {
        return success(message, null);
    }

    public static <T> Result<T> error(String message) {
        Result<T> r = new Result<>();
        r.setCode(500);
        r.setMessage(message);
        return r;
    }
}
