package com.heheda.simplerpc.common.serialize;

import com.alibaba.fastjson.JSON;

public class JsonUtil {


    public static <T> byte[] serialize(T obj) {
        return JSON.toJSONBytes(obj);
    }


    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] data, Class<T> cls) {
        return JSON.parseObject(data, cls);
    }
}
