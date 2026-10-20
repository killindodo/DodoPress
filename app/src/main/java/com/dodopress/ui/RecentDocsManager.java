package com.dodopress.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class RecentDocsManager {

    private static final String PREF_NAME = "dodopress_recents";
    private static final String KEY_ITEMS = "recent_items_json";
    private static final int MAX_RECENTS = 6;

    public static class RecentItem {
        public String uriString;
        public String title;
        public int pages;
        public long sizeKb;
        public long timestamp;

        public RecentItem(String uriString, String title, int pages, long sizeKb, long timestamp) {
            this.uriString = uriString;
            this.title = title;
            this.pages = pages;
            this.sizeKb = sizeKb;
            this.timestamp = timestamp;
        }

        public String getSubtitle() {
            return pages + " Page" + (pages > 1 ? "s" : "") + " • " + sizeKb + " KB";
        }
    }

    public static synchronized List<RecentItem> getRecents(Context context) {
        List<RecentItem> list = new ArrayList<>();
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonStr = sp.getString(KEY_ITEMS, "[]");
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new RecentItem(
                    obj.optString("uri"),
                    obj.optString("title"),
                    obj.optInt("pages"),
                    obj.optLong("sizeKb"),
                    obj.optLong("time")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static synchronized void addDoc(Context context, Uri uri, String title, int pages, long sizeKb) {
        if (uri == null || title == null) return;
        List<RecentItem> existing = getRecents(context);

        // Remove duplicate if same URI or title already present
        for (int i = existing.size() - 1; i >= 0; i--) {
            RecentItem item = existing.get(i);
            if (item.uriString.equals(uri.toString()) || item.title.equalsIgnoreCase(title)) {
                existing.remove(i);
            }
        }

        // Add to front
        existing.add(0, new RecentItem(uri.toString(), title, pages, sizeKb, System.currentTimeMillis()));

        // Limit size
        while (existing.size() > MAX_RECENTS) {
            existing.remove(existing.size() - 1);
        }

        // Persist
        try {
            JSONArray arr = new JSONArray();
            for (RecentItem it : existing) {
                JSONObject obj = new JSONObject();
                obj.put("uri", it.uriString);
                obj.put("title", it.title);
                obj.put("pages", it.pages);
                obj.put("sizeKb", it.sizeKb);
                obj.put("time", it.timestamp);
                arr.put(obj);
            }
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_ITEMS, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static synchronized void clearRecents(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_ITEMS).apply();
    }
}
