package com.quanxiaoha.xiaohashu.oss.biz.strategy.impl;

import com.aliyun.oss.OSS;
import com.quanxiaoha.xiaohashu.oss.biz.config.AliyunOSSProperties;
import com.quanxiaoha.xiaohashu.oss.biz.strategy.FileStrategy;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Slf4j
public class AliyunOSSFileStrategy implements FileStrategy {

    @Resource
    private AliyunOSSProperties aliyunOSSProperties;

    @Resource
    private OSS ossClient;

    @Override
    @SneakyThrows
    public String uploadFile(MultipartFile file) {
        log.info("##########文件上传至阿里云oss...");
        if(file==null||file.getSize()==0){
            log.error("=========文件不能为空====================");
            throw new RuntimeException("文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();

        String key = UUID.randomUUID().toString().replaceAll("-","");
        //文件名后缀
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        String fileName = String.format("%s%s", key, suffix);

        log.info("===========>开始上传文件至阿里云OSS,文件名:{}",fileName);

        ossClient.putObject(aliyunOSSProperties.getBucketName(),fileName,new ByteArrayInputStream(file.getInputStream().readAllBytes()));



        //文件地址
        String url = String.format("https://%s.%s/%s", aliyunOSSProperties.getBucketName(), aliyunOSSProperties.getEndpoint(), fileName);
        log.info("==> 上传文件至阿里云 OSS 成功，访问路径: {}", url);
        return url;
    }
}
