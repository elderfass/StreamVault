package com.example.joemovies.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VideoUrlResolver {

    private static final Pattern DRIVE_PATH = Pattern.compile("/file/d/([a-zA-Z0-9_-]+)");
    private static final Pattern DRIVE_ID = Pattern.compile("[?&]id=([a-zA-Z0-9_-]+)");
    private static final String UA =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/120.0.0.0 Mobile Safari/537.36";

    private VideoUrlResolver() {}

    @Nullable
    public static String extractDriveId(@Nullable String url) {
        if (url == null) return null;
        if (!(url.contains("drive.google.com") || url.contains("docs.google.com")
                || url.contains("usercontent.google.com"))) return null;
        Matcher m = DRIVE_PATH.matcher(url);
        if (m.find()) return m.group(1);
        m = DRIVE_ID.matcher(url);
        if (m.find()) return m.group(1);
        return null;
    }

    /** URL que ExoPlayer puede reproducir directamente, o null si requiere WebView. */
    @Nullable
    public static String resolveDirect(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String u = raw.trim();
        String l = u.toLowerCase();

        if (l.contains("dropbox.com")) {
            String r = u.replace("dl=0", "raw=1");
            if (!r.contains("raw=1")) r += (r.contains("?") ? "&" : "?") + "raw=1";
            return r;
        }
        if (l.contains("1drv.ms") || l.contains("onedrive.live.com")) {
            String b64 = Base64.encodeToString(u.getBytes(),
                    Base64.NO_WRAP | Base64.URL_SAFE | Base64.NO_PADDING);
            return "https://api.onedrive.com/v1.0/shares/u!" + b64 + "/root/content";
        }
        if (l.contains("sharepoint.com")) {
            return u.contains("download=1") ? u : u + (u.contains("?") ? "&" : "?") + "download=1";
        }
        String driveId = extractDriveId(u);
        if (driveId != null) {
            return "https://drive.usercontent.google.com/download?id=" + driveId
                    + "&export=download&confirm=t";
        }
        String path = Uri.parse(u).getPath();
        String lp = path == null ? "" : path.toLowerCase();
        boolean video = lp.endsWith(".mp4") || lp.endsWith(".mkv") || lp.endsWith(".m3u8")
                || lp.endsWith(".webm") || lp.endsWith(".mov");
        return video ? u : null;
    }

    @OptIn(markerClass = UnstableApi.class)
    @NonNull
    public static MediaSource buildMediaSource(@NonNull Context ctx, @NonNull String url) {
        DefaultHttpDataSource.Factory factory = new DefaultHttpDataSource.Factory()
                .setUserAgent(UA)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000);
        return new DefaultMediaSourceFactory(factory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(url)));
    }
}
