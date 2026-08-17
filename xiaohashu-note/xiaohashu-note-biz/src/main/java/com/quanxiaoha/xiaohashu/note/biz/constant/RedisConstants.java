package com.quanxiaoha.xiaohashu.note.biz.constant;

public class RedisConstants {
    private static final String NOTE_INFO_KEY_PREFIX = "note:info:";
    public static String buildNoteInfoKey(Long noteId) {
        return NOTE_INFO_KEY_PREFIX + noteId;
    }
}
