package com.quanxiaoha.xiaohashu.kv.biz.service.impl;

import com.quanxiaoha.framework.common.exception.BizException;
import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.xiaohashu.kv.biz.domain.dataobject.NoteContentDO;
import com.quanxiaoha.xiaohashu.kv.biz.domain.repositroy.NoteContentRepository;
import com.quanxiaoha.xiaohashu.kv.biz.enums.ResponseCodeEnum;
import com.quanxiaoha.xiaohashu.kv.biz.service.NoteContentService;
import com.quanxiaoha.xiaohashu.kv.dto.req.AddNoteContentReqDTO;
import com.quanxiaoha.xiaohashu.kv.dto.req.DeleteNoteContentReqDTO;
import com.quanxiaoha.xiaohashu.kv.dto.req.FindNoteContentReqDTO;
import com.quanxiaoha.xiaohashu.kv.dto.resp.FindNoteContentRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class NoteContentServiceImpl implements NoteContentService {

    @Resource
    NoteContentRepository noteContentRepository;


    @Override
    public Response<?> addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO) {
        log.info("=====================>开始添加笔记");
        String noteId = addNoteContentReqDTO.getUuid();

        String noteContent = addNoteContentReqDTO.getContent();



        NoteContentDO  noteContentDO = NoteContentDO.builder()
                .id(UUID.fromString(noteId))
                .content(noteContent)
                .build();

        //向Cassandra插入数据
        noteContentRepository.save(noteContentDO);
        return Response.success();
    }

    @Override
    public Response<FindNoteContentRespDTO> findNoteContentById(FindNoteContentReqDTO findNoteContentReqDTO) {
        String noteId = findNoteContentReqDTO.getNoteId();
        UUID uuid = UUID.fromString(noteId);
        Optional<NoteContentDO> noteContentDO = noteContentRepository.findById(uuid);


        //未查到笔记
        if(!noteContentDO.isPresent()){
            throw new BizException(ResponseCodeEnum.NOTE_CONTENT_NOT_FOUND);
        }


        NoteContentDO noteContentDO1 = noteContentDO.get();
        FindNoteContentRespDTO findNoteContentRespDTO = FindNoteContentRespDTO.builder()
                .noteId(noteContentDO1.getId())
                .content(noteContentDO1.getContent())
                .build();

        return Response.success(findNoteContentRespDTO);
    }

    @Override
    public Response<?> deleteNoteContentById(DeleteNoteContentReqDTO deleteNoteContentReqDTO) {
        String noteId = deleteNoteContentReqDTO.getNoteId();
        UUID uuid = UUID.fromString(noteId);
        noteContentRepository.deleteById(uuid);

       return Response.success();
    }
}
