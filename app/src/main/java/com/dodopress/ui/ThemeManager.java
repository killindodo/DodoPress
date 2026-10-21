package com.dodopress.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import com.dodopress.R;

public class ThemeManager {

    public enum AppTheme {
        PAPER_STUDIO(
            "Paper Studio",
            "The Elegant Professional • Pure White & Action Blue",
            "#316BCE",
            "#FFFFFF",
            "#F4F5F7",
            "#20242B"
        ),
        OBSIDIAN(
            "Obsidian",
            "The Dark Technical Powerhouse • Graphite & Cyan",
            "#49D9E8",
            "#111318",
            "#1B1F27",
            "#F1F4F8"
        ),
        SANDSTONE(
            "Sandstone",
            "The Warm Human-Centered Workspace • Ivory & Terracotta",
            "#A9523B",
            "#F5EFE5",
            "#E8DDCC",
            "#332D27"
        ),
        COBALT_COMMAND(
            "Cobalt Command",
            "The Bold Productivity Interface • Cobalt & Slate",
            "#2458E8",
            "#F7F9FF",
            "#DDE6FF",
            "#171F33"
        ),
        AETHER_AURORA(
            "Aether Aurora",
            "The Signature Supporter Experience • Ambient Midnight",
            "#9A7CFF",
            "#0C0D1A",
            "#19162E",
            "#E8E7FF"
        );

        public final String title;
        public final String subtitle;
        public final String previewAccentHex;
        public final String previewBgHex;
        public final String previewSurfaceHex;
        public final String previewTextHex;

        AppTheme(String title, String subtitle, String previewAccentHex, String previewBgHex, String previewSurfaceHex, String previewTextHex) {
            this.title = title;
            this.subtitle = subtitle;
            this.previewAccentHex = previewAccentHex;
            this.previewBgHex = previewBgHex;
            this.previewSurfaceHex = previewSurfaceHex;
            this.previewTextHex = previewTextHex;
        }
    }

    private static final String PREF_NAME = "dodopress_theme";
    private static final String KEY_THEME = "current_theme";

    public static AppTheme getTheme(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String name = sp.getString(KEY_THEME, AppTheme.PAPER_STUDIO.name());
        try {
            return AppTheme.valueOf(name);
        } catch (Exception e) {
            // Backward compatibility fallback mappings
            if ("MATRIX_CRT".equals(name) || "MATRIX".equals(name)) return AppTheme.OBSIDIAN;
            if ("AMBER_CRT".equals(name)) return AppTheme.SANDSTONE;
            if ("CYBER_HUD".equals(name) || "CYBERPUNK".equals(name)) return AppTheme.AETHER_AURORA;
            if ("TACTICAL_STEALTH".equals(name) || "STEALTH".equals(name)) return AppTheme.OBSIDIAN;
            if ("SWISS_INDUSTRIAL".equals(name) || "STUDIO_LIGHT".equals(name)) return AppTheme.PAPER_STUDIO;
            if ("ARCTIC_GLACIER".equals(name)) return AppTheme.COBALT_COMMAND;
            return AppTheme.PAPER_STUDIO;
        }
    }

    public static void saveTheme(Context ctx, AppTheme theme) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_THEME, theme.name()).apply();
    }

    public static class ThemePalette {
        public int bgColor;
        public int surfaceColor;
        public int cardBgColor;
        public int cardBorderColor;
        public int cardDrawableRes;
        public int primaryButtonDrawableRes = R.drawable.btn_primary;
        public int secondaryButtonDrawableRes = R.drawable.btn_secondary;
        public int chipButtonDrawableRes = R.drawable.btn_chip;
        public int textHighColor;
        public int textMedColor;
        public int textMutedColor;
        public int brandPrimaryColor;
        public int accentColor;
        public int positiveColor;
        public int primaryButtonTextColor;
        public int secondaryButtonTextColor;
        public int chipTextColor;
        public int previewStageBgColor;
        public boolean isLightStatusBar; // true = dark icons on light status bar
        public boolean isDynamicAurora;
        public int laserColor;
        public int beaconColor;
        public int tabActiveBgColor;
        public int tabInactiveTextColor;
        public int cardCornerRadiusDp;

        public static ThemePalette forTheme(AppTheme theme) {
            ThemePalette p = new ThemePalette();

            switch (theme) {
                case OBSIDIAN:
                    p.bgColor = Color.parseColor("#111318");
                    p.surfaceColor = Color.parseColor("#1B1F27");
                    p.cardBgColor = Color.parseColor("#1B1F27");
                    p.cardBorderColor = Color.parseColor("#343B47");
                    p.cardDrawableRes = R.drawable.theme_card_obsidian;
                    p.textHighColor = Color.parseColor("#F1F4F8");
                    p.textMedColor = Color.parseColor("#9BA3AF");
                    p.textMutedColor = Color.parseColor("#525A68");
                    p.brandPrimaryColor = Color.parseColor("#49D9E8");
                    p.accentColor = Color.parseColor("#49D9E8");
                    p.positiveColor = Color.parseColor("#10B981");
                    p.primaryButtonTextColor = Color.parseColor("#111318"); // high contrast dark on cyan
                    p.secondaryButtonTextColor = Color.parseColor("#F1F4F8");
                    p.chipTextColor = Color.parseColor("#F1F4F8");
                    p.previewStageBgColor = Color.parseColor("#090A0E");
                    p.isLightStatusBar = false;
                    p.isDynamicAurora = false;
                    p.laserColor = Color.parseColor("#49D9E8");
                    p.beaconColor = Color.parseColor("#49D9E8");
                    p.tabActiveBgColor = Color.parseColor("#49D9E8");
                    p.tabInactiveTextColor = Color.parseColor("#9BA3AF");
                    p.cardCornerRadiusDp = 12;
                    break;

                case SANDSTONE:
                    p.bgColor = Color.parseColor("#F5EFE5");
                    p.surfaceColor = Color.parseColor("#E8DDCC");
                    p.cardBgColor = Color.parseColor("#E8DDCC");
                    p.cardBorderColor = Color.parseColor("#D3C5B0");
                    p.cardDrawableRes = R.drawable.theme_card_sandstone;
                    p.textHighColor = Color.parseColor("#332D27");
                    p.textMedColor = Color.parseColor("#6E6257");
                    p.textMutedColor = Color.parseColor("#9E9084");
                    p.brandPrimaryColor = Color.parseColor("#A9523B");
                    p.accentColor = Color.parseColor("#64765A");
                    p.positiveColor = Color.parseColor("#64765A");
                    p.primaryButtonTextColor = Color.WHITE;
                    p.secondaryButtonTextColor = Color.parseColor("#332D27");
                    p.chipTextColor = Color.parseColor("#332D27");
                    p.previewStageBgColor = Color.parseColor("#DFD4C2");
                    p.isLightStatusBar = true;
                    p.isDynamicAurora = false;
                    p.laserColor = Color.parseColor("#A9523B");
                    p.beaconColor = Color.parseColor("#64765A");
                    p.tabActiveBgColor = Color.parseColor("#A9523B");
                    p.tabInactiveTextColor = Color.parseColor("#6E6257");
                    p.cardCornerRadiusDp = 16;
                    break;

                case COBALT_COMMAND:
                    p.bgColor = Color.parseColor("#F7F9FF");
                    p.surfaceColor = Color.parseColor("#DDE6FF");
                    p.cardBgColor = Color.parseColor("#DDE6FF");
                    p.cardBorderColor = Color.parseColor("#C5D4F5");
                    p.cardDrawableRes = R.drawable.theme_card_cobalt_command;
                    p.textHighColor = Color.parseColor("#171F33");
                    p.textMedColor = Color.parseColor("#405175");
                    p.textMutedColor = Color.parseColor("#7584A6");
                    p.brandPrimaryColor = Color.parseColor("#2458E8");
                    p.accentColor = Color.parseColor("#1539A5");
                    p.positiveColor = Color.parseColor("#059669");
                    p.primaryButtonTextColor = Color.WHITE;
                    p.secondaryButtonTextColor = Color.parseColor("#171F33");
                    p.chipTextColor = Color.parseColor("#171F33");
                    p.previewStageBgColor = Color.parseColor("#E2EAFA");
                    p.isLightStatusBar = true;
                    p.isDynamicAurora = false;
                    p.laserColor = Color.parseColor("#2458E8");
                    p.beaconColor = Color.parseColor("#2458E8");
                    p.tabActiveBgColor = Color.parseColor("#2458E8");
                    p.tabInactiveTextColor = Color.parseColor("#405175");
                    p.cardCornerRadiusDp = 10;
                    break;

                case AETHER_AURORA:
                    p.bgColor = Color.parseColor("#0C0D1A");
                    p.surfaceColor = Color.parseColor("#19162E");
                    p.cardBgColor = Color.parseColor("#19162E");
                    p.cardBorderColor = Color.parseColor("#352B5C");
                    p.cardDrawableRes = R.drawable.theme_card_aether_aurora;
                    p.textHighColor = Color.parseColor("#E8E7FF");
                    p.textMedColor = Color.parseColor("#A6A2CC");
                    p.textMutedColor = Color.parseColor("#666191");
                    p.brandPrimaryColor = Color.parseColor("#9A7CFF");
                    p.accentColor = Color.parseColor("#43DCC7");
                    p.positiveColor = Color.parseColor("#43DCC7");
                    p.primaryButtonTextColor = Color.parseColor("#0C0D1A"); // dark on luminous violet
                    p.secondaryButtonTextColor = Color.parseColor("#E8E7FF");
                    p.chipTextColor = Color.parseColor("#E8E7FF");
                    p.previewStageBgColor = Color.parseColor("#070711");
                    p.isLightStatusBar = false;
                    p.isDynamicAurora = true;
                    p.laserColor = Color.parseColor("#9A7CFF");
                    p.beaconColor = Color.parseColor("#43DCC7");
                    p.tabActiveBgColor = Color.parseColor("#9A7CFF");
                    p.tabInactiveTextColor = Color.parseColor("#A6A2CC");
                    p.cardCornerRadiusDp = 16;
                    break;

                case PAPER_STUDIO:
                default:
                    p.bgColor = Color.parseColor("#FFFFFF");
                    p.surfaceColor = Color.parseColor("#F4F5F7");
                    p.cardBgColor = Color.parseColor("#F4F5F7");
                    p.cardBorderColor = Color.parseColor("#DDE2E8");
                    p.cardDrawableRes = R.drawable.theme_card_paper_studio;
                    p.textHighColor = Color.parseColor("#20242B");
                    p.textMedColor = Color.parseColor("#5A6270");
                    p.textMutedColor = Color.parseColor("#8E97A6");
                    p.brandPrimaryColor = Color.parseColor("#316BCE");
                    p.accentColor = Color.parseColor("#316BCE");
                    p.positiveColor = Color.parseColor("#15803D");
                    p.primaryButtonTextColor = Color.WHITE;
                    p.secondaryButtonTextColor = Color.parseColor("#20242B");
                    p.chipTextColor = Color.parseColor("#20242B");
                    p.previewStageBgColor = Color.parseColor("#E8ECEF");
                    p.isLightStatusBar = true;
                    p.isDynamicAurora = false;
                    p.laserColor = Color.parseColor("#316BCE");
                    p.beaconColor = Color.parseColor("#316BCE");
                    p.tabActiveBgColor = Color.parseColor("#316BCE");
                    p.tabInactiveTextColor = Color.parseColor("#5A6270");
                    p.cardCornerRadiusDp = 12;
                    break;
            }
            return p;
        }
    }

    public static Drawable createCardDrawable(ThemePalette p, Context ctx) {
        float density = ctx.getResources().getDisplayMetrics().density;
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(p.cardBgColor);
        gd.setStroke((int) (1 * density), p.cardBorderColor);
        gd.setCornerRadius(p.cardCornerRadiusDp * density);
        return gd;
    }

    public static Drawable createPrimaryButtonDrawable(ThemePalette p, Context ctx) {
        float density = ctx.getResources().getDisplayMetrics().density;
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(p.brandPrimaryColor);
        gd.setCornerRadius(Math.min(14, p.cardCornerRadiusDp) * density);

        int ripple = (p.primaryButtonTextColor == Color.WHITE)
            ? Color.argb(60, 255, 255, 255)
            : Color.argb(40, 0, 0, 0);
        return new RippleDrawable(ColorStateList.valueOf(ripple), gd, null);
    }

    public static Drawable createSecondaryButtonDrawable(ThemePalette p, Context ctx) {
        float density = ctx.getResources().getDisplayMetrics().density;
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(p.surfaceColor);
        gd.setStroke((int) (1 * density), p.cardBorderColor);
        gd.setCornerRadius(Math.min(14, p.cardCornerRadiusDp) * density);

        int ripple = Color.argb(40, 150, 150, 150);
        return new RippleDrawable(ColorStateList.valueOf(ripple), gd, null);
    }

    public static Drawable createChipButtonDrawable(ThemePalette p, boolean selected, Context ctx) {
        float density = ctx.getResources().getDisplayMetrics().density;
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        if (selected) {
            gd.setColor(p.brandPrimaryColor);
            gd.setCornerRadius(8 * density);
        } else {
            gd.setColor(p.surfaceColor);
            gd.setStroke((int) (1 * density), p.cardBorderColor);
            gd.setCornerRadius(8 * density);
        }
        int ripple = Color.argb(40, 150, 150, 150);
        return new RippleDrawable(ColorStateList.valueOf(ripple), gd, null);
    }
}
