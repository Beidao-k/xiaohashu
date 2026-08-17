package com.quanxiaoha.framework.biz.operationlog.config;

import com.quanxiaoha.framework.biz.operationlog.aspect.ApiOperationLogAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration

public class ApiOperationLogAutoConfiguration {

   @Bean
    public ApiOperationLogAspect apiOperationLogAspect(){
       return new ApiOperationLogAspect();
   }

}
