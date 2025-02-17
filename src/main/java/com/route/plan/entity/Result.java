package com.route.plan.entity;


import lombok.Data;

@Data
public class Result<T> {

    private Integer code;

    private String msg;

    private T data;

    public Result(){
        this.code = 200;
        this.msg = "success";
    }

    public Result(T data){
        this.data = data;
        this.code = 200;
        this.msg = "success";
    }

    public Result(int code, String message) {
        this.code = code;
        this.msg = message;
        this.data = null;
    }

    public Result(int code, T data, String message) {
        this.code = code;
        this.msg = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<>();
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(data);
    }

    public static <T> Result<T> success(T data,String message) {
        return new Result<>(200,data,message);
    }
}
