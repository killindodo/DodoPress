package com.dodopress.util;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

public class DodoFileProvider extends ContentProvider {

    public static final String AUTHORITY = "com.dodopress.fileprovider";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (getContext() == null) throw new FileNotFoundException("Context is null");
        String path = uri.getPath();
        if (path == null) throw new FileNotFoundException("Invalid URI path");

        // Map path to cache dir
        File file;
        if (path.startsWith("/cache/")) {
            file = new File(getContext().getCacheDir(), path.substring(7));
        } else {
            file = new File(getContext().getCacheDir(), uri.getLastPathSegment());
        }

        int pfdMode = ParcelFileDescriptor.MODE_READ_ONLY;
        if ("w".equalsIgnoreCase(mode) || "rw".equalsIgnoreCase(mode) || "rwt".equalsIgnoreCase(mode)) {
            pfdMode = ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE;
        }

        return ParcelFileDescriptor.open(file, pfdMode);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (projection == null) {
            projection = new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        }
        MatrixCursor cursor = new MatrixCursor(projection);
        if (getContext() == null) return cursor;

        File file = new File(getContext().getCacheDir(), uri.getLastPathSegment());
        MatrixCursor.RowBuilder row = cursor.newRow();
        for (String col : projection) {
            if (OpenableColumns.DISPLAY_NAME.equals(col)) {
                row.add(file.getName());
            } else if (OpenableColumns.SIZE.equals(col)) {
                row.add(file.exists() ? file.length() : 0);
            } else {
                row.add(null);
            }
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        String name = uri.getLastPathSegment();
        if (name != null) {
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
            if (name.endsWith(".png")) return "image/png";
            if (name.endsWith(".pdf")) return "application/pdf";
        }
        return "application/octet-stream";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (getContext() == null) return 0;
        File file = new File(getContext().getCacheDir(), uri.getLastPathSegment());
        return (file.exists() && file.delete()) ? 1 : 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
