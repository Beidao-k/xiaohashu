package com.quanxiaoha.xiaohashu.note.biz.rpc;


import com.alibaba.nacos.api.common.ResponseCode;
import com.quanxiaoha.framework.common.exception.BizException;
import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.xiaohashu.kv.dto.api.NoteContentApi;
import com.quanxiaoha.xiaohashu.kv.dto.req.AddNoteContentReqDTO;
import com.quanxiaoha.xiaohashu.kv.dto.req.DeleteNoteContentReqDTO;
import com.quanxiaoha.xiaohashu.kv.dto.req.FindNoteContentReqDTO;
import com.quanxiaoha.xiaohashu.kv.dto.resp.FindNoteContentRespDTO;
import com.quanxiaoha.xiaohashu.note.biz.enums.ResponseCodeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class KeyValueRpcService {

    @Resource
    private NoteContentApi noteContentApi;

    public boolean svaeNoteContent(String uuid,String content){
        AddNoteContentReqDTO  addNoteContentReqDTO = AddNoteContentReqDTO
                .builder()
                .uuid(uuid)
                .content(content)
                .build();
        Response<?> response = noteContentApi.addNoteContent(addNoteContentReqDTO);
        if(Objects.isNull(response)|!response.isSuccess()){
            return false;
        }
        return true;
    }

    public boolean deleteNoteContent(String uuid){
        DeleteNoteContentReqDTO deleteNoteContentReqDTO = DeleteNoteContentReqDTO.builder()
                .noteId(uuid)
                .build();

        Response<?> response = noteContentApi.deleteNoteContent(deleteNoteContentReqDTO);
        if(Objects.isNull(response)||!response.isSuccess()){
            return false;
        }
        return true;
    }

    public String findNoteContent(String uuid){
        FindNoteContentReqDTO findNoteContentReqDTO = FindNoteContentReqDTO.builder().noteId(uuid).build();
        Response<?> response = noteContentApi.findNoteContent(findNoteContentReqDTO);
        if(Objects.isNull(response)||!response.isSuccess()){
            throw new BizException(ResponseCodeEnum.FIND_NOTE_BY_ID);
        }
        FindNoteContentRespDTO result = (FindNoteContentRespDTO) response.getData();
        return result.getContent();
    }

}
