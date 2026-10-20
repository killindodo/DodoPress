package com.dodopress.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import com.dodopress.R;

public class ThemeManager {

    public enum AppTheme {
        WINDOWS_XP("🪟 Windows XP"),
        CYBERPUNK("⚡ Cyberpunk"),
        DEEP_SLATE("🌙 Deep Slate"),
        MATRIX("📟 Matrix"),
        STUDIO_LIGHT("☀️ Studio Light");

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
            // Handle legacy theme names gracefully
            if ("MIDNIGHT".equals(name)) return AppTheme.DEEP_SLATE;
            if ("CYBER".equals(name)) return AppTheme.CYBERPUNK;
            if ("STUDIO".equals(name)) return AppTheme.STUDIO_LIGHT;
            return AppTheme.WINDOWS_XP;
        }
    }

    public static void saveTheme(Context ctx, AppTheme theme) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_THEME, theme.name()).apply();
    }

    public static class ThemePalette {
        public int bgColor;
        public int cardBgColor;
        public int cardBorderColor;
        public int cardDrawableRes;
        public int textHighColor;
        public int textMedColor;
        public int textMutedColor;
        public int brandPrimaryColor;
        public int accentColor;
        public int primaryButtonDrawableRes;
        public int chipButtonDrawableRes;
        public int primaryButtonTextColor;
        public int chipTextColor;
        public int laserColor;
        public int beaconColor;
        public boolean isLight;
        public boolean isXp;

        public static ThemePalette forTheme(AppTheme theme) {
            ThemePalette p = new ThemePalette();
            switch (theme) {
                case WINDOWS_XP:
                    p.bgColor = Color.parseColor("#ECE9D8");
                    p.cardBgColor = Color.parseColor("#ECE9D8");
                    p.cardBorderColor = Color.parseColor("#7F9DB9");
                    p.cardDrawableRes = R.drawable.theme_card_xp;
                    p.textHighColor = Color.parseColor("#000000");
                    p.textMedColor = Color.parseColor("#333333");
                    p.textMutedColor = Color.parseColor("#666666");
                    p.brandPrimaryColor = Color.parseColor("#0055EA");
                    p.accentColor = Color.parseColor("#003399");
                    p.primaryButtonDrawableRes = R.drawable.theme_btn_xp_primary;
                    p.chipButtonDrawableRes = R.drawable.theme_btn_xp;
                    p.primaryButtonTextColor = Color.parseColor("#000000");
                    p.chipTextColor = Color.parseColor("#000000");
                    p.laserColor = Color.parseColor("#0055EA");
                    p.beaconColor = Color.parseColor("#22C55E");
                    p.isLight = true;
                    p.isXp = true;
                    break;

                case CYBERPUNK:
                    // Zezes DNA: Deep midnight, Neon Cyan #00F0FF, Hot Magenta #FF007F, Electric Purple #BC13FE
                    p.bgColor = Color.parseColor("#0B0F19");
                    p.cardBgColor = Color.parseColor("#121722");
                    p.cardBorderColor = Color.parseColor("#00F0FF");
                    p.cardDrawableRes = R.drawable.theme_card_cyber;
                    p.textHighColor = Color.parseColor("#FFFFFF");
                    p.textMedColor = Color.parseColor("#00F0FF");
                    p.textMutedColor = Color.parseColor("#7E8B9B");
                    p.brandPrimaryColor = Color.parseColor("#00F0FF");
                    p.accentColor = Color.parseColor("#FF007F");
                    p.primaryButtonDrawableRes = R.drawable.btn_primary;
                    p.chipButtonDrawableRes = R.drawable.btn_chip;
                    p.primaryButtonTextColor = Color.parseColor("#000000");
                    p.chipTextColor = Color.parseColor("#00F0FF");
                    p.laserColor = Color.parseColor("#FF007F");
                    p.beaconColor = Color.parseColor("#00F0FF");
                    p.isLight = false;
                    p.isXp = false;
                    break;

                case MATRIX:
                    // Pinion/Zezes DNA: Pure black, phosphor green #00FF66, CRT border
                    p.bgColor = Color.parseColor("#000000");
                    p.cardBgColor = Color.parseColor("#0A120D");
                    p.cardBorderColor = Color.parseColor("#113322");
                    p.cardDrawableRes = R.drawable.theme_card_matrix;
                    p.textHighColor = Color.parseColor("#00FF66");
                    p.textMedColor = Color.parseColor("#00CC55");
                    p.textMutedColor = Color.parseColor("#008833");
                    p.brandPrimaryColor = Color.parseColor("#00FF66");
                    p.accentColor = Color.parseColor("#00E5FF");
                    p.primaryButtonDrawableRes = R.drawable.btn_primary;
                    p.chipButtonDrawableRes = R.drawable.btn_chip;
                    p.primaryButtonTextColor = Color.parseColor("#000000");
                    p.chipTextColor = Color.parseColor("#00FF66");
                    p.laserColor = Color.parseColor("#00FF66");
                    p.beaconColor = Color.parseColor("#00FF66");
                    p.isLight = false;
                    p.isXp = false;
                    break;

                case STUDIO_LIGHT:
                    // Crisp light mode: ceramic paper white, navy text, royal blue accent
                    p.bgColor = Color.parseColor("#F8FAFC");
                    p.cardBgColor = Color.parseColor("#FFFFFF");
                    p.cardBorderColor = Color.parseColor("#E2E8F0");
                    p.cardDrawableRes = R.drawable.theme_card_studio;
                    p.textHighColor = Color.parseColor("#0F172A");
                    p.textMedColor = Color.parseColor("#475569");
                    p.textMutedColor = Color.parseColor("#94A3B8");
                    p.brandPrimaryColor = Color.parseColor("#0969DA");
                    p.accentColor = Color.parseColor("#2563EB");
                    p.primaryButtonDrawableRes = R.drawable.btn_primary;
                    p.chipButtonDrawableRes = R.drawable.btn_chip;
                    p.primaryButtonTextColor = Color.parseColor("#FFFFFF");
                    p.chipTextColor = Color.parseColor("#0F172A");
                    p.laserColor = Color.parseColor("#0969DA");
                    p.beaconColor = Color.parseColor("#10B981");
                    p.isLight = true;
                    p.isXp = false;
                    break;

                case DEEP_SLATE:
                default:
                    // Pinion DNA: Slate #0F172A, Card #1E293B, Border #334155, Sky Blue #38BDF8
                    p.bgColor = Color.parseColor("#0F172A");
                    p.cardBgColor = Color.parseColor("#1E293B");
                    p.cardBorderColor = Color.parseColor("#334155");
                    p.cardDrawableRes = R.drawable.theme_card_slate;
                    p.textHighColor = Color.parseColor("#F8FAFC");
                    p.textMedColor = Color.parseColor("#94A3B8");
                    p.textMutedColor = Color.parseColor("#64748B");
                    p.brandPrimaryColor = Color.parseColor("#38BDF8");
                    p.accentColor = Color.parseColor("#818CF8");
                    p.primaryButtonDrawableRes = R.drawable.btn_primary;
                    p.chipButtonDrawableRes = R.drawable.btn_chip;
                    p.primaryButtonTextColor = Color.parseColor("#0F172A");
                    p.chipTextColor = Color.parseColor("#F8FAFC");
                    p.laserColor = Color.parseColor("#38BDF8");
                    p.beaconColor = Color.parseColor("#22C55E");
                    p.isLight = false;
                    p.isXp = false;
                    break;
            }
            return p;
        }
    }
}
