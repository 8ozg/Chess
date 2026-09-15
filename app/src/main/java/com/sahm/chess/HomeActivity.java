package com.sahm.chess;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

public class HomeActivity extends Activity {
    LinearLayout root;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render(); // لإعادة رسم الألوان إذا رجع المستخدم من الإعدادات بعد تبديل داكن/فاتح
    }

    void render() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 30, 24, 40);
        root.setBackgroundColor(Prefs.bg(this));

        TextView title = new TextView(this);
        title.setText("سهم Chess ♟");
        title.setTextSize(28);
        title.setTextColor(Prefs.text(this));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("مدرّبك العربي للشطرنج");
        sub.setTextSize(13);
        sub.setTextColor(Prefs.subtext(this));
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 4, 0, 26);
        root.addView(sub);

        // ===== أقسام مُنفَّذة فعليًا =====
        card("♟ لعب ضد الذكاء الاصطناعي", "اختر اللون والصعوبة والوقت، وابدأ مباراة حقيقية", () -> {
            Intent i = new Intent(this, PlayActivity.class);
            i.putExtra("mode", "ai");
            startActivity(i);
        });
        card("👤 لعب محلي (لاعبان)", "لاعبان يتبادلان الدور على نفس الجهاز", () -> {
            Intent i = new Intent(this, PlayActivity.class);
            i.putExtra("mode", "local");
            startActivity(i);
        });
        card("🧠 لعب ضد نفسك", "حرّك الطرفين بنفسك وراجع/حلّل اللعب لاحقًا", () -> {
            Intent i = new Intent(this, PlayActivity.class);
            i.putExtra("mode", "solo");
            startActivity(i);
        });
        card("⚙️ الإعدادات والتخصيص", "الرقعة، القطع، الأصوات، اللعب، المظهر", () ->
                startActivity(new Intent(this, SettingsActivity.class)));

        // ===== أقسام قيد التطوير (موضّحة بصراحة، بدون وهم) =====
        TextView soon = new TextView(this);
        soon.setText("قيد التطوير — المرحلة القادمة");
        soon.setTextSize(13);
        soon.setTextColor(Prefs.subtext(this));
        soon.setPadding(6, 30, 6, 10);
        root.addView(soon);

        comingCard("🎓 التدريب", "تكتيكات (Fork/Pin/Skewer...)، نهايات، ومسائل كش مات مصنّفة بالمستوى — تحتاج بنك تمارين محقّق يُبنى بمرحلة قادمة.");
        comingCard("♜ الافتتاحيات", "شجرة افتتاحيات تفاعلية مع تدريب واختبار — البنية جاهزة بالمحرك (كتاب افتتاحيات أساسي بـ Chess.java)، والتوسعة الكاملة قادمة.");
        comingCard("🏆 المباريات الشهيرة", "مباريات تاريخية موثقة بصيغة PGN صحيحة (تال، فيشر، كاسباروف...) — تحتاج تدقيق مصادر قبل إدراجها فعليًا.");
        comingCard("📚 تعلم الشطرنج", "دروس تفاعلية من الصفر حتى الاستراتيجية والنهايات.");
        comingCard("📊 الإحصائيات", "عدد المباريات، الدقة، أكثر أخطائك، افتتاحياتك المفضلة.");

        Button howto = new Button(this);
        howto.setText("كيف تعمل الأقسام قيد التطوير؟");
        howto.setAllCaps(false);
        howto.setOnClickListener(v -> Toast.makeText(this,
                "كل قسم غير مُفعّل حاليًا موضّح بدقة داخل شاشته: شو المخطط له وليش لسا ما بُني، بدل ما نحط زر ما بيسوي شي.",
                Toast.LENGTH_LONG).show());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = 20;
        root.addView(howto, lp);

        scroll.addView(root);
        setContentView(scroll);
    }

    interface Action { void run(); }

    void card(String title, String desc, Action a) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackgroundColor(Prefs.card(this));
        c.setPadding(24, 20, 24, 20);
        c.setClickable(true);
        c.setOnClickListener(v -> a.run());

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(17);
        t.setTextColor(Prefs.text(this));
        c.addView(t);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextSize(12);
        d.setTextColor(Prefs.subtext(this));
        d.setPadding(0, 4, 0, 0);
        c.addView(d);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = 14;
        root.addView(c, lp);
    }

    void comingCard(String title, String desc) {
        card(title + "  🚧", desc, () -> {
            Intent i = new Intent(this, ComingSoonActivity.class);
            i.putExtra("title", title);
            i.putExtra("desc", desc);
            startActivity(i);
        });
    }
}
