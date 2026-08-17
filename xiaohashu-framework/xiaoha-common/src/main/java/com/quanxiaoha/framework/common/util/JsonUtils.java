package com.quanxiaoha.framework.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;

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

    @SneakyThrows
    public static <K, V> Map<K, V> parseMap(String jsonStr, Class<K> keyClass, Class<V> valueClass) throws Exception {
        // 创建 TypeReference，指定泛型类型
        TypeReference<Map<K, V>> typeRef = new TypeReference<Map<K, V>>() {
        };

        // 将 JSON 字符串转换为 Map
        return OBJECT_MAPPER.readValue(jsonStr, OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, keyClass, valueClass));
    }

}
