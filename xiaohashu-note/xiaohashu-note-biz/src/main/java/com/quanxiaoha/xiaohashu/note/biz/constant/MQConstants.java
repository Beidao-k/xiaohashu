package com.quanxiaoha.xiaohashu.note.biz.constant;

public interface MQConstants {


    /**
     * 消息主题，删除本地笔记缓存
     */
    String TOPIC_DELETE_NOTE_LOCAL_CACHE = "DeleteNoteLocalCacheTopic";
    String TOPIC_DELAY_DELETE_NOTE_REDIS_CACHE = "DelayDeleteNoteRedisCacheTopic";
}
