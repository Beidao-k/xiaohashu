package com.quanxiaoha.xiaohashu.note.biz.controller;


import com.quanxiaoha.framework.biz.operationlog.aspect.ApiOperationLog;
import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.xiaohashu.note.biz.model.vo.*;
import com.quanxiaoha.xiaohashu.note.biz.service.NoteService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/note")
public class NoteController {

    @Resource
    NoteService noteService;

    @PostMapping("/publish")
    @ApiOperationLog(description = "笔记发布")
    public Response<?> publishNote(@Validated @RequestBody PublishNoteReqVO publishNoteReqVO){
        return noteService.publishNote(publishNoteReqVO);
    }

    @PostMapping("/detail")
    @ApiOperationLog(description = "笔记详情")
    public Response<FindNoteDetailRspVO> findNoteDetail(@Validated @RequestBody FindNoteDetailReqVO findNoteDetailReqVO){
        return noteService.findNoteDetailById(findNoteDetailReqVO);
    }

    @PostMapping("/update")
    @ApiOperationLog(description = "更新笔记")
    public  Response<?> updateNote(@Validated @RequestBody UpdateNoteReqVO updateNoteReqVO){
        return noteService.UpdateNote(updateNoteReqVO);
    }

    @PostMapping("/delete")
    @ApiOperationLog(description = "删除笔记")
    public Response<?> deleteNote(@Validated @RequestBody DeleteNoteReqVO deleteNoteReqVO){
        return noteService.deleteNote(deleteNoteReqVO);
    }

    @PostMapping("/visible/onlyme")
    @ApiOperationLog(description = "设置笔记仅自己可见")
    public Response<?> visibleOnlyMe(@Validated @RequestBody VisibleOnlyMeReqVO visibleOnlyMeReqVO){
        return noteService.visibleOnlyMe(visibleOnlyMeReqVO);
    }

    @PostMapping("/top")
    @ApiOperationLog(description = "笔记是否置顶")
    public Response<?> isTopNote(@Validated @RequestBody TopNoteReqVO topNoteReqVO){
        return noteService.TopNote(topNoteReqVO);
    }

}
