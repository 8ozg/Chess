package com.sahm.chess;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import android.text.SpannableStringBuilder;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.view.*;
import android.widget.*;
import java.util.*;

public class PlayActivity extends Activity {
    Chess.Board board = new Chess.Board();
    ArrayList<Chess.Board> snapshots = new ArrayList<>();
    ArrayList<String> sanList = new ArrayList<>();
    ArrayList<String> uciList = new ArrayList<>();
    int viewIndex = 0;

    String mode = "ai"; // ai | local | solo
    boolean playerIsWhite = true;
    boolean flipped = false;
    volatile boolean aiThinking = false;
    String resultOverride = null;

    long aiTimeMs = 1200; int aiDepth = 4; double aiRandomness = 0.0; String difficultyName = "متقدم";
    boolean clockEnabled = false;
    long whiteMillis, blackMillis;
    Handler clockHandler = new Handler(Looper.getMainLooper());
    Runnable clockTick;

    BoardView boardView;
    TextView status, movesView, clockView, analysisView;
    LinearLayout root;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "ai";
        buildUi();
        snapshots.add(board.copy());
        if (mode.equals("ai")) showColorDialog(); else startGame();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clockTick != null) clockHandler.removeCallbacks(clockTick);
    }

    // ===================== إعداد المباراة =====================
    void showColorDialog() {
        String[] colors = {"العب بالأبيض", "العب بالأسود", "لون عشوائي"};
        new AlertDialog.Builder(this).setTitle("اختر لونك").setCancelable(false)
                .setItems(colors, (d, which) -> {
                    playerIsWhite = which == 2 ? new Random().nextBoolean() : which == 0;
                    flipped = !playerIsWhite;
                    showDifficultyDialog();
                }).show();
    }

    void showDifficultyDialog() {
        String[] diffs = {"مبتدئ", "متوسط", "متقدم", "خبير"};
        new AlertDialog.Builder(this).setTitle("مستوى الخصم").setCancelable(false)
                .setItems(diffs, (d, which) -> {
                    difficultyName = diffs[which];
                    switch (which) {
                        case 0: aiTimeMs = 300; aiDepth = 2; aiRandomness = 0.20; break;
                        case 1: aiTimeMs = 700; aiDepth = 3; aiRandomness = 0.06; break;
                        case 2: aiTimeMs = 1500; aiDepth = 4; aiRandomness = 0.0; break;
                        default: aiTimeMs = 3000; aiDepth = 5; aiRandomness = 0.0;
                    }
                    showTimeDialog();
                }).show();
    }

    void showTimeDialog() {
        String[] times = {"بدون وقت", "5 دقائق لكل طرف", "10 دقائق لكل طرف", "15 دقيقة لكل طرف"};
        new AlertDialog.Builder(this).setTitle("وقت المباراة").setCancelable(false)
                .setItems(times, (d, which) -> {
                    int[] mins = {0, 5, 10, 15};
                    clockEnabled = which > 0;
                    whiteMillis = blackMillis = mins[which] * 60000L;
                    startGame();
                }).show();
    }

    void startGame() {
        board.reset();
        snapshots.clear(); snapshots.add(board.copy());
        sanList.clear(); uciList.clear();
        viewIndex = 0; resultOverride = null;
        updateClockView();
        boardView.invalidate();
        refreshMoveListUI();
        afterMove();
        if (clockEnabled) startClock();
        if (mode.equals("ai") && !playerIsWhite) boardView.postDelayed(this::aiMove, 400);
    }

    // ===================== الحالة العامة =====================
    boolean reviewing() { return viewIndex != snapshots.size() - 1; }
    Chess.Board disp() { return snapshots.get(viewIndex); }

    boolean canTouch() {
        if (resultOverride != null) return false;
        if (reviewing()) return false;
        if (aiThinking) return false;
        if (mode.equals("ai") && board.wtm != playerIsWhite) return false;
        return true;
    }

    void afterMove() {
        List<Chess.Move> lm = board.legal();
        if (lm.isEmpty()) {
            stopClock();
            if (board.inCheck(board.wtm)) {
                resultOverride = board.wtm ? "0-1" : "1-0";
                status.setText(board.wtm ? "كش ملك! فاز الأسود 🏆" : "كش ملك! فاز الأبيض 🏆");
            } else {
                resultOverride = "1/2-1/2";
                status.setText("تعادل بالجمود (Stalemate)");
            }
            SoundFx.gameEnd(this);
            return;
        }
        boolean chk = board.inCheck(board.wtm);
        if (chk) SoundFx.check(this);
        String turnTxt = board.wtm ? "دور الأبيض" : "دور الأسود";
        if (mode.equals("ai")) turnTxt += (board.wtm == playerIsWhite) ? " (أنت)" : " (الخصم يفكّر…)";
        status.setText(turnTxt + (chk ? " • كش!" : ""));
        if (mode.equals("ai") && board.wtm != playerIsWhite) {
            boardView.postDelayed(this::aiMove, 300);
        }
    }

    void applyMove(Chess.Move chosen) {
        boolean capture = board.s[chosen.tr][chosen.tc] != Chess.EMPTY || chosen.ep;
        String san = Chess.sanWithCheck(board, chosen);
        board.make(chosen);
        snapshots.add(board.copy());
        sanList.add(san);
        uciList.add(Chess.uci(chosen));
        viewIndex = snapshots.size() - 1;
        boardView.invalidate();
        refreshMoveListUI();
        analysisView.setVisibility(View.GONE);
        if (capture) SoundFx.capture(this); else SoundFx.move(this);
        afterMove();
    }

    void aiMove() {
        if (aiThinking || resultOverride != null) return;
        aiThinking = true;
        new Thread(() -> {
            String book = Chess.bookMove(uciList);
            Chess.Move m = book != null ? Chess.parseUci(board, book) : null;
            if (m == null) {
                if (aiRandomness > 0 && Math.random() < aiRandomness) {
                    List<Chess.Move> lm = board.legal();
                    m = lm.get(new Random().nextInt(lm.size()));
                } else {
                    m = Chess.best(board, aiTimeMs, aiDepth);
                }
            }
            final Chess.Move mv = m;
            runOnUiThread(() -> { aiThinking = false; if (mv != null) applyMove(mv); });
        }).start();
    }

    // ===================== التراجع / الاستسلام / التعادل / جديد =====================
    void undo() {
        if (reviewing() || aiThinking || snapshots.size() <= 1) return;
        undoOnce();
        if (mode.equals("ai") && board.wtm != playerIsWhite && snapshots.size() > 1) undoOnce();
        resultOverride = null;
        boardView.invalidate();
        refreshMoveListUI();
        afterMove();
    }

    void undoOnce() {
        snapshots.remove(snapshots.size() - 1);
        if (!sanList.isEmpty()) sanList.remove(sanList.size() - 1);
        if (!uciList.isEmpty()) uciList.remove(uciList.size() - 1);
        board = snapshots.get(snapshots.size() - 1).copy();
        viewIndex = snapshots.size() - 1;
    }

    void resign() {
        if (mode.equals("ai")) {
            resultOverride = playerIsWhite ? "0-1" : "1-0";
            status.setText("استسلمت — فاز الخصم");
            stopClock(); SoundFx.gameEnd(this);
        } else {
            new AlertDialog.Builder(this).setTitle("من يستسلم؟")
                    .setItems(new String[]{"الأبيض", "الأسود"}, (d, w) -> {
                        resultOverride = w == 0 ? "0-1" : "1-0";
                        status.setText((w == 0 ? "الأبيض" : "الأسود") + " استسلم");
                        stopClock(); SoundFx.gameEnd(this);
                    }).show();
        }
    }

    void offerDraw() {
        if (resultOverride != null) return;
        if (mode.equals("ai")) {
            int eval = Chess.evaluate(board);
            if (Math.abs(eval) < 150) {
                resultOverride = "1/2-1/2";
                status.setText("الخصم وافق على التعادل");
                stopClock(); SoundFx.gameEnd(this);
            } else {
                Toast.makeText(this, "الخصم لا يرى الوضع متعادلًا الآن ويرفض العرض.", Toast.LENGTH_SHORT).show();
            }
        } else {
            new AlertDialog.Builder(this).setTitle("هل يوافق الطرفان على التعادل؟")
                    .setPositiveButton("نعم", (d, w) -> {
                        resultOverride = "1/2-1/2";
                        status.setText("تعادل بالاتفاق");
                        stopClock(); SoundFx.gameEnd(this);
                    }).setNegativeButton("إلغاء", null).show();
        }
    }

    void confirmNewGame() {
        new AlertDialog.Builder(this).setTitle("لعبة جديدة؟").setMessage("سيتم فقدان المباراة الحالية.")
                .setPositiveButton("نعم", (d, w) -> { if (mode.equals("ai")) showColorDialog(); else startGame(); })
                .setNegativeButton("إلغاء", null).show();
    }

    // ===================== الساعة =====================
    void startClock() {
        clockTick = () -> {
            if (isFinishing() || resultOverride != null) return;
            if (!reviewing() && !aiThinking) {
                if (board.wtm) whiteMillis = Math.max(0, whiteMillis - 1000); else blackMillis = Math.max(0, blackMillis - 1000);
                if (whiteMillis == 0 || blackMillis == 0) {
                    resultOverride = whiteMillis == 0 ? "0-1" : "1-0";
                    status.setText((whiteMillis == 0 ? "انتهى وقت الأبيض" : "انتهى وقت الأسود") + " — انتهت المباراة");
                    SoundFx.gameEnd(this);
                    updateClockView();
                    return;
                }
            }
            updateClockView();
            clockHandler.postDelayed(clockTick, 1000);
        };
        clockHandler.postDelayed(clockTick, 1000);
    }

    void stopClock() { if (clockTick != null) clockHandler.removeCallbacks(clockTick); }

    void updateClockView() {
        if (!clockEnabled) { clockView.setVisibility(View.GONE); return; }
        clockView.setVisibility(View.VISIBLE);
        clockView.setText("⚪ " + fmtClock(whiteMillis) + "     ⚫ " + fmtClock(blackMillis));
    }

    String fmtClock(long ms) { long s = ms / 1000; return String.format(Locale.US, "%02d:%02d", s / 60, s % 60); }

    // ===================== سجل النقلات والتنقل =====================
    void refreshMoveListUI() {
        if (sanList.isEmpty()) { movesView.setText("النقلات: —"); return; }
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (int i = 0; i < sanList.size(); i++) {
            if (i % 2 == 0) sb.append((i / 2 + 1) + ". ");
            final int idx = i;
            int start = sb.length();
            sb.append(sanList.get(i));
            int end = sb.length();
            sb.setSpan(new ClickableSpan() {
                public void onClick(View v) { jumpTo(idx + 1); }
                public void updateDrawState(android.text.TextPaint ds) { ds.setColor(Prefs.text(PlayActivity.this)); ds.setUnderlineText(false); }
            }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (viewIndex == idx + 1) sb.setSpan(new BackgroundColorSpan(Prefs.accent(this)), start, end, 0);
            sb.append("  ");
        }
        movesView.setText(sb);
        movesView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    void jumpTo(int idx) {
        viewIndex = Math.max(0, Math.min(snapshots.size() - 1, idx));
        boardView.invalidate();
        refreshMoveListUI();
        analysisView.setVisibility(View.GONE);
        status.setText(reviewing() ? "وضع المراجعة — نقلة " + viewIndex + " من " + (snapshots.size() - 1) : (board.wtm ? "دور الأبيض" : "دور الأسود"));
    }

    void analyzeCurrent() {
        if (viewIndex == 0) { Toast.makeText(this, "اختر نقلة من سجل النقلات أولًا.", Toast.LENGTH_SHORT).show(); return; }
        analysisView.setVisibility(View.VISIBLE);
        analysisView.setText("جارٍ التحليل…");
        Chess.Board before = snapshots.get(viewIndex - 1).copy();
        boolean whiteToMove = before.wtm;
        int idx = viewIndex;
        new Thread(() -> {
            Chess.Move bestMv = Chess.best(before, 1500, 4);
            Chess.Board afterBest = before.copy(); afterBest.make(bestMv);
            int bestEval = Chess.evaluate(afterBest);
            int actualEval = Chess.evaluate(snapshots.get(idx));
            int bestForMover = whiteToMove ? bestEval : -bestEval;
            int actualForMover = whiteToMove ? actualEval : -actualEval;
            int delta = bestForMover - actualForMover;
            String bestSan = Chess.sanWithCheck(before, bestMv);
            String label = classify(delta);
            runOnUiThread(() -> {
                String played = sanList.get(idx - 1);
                analysisView.setText("لعبت: " + played + "  (" + fmtScore(actualForMover) + ")\n"
                        + "الأفضل: " + bestSan + "  (" + fmtScore(bestForMover) + ")\n"
                        + "التصنيف: " + label);
            });
        }).start();
    }

    String classify(int delta) {
        if (delta <= 0) return "أفضل نقلة (Best) ✅";
        if (delta < 30) return "ممتاز (Excellent)";
        if (delta < 80) return "جيد (Good)";
        if (delta < 150) return "غير دقيق (Inaccuracy) ?!";
        if (delta < 300) return "خطأ (Mistake) ?";
        return "خطأ فادح (Blunder) ??";
    }

    String fmtScore(int cp) { double p = cp / 100.0; return (p >= 0 ? "+" : "") + String.format(Locale.US, "%.1f", p); }

    void hint() {
        Chess.Board b = disp();
        Toast.makeText(this, "جارٍ البحث عن أفضل نقلة…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            Chess.Move m = Chess.best(b, 1200, 4);
            if (m == null) return;
            String san = Chess.sanWithCheck(b, m);
            runOnUiThread(() -> new AlertDialog.Builder(this).setTitle("تلميح")
                    .setMessage("أفضل نقلة مقترحة بهذه الوضعية: " + san).setPositiveButton("حسنًا", null).show());
        }).start();
    }

    String toPgn() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Event \"سهم Chess\"]\n[Site \"Mobile\"]\n[White \"")
                .append(mode.equals("ai") && !playerIsWhite ? "SahmChess AI" : "Player 1").append("\"]\n[Black \"")
                .append(mode.equals("ai") && playerIsWhite ? "SahmChess AI" : "Player 2").append("\"]\n[Result \"")
                .append(resultTag()).append("\"]\n\n");
        for (int i = 0; i < sanList.size(); i++) {
            if (i % 2 == 0) sb.append((i / 2 + 1)).append(". ");
            sb.append(sanList.get(i)).append(" ");
        }
        sb.append(resultTag());
        return sb.toString();
    }

    String resultTag() { return resultOverride != null ? resultOverride : "*"; }

    // ===================== الواجهة =====================
    void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(10, 10, 10, 10);
        root.setBackgroundColor(Prefs.bg(this));

        status = tv("جارٍ التحضير…", 14);
        status.setGravity(Gravity.CENTER);
        root.addView(status, new LinearLayout.LayoutParams(-1, 42));

        clockView = tv("", 14);
        clockView.setGravity(Gravity.CENTER);
        clockView.setVisibility(View.GONE);
        root.addView(clockView, new LinearLayout.LayoutParams(-1, 36));

        boardView = new BoardView(this);
        root.addView(boardView, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout navRow = new LinearLayout(this);
        Button navStart = smallBtn("⏮"), navPrev = smallBtn("⏪"), navLive = smallBtn("مباشر"), navNext = smallBtn("⏩"), navEnd = smallBtn("⏭");
        navStart.setOnClickListener(v -> jumpTo(0));
        navPrev.setOnClickListener(v -> jumpTo(viewIndex - 1));
        navLive.setOnClickListener(v -> jumpTo(snapshots.size() - 1));
        navNext.setOnClickListener(v -> jumpTo(viewIndex + 1));
        navEnd.setOnClickListener(v -> jumpTo(snapshots.size() - 1));
        for (Button bt : new Button[]{navStart, navPrev, navLive, navNext, navEnd}) navRow.addView(bt, new LinearLayout.LayoutParams(0, 60, 1));
        root.addView(navRow);

        HorizontalScrollView movesScroll = new HorizontalScrollView(this);
        movesView = tv("النقلات: —", 13);
        movesScroll.addView(movesView);
        root.addView(movesScroll, new LinearLayout.LayoutParams(-1, 60));

        LinearLayout row1 = new LinearLayout(this);
        Button undoB = smallBtn("↩ تراجع"), hintB = smallBtn("💡 تلميح"), analyzeB = smallBtn("🔍 تحليل"), flipB = smallBtn("🔄 قلب");
        undoB.setOnClickListener(v -> undo());
        hintB.setOnClickListener(v -> hint());
        analyzeB.setOnClickListener(v -> analyzeCurrent());
        flipB.setOnClickListener(v -> { flipped = !flipped; boardView.invalidate(); });
        for (Button bt : new Button[]{undoB, hintB, analyzeB, flipB}) row1.addView(bt, new LinearLayout.LayoutParams(0, 60, 1));
        root.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        Button resignB = smallBtn("🏳 استسلام"), drawB = smallBtn("🤝 تعادل"), newB = smallBtn("🆕 جديد"), pgnB = smallBtn("📋 نسخ PGN");
        resignB.setOnClickListener(v -> resign());
        drawB.setOnClickListener(v -> offerDraw());
        newB.setOnClickListener(v -> confirmNewGame());
        pgnB.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("PGN", toPgn()));
            Toast.makeText(this, "تم نسخ PGN", Toast.LENGTH_SHORT).show();
        });
        for (Button bt : new Button[]{resignB, drawB, newB, pgnB}) row2.addView(bt, new LinearLayout.LayoutParams(0, 60, 1));
        root.addView(row2);

        analysisView = tv("", 13);
        analysisView.setBackgroundColor(Prefs.card(this));
        analysisView.setVisibility(View.GONE);
        root.addView(analysisView, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
    }

    TextView tv(String t, int sp) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextColor(Prefs.text(this)); v.setTextSize(sp); v.setPadding(14, 8, 14, 8);
        return v;
    }

    Button smallBtn(String s) {
        Button b = new Button(this);
        b.setText(s); b.setTextColor(Prefs.text(this)); b.setTextSize(11); b.setAllCaps(false);
        return b;
    }

    // ===================== الرقعة =====================
    class BoardView extends View {
        Paint p = new Paint(1);
        int selR = -1, selC = -1;
        float size, ox, oy;
        List<int[]> highlights = new ArrayList<>();

        BoardView(Context c) { super(c); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }

        int br(int screenR) { return flipped ? 7 - screenR : screenR; }
        int bc(int screenC) { return flipped ? 7 - screenC : screenC; }

        protected void onDraw(Canvas c) {
            super.onDraw(c);
            float full = Math.min(getWidth(), getHeight());
            size = (full / 8f) * Prefs.boardSizeFactor(getContext());
            ox = (getWidth() - size * 8) / 2;
            oy = (getHeight() - size * 8) / 2;

            Object[] theme = Prefs.BOARD_THEMES[Prefs.boardTheme(getContext())];
            int light = (int) theme[1], darkc = (int) theme[2];
            boolean showCoords = Prefs.showCoords(getContext());
            boolean showLegal = Prefs.showLegalMoves(getContext());
            boolean lastMoveOn = Prefs.highlightLastMove(getContext());
            Chess.Board bd = disp();

            int lfr = -1, lfc = -1, ltr = -1, ltc = -1;
            if (lastMoveOn && viewIndex > 0) {
                String u = uciList.get(viewIndex - 1);
                lfc = u.charAt(0) - 'a'; lfr = 8 - (u.charAt(1) - '0');
                ltc = u.charAt(2) - 'a'; ltr = 8 - (u.charAt(3) - '0');
            }

            int kR = -1, kC = -1;
            boolean inChk = !reviewing() && board.inCheck(board.wtm);
            if (inChk) {
                outer:
                for (int rr = 0; rr < 8; rr++) for (int cc = 0; cc < 8; cc++)
                    if (bd.s[rr][cc] == (board.wtm ? 'K' : 'k')) { kR = rr; kC = cc; break outer; }
            }

            for (int sr = 0; sr < 8; sr++) {
                for (int sc = 0; sc < 8; sc++) {
                    int r = br(sr), col = bc(sc);
                    p.setColor((r + col) % 2 == 0 ? light : darkc);
                    c.drawRect(ox + sc * size, oy + sr * size, ox + (sc + 1) * size, oy + (sr + 1) * size, p);

                    if (lastMoveOn && ((r == lfr && col == lfc) || (r == ltr && col == ltc))) {
                        p.setColor(Color.argb(90, 246, 210, 80));
                        c.drawRect(ox + sc * size, oy + sr * size, ox + (sc + 1) * size, oy + (sr + 1) * size, p);
                    }
                    if (r == selR && col == selC) {
                        p.setColor(Color.argb(110, 240, 190, 60));
                        c.drawRect(ox + sc * size, oy + sr * size, ox + (sc + 1) * size, oy + (sr + 1) * size, p);
                    }
                    if (r == kR && col == kC) {
                        p.setColor(Color.argb(130, 220, 40, 40));
                        c.drawRect(ox + sc * size, oy + sr * size, ox + (sc + 1) * size, oy + (sr + 1) * size, p);
                    }
                    if (showLegal) for (int[] h : highlights) if (h[0] == r && h[1] == col) {
                        p.setColor(Color.argb(150, 50, 170, 90));
                        c.drawCircle(ox + (sc + .5f) * size, oy + (sr + .5f) * size, size * 0.16f, p);
                    }

                    char pc = bd.s[r][col];
                    if (pc != Chess.EMPTY) drawPiece(c, pc, ox + (sc + .5f) * size, oy + (sr + .76f) * size);

                    if (showCoords) {
                        p.setTextSize(size * .18f); p.setTextAlign(Paint.Align.LEFT); p.clearShadowLayer();
                        if (sc == 0) { p.setColor((r + col) % 2 == 0 ? darkc : light); c.drawText(String.valueOf(8 - r), ox + 3, oy + sr * size + size * .22f, p); }
                        if (sr == 7) { p.setColor((r + col) % 2 == 0 ? darkc : light); c.drawText(String.valueOf((char) ('a' + col)), ox + sc * size + size - size * .2f, oy + sr * size + size - 4, p); }
                    }
                }
            }
        }

        void drawPiece(Canvas c, char pc, float cx, float cy) {
            p.setTextSize(size * .72f);
            p.setTextAlign(Paint.Align.CENTER);
            boolean white = Character.isUpperCase(pc);
            int style = Prefs.pieceStyle(getContext());
            p.setColor(white ? Color.WHITE : Color.BLACK);
            p.setShadowLayer(3, 1, 2, Color.GRAY);
            c.drawText(glyph(pc), cx, cy, p);
            p.clearShadowLayer();
            if (style == 1) { // بولد: خط تحديد إضافي
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(2f);
                p.setColor(white ? Color.DKGRAY : Color.WHITE);
                c.drawText(glyph(pc), cx, cy, p);
                p.setStyle(Paint.Style.FILL);
            }
        }

        String glyph(char x) {
            switch (Character.toUpperCase(x)) {
                case 'K': return "♚"; case 'Q': return "♛"; case 'R': return "♜";
                case 'B': return "♝"; case 'N': return "♞"; default: return "♟";
            }
        }

        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() != MotionEvent.ACTION_UP) return true;
            if (!canTouch()) { if (reviewing()) Toast.makeText(getContext(), "أنت بوضع المراجعة — اضغط \"مباشر\" للمتابعة", Toast.LENGTH_SHORT).show(); return true; }
            int sc = (int) ((e.getX() - ox) / size), sr = (int) ((e.getY() - oy) / size);
            if (sr < 0 || sr > 7 || sc < 0 || sc > 7) return true;
            int r = br(sr), c = bc(sc);

            if (selR < 0) {
                if (board.own(board.s[r][c])) { selR = r; selC = c; fillHighlights(r, c); invalidate(); }
                return true;
            }

            List<Chess.Move> matches = new ArrayList<>();
            for (Chess.Move m : board.legal()) if (m.fr == selR && m.fc == selC && m.tr == r && m.tc == c) matches.add(m);

            if (!matches.isEmpty()) {
                selR = selC = -1; highlights.clear(); invalidate();
                if (matches.size() > 1 && !Prefs.autoQueen(getContext())) {
                    showPromotionPicker(matches);
                } else {
                    Chess.Move chosen = matches.get(0);
                    if (Prefs.confirmMoves(getContext())) {
                        String san = Chess.sanWithCheck(board, chosen);
                        new AlertDialog.Builder(getContext()).setMessage("تأكيد النقلة: " + san + "؟")
                                .setPositiveButton("تأكيد", (d, w) -> applyMove(chosen))
                                .setNegativeButton("إلغاء", null).show();
                    } else applyMove(chosen);
                }
                return true;
            }

            if (board.own(board.s[r][c])) { selR = r; selC = c; fillHighlights(r, c); } else { selR = selC = -1; highlights.clear(); }
            invalidate();
            return true;
        }

        void showPromotionPicker(List<Chess.Move> matches) {
            String[] names = new String[matches.size()];
            for (int i = 0; i < matches.size(); i++) {
                char pr = matches.get(i).promo;
                names[i] = pr == 'q' ? "وزير ♛" : pr == 'r' ? "رخ ♜" : pr == 'b' ? "فيل ♝" : "حصان ♞";
            }
            new AlertDialog.Builder(getContext()).setTitle("ترقية البيدق")
                    .setItems(names, (d, which) -> applyMove(matches.get(which))).setCancelable(false).show();
        }

        void fillHighlights(int r, int c) {
            highlights.clear();
            for (Chess.Move m : board.legal()) if (m.fr == r && m.fc == c) highlights.add(new int[]{m.tr, m.tc});
        }
    }
}
