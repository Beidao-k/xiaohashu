package com.quanxiaoha.xiaohashu.oss.biz.controller;

import com.quanxiaoha.framework.biz.context.holer.LoginUserContextHolder;
import com.quanxiaoha.framework.biz.operationlog.aspect.ApiOperationLog;
import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.xiaohashu.oss.api.FileFeignApi;
import com.quanxiaoha.xiaohashu.oss.biz.service.FileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController implements FileFeignApi {

    @Resource
    private FileService fileService;

    @Override
    @PostMapping(value = "/upload")
    public Response<?> uploadFile(@RequestPart(value = "file") MultipartFile file) {

        log.info("=========================>用户id:{}", LoginUserContextHolder.getUserId());
        return fileService.uploadFile(file);
    }

}
