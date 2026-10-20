package com.dodopress.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import com.dodopress.R;

public class ThemeManager {

    public enum AppTheme {
        WINDOWS_XP("🪟 Windows XP"),
        MIDNIGHT("🌙 Midnight"),
        STUDIO("☀️ Studio"),
        CYBER("⚡ Cyber"),
        WARM("📜 Vintage");

        public final String title;
        AppTheme(String title) {
            this.title = title;
        }
    }

    private static final String PREF_NAME = "dodopress_theme";
    private static final String KEY_THEME = "current_theme";

    public static AppTheme getTheme(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String name = sp.getString(KEY_THEME, AppTheme.WINDOWS_XP.name());
        try {
            return AppTheme.valueOf(name);
        } catch (Exception e) {
            return AppTheme.WINDOWS_XP;
        }
    }

    public static void saveTheme(Context ctx, AppTheme theme) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_THEME, theme.name()).apply();
    }

    public static class ThemePalette {
        public int bgColor;
        public int cardDrawableRes;
        public int textHighColor;
        public int textMedColor;
        public int brandPrimaryColor;
        public int primaryButtonDrawableRes;
        public int chipButtonDrawableRes;
        public int primaryButtonTextColor;
        public boolean isXp;

        public static ThemePalette forTheme(AppTheme theme) {
            ThemePalette p = new ThemePalette();
            switch (theme) {
                case WINDOWS_XP:
                    p.bgColor = Color.parseColor("#ECE9D8");
                    p.cardDrawableRes = R.drawable.theme_card_xp;
                    p.textHighColor = Color.parseColor("#000000");
                    p.textMedColor = Color.parseColor("#444444");
                    p.brandPrimaryColor = Color.parseColor("#0055EA");
                    p.primaryButtonDrawableRes = R.drawable.theme_btn_xp_primary;
                    p.chipButtonDrawableRes = R.drawable.theme_btn_xp;
                    p.primaryButtonTextColor = Color.parseColor("#000000");
                    p.isXp = true;
                    break;

                case STUDIO:
                    p.bgColor = Color.parseColor("#F1F5F9");
                    p.cardDrawableRes = R.drawable.theme_card_studio;
                    p.textHighColor = Color.parseColor("#0F172A");
                    p.textMedColor = Color.parseColor("#64748B");
                    p.brandPrimaryColor = Color.parseColor("#2563EB");
                    p.primaryButtonDrawableRes = R.drawable.btn_primary;
                    p.chipButtonDrawableRes = R.drawable.btn_chip;
                    p.primaryButtonTextColor = Color.parseColor("#FFFFFF");
                    p.isXp = false;
                    break;

                case CYBER:
                    p.bgColor = Color.parseColor("#050B08");
                    p.cardDrawableRes = R.drawable.theme_card_cyber;
                    p.textHighColor = Color.parseColor("#ECFDF5");
                    p.textMedColor = Color.parseColor("#6EE7B7");
                    p.brandPrimaryColor = Color.parseColor("#10B981");
                    p.primaryButtonDrawableRes = R.drawable.btn_primary;
                    p.chipButtonDrawableRes = R.drawable.btn_chip;
                    p.primaryButtonTextColor = Color.parseColor("#FFFFFF");
                    p.isXp = false;
                    break;

                case WARM:
                    p.bgColor = Color.parseColor("#FAF5EF");
                    p.cardDrawableRes = R.drawable.theme_card_warm;
                    p.textHighColor = Color.parseColor("#292524");
                    p.textMedColor = Color.parseColor("#78716C");
                    p.brandPrimaryColor = Color.parseColor("#D97706");
                    p.primaryButtonDrawableRes = R.drawable.btn_primary;
                    p.chipButtonDrawableRes = R.drawable.btn_chip;
                    p.primaryButtonTextColor = Color.parseColor("#FFFFFF");
                    p.isXp = false;
                    break;

                case MIDNIGHT:
                default:
                    p.bgColor = Color.parseColor("#0B0F19");
                    p.cardDrawableRes = R.drawable.theme_card_midnight;
                    p.textHighColor = Color.parseColor("#F8FAFC");
                    p.textMedColor = Color.parseColor("#94A3B8");
                    p.brandPrimaryColor = Color.parseColor("#38BDF8");
                    p.primaryButtonDrawableRes = R.drawable.btn_primary;
                    p.chipButtonDrawableRes = R.drawable.btn_chip;
                    p.primaryButtonTextColor = Color.parseColor("#FFFFFF");
                    p.isXp = false;
                    break;
            }
            return p;
        }
    }
}
