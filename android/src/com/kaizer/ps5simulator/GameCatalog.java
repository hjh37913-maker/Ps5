package com.kaizer.ps5simulator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

/** Game metadata + lazy, cached loading of original concept art from assets/. */
public final class GameCatalog {

    public static final class Game {
        public final String id;
        public final String title;
        public final String genre;
        public final String publisher;
        public final String description;
        public final int year;
        public final float size;
        public final int primary;
        public final int secondary;

        Game(JSONObject o) throws Exception {
            id = o.getString("id");
            title = o.getString("title");
            genre = o.getString("genre");
            publisher = o.getString("publisher");
            description = o.getString("description");
            year = o.getInt("year");
            size = (float) o.getDouble("size");
            primary = Color.parseColor(o.getString("primary"));
            secondary = Color.parseColor(o.getString("secondary"));
        }

        public String initials() {
            String[] parts = title.replace("'", "").split(" ");
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < parts.length && b.length() < 2; i++) {
                if (parts[i].length() > 0 && Character.isLetterOrDigit(parts[i].charAt(0))) {
                    b.append(Character.toUpperCase(parts[i].charAt(0)));
                }
            }
            return b.length() == 0 ? "PS" : b.toString();
        }
    }

    private static final HashMap<String, Bitmap> CACHE = new HashMap<String, Bitmap>();

    public static ArrayList<Game> load(Context ctx) {
        ArrayList<Game> out = new ArrayList<Game>();
        try {
            String json = readText(ctx, "catalog.json");
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++) out.add(new Game(a.getJSONObject(i)));
        } catch (Exception e) {
            android.util.Log.e("PS5Sim", "catalog load failed", e);
        }
        if (out.isEmpty()) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", "offline");
                o.put("title", "PS5 Simulator");
                o.put("genre", "Demo");
                o.put("publisher", "Kaizer");
                o.put("description", "Offline simulator interface.");
                o.put("year", 2026);
                o.put("size", 1.0);
                o.put("primary", "#4AA8FF");
                o.put("secondary", "#9A86FF");
                out.add(new Game(o));
            } catch (Exception ignored) {}
        }
        return out;
    }

    public static String readText(Context ctx, String path) {
        try {
            InputStream in = ctx.getAssets().open(path);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            return new String(out.toByteArray(), "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    /** Loads an image from assets; sample=2 halves dimensions (saves RAM). */
    public static Bitmap image(Context ctx, String path, int sample) {
        String key = path + "@" + sample;
        Bitmap cached = CACHE.get(key);
        if (cached != null && !cached.isRecycled()) return cached;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            o.inPreferredConfig = Bitmap.Config.ARGB_8888;
            InputStream in = ctx.getAssets().open(path);
            Bitmap b = BitmapFactory.decodeStream(in, null, o);
            in.close();
            if (b != null) {
                if (CACHE.size() > 46) CACHE.clear();
                CACHE.put(key, b);
            }
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap cover(Context ctx, Game g) {
        return image(ctx, "games/" + g.id + "/cover.jpg", 2);
    }

    public static Bitmap background(Context ctx, Game g) {
        return image(ctx, "games/" + g.id + "/background.jpg", 2);
    }

    public static Bitmap shot(Context ctx, Game g, int i) {
        return image(ctx, "games/" + g.id + "/shot" + i + ".jpg", 2);
    }

    private GameCatalog() {}
}
