package com.quanxiaoha.xiaohashu.auth.alarm.impl;

import com.quanxiaoha.xiaohashu.auth.alarm.AlarmInterface;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SmsAlarmHelper implements AlarmInterface {

    @Override
    public boolean send(String message) {
        log.info("=======================SmsAlarmHelper send message=======================");
        return false;
    }
}
