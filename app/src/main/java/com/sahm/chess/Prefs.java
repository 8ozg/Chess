package com.sahm.chess;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * كل الإعدادات القابلة للتخصيص بمكان واحد، محفوظة محليًا (SharedPreferences)
 * بدون أي اتصال إنترنت. أي شاشة بالتطبيق تقرأ منها مباشرة.
 */
public final class Prefs {
    static final String FILE = "sahm_chess_prefs";

    // ---- ثيمات ألوان الرقعة: {اسم عربي, لون فاتح, لون غامق} ----
    public static final Object[][] BOARD_THEMES = {
            {"كلاسيك أخضر", Color.rgb(238, 217, 181), Color.rgb(115, 149, 82)},
            {"خشبي", Color.rgb(222, 184, 135), Color.rgb(139, 90, 43)},
            {"أزرق محيطي", Color.rgb(222, 235, 247), Color.rgb(70, 130, 180)},
            {"رمادي أنيق", Color.rgb(225, 225, 225), Color.rgb(105, 105, 105)},
            {"بنفسجي", Color.rgb(232, 222, 248), Color.rgb(120, 80, 168)}
    };
    public static final String[] PIECE_STYLES = {"كلاسيك", "بولد (محدد)"};

    static SharedPreferences p(Context c) { return c.getSharedPreferences(FILE, Context.MODE_PRIVATE); }

    public static int boardTheme(Context c) { return p(c).getInt("boardTheme", 0); }
    public static void setBoardTheme(Context c, int v) { p(c).edit().putInt("boardTheme", v).apply(); }

    public static int pieceStyle(Context c) { return p(c).getInt("pieceStyle", 0); }
    public static void setPieceStyle(Context c, int v) { p(c).edit().putInt("pieceStyle", v).apply(); }

    /** نسبة حجم الرقعة: 0=صغير(0.8) 1=متوسط(1.0) 2=كبير(1.15) */
    public static int boardSizeIndex(Context c) { return p(c).getInt("boardSize", 1); }
    public static void setBoardSizeIndex(Context c, int v) { p(c).edit().putInt("boardSize", v).apply(); }
    public static float boardSizeFactor(Context c) {
        switch (boardSizeIndex(c)) { case 0: return 0.82f; case 2: return 1.0f; default: return 0.92f; }
    }

    public static boolean showCoords(Context c) { return p(c).getBoolean("showCoords", true); }
    public static void setShowCoords(Context c, boolean v) { p(c).edit().putBoolean("showCoords", v).apply(); }

    public static boolean showLegalMoves(Context c) { return p(c).getBoolean("showLegalMoves", true); }
    public static void setShowLegalMoves(Context c, boolean v) { p(c).edit().putBoolean("showLegalMoves", v).apply(); }

    public static boolean highlightLastMove(Context c) { return p(c).getBoolean("highlightLastMove", true); }
    public static void setHighlightLastMove(Context c, boolean v) { p(c).edit().putBoolean("highlightLastMove", v).apply(); }

    public static boolean animate(Context c) { return p(c).getBoolean("animate", true); }
    public static void setAnimate(Context c, boolean v) { p(c).edit().putBoolean("animate", v).apply(); }

    public static boolean sound(Context c) { return p(c).getBoolean("sound", true); }
    public static void setSound(Context c, boolean v) { p(c).edit().putBoolean("sound", v).apply(); }

    public static boolean confirmMoves(Context c) { return p(c).getBoolean("confirmMoves", false); }
    public static void setConfirmMoves(Context c, boolean v) { p(c).edit().putBoolean("confirmMoves", v).apply(); }

    public static boolean autoQueen(Context c) { return p(c).getBoolean("autoQueen", true); }
    public static void setAutoQueen(Context c, boolean v) { p(c).edit().putBoolean("autoQueen", v).apply(); }

    public static boolean darkMode(Context c) { return p(c).getBoolean("darkMode", true); }
    public static void setDarkMode(Context c, boolean v) { p(c).edit().putBoolean("darkMode", v).apply(); }

    public static void saveGamePgn(Context c, String pgn) { p(c).edit().putString("savedGame", pgn).apply(); }
    public static String loadGamePgn(Context c) { return p(c).getString("savedGame", null); }

    // ---- ألوان الواجهة العامة (تتبدل حسب داكن/فاتح) ----
    public static int bg(Context c) { return darkMode(c) ? Color.rgb(16, 19, 26) : Color.rgb(246, 247, 250); }
    public static int card(Context c) { return darkMode(c) ? Color.rgb(27, 32, 42) : Color.rgb(255, 255, 255); }
    public static int text(Context c) { return darkMode(c) ? Color.WHITE : Color.rgb(20, 22, 28); }
    public static int subtext(Context c) { return darkMode(c) ? Color.LTGRAY : Color.DKGRAY; }
    public static int accent(Context c) { return Color.rgb(217, 164, 65); }
}
