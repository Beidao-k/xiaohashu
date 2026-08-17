package com.quanxiaoha.xiaohashu.note.biz.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.quanxiaoha.xiaohashu.note.biz.model.vo.FindNoteDetailRspVO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean("noteDetailLocalCache")
    public Cache<Long, FindNoteDetailRspVO> noteDetailLocalCache() {
        return Caffeine.newBuilder()
                .initialCapacity(10000)
                .maximumSize(10000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }

}
