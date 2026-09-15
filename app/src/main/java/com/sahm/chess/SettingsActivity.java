package com.sahm.chess;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

public class SettingsActivity extends Activity {
    LinearLayout root;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        render();
    }

    void render() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 28, 28, 60);
        root.setBackgroundColor(Prefs.bg(this));

        addTitle("⚙️ الإعدادات");

        addSection("الرقعة");
        addBoardThemeRow();
        addPieceStyleRow();
        addBoardSizeRow();
        addSwitch("إظهار إحداثيات الرقعة", Prefs.showCoords(this), v -> Prefs.setShowCoords(this, v));
        addSwitch("إظهار النقلات القانونية", Prefs.showLegalMoves(this), v -> Prefs.setShowLegalMoves(this, v));
        addSwitch("تمييز آخر نقلة", Prefs.highlightLastMove(this), v -> Prefs.setHighlightLastMove(this, v));
        addSwitch("تحريك القطع (Animation)", Prefs.animate(this), v -> Prefs.setAnimate(this, v));

        addSection("الصوت");
        addSwitch("أصوات النقلات", Prefs.sound(this), v -> Prefs.setSound(this, v));

        addSection("اللعب");
        addSwitch("تأكيد النقلة قبل تنفيذها", Prefs.confirmMoves(this), v -> Prefs.setConfirmMoves(this, v));
        addSwitch("ترقية تلقائية لوزير (Auto-Queen)", Prefs.autoQueen(this), v -> Prefs.setAutoQueen(this, v));

        addSection("المظهر");
        addSwitch("الوضع الداكن (Dark Mode)", Prefs.darkMode(this), v -> { Prefs.setDarkMode(this, v); recreate(); });

        Button back = new Button(this);
        back.setText("رجوع");
        back.setAllCaps(false);
        back.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = 30;
        root.addView(back, lp);

        scroll.addView(root);
        setContentView(scroll);
    }

    void addTitle(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(24);
        t.setTextColor(Prefs.text(this));
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 10, 0, 26);
        root.addView(t);
    }

    void addSection(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(15);
        t.setTextColor(Prefs.accent(this));
        t.setPadding(4, 26, 4, 10);
        root.addView(t);
    }

    interface BoolCb { void set(boolean v); }

    void addSwitch(String label, boolean initial, BoolCb cb) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(8, 14, 8, 14);
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(Prefs.text(this));
        t.setTextSize(14);
        Switch sw = new Switch(this);
        sw.setChecked(initial);
        sw.setOnCheckedChangeListener((btn, checked) -> cb.set(checked));
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(sw, new LinearLayout.LayoutParams(-2, -2));
        root.addView(row);
    }

    void addBoardThemeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(8, 14, 8, 14);
        TextView t = new TextView(this);
        t.setText("لون الرقعة");
        t.setTextColor(Prefs.text(this));
        t.setTextSize(14);
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        int cur = Prefs.boardTheme(this);
        Button cycle = new Button(this);
        cycle.setAllCaps(false);
        cycle.setText((String) Prefs.BOARD_THEMES[cur][0]);
        cycle.setOnClickListener(v -> {
            int next = (Prefs.boardTheme(this) + 1) % Prefs.BOARD_THEMES.length;
            Prefs.setBoardTheme(this, next);
            cycle.setText((String) Prefs.BOARD_THEMES[next][0]);
        });
        row.addView(cycle);
        root.addView(row);
    }

    void addPieceStyleRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(8, 14, 8, 14);
        TextView t = new TextView(this);
        t.setText("شكل القطع");
        t.setTextColor(Prefs.text(this));
        t.setTextSize(14);
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        int cur = Prefs.pieceStyle(this);
        Button cycle = new Button(this);
        cycle.setAllCaps(false);
        cycle.setText(Prefs.PIECE_STYLES[cur]);
        cycle.setOnClickListener(v -> {
            int next = (Prefs.pieceStyle(this) + 1) % Prefs.PIECE_STYLES.length;
            Prefs.setPieceStyle(this, next);
            cycle.setText(Prefs.PIECE_STYLES[next]);
        });
        row.addView(cycle);
        root.addView(row);
    }

    void addBoardSizeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(8, 14, 8, 14);
        TextView t = new TextView(this);
        t.setText("حجم الرقعة");
        t.setTextColor(Prefs.text(this));
        t.setTextSize(14);
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        String[] names = {"صغير", "متوسط", "كبير"};
        int cur = Prefs.boardSizeIndex(this);
        Button cycle = new Button(this);
        cycle.setAllCaps(false);
        cycle.setText(names[cur]);
        cycle.setOnClickListener(v -> {
            int next = (Prefs.boardSizeIndex(this) + 1) % 3;
            Prefs.setBoardSizeIndex(this, next);
            cycle.setText(names[next]);
        });
        row.addView(cycle);
        root.addView(row);
    }
}
