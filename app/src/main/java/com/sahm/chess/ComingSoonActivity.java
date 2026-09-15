package com.sahm.chess;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

/**
 * شاشة صريحة لأي قسم لم يُبنَ بعد. لا تدّعي أي وظيفة وهمية؛
 * فقط تشرح الميزة المخطَّطة ولماذا هي مؤجلة لمرحلة لاحقة.
 */
public class ComingSoonActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        boolean dark = Prefs.darkMode(this);
        String title = getIntent().getStringExtra("title");
        String desc = getIntent().getStringExtra("desc");
        if (title == null) title = "قسم قادم";
        if (desc == null) desc = "هذا القسم قيد التطوير.";

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 40, 40, 40);
        root.setBackgroundColor(Prefs.bg(this));

        TextView icon = new TextView(this);
        icon.setText("🚧");
        icon.setTextSize(48);
        icon.setGravity(Gravity.CENTER);
        root.addView(icon);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(22);
        t.setTextColor(Prefs.text(this));
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 24, 0, 12);
        root.addView(t);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextSize(15);
        d.setTextColor(Prefs.subtext(this));
        d.setGravity(Gravity.CENTER);
        root.addView(d);

        Button back = new Button(this);
        back.setText("رجوع للرئيسية");
        back.setAllCaps(false);
        back.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = 40;
        root.addView(back, lp);

        setContentView(root);
    }
}
