package com.sahm.chess;

import java.util.*;

/**
 * محرك الشطرنج: توليد النقلات القانونية كاملة، تقييم موضعي (PST)،
 * بحث Minimax مع تقليم Alpha-Beta وترتيب نقلات وتعميق تدريجي محدود بوقت،
 * بالإضافة لكتاب افتتاحيات بسيط. كل هذا يعمل محليًا بالكامل: بدون إنترنت،
 * بدون مفاتيح API، وبدون أي حد على عدد مرات الاستخدام.
 */
public final class Chess {
    public static final char EMPTY = '.';
    static final String FILES = "abcdefgh";
    static final int[] VAL = {0, 100, 320, 330, 500, 900, 20000}; // _ P N B R Q K

    // ===================== النقلة =====================
    public static class Move {
        int fr, fc, tr, tc;
        char promo;
        boolean castle, ep;

        Move(int a, int b, int c, int d) { fr = a; fc = b; tr = c; tc = d; }
        Move(int a, int b, int c, int d, char p) { this(a, b, c, d); promo = p; }

        String san(Board before, boolean check, boolean mate) {
            char p = before.s[fr][fc];
            if (castle) {
                String base = tc > fc ? "O-O" : "O-O-O";
                return base + (mate ? "#" : check ? "+" : "");
            }
            boolean capture = before.s[tr][tc] != EMPTY || ep;
            char up = Character.toUpperCase(p);
            String piece = (up == 'P') ? "" : String.valueOf(up);
            String from = (piece.isEmpty() && capture) ? String.valueOf(FILES.charAt(fc)) : "";
            String x = piece + from + (capture ? "x" : "") + FILES.charAt(tc) + (8 - tr);
            if (promo != 0) x += "=" + Character.toUpperCase(promo);
            x += mate ? "#" : check ? "+" : "";
            return x;
        }
    }

    /** يبني SAN صحيحًا للنقلة مع إشارة كش (+) أو كش ملك (#) بفحص الوضع بعد اللعب. */
    public static String sanWithCheck(Board before, Move m) {
        Board after = before.copy();
        after.make(m);
        boolean chk = after.inCheck(after.wtm);
        boolean mate = chk && after.legal().isEmpty();
        return m.san(before, chk, mate);
    }

    /** يحول نقلة لصيغة إحداثيات مختصرة مثل e2e4 أو e7e8q عند الترقية. */
    public static String uci(Move m) {
        String s = "" + FILES.charAt(m.fc) + (8 - m.fr) + FILES.charAt(m.tc) + (8 - m.tr);
        if (m.promo != 0) s += m.promo;
        return s;
    }

    /** يحوّل نص إحداثيات لنقلة قانونية فعلية على اللوحة المعطاة (أو null إن لم توجد). */
    public static Move parseUci(Board b, String uci) {
        if (uci == null || uci.length() < 4) return null;
        int fc = uci.charAt(0) - 'a', fr = 8 - (uci.charAt(1) - '0');
        int tc = uci.charAt(2) - 'a', tr = 8 - (uci.charAt(3) - '0');
        char promo = uci.length() > 4 ? uci.charAt(4) : 0;
        for (Move m : b.legal()) {
            if (m.fr == fr && m.fc == fc && m.tr == tr && m.tc == tc && (promo == 0 || m.promo == promo)) {
                return m;
            }
        }
        return null;
    }

    // ===================== اللوحة =====================
    public static class Board {
        public char[][] s = new char[8][8];
        public boolean wtm = true, wkc = true, wqc = true, bkc = true, bqc = true;
        int epR = -1, epC = -1;

        public Board() { reset(); }

        public void reset() {
            String[] r = {"rnbqkbnr", "pppppppp", "........", "........", "........", "........", "PPPPPPPP", "RNBQKBNR"};
            for (int i = 0; i < 8; i++) s[i] = r[i].toCharArray();
            wtm = true; wkc = wqc = bkc = bqc = true; epR = epC = -1;
        }

        public Board copy() {
            Board b = new Board();
            for (int i = 0; i < 8; i++) b.s[i] = s[i].clone();
            b.wtm = wtm; b.wkc = wkc; b.wqc = wqc; b.bkc = bkc; b.bqc = bqc;
            b.epR = epR; b.epC = epC;
            return b;
        }

        boolean white(char p) { return p >= 'A' && p <= 'Z'; }
        boolean black(char p) { return p >= 'a' && p <= 'z'; }
        public boolean own(char p) { return p != EMPTY && (wtm ? white(p) : black(p)); }
        boolean enemyAt(char p, int color) { return p != EMPTY && (color == 1 ? black(p) : white(p)); }

        void make(Move m) {
            char p = s[m.fr][m.fc];
            char cap = s[m.tr][m.tc];
            if (m.ep) s[m.fr][m.tc] = EMPTY;
            s[m.tr][m.tc] = m.promo != 0 ? (white(p) ? Character.toUpperCase(m.promo) : Character.toLowerCase(m.promo)) : p;
            s[m.fr][m.fc] = EMPTY;

            if (Character.toUpperCase(p) == 'K') {
                if (white(p)) { wkc = wqc = false; } else { bkc = bqc = false; }
                if (Math.abs(m.tc - m.fc) == 2) {
                    if (m.tc > m.fc) { s[m.tr][5] = s[m.tr][7]; s[m.tr][7] = EMPTY; }
                    else { s[m.tr][3] = s[m.tr][0]; s[m.tr][0] = EMPTY; }
                }
            }
            if (Character.toUpperCase(p) == 'R') {
                if (m.fr == 7 && m.fc == 0) wqc = false;
                if (m.fr == 7 && m.fc == 7) wkc = false;
                if (m.fr == 0 && m.fc == 0) bqc = false;
                if (m.fr == 0 && m.fc == 7) bkc = false;
            }
            if (Character.toUpperCase(cap) == 'R') {
                if (m.tr == 7 && m.tc == 0) wqc = false;
                if (m.tr == 7 && m.tc == 7) wkc = false;
                if (m.tr == 0 && m.tc == 0) bqc = false;
                if (m.tr == 0 && m.tc == 7) bkc = false;
            }
            epR = epC = -1;
            if (Character.toUpperCase(p) == 'P' && Math.abs(m.tr - m.fr) == 2) { epR = (m.tr + m.fr) / 2; epC = m.fc; }
            wtm = !wtm;
        }

        public boolean inCheck(boolean whiteSide) {
            int kr = -1, kc = -1;
            for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) if (s[r][c] == (whiteSide ? 'K' : 'k')) { kr = r; kc = c; }
            if (kr < 0) return true;
            return attacked(kr, kc, !whiteSide);
        }

        boolean attacked(int r, int c, boolean byWhite) {
            int dir = byWhite ? -1 : 1;
            int pr = r - dir;
            for (int dc : new int[]{-1, 1}) {
                int rr = pr, cc = c + dc;
                if (ok(rr, cc) && s[rr][cc] == (byWhite ? 'P' : 'p')) return true;
            }
            int[][] n = {{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}};
            for (int[] d : n) {
                int rr = r + d[0], cc = c + d[1];
                if (ok(rr, cc) && s[rr][cc] == (byWhite ? 'N' : 'n')) return true;
            }
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
            for (int i = 0; i < 8; i++) {
                for (int k = 1; k < 8; k++) {
                    int rr = r + dirs[i][0] * k, cc = c + dirs[i][1] * k;
                    if (!ok(rr, cc)) break;
                    char p = s[rr][cc];
                    if (p != EMPTY) {
                        boolean mine = byWhite ? white(p) : black(p);
                        boolean sliderOk = i < 4 ? (Character.toUpperCase(p) == 'R' || Character.toUpperCase(p) == 'Q')
                                                  : (Character.toUpperCase(p) == 'B' || Character.toUpperCase(p) == 'Q');
                        if (mine && sliderOk) return true;
                        break;
                    }
                }
            }
            for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) if (dr != 0 || dc != 0) {
                int rr = r + dr, cc = c + dc;
                if (ok(rr, cc) && s[rr][cc] == (byWhite ? 'K' : 'k')) return true;
            }
            return false;
        }

        public List<Move> legal() {
            List<Move> out = new ArrayList<>();
            for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) if (own(s[r][c])) gen(r, c, out);
            List<Move> okm = new ArrayList<>();
            for (Move m : out) {
                Board b = copy();
                b.make(m);
                if (!b.inCheck(!b.wtm)) okm.add(m);
            }
            return okm;
        }

        void gen(int r, int c, List<Move> o) {
            char p = s[r][c];
            int color = white(p) ? 1 : -1;
            char u = Character.toUpperCase(p);
            if (u == 'P') {
                int d = color == 1 ? -1 : 1;
                int nr = r + d;
                if (ok(nr, c) && s[nr][c] == EMPTY) {
                    addPawn(r, c, nr, c, o);
                    if ((color == 1 ? r == 6 : r == 1) && s[r + 2 * d][c] == EMPTY) o.add(new Move(r, c, r + 2 * d, c));
                }
                for (int dc : new int[]{-1, 1}) {
                    int nc = c + dc;
                    if (ok(nr, nc) && (enemyAt(s[nr][nc], color) || (nr == epR && nc == epC && s[nr][nc] == EMPTY))) {
                        boolean isEp = (nr == epR && nc == epC && s[nr][nc] == EMPTY);
                        if (nr == 0 || nr == 7) {
                            for (char pc : new char[]{'q', 'r', 'b', 'n'}) {
                                Move m = new Move(r, c, nr, nc, pc);
                                m.ep = isEp;
                                o.add(m);
                            }
                        } else {
                            Move m = new Move(r, c, nr, nc);
                            m.ep = isEp;
                            o.add(m);
                        }
                    }
                }
                return;
            }
            if (u == 'N') {
                int[][] ds = {{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}};
                for (int[] d : ds) addIf(r, c, r + d[0], c + d[1], o);
                return;
            }
            if (u == 'K') {
                for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) if (dr != 0 || dc != 0) addIf(r, c, r + dr, c + dc, o);
                if (color == 1 && r == 7 && c == 4 && !inCheck(true)) {
                    if (wkc && s[7][5] == EMPTY && s[7][6] == EMPTY && !attacked(7, 5, false) && !attacked(7, 6, false)) {
                        Move m = new Move(7, 4, 7, 6); m.castle = true; o.add(m);
                    }
                    if (wqc && s[7][1] == EMPTY && s[7][2] == EMPTY && s[7][3] == EMPTY && !attacked(7, 3, false) && !attacked(7, 2, false)) {
                        Move m = new Move(7, 4, 7, 2); m.castle = true; o.add(m);
                    }
                }
                if (color == -1 && r == 0 && c == 4 && !inCheck(false)) {
                    if (bkc && s[0][5] == EMPTY && s[0][6] == EMPTY && !attacked(0, 5, true) && !attacked(0, 6, true)) {
                        Move m = new Move(0, 4, 0, 6); m.castle = true; o.add(m);
                    }
                    if (bqc && s[0][1] == EMPTY && s[0][2] == EMPTY && s[0][3] == EMPTY && !attacked(0, 3, true) && !attacked(0, 2, true)) {
                        Move m = new Move(0, 4, 0, 2); m.castle = true; o.add(m);
                    }
                }
                return;
            }
            int[][] ds = (u == 'B') ? new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}}
                    : (u == 'R') ? new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}
                    : new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : ds) {
                for (int k = 1; k < 8; k++) {
                    int rr = r + d[0] * k, cc = c + d[1] * k;
                    if (!ok(rr, cc)) break;
                    if (s[rr][cc] == EMPTY) { o.add(new Move(r, c, rr, cc)); }
                    else { if (enemyAt(s[rr][cc], color)) o.add(new Move(r, c, rr, cc)); break; }
                }
            }
        }

        void addPawn(int r, int c, int nr, int nc, List<Move> o) {
            if (nr == 0 || nr == 7) { for (char p : new char[]{'q', 'r', 'b', 'n'}) o.add(new Move(r, c, nr, nc, p)); }
            else o.add(new Move(r, c, nr, nc));
        }

        void addIf(int fr, int fc, int r, int c, List<Move> o) {
            if (ok(r, c) && !own(s[r][c])) o.add(new Move(fr, fc, r, c));
        }

        static boolean ok(int r, int c) { return r >= 0 && r < 8 && c >= 0 && c < 8; }
    }

    // ===================== التقييم (مادي + جداول مواقع) =====================
    static final int[] PAWN_PST = {
            0, 0, 0, 0, 0, 0, 0, 0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5, 5, 10, 25, 25, 10, 5, 5,
            0, 0, 0, 20, 20, 0, 0, 0,
            5, -5, -10, 0, 0, -10, -5, 5,
            5, 10, 10, -20, -20, 10, 10, 5,
            0, 0, 0, 0, 0, 0, 0, 0
    };
    static final int[] KNIGHT_PST = {
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20, 0, 0, 0, 0, -20, -40,
            -30, 0, 10, 15, 15, 10, 0, -30,
            -30, 5, 15, 20, 20, 15, 5, -30,
            -30, 0, 15, 20, 20, 15, 0, -30,
            -30, 5, 10, 15, 15, 10, 5, -30,
            -40, -20, 0, 5, 5, 0, -20, -40,
            -50, -40, -30, -30, -30, -30, -40, -50
    };
    static final int[] KING_PST = {
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -20, -30, -30, -40, -40, -30, -30, -20,
            -10, -20, -20, -20, -20, -20, -20, -10,
            20, 20, 0, 0, 0, 0, 20, 20,
            20, 30, 10, 0, 0, 10, 30, 20
    };

    static int pst(int[] table, int r, int c, boolean white) {
        int rr = white ? (7 - r) : r;
        return table[rr * 8 + c];
    }

    public static int evaluate(Board b) {
        int score = 0;
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            char p = b.s[r][c];
            if (p == EMPTY) continue;
            boolean white = Character.isUpperCase(p);
            char u = Character.toUpperCase(p);
            int val = VAL[" PNBRQK".indexOf(u)];
            int posBonus;
            switch (u) {
                case 'P': posBonus = pst(PAWN_PST, r, c, white); break;
                case 'N': posBonus = pst(KNIGHT_PST, r, c, white); break;
                case 'K': posBonus = pst(KING_PST, r, c, white); break;
                default: posBonus = (r == 3 || r == 4) && (c == 3 || c == 4) ? 10 : ((r == 3 || r == 4 || c == 3 || c == 4) ? 4 : 0);
            }
            int total = val + posBonus;
            score += white ? total : -total;
        }
        return score;
    }

    // ===================== البحث: Alpha-Beta + ترتيب نقلات + تعميق تدريجي =====================
    static int captureValue(Board b, Move m) {
        char cap = b.s[m.tr][m.tc];
        if (cap == EMPTY && !m.ep) return 0;
        char u = m.ep ? 'P' : Character.toUpperCase(cap);
        return VAL[" PNBRQK".indexOf(u)];
    }

    static void orderMoves(Board b, List<Move> ms) {
        ms.sort((m1, m2) -> captureValue(b, m2) - captureValue(b, m1));
    }

    static int search(Board b, int d, int a, int z) {
        List<Move> ms = b.legal();
        if (ms.isEmpty()) {
            if (b.inCheck(b.wtm)) return b.wtm ? -100000 - d : 100000 + d; // كش ملك: كلما كان أسرع كان أفضل
            return 0; // جمود = تعادل
        }
        if (d == 0) return evaluate(b);
        orderMoves(b, ms);
        if (b.wtm) {
            int x = -999999;
            for (Move m : ms) {
                Board n = b.copy(); n.make(m);
                x = Math.max(x, search(n, d - 1, a, z));
                a = Math.max(a, x);
                if (a >= z) break;
            }
            return x;
        } else {
            int x = 999999;
            for (Move m : ms) {
                Board n = b.copy(); n.make(m);
                x = Math.min(x, search(n, d - 1, a, z));
                z = Math.min(z, x);
                if (a >= z) break;
            }
            return x;
        }
    }

    /**
     * يبحث عن أفضل نقلة بتعميق تدريجي (Iterative Deepening) ضمن ميزانية وقت محددة (ms).
     * هذا يجعل الخصم أقوى بكثير من بحث عمق ثابت صغير، وبدون تجميد الواجهة (يُستدعى من خيط منفصل).
     */
    public static Move best(Board b, long timeBudgetMs) { return best(b, timeBudgetMs, 5); }

    /**
     * نفس البحث، لكن بعمق أقصى قابل للتحكم — تُستخدم لمستويات الصعوبة
     * (مبتدئ = عمق ضحل، خبير = العمق الكامل ضمن نفس ميزانية الوقت).
     */
    public static Move best(Board b, long timeBudgetMs, int maxDepth) {
        List<Move> ms = b.legal();
        if (ms.isEmpty()) return null;
        orderMoves(b, ms);
        Move bestMove = ms.get(0);
        long start = System.currentTimeMillis();
        for (int depth = 1; depth <= maxDepth; depth++) {
            int alpha = -999999, beta = 999999;
            Move currentBest = null;
            int currentScore = b.wtm ? -999999 : 999999;
            boolean timedOut = false;
            for (Move m : ms) {
                Board n = b.copy(); n.make(m);
                int sc = search(n, depth - 1, alpha, beta);
                if (b.wtm ? sc > currentScore : sc < currentScore) { currentScore = sc; currentBest = m; }
                if (b.wtm) alpha = Math.max(alpha, currentScore); else beta = Math.min(beta, currentScore);
                if (System.currentTimeMillis() - start > timeBudgetMs) { timedOut = true; break; }
            }
            if (currentBest != null) {
                bestMove = currentBest;
                ms.remove(currentBest);
                ms.add(0, currentBest); // جرّب أفضل نقلة أولاً بالعمق التالي (تسريع التقليم)
            }
            if (timedOut || System.currentTimeMillis() - start > timeBudgetMs) break;
        }
        return bestMove;
    }

    // ===================== كتاب افتتاحيات مبسّط =====================
    // مجموعة افتتاحيات معروفة عالميًا (روي لوبيز، الصقلية، فرنسية، هولندية، ملكة مرفوضة، هندية ملك...)
    static final String[][] BOOK = {
            {"e2e4", "e7e5", "g1f3", "b8c6", "f1b5"},            // روي لوبيز
            {"e2e4", "e7e5", "g1f3", "b8c6", "f1c4"},            // الإيطالية
            {"e2e4", "c7c5", "g1f3", "d7d6", "d2d4"},            // الصقلية
            {"e2e4", "c7c5", "g1f3", "b8c6", "d2d4"},            // الصقلية
            {"e2e4", "e7e6", "d2d4", "d7d5", "b1c3"},            // الفرنسية
            {"e2e4", "c7c6", "d2d4", "d7d5"},                    // الدفاع الكاروكان
            {"d2d4", "d7d5", "c2c4", "e7e6", "b1c3"},            // الملكة المرفوضة
            {"d2d4", "d7d5", "c2c4", "c7c6"},                    // السلافية
            {"d2d4", "g8f6", "c2c4", "g7g6", "b1c3"},            // الهندية الملكية
            {"d2d4", "g8f6", "c2c4", "e7e6", "b1c3"},            // النيمزو-هندية
            {"g1f3", "d7d5", "c2c4", "c7c6"},                    // الريتي
            {"e2e4", "d7d5"},                                    // دفاع سكاندنافي
    };

    public static String bookMove(List<String> history) {
        List<String> candidates = new ArrayList<>();
        for (String[] line : BOOK) {
            if (line.length > history.size()) {
                boolean match = true;
                for (int i = 0; i < history.size(); i++) if (!line[i].equals(history.get(i))) { match = false; break; }
                if (match) candidates.add(line[history.size()]);
            }
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(new Random().nextInt(candidates.size()));
    }
}
