package com.dodopress.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import com.dodopress.R;

public class ThemeManager {

    public enum AppTheme {
        MATRIX_CRT("🟢 Matrix CRT", "P1 Phosphor • 525nm Laser Terminal", "#00FF66", "#000000"),
        AMBER_CRT("🟡 Amber CRT", "DEC VT220 • Warm P3 Vintage Terminal", "#FFB000", "#050300"),
        CYBER_HUD("⚡ Cyber-HUD", "Tokyo Night • Neon Cyan & Hot Magenta", "#00F0FF", "#060913"),
        TACTICAL_STEALTH("🎯 Stealth OLED", "Night Vision • Precision Laser Lime", "#A3E635", "#000000"),
        SWISS_INDUSTRIAL("📐 Swiss Studio", "Dieter Rams • Basalt & Signal Orange", "#FF5500", "#121214"),
        ARCTIC_GLACIER("❄️ Arctic Frost", "Nordic Deep Glass • Electric Glacier", "#38BDF8", "#080E1E");

        public final String title;
        public final String subtitle;
        public final String previewAccentHex;
        public final String previewBgHex;

        AppTheme(String title, String subtitle, String previewAccentHex, String previewBgHex) {
            this.title = title;
            this.subtitle = subtitle;
            this.previewAccentHex = previewAccentHex;
            this.previewBgHex = previewBgHex;
        }
    }

    private static final String PREF_NAME = "dodopress_theme";
    private static final String KEY_THEME = "current_theme";

    public static AppTheme getTheme(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String name = sp.getString(KEY_THEME, AppTheme.MATRIX_CRT.name());
        try {
            return AppTheme.valueOf(name);
        } catch (Exception e) {
            if ("MATRIX".equals(name)) return AppTheme.MATRIX_CRT;
            if ("CYBERPUNK".equals(name) || "CYBER".equals(name)) return AppTheme.CYBER_HUD;
            if ("DEEP_SLATE".equals(name) || "MIDNIGHT".equals(name)) return AppTheme.TACTICAL_STEALTH;
            if ("STUDIO_LIGHT".equals(name) || "STUDIO".equals(name)) return AppTheme.SWISS_INDUSTRIAL;
            if ("WINDOWS_XP".equals(name)) return AppTheme.MATRIX_CRT;
            return AppTheme.MATRIX_CRT;
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

        public static ThemePalette forTheme(AppTheme theme) {
            ThemePalette p = new ThemePalette();
            p.primaryButtonDrawableRes = R.drawable.btn_primary;
            p.chipButtonDrawableRes = R.drawable.btn_chip;

            switch (theme) {
                case AMBER_CRT:
                    p.bgColor = Color.parseColor("#050300");
                    p.cardBgColor = Color.parseColor("#120A02");
                    p.cardBorderColor = Color.parseColor("#FFB000");
                    p.cardDrawableRes = R.drawable.theme_card_amber;
                    p.textHighColor = Color.parseColor("#FFB000");
                    p.textMedColor = Color.parseColor("#FDBA74");
                    p.textMutedColor = Color.parseColor("#9A3412");
                    p.brandPrimaryColor = Color.parseColor("#FFB000");
                    p.accentColor = Color.parseColor("#FF9100");
                    p.primaryButtonTextColor = Color.parseColor("#050300");
                    p.chipTextColor = Color.parseColor("#FFB000");
                    p.laserColor = Color.parseColor("#FFB000");
                    p.beaconColor = Color.parseColor("#FFB000");
                    break;

                case CYBER_HUD:
                    p.bgColor = Color.parseColor("#060913");
                    p.cardBgColor = Color.parseColor("#0B1220");
                    p.cardBorderColor = Color.parseColor("#00F0FF");
                    p.cardDrawableRes = R.drawable.theme_card_cyber;
                    p.textHighColor = Color.parseColor("#FFFFFF");
                    p.textMedColor = Color.parseColor("#38BDF8");
                    p.textMutedColor = Color.parseColor("#64748B");
                    p.brandPrimaryColor = Color.parseColor("#00F0FF");
                    p.accentColor = Color.parseColor("#FF007F");
                    p.primaryButtonTextColor = Color.parseColor("#060913");
                    p.chipTextColor = Color.parseColor("#00F0FF");
                    p.laserColor = Color.parseColor("#FF007F");
                    p.beaconColor = Color.parseColor("#00F0FF");
                    break;

                case TACTICAL_STEALTH:
                    p.bgColor = Color.parseColor("#000000");
                    p.cardBgColor = Color.parseColor("#11141A");
                    p.cardBorderColor = Color.parseColor("#A3E635");
                    p.cardDrawableRes = R.drawable.theme_card_stealth;
                    p.textHighColor = Color.parseColor("#F8FAFC");
                    p.textMedColor = Color.parseColor("#A3E635");
                    p.textMutedColor = Color.parseColor("#64748B");
                    p.brandPrimaryColor = Color.parseColor("#A3E635");
                    p.accentColor = Color.parseColor("#4ADE80");
                    p.primaryButtonTextColor = Color.parseColor("#000000");
                    p.chipTextColor = Color.parseColor("#A3E635");
                    p.laserColor = Color.parseColor("#A3E635");
                    p.beaconColor = Color.parseColor("#A3E635");
                    break;

                case SWISS_INDUSTRIAL:
                    p.bgColor = Color.parseColor("#121214");
                    p.cardBgColor = Color.parseColor("#1C1C20");
                    p.cardBorderColor = Color.parseColor("#FF5500");
                    p.cardDrawableRes = R.drawable.theme_card_swiss;
                    p.textHighColor = Color.parseColor("#F4F4F5");
                    p.textMedColor = Color.parseColor("#D4D4D8");
                    p.textMutedColor = Color.parseColor("#71717A");
                    p.brandPrimaryColor = Color.parseColor("#FF5500");
                    p.accentColor = Color.parseColor("#FAFAFA");
                    p.primaryButtonTextColor = Color.parseColor("#FFFFFF");
                    p.chipTextColor = Color.parseColor("#F4F4F5");
                    p.laserColor = Color.parseColor("#FF5500");
                    p.beaconColor = Color.parseColor("#FF5500");
                    break;

                case ARCTIC_GLACIER:
                    p.bgColor = Color.parseColor("#080E1E");
                    p.cardBgColor = Color.parseColor("#0F1A34");
                    p.cardBorderColor = Color.parseColor("#38BDF8");
                    p.cardDrawableRes = R.drawable.theme_card_arctic;
                    p.textHighColor = Color.parseColor("#F0F9FF");
                    p.textMedColor = Color.parseColor("#BAE6FD");
                    p.textMutedColor = Color.parseColor("#64748B");
                    p.brandPrimaryColor = Color.parseColor("#38BDF8");
                    p.accentColor = Color.parseColor("#67E8F9");
                    p.primaryButtonTextColor = Color.parseColor("#080E1E");
                    p.chipTextColor = Color.parseColor("#F0F9FF");
                    p.laserColor = Color.parseColor("#38BDF8");
                    p.beaconColor = Color.parseColor("#38BDF8");
                    break;

                case MATRIX_CRT:
                default:
                    p.bgColor = Color.parseColor("#000000");
                    p.cardBgColor = Color.parseColor("#040D07");
                    p.cardBorderColor = Color.parseColor("#00FF66");
                    p.cardDrawableRes = R.drawable.theme_card_matrix;
                    p.textHighColor = Color.parseColor("#00FF66");
                    p.textMedColor = Color.parseColor("#34D399");
                    p.textMutedColor = Color.parseColor("#065F46");
                    p.brandPrimaryColor = Color.parseColor("#00FF66");
                    p.accentColor = Color.parseColor("#00E5FF");
                    p.primaryButtonTextColor = Color.parseColor("#000000");
                    p.chipTextColor = Color.parseColor("#00FF66");
                    p.laserColor = Color.parseColor("#00FF66");
                    p.beaconColor = Color.parseColor("#00FF66");
                    break;
            }
            return p;
        }
    }
}
