package com.quanxiaoha.xiaohashu.note.biz.service;

import com.quanxiaoha.framework.common.response.Response;
import com.quanxiaoha.xiaohashu.note.biz.model.vo.*;

public interface NoteService {

    Response<?> publishNote(PublishNoteReqVO publishNoteReqVO);
    Response<FindNoteDetailRspVO> findNoteDetailById(FindNoteDetailReqVO findNoteDetailReqVO);

    Response<?> UpdateNote(UpdateNoteReqVO updateNoteReqVO);

    void deleteNoteLocalCache(Long noteId);

    Response<?> deleteNote(DeleteNoteReqVO deleteNoteReqVO);

    Response<?> visibleOnlyMe(VisibleOnlyMeReqVO visibleOnlyMeReqVO);

    Response<?> TopNote(TopNoteReqVO topNoteReqVO);


}
