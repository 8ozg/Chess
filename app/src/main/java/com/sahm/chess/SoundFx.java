package com.sahm.chess;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;

/**
 * أصوات النقلات بدون أي ملفات صوتية خارجية (ToneGenerator مبني بأندرويد نفسه)،
 * فيها احترام لإعداد تشغيل/إيقاف الصوت بالإعدادات.
 */
final class SoundFx {
    private static ToneGenerator tg;

    private static ToneGenerator get() {
        if (tg == null) {
            try { tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 70); } catch (Exception e) { tg = null; }
        }
        return tg;
    }

    static void move(Context c) { play(c, ToneGenerator.TONE_PROP_BEEP, 60); }
    static void capture(Context c) { play(c, ToneGenerator.TONE_PROP_BEEP2, 90); }
    static void check(Context c) { play(c, ToneGenerator.TONE_CDMA_PIP, 140); }
    static void gameEnd(Context c) { play(c, ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 220); }

    private static void play(Context c, int tone, int durationMs) {
        if (!Prefs.sound(c)) return;
        ToneGenerator g = get();
        if (g != null) { try { g.startTone(tone, durationMs); } catch (Exception ignored) {} }
    }
}
