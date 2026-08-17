package com.quanxiaoha.framework.common.response;


import com.quanxiaoha.framework.common.exception.BaseExceptionInterface;
import com.quanxiaoha.framework.common.exception.BizException;
import lombok.Data;

import java.io.Serializable;

@Data
public class Response<T> implements Serializable {

    //请求是否成功，默认为true
    private  boolean success =true;

    //响应消息
    private String message;

    //异常码
    private String errorCode;

    private T data;

    //==========================成功响应===================
    public static <T> Response<T> success(){
        Response<T> response = new Response<T>();
        return response;
    }
    public static <T> Response<T> success(T data){
        Response<T> response = new Response<T>();
        response.setData(data);
        return response;
    }

    //==================失败响应=========================
    public static <T> Response<T> fail(){
        Response<T> response = new Response<T>();
        response.setSuccess(false);
        return response;
    }

    //返回错误消息
    public static <T> Response<T> fail(String message){
        Response<T> response = new Response<T>();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    public static <T> Response<T> fail(String errorCode,String message){
        Response<T> response = new Response<T>();
        response.setSuccess(false);
        response.setErrorCode(errorCode);
        response.setMessage(message);
        return response;
    }

    public static <T> Response<T> fail(BizException  bizException){
        Response<T> response = new Response<T>();
        response.setSuccess(false);
        response.setErrorCode(bizException.getErrorCode());
        response.setMessage(bizException.getErrorMessage());
        return response;
    }

    public static<T> Response<T> fail(BaseExceptionInterface  baseExceptionInterface){
        Response<T> response = new Response<T>();
        response.setSuccess(false);
        response.setErrorCode(baseExceptionInterface.getErrorCode());
        response.setMessage(baseExceptionInterface.getErrorMessage());
        return response;

    }

}
