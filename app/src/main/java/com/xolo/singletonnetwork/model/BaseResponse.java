package com.xolo.singletonnetwork.model;

public class BaseResponse<T> {

    /**
     * code : 200
     * message : 请求成功
     * data : T
     */

    private int code;
    private String message;
    private T data;

    @Override
    public String toString() {
        return "BaseResponse{" +
                "data=" + data +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
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
}
