package com.polaris.socket.core.utils;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

public class JsonUtils {

    private static final Gson gson = new Gson();

    public static byte[] toJsonBytes(Object obj) {
        return gson.toJson(obj).getBytes(StandardCharsets.UTF_8);
    }

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

}
