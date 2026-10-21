package com.example.joemovies.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "JoeMoviesSession";
    private static final String KEY_LAST_ACTIVITY = "last_activity_time";
    // 5 minutos de inactividad por defecto
    private static final long INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000;

    public static void updateLastActivity(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply();
    }

    public static boolean isSessionExpired(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long lastActivity = prefs.getLong(KEY_LAST_ACTIVITY, 0);
        if (lastActivity == 0) return true;
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastActivity) > INACTIVITY_TIMEOUT_MS;
    }

    public static void clearSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}
