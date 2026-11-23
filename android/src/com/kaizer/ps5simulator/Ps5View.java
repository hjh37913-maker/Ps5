package com.kaizer.ps5simulator;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

/** Immediate-mode PS5-style interface drawn with the Android framework Canvas. */
public final class Ps5View extends View {

    static final int S_HOME = 0, S_LIBRARY = 1, S_STORE = 2, S_GAMEBASE = 3,
            S_PROFILE = 4, S_SETTINGS = 5, S_TROPHIES = 6, S_NOTIFICATIONS = 7,
            S_DETAILS = 8, S_LAUNCH = 9, S_MEDIA = 10;

    static final int A_CARD = 1, A_TAB = 2, A_NAV = 3, A_PLAY = 4, A_FAV = 5,
            A_BACK = 6, A_FILTER = 7, A_SETTING = 8, A_CC_TOGGLE = 9,
            A_CC_CYCLE = 10, A_CC_OPEN = 11, A_CC_CLOSE = 12, A_MEDIA = 13,
            A_QUICK = 14, A_LAUNCH_SKIP = 15, A_CLEAR = 16, A_RESET = 17,
            A_FRIEND = 18, A_PARTY = 19, A_STORE = 20;

    static final int WHITE = 0xFFF3F8FF, DIM = 0xFFA9BAD8, FAINT = 0xFF7186AC,
            ACCENT = 0xFF4AA8FF, ACCENT2 = 0xFF9A86FF, PANEL = 0xCC0C1528,
            PANEL2 = 0xE6101B32, EDGE = 0x33FFFFFF, GREEN = 0xFF5AD8A0,
            RED = 0xFFFF7184, GOLD = 0xFFFFD166, DARK = 0xFF050A16;

    private final float density;
    private final ArrayList<GameCatalog.Game> games;
    private final ArrayList<Hit> hits = new ArrayList<Hit>();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final SimpleDateFormat clockFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SharedPreferences prefs;
    private SoundPool sp;
    private final int[] snd = new int[4];

    private int w = 1, h = 1;
    private boolean running = false;
    private long anim = 0L, lastFrame = 0L;

    private int screen = S_HOME, tab = 0, selected = 0, current = 0, libFilter = 0;
    private float cardScroll = 0f, targetScroll = 0f, maxCardScroll = 0f;
    private float libraryScroll = 0f, maxLibScroll = 0f;
    private float cardRowTop = 0f, cardRowBottom = 0f;
    private final ArrayList<GameCatalog.Game> filtered = new ArrayList<GameCatalog.Game>();

    private float ccProgress = 0f;
    private boolean ccOpen = false;
    private String toastText = null;
    private long toastUntil = 0L;
    private long launchStart = 0L;
    private boolean launchDone = false;

    private final ArrayList<String> history = new ArrayList<String>();
    private final ArrayList<String> notes = new ArrayList<String>();
    private final HashSet<String> favorites = new HashSet<String>();
    private int lang = 1, playCount = 0, trophies = 0, brightness = 70;
    private boolean soundOn = true, animOn = true, dynBg = true, wifi = true, bt = true;
    private float uiScale = 1.0f;

    private float downX, downY, dragStartScroll, libDragStart;
    private long downT;
    private boolean dragging = false, swipingCc = false;
    private int dragMode = 0;

    public Ps5View(Context ctx) {
        super(ctx);
        density = getResources().getDisplayMetrics().density;
        prefs = ctx.getSharedPreferences("ps5sim", Context.MODE_PRIVATE);
        games = GameCatalog.load(ctx);
        loadPrefs();
        initSound();
        setBackgroundColor(DARK);
        if (notes.isEmpty()) {
            notes.add(L("Welcome to PS5 Mobile Simulator", "Добро пожаловать в PS5 Mobile Simulator",
                    "Ласкаво просимо до PS5 Mobile Simulator"));
            notes.add(L("Demo interface — no PSN connection", "Демо-интерфейс — без подключения к PSN",
                    "Демо-інтерфейс — без підключення до PSN"));
        }
    }

    public void resume() {
        running = true;
        lastFrame = System.currentTimeMillis();
        invalidate();
    }

    public void pause() {
        running = false;
        save();
    }

    public void showExitHint() {
        toast(L("Press back again to exit", "Нажмите «назад» ещё раз для выхода",
                "Натисніть «назад» ще раз для виходу"), 2200);
    }

    @Override
    protected void onSizeChanged(int nw, int nh, int ow, int oh) {
        w = nw; h = nh;
    }

    // ---------------------------------------------------------------- helpers
    private float dp(float v) { return v * density * uiScale; }

    /** Converts a pixel measurement into dp units (for text sizes). */
    private float S(float px) { return px / (density * uiScale); }

    private static float clamp(float v, float a, float b) {
        return v < a ? a : (v > b ? b : v);
    }

    private String L(String en, String ru, String uk) {
        return lang == 1 ? ru : (lang == 2 ? uk : en);
    }

    private static int alpha(int color, int a) {
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    private GameCatalog.Game cur() { return games.get(Math.max(0, Math.min(games.size() - 1, current))); }
    private GameCatalog.Game sel() { return games.get(Math.max(0, Math.min(games.size() - 1, selected))); }

    private void toast(String s, long ms) {
        toastText = s;
        toastUntil = System.currentTimeMillis() + ms;
    }

    private void play(int i) {
        if (soundOn && sp != null && i >= 0 && i < snd.length && snd[i] != 0) {
            try { sp.play(snd[i], 0.65f, 0.65f, 1, 0, 1f); } catch (Exception ignored) {}
        }
    }

    private void initSound() {
        try {
            AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            sp = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(aa).build();
            String[] names = { "nav", "select", "back", "launch" };
            for (int i = 0; i < names.length; i++) {
                try {
                    snd[i] = sp.load(getContext().getAssets().openFd("sounds/" + names[i] + ".wav"), 1);
                } catch (Exception e) { snd[i] = 0; }
            }
        } catch (Exception e) { sp = null; }
    }

    private void rect(Canvas c, float l, float t, float r, float b, float rad, int color) {
        fill.setShader(null);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(color);
        c.drawRoundRect(new RectF(l, t, r, b), rad, rad, fill);
    }

    private void outline(Canvas c, float l, float t, float r, float b, float rad, int color, float width) {
        stroke.setShader(null);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(width));
        stroke.setColor(color);
        c.drawRoundRect(new RectF(l, t, r, b), rad, rad, stroke);
    }

    private void grad(Canvas c, float l, float t, float r, float b, float rad, int c1, int c2) {
        fill.setStyle(Paint.Style.FILL);
        fill.setShader(new LinearGradient(l, t, r, b, c1, c2, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(l, t, r, b), rad, rad, fill);
        fill.setShader(null);
    }

    private void glow(Canvas c, float cx, float cy, float r, int color) {
        fill.setStyle(Paint.Style.FILL);
        fill.setShader(new RadialGradient(cx, cy, r, color, 0x00000000, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, fill);
        fill.setShader(null);
    }

    private void text(Canvas c, String s, float x, float y, float size, int color, boolean bold, int align) {
        txt.setShader(null);
        txt.setColor(color);
        txt.setTextSize(dp(size));
        txt.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        txt.setTextAlign(align == 1 ? Paint.Align.CENTER : (align == 2 ? Paint.Align.RIGHT : Paint.Align.LEFT));
        txt.setLetterSpacing(bold ? 0.02f : 0.01f);
        c.drawText(s, x, y, txt);
    }

    private void wrapped(Canvas c, String s, float x, float y, float maxW, float size, int color, float lineH) {
        txt.setShader(null);
        txt.setColor(color);
        txt.setTextSize(dp(size));
        txt.setTypeface(Typeface.DEFAULT);
        txt.setTextAlign(Paint.Align.LEFT);
        txt.setLetterSpacing(0.01f);
        String[] words = s.split(" ");
        StringBuilder line = new StringBuilder();
        float cy = y;
        for (String word : words) {
            String test = line.length() == 0 ? word : line.toString() + " " + word;
            if (txt.measureText(test) > maxW && line.length() > 0) {
                c.drawText(line.toString(), x, cy, txt);
                cy += dp(lineH);
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) c.drawText(line.toString(), x, cy, txt);
    }

    private void bmp(Canvas c, Bitmap b, float l, float t, float r, float bottom, float rad) {
        if (b == null || b.isRecycled()) {
            rect(c, l, t, r, bottom, rad, 0xFF1A2844);
            return;
        }
        BitmapShader sh = new BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Matrix m = new Matrix();
        m.setScale((r - l) / b.getWidth(), (bottom - t) / b.getHeight());
        m.postTranslate(l, t);
        sh.setLocalMatrix(m);
        shaderPaint.setShader(sh);
        c.drawRoundRect(new RectF(l, t, r, bottom), rad, rad, shaderPaint);
        shaderPaint.setShader(null);
    }

    private void hit(float l, float t, float r, float b, int action, int arg) {
        hits.add(new Hit(new RectF(l, t, r, b), action, arg));
    }

    private void pill(Canvas c, float l, float t, float r, float b, int color, String label, int textColor) {
        grad(c, l, t, r, b, (b - t) / 2f, alpha(color, 235), alpha(color, 165));
        text(c, label, (l + r) / 2f, (t + b) / 2f + dp(4.5f), 12f, textColor, true, 1);
    }

    private void icon(Canvas c, int kind, float cx, float cy, float s, int color) {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(1.8f));
        stroke.setColor(color);
        fill.setShader(null);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(color);
        if (kind == 0) { // home
            Path p = new Path();
            p.moveTo(cx - s, cy); p.lineTo(cx, cy - s * 0.8f); p.lineTo(cx + s, cy);
            c.drawPath(p, stroke);
            c.drawRect(new RectF(cx - s * 0.72f, cy, cx + s * 0.72f, cy + s * 0.8f), stroke);
        } else if (kind == 1) { // library grid
            c.drawRect(new RectF(cx - s, cy - s, cx - s * 0.15f, cy - s * 0.15f), stroke);
            c.drawRect(new RectF(cx + s * 0.15f, cy - s, cx + s, cy - s * 0.15f), stroke);
            c.drawRect(new RectF(cx - s, cy + s * 0.15f, cx - s * 0.15f, cy + s), stroke);
            c.drawRect(new RectF(cx + s * 0.15f, cy + s * 0.15f, cx + s, cy + s), stroke);
        } else if (kind == 2) { // store bag
            c.drawRoundRect(new RectF(cx - s * 0.8f, cy - s * 0.45f, cx + s * 0.8f, cy + s), dp(2), dp(2), stroke);
            c.drawArc(new RectF(cx - s * 0.4f, cy - s * 1.0f, cx + s * 0.4f, cy - s * 0.1f), 180, 180, false, stroke);
        } else if (kind == 3) { // game base chat
            c.drawRoundRect(new RectF(cx - s, cy - s * 0.8f, cx + s * 0.55f, cy + s * 0.35f), dp(3), dp(3), stroke);
            c.drawCircle(cx + s * 0.55f, cy + s * 0.55f, s * 0.42f, stroke);
        } else { // profile
            c.drawCircle(cx, cy - s * 0.42f, s * 0.42f, stroke);
            c.drawArc(new RectF(cx - s * 0.85f, cy - s * 0.1f, cx + s * 0.85f, cy + s * 1.25f), 190, 160, false, stroke);
        }
    }

    // -------------------------------------------------------------- main draw
    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        long now = System.currentTimeMillis();
        if (lastFrame == 0L) lastFrame = now;
        float dt = clamp((float) (now - lastFrame), 0f, 80f);
        lastFrame = now;
        if (animOn) anim += dt;
        hits.clear();

        drawBackground(c);
        if (screen == S_LAUNCH) {
            drawLaunch(c, now);
        } else {
            float barH = Math.min(dp(56), h * 0.16f);
            float navH = Math.min(dp(72), h * 0.20f);
            float navTop = h - navH;
            drawTopBar(c, barH, now);
            switch (screen) {
                case S_HOME: drawHome(c, barH, navTop); break;
                case S_LIBRARY: drawLibrary(c, barH, navTop); break;
                case S_STORE: drawStore(c, barH, navTop); break;
                case S_GAMEBASE: drawGameBase(c, barH, navTop); break;
                case S_PROFILE: drawProfile(c, barH, navTop); break;
                case S_SETTINGS: drawSettings(c, barH, navTop); break;
                case S_TROPHIES: drawTrophies(c, barH, navTop); break;
                case S_NOTIFICATIONS: drawNotifications(c, barH, navTop); break;
                case S_DETAILS: drawDetails(c, barH, navTop); break;
                case S_MEDIA: drawMediaCenter(c, barH, navTop); break;
                default: break;
            }
            drawBottomNav(c, navTop, navH);
        }

        if (dragMode != 1) cardScroll += (targetScroll - cardScroll) * 0.16f;
        if (ccOpen) ccProgress = Math.min(1f, ccProgress + dt / 240f);
        else ccProgress = Math.max(0f, ccProgress - dt / 240f);
        if (ccProgress > 0.01f || ccOpen) drawControlCenter(c);
        drawToast(c, now);

        if (screen == S_LAUNCH && !launchDone && now - launchStart > 4700L) finishLaunch();
        if (running) postInvalidateOnAnimation();
    }

    private void drawBackground(Canvas c) {
        grad(c, 0, 0, w, h, 0, 0xFF040814, 0xFF0C1A36);
        GameCatalog.Game g = cur();
        glow(c, w * 0.78f, h * 0.18f, w * 0.62f, alpha(g.primary, 42));
        glow(c, w * 0.15f, h * 0.9f, w * 0.5f, alpha(g.secondary, 36));
        if (dynBg) {
            Bitmap b = GameCatalog.background(getContext(), g);
            if (b != null) {
                fill.setAlpha(48);
                c.drawBitmap(b, new Rect(0, 0, b.getWidth(), b.getHeight()), new RectF(0, 0, w, h), fill);
                fill.setAlpha(255);
            }
        }
        fill.setShader(null);
        fill.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 46; i++) {
            float px = (float) ((i * 137.0 + anim * 0.005 * (i % 3 + 1)) % Math.max(1, w));
            float py = (float) ((i * 61.0 % Math.max(1, h)) + Math.sin(anim / 900.0 + i) * 7.0);
            int a = 20 + (i % 5) * 9;
            fill.setColor(Color.argb(a, 200, 222, 255));
            c.drawCircle(px, py, dp(0.7f + (i % 3) * 0.35f), fill);
        }
    }

    private void drawTopBar(Canvas c, float barH, long now) {
        rect(c, 0, 0, w, barH, 0, 0xB0060E1E);
        grad(c, 0, 0, w * 0.22f, barH, 0, 0x00000000, alpha(sel().primary, 40));
        // logo
        float lx = dp(26);
        glow(c, lx + dp(10), barH / 2f, dp(26), alpha(ACCENT, 90));
        rect(c, lx, barH / 2f - dp(14), lx + dp(28), barH / 2f + dp(14), dp(8), 0xFF1B3E74);
        text(c, "PS", lx + dp(14), barH / 2f + dp(6), 14, WHITE, true, 1);
        text(c, "PS5", lx + dp(38), barH / 2f + dp(1), 18, WHITE, true, 0);
        text(c, L("SIMULATOR", "СИМУЛЯТОР", "СИМУЛЯТОР"), lx + dp(38), barH / 2f + dp(15), 8, ACCENT, true, 0);
        // tabs (home only)
        if (screen == S_HOME) {
            float cx = w * 0.5f;
            String a = L("Games", "Игры", "Ігри"), b = L("Media", "Медиа", "Медіа");
            text(c, a, cx - dp(64), barH / 2f + dp(6), 14, tab == 0 ? WHITE : FAINT, true, 1);
            text(c, b, cx + dp(64), barH / 2f + dp(6), 14, tab == 1 ? WHITE : FAINT, true, 1);
            hit(cx - dp(110), 0, cx - dp(18), barH, A_TAB, 0);
            hit(cx + dp(18), 0, cx + dp(110), barH, A_TAB, 1);
            float ux = (tab == 0 ? cx - dp(64) : cx + dp(64));
            rect(c, ux - dp(20), barH - dp(4), ux + dp(20), barH - dp(1), dp(2), ACCENT);
        } else {
            String t = screenTitle();
            text(c, t, w * 0.5f, barH / 2f + dp(6), 16, WHITE, true, 1);
            hit(dp(16), dp(10), dp(58), barH - dp(8), A_BACK, 0);
            outline(c, dp(16), dp(12), dp(54), barH - dp(12), dp(8), EDGE, 1);
            text(c, "<", dp(35), barH / 2f + dp(6), 17, DIM, true, 1);
        }
        // status
        float rx = w - dp(28);
        text(c, clockFmt.format(new Date()), rx - dp(132), barH / 2f + dp(6), 14, WHITE, true, 0);
        stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(dp(1.6f)); stroke.setColor(DIM);
        c.drawArc(new RectF(rx - dp(96), barH / 2f - dp(10), rx - dp(76), barH / 2f + dp(8)), 220, 100, false, stroke);
        c.drawCircle(rx - dp(86), barH / 2f + dp(6), dp(1.6f), stroke);
        rect(c, rx - dp(64), barH / 2f - dp(7), rx - dp(40), barH / 2f + dp(7), dp(2), 0x00000000);
        outline(c, rx - dp(64), barH / 2f - dp(7), rx - dp(40), barH / 2f + dp(7), dp(2), DIM, 1.2f);
        rect(c, rx - dp(62), barH / 2f - dp(5), rx - dp(48), barH / 2f + dp(5), dp(1), GREEN);
        glow(c, rx, barH / 2f, dp(20), alpha(ACCENT2, 70));
        fill.setShader(null); fill.setStyle(Paint.Style.FILL); fill.setColor(0xFF2B4E86);
        c.drawCircle(rx, barH / 2f, dp(15), fill);
        text(c, "KZ", rx, barH / 2f + dp(5), 10, WHITE, true, 1);
        hit(rx - dp(150), 0, w, barH, A_CC_OPEN, 0);
        text(c, L("Control Center", "Центр управления", "Центр керування"), w - dp(28), barH + dp(14), 8, FAINT, false, 2);
    }

    private String screenTitle() {
        switch (screen) {
            case S_LIBRARY: return L("Game Library", "Библиотека игр", "Бібліотека ігор");
            case S_STORE: return L("Store — demo", "Магазин — демо", "Магазин — демо");
            case S_GAMEBASE: return L("Game Base", "Game Base", "Game Base");
            case S_PROFILE: return L("Profile", "Профиль", "Профіль");
            case S_SETTINGS: return L("Settings", "Настройки", "Налаштування");
            case S_TROPHIES: return L("Trophies", "Трофеи", "Трофеї");
            case S_NOTIFICATIONS: return L("Notifications", "Уведомления", "Сповіщення");
            case S_DETAILS: return L("Game details", "Об игре", "Про гру");
            case S_MEDIA: return L("Media Center", "Медиацентр", "Медіацентр");
            default: return "PS5";
        }
    }

    private void drawBottomNav(Canvas c, float top, float navH) {
        rect(c, 0, top, w, h, 0, 0xE0071020);
        rect(c, 0, top, w, top + dp(1), 0, 0x22FFFFFF);
        String[] names = {
            L("Home", "Главная", "Головна"), L("Library", "Библиотека", "Бібліотека"),
            L("Store", "Магазин", "Магазин"), L("Game Base", "Game Base", "Game Base"),
            L("Profile", "Профиль", "Профіль")
        };
        int[] screens = { S_HOME, S_LIBRARY, S_STORE, S_GAMEBASE, S_PROFILE };
        for (int i = 0; i < 5; i++) {
            float cxi = w * (i + 0.5f) / 5f;
            float cy = top + navH * 0.40f;
            boolean on = screen == screens[i] || (screen == S_DETAILS && i == 1) || (screen == S_MEDIA && i == 0);
            if (on) rect(c, cxi - dp(52), top + navH * 0.10f, cxi + dp(52), h - navH * 0.10f, dp(12), alpha(ACCENT, 28));
            icon(c, i, cxi, cy - dp(4), dp(9), on ? ACCENT : FAINT);
            text(c, names[i], cxi, top + navH * 0.78f, Math.min(9.5f, S(navH * 0.21f)), on ? WHITE : FAINT, on, 1);
            hit(cxi - dp(52), top, cxi + dp(52), h, A_NAV, i);
            if (on) rect(c, cxi - dp(20), top, cxi + dp(20), top + dp(2.5f), dp(1), ACCENT);
        }
    }

    // ---------------------------------------------------------------- screens
    private String shorten(String s, float maxW, float size) {
        txt.setTypeface(Typeface.DEFAULT_BOLD);
        txt.setTextSize(dp(size));
        if (txt.measureText(s) <= maxW) return s;
        String t = s;
        while (t.length() > 1 && txt.measureText(t + "…") > maxW) t = t.substring(0, t.length() - 1);
        return t + "…";
    }

    private void drawHome(Canvas c, float barH, float navTop) {
        float left = dp(28), right = w - dp(28);
        float top = barH + dp(6), bottom = navTop - dp(6);
        float ch = Math.max(dp(120), bottom - top);
        float headerH = Math.max(dp(16), ch * 0.11f);
        float cardH = Math.max(dp(52), ch * 0.38f);
        float bannerH = Math.max(dp(60), ch - headerH - cardH - dp(8));
        float cardW = cardH * 1.16f;
        float gap = dp(10), stride = cardW + gap;
        GameCatalog.Game g = sel();
        float bw = left + (right - left) * 0.60f;

        bmp(c, GameCatalog.background(getContext(), g), left, top, bw, top + bannerH, dp(16));
        rect(c, left, top, bw, top + bannerH, dp(16), 0x88060C1A);
        grad(c, left, top + bannerH * 0.40f, bw, top + bannerH, dp(16), 0x00000000, 0xF0050A16);
        outline(c, left, top, bw, top + bannerH, dp(16), alpha(g.primary, 90), 1.2f);
        float featSize = clamp(S(bannerH * 0.17f), 13f, 22f);
        float pw = Math.min(dp(108), (bw - left) * 0.36f);
        float ph = Math.min(dp(30), bannerH * 0.28f);
        float pl = bw - pw - dp(12), pt = top + bannerH - ph - dp(10);
        text(c, L("FEATURED", "ВЫБОР РЕДАКЦИИ", "ВИБІР РЕДАКЦІЇ"), left + dp(16),
                top + bannerH * 0.20f, clamp(S(bannerH * 0.11f), 7f, 9f), g.primary, true, 0);
        text(c, shorten(g.title, bw - left - pw - dp(40), featSize), left + dp(16),
                top + bannerH * 0.60f, featSize, WHITE, true, 0);
        text(c, g.genre + "  •  " + g.publisher + "  •  " + g.year, left + dp(16),
                top + bannerH * 0.80f, clamp(S(bannerH * 0.12f), 8f, 10f), DIM, false, 0);
        pill(c, pl, pt, pl + pw, pt + ph, g.primary, L("PLAY", "ИГРАТЬ", "ГРАТИ"), WHITE);
        hit(pl, pt, pl + pw, pt + ph, A_PLAY, selected);
        hit(left, top, bw - pw - dp(20), top + bannerH - ph - dp(12), A_QUICK, 7);

        float rx = bw + gap, rw = right - rx;
        float rh = (bannerH - dp(8)) / 2f;
        rect(c, rx, top, rx + rw, top + rh, dp(14), PANEL);
        outline(c, rx, top, rx + rw, top + rh, dp(14), EDGE, 1);
        text(c, L("Continue playing", "Продолжить игру", "Продовжити гру"), rx + dp(12),
                top + rh * 0.28f, clamp(S(rh * 0.22f), 8f, 10f), FAINT, true, 0);
        String last = history.isEmpty() ? g.title : gameById(history.get(0)).title;
        float lastSize = clamp(S(rh * 0.30f), 11f, 15f);
        text(c, shorten(last, rw - dp(24), lastSize), rx + dp(12), top + rh * 0.62f, lastSize, WHITE, true, 0);
        text(c, L("Tap to open", "Нажмите, чтобы открыть", "Торкніться, щоб відкрити"), rx + dp(12),
                top + rh * 0.90f, clamp(S(rh * 0.18f), 7f, 9f), ACCENT, false, 0);
        hit(rx, top, rx + rw, top + rh, A_QUICK, 0);
        float t2 = top + rh + dp(8);
        rect(c, rx, t2, rx + rw, top + bannerH, dp(14), PANEL);
        outline(c, rx, t2, rx + rw, top + bannerH, dp(14), EDGE, 1);
        text(c, L("Trophies", "Трофеи", "Трофеї"), rx + dp(12), t2 + rh * 0.28f,
                clamp(S(rh * 0.22f), 8f, 10f), FAINT, true, 0);
        text(c, trophies + " / 8", rx + dp(12), t2 + rh * 0.62f,
                clamp(S(rh * 0.36f), 13f, 18f), GOLD, true, 0);
        float barw = rw - dp(24);
        rect(c, rx + dp(12), t2 + rh * 0.82f, rx + dp(12) + barw, t2 + rh * 0.88f, dp(3), 0x33FFFFFF);
        rect(c, rx + dp(12), t2 + rh * 0.82f, rx + dp(12) + barw * (trophies / 8f), t2 + rh * 0.88f, dp(3), GOLD);
        hit(rx, t2, rx + rw, top + bannerH, A_QUICK, 1);

        String section = tab == 0
                ? L("Your games", "Ваши игры", "Ваші ігри") + "  •  " + games.size()
                : L("Media & apps", "Медиа и приложения", "Медіа та застосунки");
        float rowTop = top + bannerH + dp(6);
        float headSize = clamp(S(headerH * 0.60f), 9f, 12f);
        text(c, section, left, rowTop + headerH * 0.72f, headSize, WHITE, true, 0);
        text(c, tab == 0 ? L("Select a game, then tap again to open", "Выберите игру и нажмите ещё раз", "Виберіть гру та натисніть ще раз")
                        : L("Demo tiles — no external services", "Демо-плитки — без внешних сервисов", "Демо-плитки — без зовнішніх сервісів"),
                right, rowTop + headerH * 0.72f, clamp(S(headerH * 0.44f), 7f, 9f), FAINT, false, 2);

        float cardTop = rowTop + headerH;
        cardRowTop = cardTop; cardRowBottom = cardTop + cardH + dp(18);
        maxCardScroll = Math.max(0, (tab == 0 ? games.size() : 6) * stride - gap - (right - left));
        targetScroll = clamp(targetScroll, 0, maxCardScroll);
        c.save();
        c.clipRect(left, cardTop - dp(6), right, cardTop + cardH + dp(24));
        if (tab == 0) {
            for (int i = 0; i < games.size(); i++) {
                float x = left + i * stride - cardScroll;
                if (x > right || x + cardW < left) continue;
                GameCatalog.Game gm = games.get(i);
                float grow = i == selected ? dp(4) : 0f;
                float l = x - grow, t = cardTop - grow, r = x + cardW + grow, bt = cardTop + cardH + grow;
                if (i == selected) glow(c, (l + r) / 2f, (t + bt) / 2f, cardW, alpha(gm.primary, 70));
                bmp(c, GameCatalog.cover(getContext(), gm), l, t, r, bt, dp(10));
                grad(c, l, t + cardH * 0.55f, r, bt, dp(10), 0x00000000, 0xCC050A16);
                text(c, shorten(gm.title, cardW - dp(16), clamp(S(cardH * 0.12f), 8f, 9f)),
                        l + dp(8), bt - dp(9), clamp(S(cardH * 0.12f), 8f, 9f), WHITE, true, 0);
                if (favorites.contains(gm.id)) {
                    fill.setShader(null); fill.setStyle(Paint.Style.FILL); fill.setColor(GOLD);
                    c.drawCircle(r - dp(11), t + dp(11), dp(4), fill);
                }
                if (i == selected) outline(c, l - dp(2), t - dp(2), r + dp(2), bt + dp(2), dp(12), gm.primary, 2);
                hit(l, t, r, bt + dp(18), A_CARD, i);
            }
        } else {
            String[] names = { L("Video", "Видео", "Відео"), L("Music", "Музыка", "Музика"),
                    L("Photos", "Фото", "Фото"), L("Streaming", "Стриминг", "Стримінг"),
                    L("Captures", "Снимки", "Знімки"), L("Media Center", "Медиацентр", "Медіацентр") };
            int[] cols = { 0xFF3D6FD8, 0xFFB23D8A, 0xFF2FA88E, 0xFFD8703D, 0xFF6A5BD0, 0xFF2C7FB8 };
            for (int i = 0; i < names.length; i++) {
                float x = left + i * stride - cardScroll;
                if (x > right || x + cardW < left) continue;
                grad(c, x, cardTop, x + cardW, cardTop + cardH, dp(12), alpha(cols[i], 220), 0xFF0D1830);
                glow(c, x + cardW * 0.7f, cardTop + cardH * 0.25f, cardW * 0.6f, alpha(cols[i], 90));
                text(c, names[i], x + dp(10), cardTop + cardH - dp(12),
                        clamp(S(cardH * 0.12f), 8f, 10f), WHITE, true, 0);
                text(c, "PS5", x + dp(10), cardTop + dp(22), 12, 0xCCFFFFFF, true, 0);
                hit(x, cardTop, x + cardW, cardTop + cardH, A_MEDIA, i);
            }
        }
        c.restore();
    }

    private void drawLibrary(Canvas c, float barH, float navTop) {
        float left = dp(28), right = w - dp(28);
        float top = barH + dp(10), bottom = navTop - dp(8);
        String[] f = { L("All", "Все", "Усі"), L("Favorites", "Избранное", "Обране"),
                L("Recent", "Недавние", "Нещодавні"), L("A–Z", "А–Я", "А–Я") };
        float chipW = dp(112), chipH = dp(28);
        for (int i = 0; i < f.length; i++) {
            float l = left + i * (chipW + dp(8));
            rect(c, l, top, l + chipW, top + chipH, dp(14), libFilter == i ? alpha(ACCENT, 55) : PANEL);
            outline(c, l, top, l + chipW, top + chipH, dp(14), libFilter == i ? ACCENT : EDGE, 1);
            text(c, f[i], l + chipW / 2f, top + chipH / 2f + dp(4), 10, libFilter == i ? WHITE : DIM, true, 1);
            hit(l, top, l + chipW, top + chipH, A_FILTER, i);
        }
        filtered.clear();
        if (libFilter == 1) {
            for (GameCatalog.Game g : games) if (favorites.contains(g.id)) filtered.add(g);
        } else if (libFilter == 2) {
            for (String id : history) { GameCatalog.Game g = gameById(id); if (g != null && !filtered.contains(g)) filtered.add(g); }
            for (GameCatalog.Game g : games) if (!filtered.contains(g)) filtered.add(g);
        } else if (libFilter == 3) {
            filtered.addAll(games);
            java.util.Collections.sort(filtered, new java.util.Comparator<GameCatalog.Game>() {
                public int compare(GameCatalog.Game a, GameCatalog.Game b) { return a.title.compareToIgnoreCase(b.title); }
            });
        } else {
            filtered.addAll(games);
        }
        text(c, filtered.size() + " " + L("games", "игр", "ігор"), right, top + dp(19), 10, FAINT, false, 2);

        int cols = 6;
        float gap = dp(12);
        float gridTop = top + chipH + dp(14);
        float cw = Math.min((right - left - gap * (cols - 1)) / cols,
                Math.max(dp(48), (bottom - gridTop) * 0.52f));
        float rowH = cw + dp(24) + gap;
        maxLibScroll = Math.max(0, (float) Math.ceil(filtered.size() / (float) cols) * rowH - (bottom - gridTop));
        libraryScroll = clamp(libraryScroll, 0, maxLibScroll);
        c.save();
        c.clipRect(left, gridTop, right, bottom);
        for (int i = 0; i < filtered.size(); i++) {
            float x = left + (i % cols) * (cw + gap);
            float y = gridTop + (i / cols) * rowH - libraryScroll;
            if (y > bottom || y + rowH < gridTop) continue;
            GameCatalog.Game g = filtered.get(i);
            bmp(c, GameCatalog.cover(getContext(), g), x, y, x + cw, y + cw, dp(10));
            if (favorites.contains(g.id)) outline(c, x - dp(1), y - dp(1), x + cw + dp(1), y + cw + dp(1), dp(11), GOLD, 1.5f);
            text(c, shorten(g.title, cw, 9), x, y + cw + dp(15), 9, WHITE, true, 0);
            hit(x, y, x + cw, y + cw + dp(20), A_CARD, indexOf(g));
        }
        c.restore();
        if (filtered.isEmpty()) {
            text(c, L("No games here yet. Add favorites from game details.",
                    "Здесь пока нет игр. Добавляйте избранное в карточке игры.",
                    "Тут поки немає ігор. Додавайте обране в картці гри."),
                    w * 0.5f, gridTop + dp(60), 12, DIM, false, 1);
        }
    }

    private void drawStore(Canvas c, float barH, float navTop) {
        float left = dp(28), right = w - dp(28), top = barH + dp(14);
        float gap = dp(14), tw = (right - left - gap * 2) / 3f, th = (navTop - top) * 0.72f;
        String[] t = { L("Featured", "Рекомендуемое", "Рекомендоване"), L("New releases", "Новинки", "Новинки"),
                L("Coming soon", "Скоро", "Незабаром") };
        int[] col = { 0xFF2E6FD0, 0xFF8A4FC8, 0xFF2B9E92 };
        for (int i = 0; i < 3; i++) {
            float l = left + i * (tw + gap);
            grad(c, l, top, l + tw, top + th, dp(16), alpha(col[i], 200), 0xFF0A1226);
            glow(c, l + tw * 0.75f, top + th * 0.25f, tw * 0.7f, alpha(col[i], 110));
            text(c, t[i], l + dp(18), top + dp(34), 15, WHITE, true, 0);
            text(c, L("DEMO ONLY · NO PURCHASES", "ДЕМО · ПОКУПОК НЕТ", "ДЕМО · БЕЗ ПОКУПОК"),
                    l + dp(18), top + dp(54), 8, 0xCCE2EEFF, true, 0);
            GameCatalog.Game g = games.get((i * 7 + 3) % games.size());
            bmp(c, GameCatalog.cover(getContext(), g), l + dp(16), top + dp(70), l + tw - dp(16), top + th - dp(52), dp(12));
            text(c, shorten(g.title, tw - dp(36), 11), l + dp(18), top + th - dp(26), 11, WHITE, true, 0);
            hit(l, top, l + tw, top + th, A_STORE, i);
        }
        text(c, L("This store is a visual demo. No accounts, payments or downloads are involved.",
                "Магазин — визуальное демо: без аккаунтов, платежей и загрузок.",
                "Магазин — візуальне демо: без акаунтів, платежів і завантажень."),
                w * 0.5f, navTop - dp(18), 10, FAINT, false, 1);
    }

    private void drawGameBase(Canvas c, float barH, float navTop) {
        float left = dp(28), right = w - dp(28), top = barH + dp(14);
        float cw = (right - left) * 0.46f;
        rect(c, left, top, left + cw, navTop - dp(12), dp(16), PANEL);
        outline(c, left, top, left + cw, navTop - dp(12), dp(16), EDGE, 1);
        text(c, L("Friends", "Друзья", "Друзі"), left + dp(18), top + dp(28), 13, WHITE, true, 0);
        String[] names = { "Kaizer", "Nova_77", "Astra", "PixelFox", "Raven", "Lumen" };
        int[] cc = { 0xFF4AA8FF, 0xFFFF8A6B, 0xFF8A7BFF, 0xFF5AD8A0, 0xFFFFD166, 0xFFFF71A8 };
        for (int i = 0; i < names.length; i++) {
            float y = top + dp(48) + i * dp(32);
            fill.setShader(null); fill.setStyle(Paint.Style.FILL); fill.setColor(cc[i]);
            c.drawCircle(left + dp(34), y, dp(11), fill);
            fill.setColor(GREEN);
            c.drawCircle(left + dp(34) + dp(10), y + dp(9), dp(3.5f), fill);
            text(c, names[i], left + dp(54), y + dp(4), 11, WHITE, true, 0);
            text(c, i % 2 == 0 ? L("Online", "В сети", "У мережі") : L("In game", "В игре", "У грі"),
                    left + cw - dp(18), y + dp(4), 9, i % 2 == 0 ? GREEN : FAINT, false, 2);
            hit(left + dp(8), y - dp(14), left + cw - dp(8), y + dp(14), A_FRIEND, i);
        }
        float rx = left + cw + dp(16), rw = right - rx;
        rect(c, rx, top, right, navTop - dp(12), dp(16), PANEL);
        outline(c, rx, top, right, navTop - dp(12), dp(16), EDGE, 1);
        text(c, L("Party", "Группа", "Група"), rx + dp(18), top + dp(28), 13, WHITE, true, 0);
        text(c, L("Voice chat is not available in this offline simulator.",
                "Голосовой чат недоступен в этом офлайн-симуляторе.",
                "Голосовий чат недоступний у цьому офлайн-симуляторі."),
                rx + dp(18), top + dp(56), 10, DIM, false, 0);
        float bw = dp(170), bh = dp(34), by = top + dp(90);
        pill(c, rx + dp(18), by, rx + dp(18) + bw, by + bh, ACCENT, L("Create party", "Создать группу", "Створити групу"), WHITE);
        hit(rx + dp(18), by, rx + dp(18) + bw, by + bh, A_PARTY, 0);
        text(c, L("Messages", "Сообщения", "Повідомлення"), rx + dp(18), by + bh + dp(40), 12, WHITE, true, 0);
        String[] msgs = { "Kaizer: " + L("see you in the hub", "увидимся в хабе", "побачимось у хабі"),
                "Nova_77: " + L("nice run!", "отличный забег!", "чудовий забіг!"),
                "Astra: " + L("screenshots look great", "скриншоты классные", "скріншоти чудові") };
        for (int i = 0; i < msgs.length; i++) {
            float y = by + bh + dp(58) + i * dp(26);
            rect(c, rx + dp(18), y - dp(16), right - dp(18), y + dp(9), dp(8), 0x221E3358);
            text(c, msgs[i], rx + dp(28), y, 10, DIM, false, 0);
        }
    }

    private void drawProfile(Canvas c, float barH, float navTop) {
        float left = dp(28), right = w - dp(28), top = barH + dp(14);
        rect(c, left, top, right, navTop - dp(12), dp(18), PANEL);
        outline(c, left, top, right, navTop - dp(12), dp(18), EDGE, 1);
        float cx = left + dp(90), cy = top + dp(86);
        glow(c, cx, cy, dp(70), alpha(ACCENT2, 90));
        fill.setShader(null); fill.setStyle(Paint.Style.FILL); fill.setColor(0xFF2B4E86);
        c.drawCircle(cx, cy, dp(38), fill);
        text(c, "KZ", cx, cy + dp(11), 22, WHITE, true, 1);
        text(c, "Kaizer", left + dp(150), top + dp(62), 24, WHITE, true, 0);
        text(c, L("Level 12 · Offline profile", "Уровень 12 · Офлайн-профиль", "Рівень 12 · Офлайн-профіль"),
                left + dp(150), top + dp(84), 11, DIM, false, 0);
        rect(c, left + dp(150), top + dp(96), left + dp(340), top + dp(102), dp(3), 0x33FFFFFF);
        rect(c, left + dp(150), top + dp(96), left + dp(150) + dp(190) * 0.62f, top + dp(102), dp(3), ACCENT);

        String[] labels = { L("Games played", "Запусков игр", "Запусків ігор"), L("Trophies", "Трофеи", "Трофеї"),
                L("Sessions", "Сессии", "Сеанси"), L("Hours (demo)", "Часы (демо)", "Години (демо)") };
        String[] vals = { String.valueOf(playCount), trophies + " / 8", String.valueOf(Math.max(1, playCount)),
                String.format(Locale.US, "%.1f", playCount * 0.12f) };
        float cw = (right - left - dp(40) - dp(12) * 3) / 4f;
        for (int i = 0; i < 4; i++) {
            float x = left + dp(20) + i * (cw + dp(12)), y = top + dp(150);
            rect(c, x, y, x + cw, y + dp(62), dp(12), 0x221A2C4A);
            text(c, vals[i], x + dp(14), y + dp(32), 20, WHITE, true, 0);
            text(c, labels[i], x + dp(14), y + dp(51), 9, DIM, false, 0);
        }
        String[] btns = { L("Trophies", "Трофеи", "Трофеї"), L("Notifications", "Уведомления", "Сповіщення"),
                L("Settings", "Настройки", "Налаштування"), L("Media Center", "Медиацентр", "Медіацентр") };
        int[] acts = { 1, 3, 2, 4 };
        float bw = Math.min(dp(160), (right - left - dp(40) - dp(36)) / 4f), bh = dp(36), by = navTop - dp(64);
        for (int i = 0; i < btns.length; i++) {
            float x = left + dp(20) + i * (bw + dp(12));
            pill(c, x, by, x + bw, by + bh, 0xFF24406E, btns[i], WHITE);
            hit(x, by, x + bw, by + bh, A_QUICK, acts[i]);
        }
    }

    private void drawSettings(Canvas c, float barH, float navTop) {
        float left = dp(28), right = w - dp(28), top = barH + dp(12);
        String[] keys = { L("Language", "Язык", "Мова"), L("Interface sounds", "Звуки интерфейса", "Звуки інтерфейсу"),
                L("Animations", "Анимации", "Анімації"), L("Dynamic background", "Динамический фон", "Динамічний фон"),
                L("UI scale", "Масштаб интерфейса", "Масштаб інтерфейсу"),
                L("Reset progress", "Сбросить прогресс", "Скинути прогрес") };
        String[] vals = {
                lang == 0 ? "English" : (lang == 1 ? L("Русский", "Русский", "Російська") : L("Українська", "Українська", "Українська")),
                soundOn ? L("On", "Вкл", "Увімк") : L("Off", "Выкл", "Вимк"),
                animOn ? L("On", "Вкл", "Увімк") : L("Off", "Выкл", "Вимк"),
                dynBg ? L("On", "Вкл", "Увімк") : L("Off", "Выкл", "Вимк"),
                uiScale < 0.95f ? L("Small", "Маленький", "Малий") : (uiScale > 1.05f ? L("Large", "Крупный", "Великий") : L("Normal", "Обычный", "Звичайний")),
                L("Tap to reset", "Нажмите для сброса", "Натисніть для скидання")
        };
        float rh = Math.min(dp(52), (navTop - top - dp(20)) / 6f);
        for (int i = 0; i < keys.length; i++) {
            float y = top + i * (rh + dp(6));
            rect(c, left, y, right, y + rh, dp(12), PANEL);
            outline(c, left, y, right, y + rh, dp(12), EDGE, 1);
            text(c, keys[i], left + dp(18), y + rh / 2f + dp(5), 12, WHITE, true, 0);
            float vw = dp(160);
            rect(c, right - vw - dp(18), y + dp(9), right - dp(18), y + rh - dp(9), dp(9),
                    i == 5 ? alpha(RED, 40) : alpha(ACCENT, 42));
            text(c, vals[i], right - dp(18) - vw / 2f, y + rh / 2f + dp(4.5f), 10, i == 5 ? RED : WHITE, true, 1);
            hit(left, y, right, y + rh, A_SETTING, i);
        }
        text(c, L("All settings are saved locally on the device.",
                "Все настройки сохраняются локально на устройстве.",
                "Усі налаштування зберігаються локально на пристрої."),
                w * 0.5f, navTop - dp(14), 9, FAINT, false, 1);
    }

    private void drawTrophies(Canvas c, float barH, float navTop) {
        float left = dp(28), right = w - dp(28), top = barH + dp(12);
        String[] names = { L("First Boot", "Первый запуск", "Перший запуск"), L("Game On", "Игра началась", "Гра почалася"),
                L("Explorer", "Исследователь", "Дослідник"), L("Collector", "Коллекционер", "Колекціонер"),
                L("Night Owl", "Ночной игрок", "Нічний гравець"), L("Marathon", "Марафон", "Марафон"),
                L("Curator", "Куратор", "Куратор"), L("Legend", "Легенда", "Легенда") };
        String[] desc = { L("Start the simulator", "Запустить симулятор", "Запустити симулятор"),
                L("Launch your first game", "Запустить первую игру", "Запустити першу гру"),
                L("Open 5 game pages", "Открыть 5 карточек игр", "Відкрити 5 карток ігор"),
                L("Add a favorite", "Добавить в избранное", "Додати в обране"),
                L("Play after midnight", "Играть после полуночі", "Грати після півночі"),
                L("Complete 10 sessions", "Завершить 10 сеансов", "Завершити 10 сеансів"),
                L("Visit the Media Center", "Посетить медиацентр", "Відвідати медіацентр"),
                L("Unlock every trophy", "Открыть все трофеи", "Відкрити всі трофеї") };
        float rh = Math.min(dp(50), (navTop - top - dp(10)) / 8f);
        for (int i = 0; i < 8; i++) {
            float y = top + i * (rh + dp(4));
            boolean got = i < trophies;
            rect(c, left, y, right, y + rh, dp(12), PANEL);
            outline(c, left, y, right, y + rh, dp(12), got ? alpha(GOLD, 90) : EDGE, 1);
            glow(c, left + dp(30), y + rh / 2f, dp(22), alpha(got ? GOLD : FAINT, got ? 100 : 25));
            fill.setShader(null); fill.setStyle(Paint.Style.FILL); fill.setColor(got ? GOLD : 0xFF40506E);
            c.drawCircle(left + dp(30), y + rh / 2f, dp(12), fill);
            text(c, got ? "★" : "?", left + dp(30), y + rh / 2f + dp(5), 12, got ? 0xFF3A2A00 : 0xFF1A2438, true, 1);
            text(c, names[i], left + dp(56), y + rh / 2f - dp(2), 12, got ? WHITE : DIM, true, 0);
            text(c, desc[i], left + dp(56), y + rh / 2f + dp(14), 9, FAINT, false, 0);
            text(c, got ? L("UNLOCKED", "ПОЛУЧЕН", "ОТРИМАНО") : L("LOCKED", "ЗАКРЫТ", "ЗАЧИНЕНО"),
                    right - dp(18), y + rh / 2f + dp(4), 9, got ? GOLD : FAINT, true, 2);
        }
    }

    private void drawNotifications(Canvas c, float barH, float navTop) {
        float left = dp(28), right = w - dp(28), top = barH + dp(12);
        float bw = dp(140), bh = dp(30);
        pill(c, right - bw, top, right, top + bh, 0xFF24406E, L("Clear all", "Очистить всё", "Очистити все"), WHITE);
        hit(right - bw, top, right, top + bh, A_CLEAR, 0);
        float y0 = top + bh + dp(12);
        float rh = dp(42);
        for (int i = 0; i < notes.size() && i < 10; i++) {
            float y = y0 + i * (rh + dp(6));
            if (y + rh > navTop - dp(8)) break;
            rect(c, left, y, right, y + rh, dp(10), PANEL);
            outline(c, left, y, right, y + rh, dp(10), EDGE, 1);
            fill.setShader(null); fill.setStyle(Paint.Style.FILL); fill.setColor(i == 0 ? ACCENT : 0xFF40506E);
            c.drawCircle(left + dp(22), y + rh / 2f, dp(6), fill);
            text(c, notes.get(i), left + dp(42), y + rh / 2f + dp(4), 11, WHITE, false, 0);
            text(c, i == 0 ? L("now", "сейчас", "зараз") : L("earlier", "ранее", "раніше"),
                    right - dp(16), y + rh / 2f + dp(4), 9, FAINT, false, 2);
        }
        if (notes.isEmpty()) text(c, L("No notifications", "Уведомлений нет", "Сповіщень немає"),
                w * 0.5f, y0 + dp(50), 12, DIM, false, 1);
    }

    private void drawDetails(Canvas c, float barH, float navTop) {
        GameCatalog.Game g = cur();
        float left = dp(28), right = w - dp(28), top = barH + dp(12), bottom = navTop - dp(10);
        Bitmap bg = GameCatalog.background(getContext(), g);
        if (bg != null) {
            fill.setAlpha(70);
            c.drawBitmap(bg, new Rect(0, 0, bg.getWidth(), bg.getHeight()), new RectF(0, top, w, bottom), fill);
            fill.setAlpha(255);
        }
        rect(c, left, top, right, bottom, dp(18), 0xDD091228);
        outline(c, left, top, right, bottom, dp(18), alpha(g.primary, 90), 1.2f);
        float unit = Math.max(dp(140), bottom - top);
        float cs = Math.min(dp(190), unit * 0.60f);
        bmp(c, GameCatalog.cover(getContext(), g), left + dp(20), top + dp(20), left + dp(20) + cs, top + dp(20) + cs, dp(14));
        float infoX = left + cs + dp(40);
        text(c, shorten(g.title, right - infoX - dp(20), 24), infoX, top + unit * 0.20f, 24, WHITE, true, 0);
        text(c, g.genre + "  •  " + g.publisher + "  •  " + g.year + "  •  "
                        + String.format(Locale.US, "%.1f GB", g.size / 1024f),
                infoX, top + unit * 0.32f, 10, DIM, false, 0);
        wrapped(c, g.description, infoX, top + unit * 0.43f,
                Math.max(dp(120), right - infoX - dp(24)), 11, DIM, 16);
        boolean fav = favorites.contains(g.id);
        float bw = Math.min(dp(132), (right - infoX - dp(12)) / 2f), bh = Math.min(dp(36), unit * 0.20f), by = top + unit * 0.68f;
        pill(c, infoX, by, infoX + bw, by + bh, g.primary,
                L("PLAY", "ИГРАТЬ", "ГРАТИ"), WHITE);
        hit(infoX, by, infoX + bw, by + bh, A_PLAY, current);
        float fx = infoX + bw + dp(12);
        pill(c, fx, by, fx + bw, by + bh, fav ? GOLD : 0xFF24406E,
                fav ? L("★ Favorite", "★ В избранном", "★ В обраному") : L("☆ Add favorite", "☆ В избранное", "☆ В обране"), WHITE);
        hit(fx, by, fx + bw, by + bh, A_FAV, current);
        float by2 = by + bh + dp(12);
        pill(c, infoX, by2, infoX + bw, by2 + bh, 0xFF24406E,
                L("Back", "Назад", "Назад"), WHITE);
        hit(infoX, by2, infoX + bw, by2 + bh, A_BACK, 0);

        float sy = top + dp(20) + cs + dp(20);
        if (sy + dp(90) < bottom) {
            text(c, L("Screenshots · concept art", "Скриншоты · концепт-арт", "Скріншоти · концепт-арт"),
                    left + dp(20), sy + dp(12), 10, FAINT, true, 0);
            float sw = Math.min(dp(180), (right - left - dp(60)) / 3f);
            for (int i = 0; i < 3; i++) {
                float x = left + dp(20) + i * (sw + dp(12));
                bmp(c, GameCatalog.shot(getContext(), g, i + 1), x, sy + dp(20), x + sw, sy + dp(20) + sw * 0.56f, dp(10));
                hit(x, sy + dp(20), x + sw, sy + dp(20) + sw * 0.56f, A_STORE, 10 + i);
            }
        }
    }

    private void drawMediaCenter(Canvas c, float barH, float navTop) {
        GameCatalog.Game g = cur();
        float left = dp(28), right = w - dp(28), top = barH + dp(12);
        text(c, g.title + "  ·  " + L("concept gallery", "галерея концептов", "галерея концептів"),
                left, top + dp(16), 12, WHITE, true, 0);
        float gap = dp(12), tw = (right - left - gap * 2) / 3f, th = (navTop - top - dp(40)) * 0.58f;
        for (int i = 0; i < 3; i++) {
            float x = left + i * (tw + gap);
            bmp(c, GameCatalog.shot(getContext(), g, i + 1), x, top + dp(30), x + tw, top + dp(30) + th, dp(14));
            outline(c, x, top + dp(30), x + tw, top + dp(30) + th, dp(14), EDGE, 1);
            text(c, L("Capture", "Снимок", "Знімок") + " " + (i + 1), x + dp(12), top + dp(30) + th - dp(14), 10, WHITE, true, 0);
            hit(x, top + dp(30), x + tw, top + dp(30) + th, A_STORE, 10 + i);
        }
        bmp(c, GameCatalog.background(getContext(), g), left, top + dp(42) + th, right,
                top + dp(42) + th + (navTop - top - dp(40)) * 0.32f, dp(14));
        text(c, L("SIMULATOR CONCEPT ART · no copyrighted media bundled",
                "КОНЦЕПТ-АРТ СИМУЛЯТОРА · без материалов правообладателей",
                "КОНЦЕПТ-АРТ СИМУЛЯТОРА · без матеріалів правовласників"),
                w * 0.5f, navTop - dp(14), 9, FAINT, false, 1);
    }

    private void drawLaunch(Canvas c, long now) {
        GameCatalog.Game g = cur();
        rect(c, 0, 0, w, h, 0, 0xFF040814);
        Bitmap b = GameCatalog.background(getContext(), g);
        if (b != null) {
            fill.setAlpha(95);
            c.drawBitmap(b, new Rect(0, 0, b.getWidth(), b.getHeight()), new RectF(0, 0, w, h), fill);
            fill.setAlpha(255);
        }
        rect(c, 0, 0, w, h, 0, 0x88050A16);
        float p = clamp((now - launchStart) / 4700f, 0f, 1f);
        glow(c, w * 0.5f, h * 0.42f, h * 0.55f, alpha(g.primary, 60));
        text(c, g.initials(), w * 0.5f, h * 0.34f, 52, WHITE, true, 1);
        text(c, shorten(g.title, w * 0.7f, 24), w * 0.5f, h * 0.45f, 24, WHITE, true, 1);
        String phase = p < 0.22f ? L("Starting application…", "Запуск приложения…", "Запуск застосунку…")
                : p < 0.5f ? L("Loading assets…", "Загрузка ресурсов…", "Завантаження ресурсів…")
                : p < 0.78f ? L("Preparing experience…", "Подготовка…", "Підготовка…")
                : L("Almost ready", "Почти готово", "Майже готово");
        text(c, phase, w * 0.5f, h * 0.55f, 12, DIM, false, 1);
        float cx = w * 0.5f, cy = h * 0.67f, r = dp(17);
        stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(dp(3)); stroke.setColor(0x33FFFFFF);
        c.drawCircle(cx, cy, r, stroke);
        stroke.setColor(WHITE);
        float sweep = animOn ? (float) (anim / 3.0 % 360.0) : 210f;
        c.drawArc(new RectF(cx - r, cy - r, cx + r, cy + r), sweep, 95, false, stroke);
        float bw = w * 0.34f, bl = w * 0.5f - bw / 2f, by = h * 0.79f, bh = dp(5);
        rect(c, bl, by, bl + bw, by + bh, dp(3), 0x33FFFFFF);
        if (p > 0.001f) grad(c, bl, by, bl + bw * p, by + bh, dp(3), g.primary, ACCENT);
        text(c, (int) (p * 100) + "%", w * 0.5f, by + dp(24), 10, WHITE, true, 1);
        text(c, L("Tap anywhere to skip", "Нажмите в любом месте, чтобы пропустить",
                "Торкніться будь-де, щоб пропустити"), w * 0.5f, h * 0.93f, 9, FAINT, false, 1);
        hit(0, 0, w, h, A_LAUNCH_SKIP, 0);
    }

    private void drawControlCenter(Canvas c) {
        float panelH = Math.min(h * 0.80f, dp(292));
        float p = ccProgress;
        float off = -panelH * (1f - p);
        rect(c, 0, 0, w, h, 0, Color.argb((int) (140 * p), 2, 6, 14));
        hit(0, 0, w, h, A_CC_CLOSE, 0);
        float l = dp(18), r = w - dp(18), t = dp(10), b = panelH - dp(10);
        rect(c, l, t + off, r, b + off, dp(20), PANEL2);
        outline(c, l, t + off, r, b + off, dp(20), EDGE, 1);
        text(c, L("Control Center", "Центр управления", "Центр керування"),
                w * 0.5f, t + dp(30) + off, 15, WHITE, true, 1);
        text(c, clockFmt.format(new Date()) + "  ·  " + L("Offline", "Офлайн", "Офлайн"),
                w * 0.5f, t + dp(48) + off, 9, FAINT, false, 1);

        String[] labels = { "Wi-Fi", "Bluetooth", L("Sound", "Звук", "Звук"),
                L("Animations", "Анимации", "Анімації"), L("Dynamic BG", "Динам. фон", "Динамічний фон"),
                L("Brightness", "Яркость", "Яскравість") };
        boolean[] states = { wifi, bt, soundOn, animOn, dynBg, brightness > 50 };
        float cols = 3f, gx = dp(12), gy = dp(10);
        float tw = (r - l - dp(40) - gx * (cols - 1)) / cols;
        float th = dp(50);
        float baseY = t + dp(62) + off;
        for (int i = 0; i < 6; i++) {
            float x = l + dp(20) + (i % 3) * (tw + gx);
            float y = baseY + (i / 3) * (th + gy);
            rect(c, x, y, x + tw, y + th, dp(12), states[i] ? alpha(ACCENT, 60) : 0x221A2C4A);
            outline(c, x, y, x + tw, y + th, dp(12), states[i] ? ACCENT : EDGE, 1);
            text(c, labels[i], x + dp(14), y + dp(21), 10, WHITE, true, 0);
            text(c, states[i] ? L("On", "Вкл", "Увімк") : L("Off", "Выкл", "Вимк"),
                    x + dp(14), y + dp(38), 9, states[i] ? ACCENT : FAINT, false, 0);
            hit(x, y, x + tw, y + th, A_CC_TOGGLE, i);
        }
        float rowY = baseY + 2 * (th + gy) + dp(8);
        float half = (r - l - dp(40) - gx) / 2f;
        String langName = lang == 0 ? "English" : (lang == 1 ? L("Русский", "Русский", "Російська") : L("Українська", "Українська", "Українська"));
        String scaleName = uiScale < 0.95f ? L("Small", "Маленький", "Малий") : (uiScale > 1.05f ? L("Large", "Крупный", "Великий") : L("Normal", "Обычный", "Звичайний"));
        float bx = l + dp(20);
        rect(c, bx, rowY, bx + half, rowY + dp(40), dp(12), 0x221A2C4A);
        text(c, L("Language", "Язык", "Мова") + ": " + langName, bx + dp(14), rowY + dp(25), 10, WHITE, true, 0);
        hit(bx, rowY, bx + half, rowY + dp(40), A_CC_CYCLE, 0);
        float bx2 = bx + half + gx;
        rect(c, bx2, rowY, bx2 + half, rowY + dp(40), dp(12), 0x221A2C4A);
        text(c, L("UI scale", "Масштаб", "Масштаб") + ": " + scaleName, bx2 + dp(14), rowY + dp(25), 10, WHITE, true, 0);
        hit(bx2, rowY, bx2 + half, rowY + dp(40), A_CC_CYCLE, 1);

        float qy = rowY + dp(50);
        float qw = (r - l - dp(40) - gx * 2) / 3f;
        String[] qs = { L("Notifications", "Уведомления", "Сповіщення"), L("Settings", "Настройки", "Налаштування"),
                L("Close", "Закрыть", "Закрити") };
        int[] qa = { 3, 2, -1 };
        for (int i = 0; i < 3; i++) {
            float x = l + dp(20) + i * (qw + gx);
            pill(c, x, qy, x + qw, qy + dp(34), i == 2 ? 0xFF3A2740 : 0xFF24406E, qs[i], WHITE);
            hit(x, qy, x + qw, qy + dp(34), qa[i] < 0 ? A_CC_CLOSE : A_QUICK, qa[i] < 0 ? 0 : qa[i]);
        }
    }

    private void drawToast(Canvas c, long now) {
        if (toastText == null) return;
        long left = toastUntil - now;
        if (left <= 0L) { toastText = null; return; }
        float a = clamp(left / 320f, 0f, 1f);
        float tw = Math.min(w * 0.62f, dp(420)), th = dp(44);
        float l = w * 0.5f - tw / 2f, t = dp(64);
        rect(c, l, t, l + tw, t + th, dp(12), Color.argb((int) (230 * a), 14, 24, 44));
        outline(c, l, t, l + tw, t + th, dp(12), alpha(ACCENT, (int) (130 * a)), 1);
        fill.setShader(null); fill.setStyle(Paint.Style.FILL); fill.setColor(alpha(ACCENT, (int) (255 * a)));
        c.drawCircle(l + dp(18), t + th / 2f, dp(5), fill);
        text(c, shorten(toastText, tw - dp(44), 11), l + dp(32), t + th / 2f + dp(4), 11, WHITE, true, 0);
    }

    // ----------------------------------------------------------------- input
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            downX = x; downY = y; downT = System.currentTimeMillis();
            dragStartScroll = cardScroll; libDragStart = libraryScroll;
            dragging = false;
            swipingCc = y < dp(44) && screen != S_LAUNCH;
            if (screen == S_HOME && tab == 0 && y >= cardRowTop - dp(8) && y <= cardRowBottom + dp(8)) dragMode = 1;
            else if (screen == S_LIBRARY) dragMode = 2;
            else dragMode = 0;
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            float dx = x - downX, dy = y - downY;
            if (Math.abs(dx) > dp(8) || Math.abs(dy) > dp(8)) dragging = true;
            if (swipingCc && dy > dp(46) && !ccOpen) {
                ccOpen = true; swipingCc = false; play(0);
            }
            if (dragMode == 1 && Math.abs(dx) > Math.abs(dy)) {
                cardScroll = clamp(dragStartScroll - dx, 0, Math.max(1f, maxCardScroll));
                targetScroll = cardScroll;
            } else if (dragMode == 2 && Math.abs(dy) > Math.abs(dx)) {
                libraryScroll = clamp(libDragStart - dy, 0, Math.max(1f, maxLibScroll));
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            boolean tap = !dragging && System.currentTimeMillis() - downT < 700L;
            if (tap && action == MotionEvent.ACTION_UP) handleTap(x, y);
            else if (dragMode == 1 && !tap) {
                float stride = dp(112) + dp(12);
                targetScroll = clamp((float) Math.round(cardScroll / stride) * stride, 0, Math.max(0f, maxCardScroll));
            }
            dragMode = 0; swipingCc = false;
            return true;
        }
        return true;
    }

    private void handleTap(float x, float y) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (hit.r.contains(x, y)) {
                act(hit.a, hit.g);
                return;
            }
        }
    }

    private void act(int a, int arg) {
        switch (a) {
            case A_CARD: {
                long now = System.currentTimeMillis();
                if (selected == arg && now - lastCardTap < 750L) {
                    current = arg; screen = S_DETAILS; play(1);
                } else {
                    selected = arg; current = arg; play(0);
                    float stride = dp(112) + dp(12);
                    targetScroll = clamp(arg * stride - (w - dp(56)) / 2f + dp(56), 0, Math.max(0f, maxCardScroll));
                }
                lastCardTap = now;
                break;
            }
            case A_TAB: tab = arg; play(0); break;
            case A_NAV: {
                int[] s = { S_HOME, S_LIBRARY, S_STORE, S_GAMEBASE, S_PROFILE };
                screen = s[arg]; tab = 0; play(0);
                break;
            }
            case A_PLAY: current = arg; startLaunch(); break;
            case A_FAV: {
                String id = games.get(Math.max(0, Math.min(games.size() - 1, arg))).id;
                if (favorites.contains(id)) favorites.remove(id); else favorites.add(id);
                save(); play(1);
                toast(L("Favorites updated", "Избранное обновлено", "Обране оновлено"), 1600);
                break;
            }
            case A_BACK: screen = S_HOME; tab = 0; play(2); break;
            case A_FILTER: libFilter = arg; libraryScroll = 0f; play(0); break;
            case A_SETTING: {
                if (arg == 0) lang = (lang + 1) % 3;
                else if (arg == 1) soundOn = !soundOn;
                else if (arg == 2) animOn = !animOn;
                else if (arg == 3) dynBg = !dynBg;
                else if (arg == 4) uiScale = uiScale < 0.95f ? 1.0f : (uiScale > 1.05f ? 0.85f : 1.15f);
                else {
                    playCount = 0; trophies = 0; history.clear(); notes.clear(); favorites.clear();
                    toast(L("Progress reset", "Прогресс сброшен", "Прогрес скинуто"), 2000);
                    save(); play(2); return;
                }
                save(); play(0);
                break;
            }
            case A_CC_TOGGLE: {
                if (arg == 0) wifi = !wifi;
                else if (arg == 1) bt = !bt;
                else if (arg == 2) soundOn = !soundOn;
                else if (arg == 3) animOn = !animOn;
                else if (arg == 4) dynBg = !dynBg;
                else brightness = brightness > 50 ? 30 : 70;
                save(); play(0);
                break;
            }
            case A_CC_CYCLE: {
                if (arg == 0) lang = (lang + 1) % 3;
                else uiScale = uiScale < 0.95f ? 1.0f : (uiScale > 1.05f ? 0.85f : 1.15f);
                save(); play(0);
                break;
            }
            case A_CC_OPEN: ccOpen = true; play(0); break;
            case A_CC_CLOSE: ccOpen = false; play(2); break;
            case A_MEDIA:
                if (arg == 5) { screen = S_MEDIA; play(1); }
                else { tab = 1; toast(L("Demo tile — offline content only", "Демо-плитка — только офлайн-контент",
                        "Демо-плитка — лише офлайн-контент"), 1800); play(0); }
                break;
            case A_QUICK: {
                if (arg == 0) {
                    String id = history.isEmpty() ? sel().id : history.get(0);
                    current = Math.max(0, indexOf(gameById(id))); screen = S_DETAILS;
                } else if (arg == 1) screen = S_TROPHIES;
                else if (arg == 2) screen = S_SETTINGS;
                else if (arg == 3) screen = S_NOTIFICATIONS;
                else if (arg == 4) screen = S_MEDIA;
                else if (arg == 5) screen = S_GAMEBASE;
                else if (arg == 6) screen = S_STORE;
                else if (arg == 7) { current = selected; screen = S_DETAILS; }
                ccOpen = false; play(1);
                break;
            }
            case A_LAUNCH_SKIP: finishLaunch(); break;
            case A_CLEAR: notes.clear(); save(); toast(L("Notifications cleared", "Уведомления очищены",
                    "Сповіщення очищено"), 1600); play(2); break;
            case A_FRIEND: toast(L("Offline friend list (demo)", "Офлайн-список друзей (демо)",
                    "Офлайн-список друзів (демо)"), 1600); play(0); break;
            case A_PARTY: toast(L("Party created (demo)", "Группа создана (демо)", "Групу створено (демо)"), 1800); play(1); break;
            case A_STORE: toast(L("Demo content — no purchases", "Демо-контент — покупок нет",
                    "Демо-контент — покупок немає"), 1700); play(0); break;
            default: play(0); break;
        }
    }

    private long lastCardTap = 0L;

    public boolean onBack() {
        if (ccOpen) { ccOpen = false; play(2); return true; }
        if (screen == S_LAUNCH) { finishLaunch(); return true; }
        if (screen != S_HOME) { screen = S_HOME; tab = 0; play(2); return true; }
        return false;
    }

    private void startLaunch() {
        screen = S_LAUNCH;
        launchStart = System.currentTimeMillis();
        launchDone = false;
        ccOpen = false;
        play(3);
    }

    private void finishLaunch() {
        if (launchDone) return;
        launchDone = true;
        screen = S_HOME; tab = 0;
        playCount++;
        trophies = Math.min(8, trophies + 1);
        GameCatalog.Game g = cur();
        history.remove(g.id);
        history.add(0, g.id);
        notes.add(0, L("You played ", "Вы играли в ", "Ви грали в ") + g.title);
        if (notes.size() > 12) notes.remove(notes.size() - 1);
        save();
        toast(L("Session complete · trophy unlocked", "Сеанс завершён · трофей получен",
                "Сеанс завершено · трофей отримано"), 2600);
        play(1);
    }

    // ------------------------------------------------------------- persistence
    private static ArrayList<String> parseList(String s) {
        ArrayList<String> a = new ArrayList<String>();
        if (s == null || s.length() == 0) return a;
        String[] parts = s.split("\n");
        for (String p : parts) if (p.length() > 0) a.add(p);
        return a;
    }

    private static String joinList(ArrayList<String> a) {
        StringBuilder b = new StringBuilder();
        for (String s : a) { if (b.length() > 0) b.append("\n"); b.append(s); }
        return b.toString();
    }

    private static HashSet<String> parseSet(String s) {
        HashSet<String> a = new HashSet<String>();
        if (s == null || s.length() == 0) return a;
        for (String p : s.split(",")) if (p.length() > 0) a.add(p);
        return a;
    }

    private static String joinSet(HashSet<String> a) {
        StringBuilder b = new StringBuilder();
        for (String s : a) { if (b.length() > 0) b.append(","); b.append(s); }
        return b.toString();
    }

    private void loadPrefs() {
        lang = prefs.getInt("lang", 1);
        soundOn = prefs.getBoolean("sound", true);
        animOn = prefs.getBoolean("anim", true);
        dynBg = prefs.getBoolean("dynbg", true);
        uiScale = prefs.getFloat("scale", 1.0f);
        playCount = prefs.getInt("plays", 0);
        trophies = prefs.getInt("trophies", 0);
        brightness = prefs.getInt("bright", 70);
        history.addAll(parseList(prefs.getString("history", "")));
        notes.addAll(parseList(prefs.getString("notes", "")));
        favorites.addAll(parseSet(prefs.getString("favorites", "")));
    }

    private void save() {
        SharedPreferences.Editor e = prefs.edit();
        e.putInt("lang", lang);
        e.putBoolean("sound", soundOn);
        e.putBoolean("anim", animOn);
        e.putBoolean("dynbg", dynBg);
        e.putFloat("scale", uiScale);
        e.putInt("plays", playCount);
        e.putInt("trophies", trophies);
        e.putInt("bright", brightness);
        e.putString("history", joinList(history));
        e.putString("notes", joinList(notes));
        e.putString("favorites", joinSet(favorites));
        e.apply();
    }

    private GameCatalog.Game gameById(String id) {
        for (GameCatalog.Game g : games) if (g.id.equals(id)) return g;
        return games.isEmpty() ? null : games.get(0);
    }

    private int indexOf(GameCatalog.Game g) {
        if (g == null) return 0;
        for (int i = 0; i < games.size(); i++) if (games.get(i) == g) return i;
        return 0;
    }

    private static final class Hit {
        final RectF r;
        final int a, g;
        Hit(RectF r, int a, int g) { this.r = r; this.a = a; this.g = g; }
    }
}
