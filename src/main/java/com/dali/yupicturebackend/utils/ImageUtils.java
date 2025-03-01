package com.dali.yupicturebackend.utils;

import cn.hutool.core.io.FileUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class ImageUtils {

    // 判断传入的字符串是否为 URL
    private static boolean isValidUrl(String urlString) {
        try {
            new URL(urlString).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 获取图片类型
    public static String getImageType(Object input) throws IOException {
        // 如果是 URL，尝试从 HTTP 请求获取图片类型
        if (input instanceof String && isValidUrl((String) input)) {
            return getImageTypeFromUrl((String) input);
        }else

         {
            return getImageTypeFromFile(input);
        }

    }

    // 获取图片类型（从 URL）
    public static String getImageTypeFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD"); // 不下载文件，只请求文件头
            connection.connect();

            // 获取返回的 Content-Type 头
            String contentType = connection.getContentType();

            if (contentType != null) {
                if (contentType.startsWith("image/jpeg")) {
                    return "jpg";
                } else if (contentType.startsWith("image/png")) {
                    return "png";
                } else if (contentType.startsWith("image/gif")) {
                    return "gif";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null; // 如果无法确定类型，返回 null
    }

    // 获取图片类型（从文件）
    private static String getImageTypeFromFile(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;

        return FileUtil.getSuffix(multipartFile.getOriginalFilename());
    }
}
