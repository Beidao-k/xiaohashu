package com.quanxiaoha.framework.biz.operationlog.aspect;


import com.quanxiaoha.framework.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

@Aspect
@Slf4j
public class ApiOperationLogAspect {

    //定义切点
    @Pointcut("@annotation(com.quanxiaoha.framework.biz.operationlog.aspect.ApiOperationLog)")
    public void apiOperationLog(){}



    @Around("apiOperationLog()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        //请求开始时间
        long startTime = System.currentTimeMillis();

        //被请求的类和方法
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();

        //请求参数
        Object[] args = joinPoint.getArgs();
        // 修复：过滤 MultipartFile，不能直接json序列化
        String argsJsonStr = Arrays.stream(args)
                .map(arg -> {
                    if (arg instanceof MultipartFile) {
                        MultipartFile multipartFile = (MultipartFile) arg;
                        // 文件上传对象，打印文件名占位，不序列化
                        return "[MultipartFile filename=" + multipartFile.getOriginalFilename() + ",size=" + multipartFile.getSize() + "]";
                    } else {
                        try {
                            return JsonUtils.toJsonString(arg);
                        } catch (Exception e) {
                            return "[序列化失败]";
                        }
                    }
                })
                .collect(Collectors.joining("，"));


        //功能描述
        String description = getApiOperationLogDescription(joinPoint);

        //执行切入点方法
        Object result = joinPoint.proceed();

        long executionTime = System.currentTimeMillis() - startTime;
        log.info("================请求结束:[{}],耗时:{}ms,出参:{}==================",description,executionTime,JsonUtils.toJsonString(result));
        return result;


    }

    private  String getApiOperationLogDescription(ProceedingJoinPoint joinPoint){
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        ApiOperationLog apiOperationLog = method.getAnnotation(ApiOperationLog.class);
        return apiOperationLog.description();
    }


    private Function<Object, String> toJsonStr(){
        return JsonUtils::toJsonString;
    }

}
