package com.dodopress.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dodopress.engine.PrintEngine;
import com.dodopress.engine.ToneEngine;
import com.dodopress.usb.UsbPrinterConnection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {

    private static final int REQUEST_PICK_PDF = 2001;

    private UsbPrinterConnection usbPrinterConnection;
    private PrintEngine printEngine;
    private BroadcastReceiver usbHotplugReceiver;

    // Theming views
    private ScrollView rootScrollView;
    private View layoutXpTitlebar;
    private View layoutModernHeader;

    private Button btnThemeXp;
    private Button btnThemeMidnight;
    private Button btnThemeStudio;
    private Button btnThemeCyber;
    private Button btnThemeWarm;

    private View cardDocPreview;
    private View cardTonerDensity;
    private View cardPageConfig;
    private View cardFooter;

    private TextView tvXpGroupDoc;
    private TextView tvXpGroupDensity;
    private TextView tvXpGroupPages;

    private TextView tvAppTitle;
    private TextView tvProBadge;
    private TextView tvThemeLabel;
    private TextView tvDensityTitle;
    private TextView tvDensityDesc;
    private TextView tvCopiesTitle;
    private TextView tvPagesTitle;
    private TextView tvEngineInfo;
    private TextView tvEngineTag;
    private View layoutEngineBadge;
    private View layoutPageSummaryBadge;
    private TextView tvFooterBranding;
    private TextView tvFooterSub;

    // Controls
    private TextView tvPrinterStatus;
    private Button btnPickFile;
    private TextView tvFileInfo;
    private LinearLayout layoutPreviewContainer;
    private ZoomableImageView ivPagePreview;
    private TextView tvPreviewModeBadge;
    private Button btnTogglePreviewMode;
    private Button btnQuickZoom;
    private Button btnPrevPage;
    private Button btnNextPage;
    private TextView tvPageIndicator;
    private SeekBar seekDarkness;
    private TextView tvDarknessLabel;
    private TextView tvDarknessPct;

    private Button btnPresetDraft;
    private Button btnPresetNormal;
    private Button btnPresetDeep;
    private Button btnPresetUltra;

    private Bitmap toneAdjustedBitmap;
    private boolean isShowingOriginal = false;

    private RadioGroup rgPages;
    private RadioButton rbPagesAll;
    private RadioButton rbPagesCurrent;
    private RadioButton rbPagesOdd;
    private RadioButton rbPagesEven;
    private RadioButton rbPagesCustom;
    private View layoutCustomRange;
    private EditText etCustomPages;
    private CheckBox cbReverseOrder;
    private CheckBox cbCollate;
    private TextView tvPageSummary;

    private Button btnCopiesMinus;
    private Button btnCopiesPlus;
    private TextView tvCopiesCount;

    private ProgressBar pbPrintProgress;
    private TextView tvPrintStatus;
    private Button btnPrintNow;

    private File currentPdfFile;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor pdfPfd;
    private int totalPages = 0;
    private int currentPageIndex = 0;
    private Bitmap rawPageBitmap;
    private int copies = 1;

    private ThemeManager.AppTheme currentTheme = ThemeManager.AppTheme.WINDOWS_XP;

    // Background thread for real-time darkness rendering
    private final ExecutorService previewExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger pendingDarkness = new AtomicInteger(-1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.dodopress.R.layout.activity_main);

        usbPrinterConnection = new UsbPrinterConnection(this);
        printEngine = new PrintEngine(this, usbPrinterConnection);

        initViews();
        setupListeners();
        registerUsbHotplugReceiver();

        currentTheme = ThemeManager.getTheme(this);
        applyAppTheme(currentTheme);

        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            loadPdfFromUri(intent.getData());
        } else if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                loadPdfFromUri(uri);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPrinterStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterUsbHotplugReceiver();
        closePdf();
        usbPrinterConnection.close();
        previewExecutor.shutdown();
    }

    private void initViews() {
        rootScrollView = findViewById(com.dodopress.R.id.root_scroll_view);
        layoutXpTitlebar = findViewById(com.dodopress.R.id.layout_xp_titlebar);
        layoutModernHeader = findViewById(com.dodopress.R.id.layout_modern_header);

        btnThemeXp = findViewById(com.dodopress.R.id.btn_theme_xp);
        btnThemeMidnight = findViewById(com.dodopress.R.id.btn_theme_midnight);
        btnThemeStudio = findViewById(com.dodopress.R.id.btn_theme_studio);
        btnThemeCyber = findViewById(com.dodopress.R.id.btn_theme_cyber);
        btnThemeWarm = findViewById(com.dodopress.R.id.btn_theme_warm);

        cardDocPreview = findViewById(com.dodopress.R.id.card_doc_preview);
        cardTonerDensity = findViewById(com.dodopress.R.id.card_toner_density);
        cardPageConfig = findViewById(com.dodopress.R.id.card_page_config);
        cardFooter = findViewById(com.dodopress.R.id.card_footer);

        tvXpGroupDoc = findViewById(com.dodopress.R.id.tv_xp_group_doc);
        tvXpGroupDensity = findViewById(com.dodopress.R.id.tv_xp_group_density);
        tvXpGroupPages = findViewById(com.dodopress.R.id.tv_xp_group_pages);

        tvAppTitle = findViewById(com.dodopress.R.id.tv_app_title);
        tvProBadge = findViewById(com.dodopress.R.id.tv_pro_badge);
        tvThemeLabel = findViewById(com.dodopress.R.id.tv_theme_label);
        tvDensityTitle = findViewById(com.dodopress.R.id.tv_density_title);
        tvDensityDesc = findViewById(com.dodopress.R.id.tv_density_desc);
        tvCopiesTitle = findViewById(com.dodopress.R.id.tv_copies_title);
        tvPagesTitle = findViewById(com.dodopress.R.id.tv_pages_title);
        tvEngineInfo = findViewById(com.dodopress.R.id.tv_engine_info);
        tvEngineTag = findViewById(com.dodopress.R.id.tv_engine_tag);
        layoutEngineBadge = findViewById(com.dodopress.R.id.layout_engine_badge);
        layoutPageSummaryBadge = findViewById(com.dodopress.R.id.layout_page_summary_badge);
        tvFooterBranding = findViewById(com.dodopress.R.id.tv_footer_branding);
        tvFooterSub = findViewById(com.dodopress.R.id.tv_footer_sub);

        tvPrinterStatus = findViewById(com.dodopress.R.id.tv_printer_status);
        btnPickFile = findViewById(com.dodopress.R.id.btn_pick_file);
        tvFileInfo = findViewById(com.dodopress.R.id.tv_file_info);
        layoutPreviewContainer = findViewById(com.dodopress.R.id.layout_preview_container);
        tvPreviewModeBadge = findViewById(com.dodopress.R.id.tv_preview_mode_badge);
        btnTogglePreviewMode = findViewById(com.dodopress.R.id.btn_toggle_preview_mode);
        btnQuickZoom = findViewById(com.dodopress.R.id.btn_quick_zoom);
        ivPagePreview = findViewById(com.dodopress.R.id.iv_page_preview);
        btnPrevPage = findViewById(com.dodopress.R.id.btn_prev_page);
        btnNextPage = findViewById(com.dodopress.R.id.btn_next_page);
        tvPageIndicator = findViewById(com.dodopress.R.id.tv_page_indicator);
        seekDarkness = findViewById(com.dodopress.R.id.seek_darkness);
        tvDarknessLabel = findViewById(com.dodopress.R.id.tv_darkness_label);
        tvDarknessPct = findViewById(com.dodopress.R.id.tv_darkness_pct);

        btnPresetDraft = findViewById(com.dodopress.R.id.btn_preset_draft);
        btnPresetNormal = findViewById(com.dodopress.R.id.btn_preset_normal);
        btnPresetDeep = findViewById(com.dodopress.R.id.btn_preset_deep);
        btnPresetUltra = findViewById(com.dodopress.R.id.btn_preset_ultra);

        rgPages = findViewById(com.dodopress.R.id.rg_pages);
        rbPagesAll = findViewById(com.dodopress.R.id.rb_pages_all);
        rbPagesCurrent = findViewById(com.dodopress.R.id.rb_pages_current);
        rbPagesOdd = findViewById(com.dodopress.R.id.rb_pages_odd);
        rbPagesEven = findViewById(com.dodopress.R.id.rb_pages_even);
        rbPagesCustom = findViewById(com.dodopress.R.id.rb_pages_custom);
        layoutCustomRange = findViewById(com.dodopress.R.id.layout_custom_range);
        etCustomPages = findViewById(com.dodopress.R.id.et_custom_pages);
        cbReverseOrder = findViewById(com.dodopress.R.id.cb_reverse_order);
        cbCollate = findViewById(com.dodopress.R.id.cb_collate);
        tvPageSummary = findViewById(com.dodopress.R.id.tv_page_summary);

        btnCopiesMinus = findViewById(com.dodopress.R.id.btn_copies_minus);
        btnCopiesPlus = findViewById(com.dodopress.R.id.btn_copies_plus);
        tvCopiesCount = findViewById(com.dodopress.R.id.tv_copies_count);

        pbPrintProgress = findViewById(com.dodopress.R.id.pb_print_progress);
        tvPrintStatus = findViewById(com.dodopress.R.id.tv_print_status);
        btnPrintNow = findViewById(com.dodopress.R.id.btn_print_now);
    }

    private void setupListeners() {
        btnThemeXp.setOnClickListener(v -> applyAppTheme(ThemeManager.AppTheme.WINDOWS_XP));
        btnThemeMidnight.setOnClickListener(v -> applyAppTheme(ThemeManager.AppTheme.MIDNIGHT));
        btnThemeStudio.setOnClickListener(v -> applyAppTheme(ThemeManager.AppTheme.STUDIO));
        btnThemeCyber.setOnClickListener(v -> applyAppTheme(ThemeManager.AppTheme.CYBER));
        btnThemeWarm.setOnClickListener(v -> applyAppTheme(ThemeManager.AppTheme.WARM));

        btnPickFile.setOnClickListener(v -> pickPdf());

        btnTogglePreviewMode.setOnClickListener(v -> {
            isShowingOriginal = !isShowingOriginal;
            if (isShowingOriginal) {
                tvPreviewModeBadge.setText("📄 Original PDF (Raw)");
                tvPreviewModeBadge.setTextColor(Color.parseColor("#94A3B8"));
                btnTogglePreviewMode.setText("View: Deep Ink");
                if (rawPageBitmap != null && !rawPageBitmap.isRecycled()) {
                    ivPagePreview.setImageBitmap(rawPageBitmap);
                }
            } else {
                tvPreviewModeBadge.setText("⚡ Deep Laser Ink Preview");
                tvPreviewModeBadge.setTextColor(Color.parseColor("#38BDF8"));
                btnTogglePreviewMode.setText("Compare: Original");
                if (toneAdjustedBitmap != null && !toneAdjustedBitmap.isRecycled()) {
                    ivPagePreview.setImageBitmap(toneAdjustedBitmap);
                } else {
                    scheduleDarknessPreview(seekDarkness.getProgress());
                }
            }
        });

        btnQuickZoom.setOnClickListener(v -> {
            ivPagePreview.toggleZoom();
            btnQuickZoom.setText(ivPagePreview.isCurrentlyZoomed() ? "↺ Fit" : "🔍 Zoom");
        });

        btnPrevPage.setOnClickListener(v -> {
            if (currentPageIndex > 0) {
                currentPageIndex--;
                renderPageThumb();
            }
        });

        btnNextPage.setOnClickListener(v -> {
            if (currentPageIndex < totalPages - 1) {
                currentPageIndex++;
                renderPageThumb();
            }
        });

        seekDarkness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateDarknessDisplay(progress);
                scheduleDarknessPreview(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnPresetDraft.setOnClickListener(v -> setDarkness(ToneEngine.PRESET_DRAFT));
        btnPresetNormal.setOnClickListener(v -> setDarkness(ToneEngine.PRESET_NORMAL));
        btnPresetDeep.setOnClickListener(v -> setDarkness(ToneEngine.PRESET_DEEP));
        btnPresetUltra.setOnClickListener(v -> setDarkness(ToneEngine.PRESET_ULTRA));

        btnCopiesMinus.setOnClickListener(v -> {
            if (copies > 1) {
                copies--;
                tvCopiesCount.setText(String.valueOf(copies));
                updatePageSummary();
            }
        });

        btnCopiesPlus.setOnClickListener(v -> {
            if (copies < 99) {
                copies++;
                tvCopiesCount.setText(String.valueOf(copies));
                updatePageSummary();
            }
        });

        rgPages.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isCustom = (checkedId == com.dodopress.R.id.rb_pages_custom);
            layoutCustomRange.setVisibility(isCustom ? View.VISIBLE : View.GONE);
            updatePageSummary();
        });

        etCustomPages.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePageSummary();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        cbReverseOrder.setOnCheckedChangeListener((buttonView, isChecked) -> updatePageSummary());
        cbCollate.setOnCheckedChangeListener((buttonView, isChecked) -> updatePageSummary());

        btnPrintNow.setOnClickListener(v -> startPrinting());
    }

    private void applyAppTheme(ThemeManager.AppTheme theme) {
        currentTheme = theme;
        ThemeManager.saveTheme(this, theme);
        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(theme);

        // 1. Root background
        rootScrollView.setBackgroundColor(p.bgColor);

        // 2. XP titlebar vs Modern header
        if (p.isXp) {
            layoutXpTitlebar.setVisibility(View.VISIBLE);
            layoutModernHeader.setVisibility(View.GONE);
            tvXpGroupDoc.setVisibility(View.VISIBLE);
            tvXpGroupDensity.setVisibility(View.VISIBLE);
            tvXpGroupPages.setVisibility(View.VISIBLE);

            cardDocPreview.setBackgroundResource(com.dodopress.R.drawable.theme_card_xp);
            cardTonerDensity.setBackgroundResource(com.dodopress.R.drawable.theme_card_xp);
            cardPageConfig.setBackgroundResource(com.dodopress.R.drawable.theme_card_xp);

            btnPickFile.setBackgroundResource(com.dodopress.R.drawable.theme_btn_xp);
            btnPickFile.setTextColor(Color.parseColor("#000000"));

            btnPrintNow.setBackgroundResource(com.dodopress.R.drawable.theme_btn_xp_primary);
            btnPrintNow.setTextColor(Color.parseColor("#000000"));
            btnPrintNow.setText("OK (Print Document)");

            btnCopiesMinus.setBackgroundResource(com.dodopress.R.drawable.theme_btn_xp);
            btnCopiesMinus.setTextColor(Color.parseColor("#000000"));
            btnCopiesPlus.setBackgroundResource(com.dodopress.R.drawable.theme_btn_xp);
            btnCopiesPlus.setTextColor(Color.parseColor("#000000"));

            btnPrevPage.setBackgroundResource(com.dodopress.R.drawable.theme_btn_xp);
            btnPrevPage.setTextColor(Color.parseColor("#000000"));
            btnNextPage.setBackgroundResource(com.dodopress.R.drawable.theme_btn_xp);
            btnNextPage.setTextColor(Color.parseColor("#000000"));

            btnTogglePreviewMode.setBackgroundResource(com.dodopress.R.drawable.theme_btn_xp);
            btnTogglePreviewMode.setTextColor(Color.parseColor("#000000"));
            btnQuickZoom.setBackgroundResource(com.dodopress.R.drawable.theme_btn_xp);
            btnQuickZoom.setTextColor(Color.parseColor("#000000"));

            tvAppTitle.setTextColor(Color.parseColor("#000000"));
            tvThemeLabel.setTextColor(Color.parseColor("#333333"));
            tvDensityTitle.setTextColor(Color.parseColor("#000000"));
            tvDensityDesc.setTextColor(Color.parseColor("#444444"));
            tvCopiesTitle.setTextColor(Color.parseColor("#000000"));
            tvPagesTitle.setTextColor(Color.parseColor("#000000"));

            rbPagesAll.setTextColor(Color.parseColor("#000000"));
            rbPagesCurrent.setTextColor(Color.parseColor("#000000"));
            rbPagesOdd.setTextColor(Color.parseColor("#000000"));
            rbPagesEven.setTextColor(Color.parseColor("#000000"));
            rbPagesCustom.setTextColor(Color.parseColor("#000000"));
            cbCollate.setTextColor(Color.parseColor("#333333"));
            cbReverseOrder.setTextColor(Color.parseColor("#333333"));

            tvFooterBranding.setTextColor(Color.parseColor("#003399"));
            tvFooterSub.setTextColor(Color.parseColor("#555555"));
        } else {
            layoutXpTitlebar.setVisibility(View.GONE);
            layoutModernHeader.setVisibility(View.VISIBLE);
            tvXpGroupDoc.setVisibility(View.GONE);
            tvXpGroupDensity.setVisibility(View.GONE);
            tvXpGroupPages.setVisibility(View.GONE);

            cardDocPreview.setBackgroundResource(p.cardDrawableRes);
            cardTonerDensity.setBackgroundResource(p.cardDrawableRes);
            cardPageConfig.setBackgroundResource(p.cardDrawableRes);

            btnPickFile.setBackgroundResource(com.dodopress.R.drawable.btn_primary);
            btnPickFile.setTextColor(Color.parseColor("#FFFFFF"));

            btnPrintNow.setBackgroundResource(com.dodopress.R.drawable.btn_primary);
            btnPrintNow.setTextColor(Color.parseColor("#FFFFFF"));
            btnPrintNow.setText("START PRINTING");

            btnCopiesMinus.setBackgroundResource(com.dodopress.R.drawable.btn_secondary);
            btnCopiesMinus.setTextColor(p.textHighColor);
            btnCopiesPlus.setBackgroundResource(com.dodopress.R.drawable.btn_secondary);
            btnCopiesPlus.setTextColor(p.textHighColor);

            btnPrevPage.setBackgroundResource(com.dodopress.R.drawable.btn_secondary);
            btnPrevPage.setTextColor(p.textHighColor);
            btnNextPage.setBackgroundResource(com.dodopress.R.drawable.btn_secondary);
            btnNextPage.setTextColor(p.textHighColor);

            btnTogglePreviewMode.setBackgroundResource(com.dodopress.R.drawable.btn_chip);
            btnTogglePreviewMode.setTextColor(p.textHighColor);
            btnQuickZoom.setBackgroundResource(com.dodopress.R.drawable.btn_chip);
            btnQuickZoom.setTextColor(p.brandPrimaryColor);

            tvAppTitle.setTextColor(p.textHighColor);
            tvThemeLabel.setTextColor(p.textMedColor);
            tvDensityTitle.setTextColor(p.textHighColor);
            tvDensityDesc.setTextColor(p.textMedColor);
            tvCopiesTitle.setTextColor(p.textHighColor);
            tvPagesTitle.setTextColor(p.textHighColor);

            rbPagesAll.setTextColor(p.textHighColor);
            rbPagesCurrent.setTextColor(p.textHighColor);
            rbPagesOdd.setTextColor(p.textHighColor);
            rbPagesEven.setTextColor(p.textHighColor);
            rbPagesCustom.setTextColor(p.textHighColor);
            cbCollate.setTextColor(p.textMedColor);
            cbReverseOrder.setTextColor(p.textHighColor);

            tvFooterBranding.setTextColor(p.brandPrimaryColor);
            tvFooterSub.setTextColor(p.textMedColor);
        }

        // Highlight active theme chip
        btnThemeXp.setBackgroundResource(theme == ThemeManager.AppTheme.WINDOWS_XP ? com.dodopress.R.drawable.btn_theme_chip_active : com.dodopress.R.drawable.btn_theme_chip);
        btnThemeMidnight.setBackgroundResource(theme == ThemeManager.AppTheme.MIDNIGHT ? com.dodopress.R.drawable.btn_theme_chip_active : com.dodopress.R.drawable.btn_theme_chip);
        btnThemeStudio.setBackgroundResource(theme == ThemeManager.AppTheme.STUDIO ? com.dodopress.R.drawable.btn_theme_chip_active : com.dodopress.R.drawable.btn_theme_chip);
        btnThemeCyber.setBackgroundResource(theme == ThemeManager.AppTheme.CYBER ? com.dodopress.R.drawable.btn_theme_chip_active : com.dodopress.R.drawable.btn_theme_chip);
        btnThemeWarm.setBackgroundResource(theme == ThemeManager.AppTheme.WARM ? com.dodopress.R.drawable.btn_theme_chip_active : com.dodopress.R.drawable.btn_theme_chip);

        btnThemeXp.setTextColor(theme == ThemeManager.AppTheme.WINDOWS_XP ? Color.WHITE : Color.parseColor("#94A3B8"));
        btnThemeMidnight.setTextColor(theme == ThemeManager.AppTheme.MIDNIGHT ? Color.WHITE : Color.parseColor("#94A3B8"));
        btnThemeStudio.setTextColor(theme == ThemeManager.AppTheme.STUDIO ? Color.WHITE : Color.parseColor("#94A3B8"));
        btnThemeCyber.setTextColor(theme == ThemeManager.AppTheme.CYBER ? Color.WHITE : Color.parseColor("#94A3B8"));
        btnThemeWarm.setTextColor(theme == ThemeManager.AppTheme.WARM ? Color.WHITE : Color.parseColor("#94A3B8"));

        updatePresetChipHighlight(seekDarkness.getProgress());
        updatePageSummary();
    }

    private void setDarkness(int value) {
        seekDarkness.setProgress(value);
        updatePresetChipHighlight(value);
    }

    private void updatePresetChipHighlight(int progress) {
        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);
        if (p.isXp) {
            btnPresetDraft.setBackgroundResource(progress == ToneEngine.PRESET_DRAFT ? com.dodopress.R.drawable.theme_btn_xp_primary : com.dodopress.R.drawable.theme_btn_xp);
            btnPresetNormal.setBackgroundResource(progress == ToneEngine.PRESET_NORMAL ? com.dodopress.R.drawable.theme_btn_xp_primary : com.dodopress.R.drawable.theme_btn_xp);
            btnPresetDeep.setBackgroundResource(progress == ToneEngine.PRESET_DEEP ? com.dodopress.R.drawable.theme_btn_xp_primary : com.dodopress.R.drawable.theme_btn_xp);
            btnPresetUltra.setBackgroundResource(progress == ToneEngine.PRESET_ULTRA ? com.dodopress.R.drawable.theme_btn_xp_primary : com.dodopress.R.drawable.theme_btn_xp);

            btnPresetDraft.setTextColor(Color.parseColor("#000000"));
            btnPresetNormal.setTextColor(Color.parseColor("#000000"));
            btnPresetDeep.setTextColor(Color.parseColor("#000000"));
            btnPresetUltra.setTextColor(Color.parseColor("#000000"));
        } else {
            btnPresetDraft.setBackgroundResource(progress == ToneEngine.PRESET_DRAFT ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
            btnPresetNormal.setBackgroundResource(progress == ToneEngine.PRESET_NORMAL ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
            btnPresetDeep.setBackgroundResource(progress == ToneEngine.PRESET_DEEP ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
            btnPresetUltra.setBackgroundResource(progress == ToneEngine.PRESET_ULTRA ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);

            btnPresetDraft.setTextColor(progress == ToneEngine.PRESET_DRAFT ? Color.WHITE : p.textMedColor);
            btnPresetNormal.setTextColor(progress == ToneEngine.PRESET_NORMAL ? Color.WHITE : p.textMedColor);
            btnPresetDeep.setTextColor(progress == ToneEngine.PRESET_DEEP ? Color.WHITE : p.textMedColor);
            btnPresetUltra.setTextColor(progress == ToneEngine.PRESET_ULTRA ? Color.WHITE : p.textMedColor);
        }
    }

    private void scheduleDarknessPreview(int progress) {
        if (rawPageBitmap == null || rawPageBitmap.isRecycled()) return;
        pendingDarkness.set(progress);

        previewExecutor.execute(() -> {
            int target = pendingDarkness.getAndSet(-1);
            if (target != -1 && rawPageBitmap != null && !rawPageBitmap.isRecycled()) {
                Bitmap updated = ToneEngine.processPreviewBitmap(rawPageBitmap, target);
                if (updated != null) {
                    mainHandler.post(() -> {
                        toneAdjustedBitmap = updated;
                        if (!isShowingOriginal) {
                            ivPagePreview.setImageBitmap(updated);
                        }
                    });
                }
            }
        });
    }

    private void registerUsbHotplugReceiver() {
        usbHotplugReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) ||
                    UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    refreshPrinterStatus();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(usbHotplugReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(usbHotplugReceiver, filter);
        }
    }

    private void unregisterUsbHotplugReceiver() {
        if (usbHotplugReceiver != null) {
            try {
                unregisterReceiver(usbHotplugReceiver);
            } catch (Exception ignored) {}
            usbHotplugReceiver = null;
        }
    }

    private void refreshPrinterStatus() {
        UsbDevice printer = usbPrinterConnection.findPrinter();
        if (printer != null) {
            String displayName = usbPrinterConnection.getPrinterDisplayName(printer);
            tvPrinterStatus.setText("● " + displayName);
            tvPrinterStatus.setBackgroundResource(com.dodopress.R.drawable.status_badge_connected);
            tvPrinterStatus.setTextColor(getResources().getColor(com.dodopress.R.color.status_connected_text));
        } else {
            tvPrinterStatus.setText("○ Connect USB OTG");
            tvPrinterStatus.setBackgroundResource(com.dodopress.R.drawable.status_badge_waiting);
            tvPrinterStatus.setTextColor(getResources().getColor(com.dodopress.R.color.status_waiting_text));
        }
    }

    private void updateDarknessDisplay(int progress) {
        tvDarknessPct.setText(progress + "%");
        String desc;
        if (progress <= 35) {
            desc = progress + "% — Draft / Toner Saver (Light)";
        } else if (progress <= 65) {
            desc = progress + "% — Standard Laser Print";
        } else if (progress <= 85) {
            desc = progress + "% — Deep Black (PC Match — Locked Default)";
        } else {
            desc = progress + "% — Max Deep (Anti-Blackout Protected)";
        }
        tvDarknessLabel.setText(desc);
    }

    private void pickPdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, REQUEST_PICK_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_PDF && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                loadPdfFromUri(uri);
            }
        }
    }

    private void loadPdfFromUri(Uri uri) {
        try {
            closePdf();

            currentPdfFile = new File(getCacheDir(), "active_print_job.pdf");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(currentPdfFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }

            pdfPfd = ParcelFileDescriptor.open(currentPdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(pdfPfd);
            totalPages = pdfRenderer.getPageCount();
            currentPageIndex = 0;

            long sizeKb = currentPdfFile.length() / 1024;
            tvFileInfo.setText(totalPages + " Page" + (totalPages > 1 ? "s" : "") + " (" + sizeKb + " KB)");
            layoutPreviewContainer.setVisibility(View.VISIBLE);

            renderPageThumb();
            updatePageRangeLabels();
            updatePageSummary();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to read PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void renderPageThumb() {
        if (pdfRenderer == null || totalPages == 0) return;

        try {
            PdfRenderer.Page page = pdfRenderer.openPage(currentPageIndex);
            int pw = page.getWidth();
            int ph = page.getHeight();
            float scale = 800.0f / (float) pw;
            int previewW = 800;
            int previewH = (int) (ph * scale);

            if (rawPageBitmap != null && !rawPageBitmap.isRecycled()) {
                rawPageBitmap.recycle();
            }
            rawPageBitmap = Bitmap.createBitmap(previewW, previewH, Bitmap.Config.ARGB_8888);
            rawPageBitmap.eraseColor(0xFFFFFFFF);

            page.render(rawPageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            page.close();

            tvPageIndicator.setText("Page " + (currentPageIndex + 1) + " of " + totalPages);
            btnPrevPage.setEnabled(currentPageIndex > 0);
            btnNextPage.setEnabled(currentPageIndex < totalPages - 1);
            btnQuickZoom.setText("🔍 Zoom");

            updatePageRangeLabels();
            scheduleDarknessPreview(seekDarkness.getProgress());

        } catch (Exception e) {
            Toast.makeText(this, "Render thumbnail error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePageRangeLabels() {
        if (totalPages <= 0) return;
        rbPagesAll.setText("All Pages (1 – " + totalPages + ")");
        rbPagesCurrent.setText("Current Page Only (" + (currentPageIndex + 1) + ")");
        rbPagesOdd.setText("Odd Pages Only (1, 3, 5...)");
        rbPagesEven.setText("Even Pages Only (2, 4, 6...)");
        updatePageSummary();
    }

    private List<Integer> resolveSelectedPages() {
        List<Integer> pages = new ArrayList<>();
        if (totalPages <= 0) return pages;

        int checkedId = rgPages.getCheckedRadioButtonId();
        if (checkedId == com.dodopress.R.id.rb_pages_all) {
            for (int i = 0; i < totalPages; i++) {
                pages.add(i);
            }
        } else if (checkedId == com.dodopress.R.id.rb_pages_current) {
            pages.add(currentPageIndex);
        } else if (checkedId == com.dodopress.R.id.rb_pages_odd) {
            for (int i = 0; i < totalPages; i += 2) {
                pages.add(i);
            }
        } else if (checkedId == com.dodopress.R.id.rb_pages_even) {
            for (int i = 1; i < totalPages; i += 2) {
                pages.add(i);
            }
        } else if (checkedId == com.dodopress.R.id.rb_pages_custom) {
            String custom = etCustomPages.getText().toString();
            pages = parseCustomPageRange(custom, totalPages);
        }

        if (cbReverseOrder.isChecked() && pages.size() > 1) {
            Collections.reverse(pages);
        }

        return pages;
    }

    public static List<Integer> parseCustomPageRange(String input, int total) {
        List<Integer> result = new ArrayList<>();
        if (input == null || total <= 0) return result;
        Set<Integer> seen = new HashSet<>();
        String[] parts = input.split("[,;\\s]+");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            if (part.contains("-")) {
                int dash = part.indexOf('-');
                String s1 = part.substring(0, dash).trim();
                String s2 = part.substring(dash + 1).trim();
                int start = 1;
                int end = total;
                try {
                    if (!s1.isEmpty()) start = Integer.parseInt(s1);
                    if (!s2.isEmpty()) end = Integer.parseInt(s2);
                } catch (NumberFormatException ignored) {
                    continue;
                }
                start = Math.max(1, Math.min(start, total));
                end = Math.max(1, Math.min(end, total));
                int step = (start <= end) ? 1 : -1;
                for (int p = start; (step > 0 ? p <= end : p >= end); p += step) {
                    int idx = p - 1;
                    if (seen.add(idx)) {
                        result.add(idx);
                    }
                }
            } else {
                try {
                    int p = Integer.parseInt(part);
                    if (p >= 1 && p <= total) {
                        int idx = p - 1;
                        if (seen.add(idx)) {
                            result.add(idx);
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    private void updatePageSummary() {
        if (totalPages <= 0) {
            tvPageSummary.setText("📄 No document loaded yet");
            tvPageSummary.setTextColor(Color.parseColor("#94A3B8"));
            return;
        }

        List<Integer> pages = resolveSelectedPages();
        int count = pages.size();
        int totalSheets = count * copies;

        if (count == 0) {
            tvPageSummary.setText("⚠️ No pages selected (check custom range)");
            tvPageSummary.setTextColor(Color.parseColor("#F87171"));
            return;
        }

        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);
        tvPageSummary.setTextColor(p.brandPrimaryColor);

        StringBuilder sb = new StringBuilder("📄 Ready: ");
        sb.append(count).append(count == 1 ? " page" : " pages");

        if (count <= 6) {
            sb.append(" (");
            for (int i = 0; i < count; i++) {
                if (i > 0) sb.append(", ");
                sb.append(pages.get(i) + 1);
            }
            sb.append(")");
        } else {
            sb.append(" (").append(pages.get(0) + 1).append(" ... ").append(pages.get(count - 1) + 1).append(")");
        }

        if (copies > 1) {
            sb.append(" • ").append(copies).append(" copies");
            if (cbCollate.isChecked()) {
                sb.append(" [Collated]");
            } else {
                sb.append(" [Uncollated]");
            }
        }

        sb.append(" = ").append(totalSheets).append(totalSheets == 1 ? " sheet" : " sheets");

        if (cbReverseOrder.isChecked()) {
            sb.append(" ↺ Reverse");
        }

        tvPageSummary.setText(sb.toString());
    }

    private void startPrinting() {
        if (currentPdfFile == null || !currentPdfFile.exists()) {
            Toast.makeText(this, "Please select a PDF document first.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Integer> pagesToPrint = resolveSelectedPages();
        if (pagesToPrint.isEmpty()) {
            Toast.makeText(this, "Please enter or select a valid page range.", Toast.LENGTH_SHORT).show();
            return;
        }

        UsbDevice printer = usbPrinterConnection.findPrinter();
        if (printer == null) {
            Toast.makeText(this, "No USB printer detected. Connect printer via USB OTG cable.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!usbPrinterConnection.hasPermission(printer)) {
            usbPrinterConnection.requestPermission(printer, this::executePrintJob, () -> {
                Toast.makeText(this, "USB permission was denied.", Toast.LENGTH_SHORT).show();
            });
        } else {
            executePrintJob();
        }
    }

    private void executePrintJob() {
        List<Integer> pagesToPrint = resolveSelectedPages();
        if (pagesToPrint.isEmpty()) {
            Toast.makeText(this, "Please enter a valid page range to print.", Toast.LENGTH_SHORT).show();
            return;
        }

        int darkness = seekDarkness.getProgress();
        int dpi = 300;
        boolean collate = cbCollate.isChecked();

        btnPrintNow.setEnabled(false);
        pbPrintProgress.setVisibility(View.VISIBLE);
        tvPrintStatus.setVisibility(View.VISIBLE);
        pbPrintProgress.setProgress(0);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        printEngine.printPdf(currentPdfFile, darkness, dpi, copies, collate, pagesToPrint, new PrintEngine.PrintProgressListener() {
            @Override
            public void onProgress(int currentPage, int total, String statusMessage) {
                runOnUiThread(() -> {
                    tvPrintStatus.setText(statusMessage);
                    if (total > 0) {
                        pbPrintProgress.setMax(total);
                        pbPrintProgress.setProgress(currentPage);
                    }
                });
            }

            @Override
            public void onComplete() {
                runOnUiThread(() -> {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    btnPrintNow.setEnabled(true);
                    pbPrintProgress.setVisibility(View.GONE);
                    tvPrintStatus.setText("Print job finished successfully!");
                    Toast.makeText(MainActivity.this, "Document printed!", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    btnPrintNow.setEnabled(true);
                    pbPrintProgress.setVisibility(View.GONE);
                    tvPrintStatus.setText("Error: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Printing failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void closePdf() {
        try {
            if (toneAdjustedBitmap != null) {
                toneAdjustedBitmap.recycle();
                toneAdjustedBitmap = null;
            }
            if (rawPageBitmap != null) {
                rawPageBitmap.recycle();
                rawPageBitmap = null;
            }
            if (pdfRenderer != null) {
                pdfRenderer.close();
                pdfRenderer = null;
            }
            if (pdfPfd != null) {
                pdfPfd.close();
                pdfPfd = null;
            }
        } catch (Exception ignored) {}
    }
}
