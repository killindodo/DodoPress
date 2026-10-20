package com.dodopress.engine;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class SpoolManager {

    private static final String PREF_NAME = "dodopress_spool";
    private static final String KEY_JOBS = "spool_jobs_json";

    public static class SpoolJob {
        public String id;
        public String title;
        public String filePath;
        public int pages;
        public int copies;
        public int darkness;
        public String paperSize;
        public String placementRegion;
        public long timestamp;

        public SpoolJob(String id, String title, String filePath, int pages, int copies, int darkness, String paperSize, String placementRegion, long timestamp) {
            this.id = id;
            this.title = title;
            this.filePath = filePath;
            this.pages = pages;
            this.copies = copies;
            this.darkness = darkness;
            this.paperSize = paperSize;
            this.placementRegion = placementRegion;
            this.timestamp = timestamp;
        }

        public String getSubtitle() {
            return pages + " Page" + (pages > 1 ? "s" : "") + " • " + copies + " Copy • " + paperSize + " (" + placementRegion + ")";
        }
    }

    public static synchronized List<SpoolJob> getQueuedJobs(Context context) {
        List<SpoolJob> list = new ArrayList<>();
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonStr = sp.getString(KEY_JOBS, "[]");
        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new SpoolJob(
                    obj.optString("id"),
                    obj.optString("title"),
                    obj.optString("filePath"),
                    obj.optInt("pages"),
                    obj.optInt("copies"),
                    obj.optInt("darkness"),
                    obj.optString("paperSize", "A4"),
                    obj.optString("placementRegion", "Full Page"),
                    obj.optLong("time")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static synchronized SpoolJob enqueueJob(Context context, File srcPdf, String title, int pages, int copies, int darkness, SheetGeometry.PaperSize paper, SheetGeometry.PlacementRegion region) {
        if (srcPdf == null || !srcPdf.exists()) return null;

        File spoolDir = new File(context.getFilesDir(), "spool_queue");
        if (!spoolDir.exists()) spoolDir.mkdirs();

        String jobId = "job_" + System.currentTimeMillis();
        File dstFile = new File(spoolDir, jobId + ".pdf");

        try (InputStream in = new FileInputStream(srcPdf);
             OutputStream out = new FileOutputStream(dstFile)) {
            byte[] buf = new byte[16384];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        } catch (Exception e) {
            return null;
        }

        List<SpoolJob> list = getQueuedJobs(context);
        SpoolJob job = new SpoolJob(
            jobId,
            title,
            dstFile.getAbsolutePath(),
            pages,
            copies,
            darkness,
            (paper != null ? paper.name() : "A4"),
            (region != null ? region.title : "Full Page"),
            System.currentTimeMillis()
        );
        list.add(0, job);
        saveJobs(context, list);
        return job;
    }

    public static synchronized void removeJob(Context context, String jobId) {
        List<SpoolJob> list = getQueuedJobs(context);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).id.equals(jobId)) {
                File f = new File(list.get(i).filePath);
                if (f.exists()) f.delete();
                list.remove(i);
            }
        }
        saveJobs(context, list);
    }

    public static synchronized void clearQueue(Context context) {
        List<SpoolJob> list = getQueuedJobs(context);
        for (SpoolJob job : list) {
            File f = new File(job.filePath);
            if (f.exists()) f.delete();
        }
        saveJobs(context, new ArrayList<>());
    }

    private static void saveJobs(Context context, List<SpoolJob> list) {
        try {
            JSONArray arr = new JSONArray();
            for (SpoolJob job : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", job.id);
                obj.put("title", job.title);
                obj.put("filePath", job.filePath);
                obj.put("pages", job.pages);
                obj.put("copies", job.copies);
                obj.put("darkness", job.darkness);
                obj.put("paperSize", job.paperSize);
                obj.put("placementRegion", job.placementRegion);
                obj.put("time", job.timestamp);
                arr.put(obj);
            }
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_JOBS, arr.toString()).apply();
        } catch (Exception ignored) {}
    }
}
