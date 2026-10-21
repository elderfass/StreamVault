package com.example.joemovies.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageUtils {

    /**
     * Convierte cualquier enlace de imagen compartido de Google Drive a un enlace directo
     * que Glide o los visores de imágenes puedan renderizar instantáneamente.
     */
    public static String getDirectImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }

        String trimmedUrl = url.trim();

        if (trimmedUrl.contains("drive.google.com") || trimmedUrl.contains("docs.google.com")) {
            String fileId = null;

            // Patrón 1: /file/d/FILE_ID/
            Pattern pattern1 = Pattern.compile("/file/d/([a-zA-Z0-9_-]+)");
            Matcher matcher1 = pattern1.matcher(trimmedUrl);
            if (matcher1.find()) {
                fileId = matcher1.group(1);
            }

            // Patrón 2: id=FILE_ID
            if (fileId == null) {
                Pattern pattern2 = Pattern.compile("id=([a-zA-Z0-9_-]+)");
                Matcher matcher2 = pattern2.matcher(trimmedUrl);
                if (matcher2.find()) {
                    fileId = matcher2.group(1);
                }
            }

            if (fileId != null) {
                return "https://lh3.googleusercontent.com/d/" + fileId;
            }
        }

        return trimmedUrl;
    }
}
