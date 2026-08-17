package com.quanxiaoha.xiaohashu.kv.biz.service;

import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.xiaohashu.kv.dto.req.AddNoteContentReqDTO;
import com.quanxiaoha.xiaohashu.kv.dto.req.DeleteNoteContentReqDTO;
import com.quanxiaoha.xiaohashu.kv.dto.req.FindNoteContentReqDTO;
import com.quanxiaoha.xiaohashu.kv.dto.resp.FindNoteContentRespDTO;

public interface NoteContentService {

    Response<?> addNoteContent(AddNoteContentReqDTO  addNoteContentReqDTO);
    Response<FindNoteContentRespDTO> findNoteContentById(FindNoteContentReqDTO findNoteContentReqDTO);
    Response<?> deleteNoteContentById(DeleteNoteContentReqDTO deleteNoteContentReqDTO);
}
