package com.quanxiaoha.xiaohashu.auth.alarm;


import com.quanxiaoha.xiaohashu.auth.alarm.impl.MailAlarmHelper;
import com.quanxiaoha.xiaohashu.auth.alarm.impl.SmsAlarmHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RefreshScope
public class AlarmConfig {

    @Value("${alarm.type}")
    private String alarmType;


    @Bean
    @RefreshScope
    public AlarmInterface alarmInterface(){

        if(StringUtils.isNotBlank(alarmType)){
            if(StringUtils.equals("sms",alarmType)){
                return new SmsAlarmHelper();
            }else if(StringUtils.equals("email",alarmType)){
                return new MailAlarmHelper();
            }
        }
        return null;
    }
}
