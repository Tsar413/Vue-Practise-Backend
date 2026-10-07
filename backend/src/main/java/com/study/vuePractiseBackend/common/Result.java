package com.study.vuePractiseBackend.common;

public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    /**
     * 无参构造
     */
    public Result() {

    }

    /**
     * 全参构造
     */
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功返回
     *
     * 示例：
     *
     * Result.success(userList)
     *
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 失败返回
     *
     * 示例：
     *
     * Result.error("用户名不存在")
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /**
     * 未登录 / Token错误
     */
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null);
    }

    /**
     * 参数错误返回
     */
    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
