package com.quanxiaoha.framework.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
public class JsonUtils {
    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();



  public static void init(ObjectMapper objectMapper) {
        OBJECT_MAPPER = objectMapper;
  }

  @SneakyThrows
    public static String toJsonString(Object result) {
      String s = OBJECT_MAPPER.writeValueAsString(result);
      return s;
  }

    @SneakyThrows
    public static <T> T parseObject(String jsonStr, Class<T> clazz) {
        if (StringUtils.isBlank(jsonStr)) {
            return null;
        }

        return OBJECT_MAPPER.readValue(jsonStr, clazz);
    }
}
