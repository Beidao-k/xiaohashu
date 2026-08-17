package com.quanxiaoha.xiaohashu.auth;

import com.quanxiaoha.xiaohashu.auth.config.RedisTemplateConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
@Slf4j
public class RedisTests {

    @Resource
    RedisTemplate<String,String> redisTemplate;

    @Test
    public void testRedis(){
        redisTemplate.opsForValue().set("项目名","xiaohashu");
    }

    @Test
    public void getOneValue(){
        String s = redisTemplate.opsForValue().get("姓名");
        log.info("姓名：{}",s);
    }

    @Test
    public void isExist(){
        Boolean s = redisTemplate.hasKey("项目名");
        System.out.println("结果：======"+s);
    }


    @Test
    public void deleteKey(){
        redisTemplate.delete("姓名");
    }
}
