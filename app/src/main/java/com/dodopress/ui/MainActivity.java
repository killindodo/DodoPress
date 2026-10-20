package com.dodopress.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.pdf.PdfRenderer;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
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
import com.dodopress.engine.SheetGeometry;
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

    // Theming & Navigation views
    private ScrollView rootScrollView;
    private View layoutXpTitlebar;
    private View layoutModernHeader;

    private AnimatedLogoView viewAnimatedLogo;
    private Button btnDiagnostics;
    private View layoutStatusCapsule;
    private PulsingBeaconView viewBeacon;
    private LaserScanView laserScanView;

    private Button btnThemeXp;
    private Button btnThemeCyber;
    private Button btnThemeSlate;
    private Button btnThemeMatrix;
    private Button btnThemeStudio;

    // Recent Prints Shelf
    private View cardRecents;
    private LinearLayout layoutRecentsContainer;
    private TextView tvRecentsTitle;
    private TextView btnClearRecents;

    // Cards
    private View cardDocPreview;
    private View cardTonerDensity;
    private View cardGeometry;
    private View cardPageConfig;
    private View cardFooter;

    private TextView tvXpGroupDoc;
    private TextView tvXpGroupDensity;
    private TextView tvXpGroupGeometry;
    private TextView tvXpGroupPages;

    private TextView tvAppTitle;
    private TextView tvAppSubtitle;
    private TextView tvProBadge;
    private TextView tvThemeLabel;
    private TextView tvDensityTitle;
    private TextView tvDensityDesc;
    private TextView tvGeometryTitle;
    private TextView tvGeometryBadge;
    private TextView tvGeometryDesc;
    private TextView labelPaperSize;
    private TextView labelPlacementPreset;
    private TextView tvCopiesTitle;
    private TextView tvPagesTitle;
    private TextView tvEngineInfo;
    private TextView tvEngineTag;
    private View layoutEngineBadge;
    private View layoutPageSummaryBadge;
    private TextView tvFooterBranding;
    private TextView tvFooterSub;

    // Paper Size & Sheet Placement Controls
    private Button btnPaperA4;
    private Button btnPaperLetter;
    private Button btnPaperLegal;
    private Button btnPaperA5;
    private SheetGeometrySelectorView viewSheetGeometry;
    private Button btnPlaceFull;
    private Button btnPlaceTopHalf;
    private Button btnPlaceBottomHalf;
    private Button btnPlaceLeftHalf;
    private Button btnPlaceRightHalf;
    private Button btnPlaceQuadTl;

    private SheetGeometry.PaperSize currentPaperSize = SheetGeometry.PaperSize.A4;
    private SheetGeometry.PlacementRegion currentPlacementRegion = SheetGeometry.PlacementRegion.FULL_PAGE;

    // Preview Controls
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

        renderRecentsShelf();
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
        if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_SEND.equals(action)) {
            Uri uri = null;
            if (Intent.ACTION_SEND.equals(action)) {
                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            } else {
                uri = intent.getData();
            }
            if (uri != null) {
                DiagnosticLogManager.log("INTENT", "Opened via external Share/View intent: " + uri);
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
        closeCurrentPdf();
        previewExecutor.shutdownNow();
    }

    private void initViews() {
        rootScrollView = findViewById(com.dodopress.R.id.root_scroll_view);
        layoutXpTitlebar = findViewById(com.dodopress.R.id.layout_xp_titlebar);
        layoutModernHeader = findViewById(com.dodopress.R.id.layout_modern_header);

        viewAnimatedLogo = findViewById(com.dodopress.R.id.view_animated_logo);
        btnDiagnostics = findViewById(com.dodopress.R.id.btn_diagnostics);
        layoutStatusCapsule = findViewById(com.dodopress.R.id.layout_status_capsule);
        viewBeacon = findViewById(com.dodopress.R.id.view_beacon);
        laserScanView = findViewById(com.dodopress.R.id.laser_scan_view);

        btnThemeXp = findViewById(com.dodopress.R.id.btn_theme_xp);
        btnThemeCyber = findViewById(com.dodopress.R.id.btn_theme_cyber);
        btnThemeSlate = findViewById(com.dodopress.R.id.btn_theme_slate);
        btnThemeMatrix = findViewById(com.dodopress.R.id.btn_theme_matrix);
        btnThemeStudio = findViewById(com.dodopress.R.id.btn_theme_studio);

        cardRecents = findViewById(com.dodopress.R.id.card_recents);
        layoutRecentsContainer = findViewById(com.dodopress.R.id.layout_recents_container);
        tvRecentsTitle = findViewById(com.dodopress.R.id.tv_recents_title);
        btnClearRecents = findViewById(com.dodopress.R.id.btn_clear_recents);

        cardDocPreview = findViewById(com.dodopress.R.id.card_doc_preview);
        cardTonerDensity = findViewById(com.dodopress.R.id.card_toner_density);
        cardGeometry = findViewById(com.dodopress.R.id.card_geometry);
        cardPageConfig = findViewById(com.dodopress.R.id.card_page_config);
        cardFooter = findViewById(com.dodopress.R.id.card_footer);

        tvXpGroupDoc = findViewById(com.dodopress.R.id.tv_xp_group_doc);
        tvXpGroupDensity = findViewById(com.dodopress.R.id.tv_xp_group_density);
        tvXpGroupGeometry = findViewById(com.dodopress.R.id.tv_xp_group_geometry);
        tvXpGroupPages = findViewById(com.dodopress.R.id.tv_xp_group_pages);

        tvAppTitle = findViewById(com.dodopress.R.id.tv_app_title);
        tvAppSubtitle = findViewById(com.dodopress.R.id.tv_app_subtitle);
        tvProBadge = findViewById(com.dodopress.R.id.tv_pro_badge);
        tvThemeLabel = findViewById(com.dodopress.R.id.tv_theme_label);
        tvDensityTitle = findViewById(com.dodopress.R.id.tv_density_title);
        tvDensityDesc = findViewById(com.dodopress.R.id.tv_density_desc);
        tvGeometryTitle = findViewById(com.dodopress.R.id.tv_geometry_title);
        tvGeometryBadge = findViewById(com.dodopress.R.id.tv_geometry_badge);
        tvGeometryDesc = findViewById(com.dodopress.R.id.tv_geometry_desc);
        labelPaperSize = findViewById(com.dodopress.R.id.label_paper_size);
        labelPlacementPreset = findViewById(com.dodopress.R.id.label_placement_preset);
        tvCopiesTitle = findViewById(com.dodopress.R.id.tv_copies_title);
        tvPagesTitle = findViewById(com.dodopress.R.id.tv_pages_title);
        tvEngineInfo = findViewById(com.dodopress.R.id.tv_engine_info);
        tvEngineTag = findViewById(com.dodopress.R.id.tv_engine_tag);
        layoutEngineBadge = findViewById(com.dodopress.R.id.layout_engine_badge);
        layoutPageSummaryBadge = findViewById(com.dodopress.R.id.layout_page_summary_badge);
        tvFooterBranding = findViewById(com.dodopress.R.id.tv_footer_branding);
        tvFooterSub = findViewById(com.dodopress.R.id.tv_footer_sub);

        // Paper & Placement Views
        btnPaperA4 = findViewById(com.dodopress.R.id.btn_paper_a4);
        btnPaperLetter = findViewById(com.dodopress.R.id.btn_paper_letter);
        btnPaperLegal = findViewById(com.dodopress.R.id.btn_paper_legal);
        btnPaperA5 = findViewById(com.dodopress.R.id.btn_paper_a5);
        viewSheetGeometry = findViewById(com.dodopress.R.id.view_sheet_geometry);
        btnPlaceFull = findViewById(com.dodopress.R.id.btn_place_full);
        btnPlaceTopHalf = findViewById(com.dodopress.R.id.btn_place_top_half);
        btnPlaceBottomHalf = findViewById(com.dodopress.R.id.btn_place_bottom_half);
        btnPlaceLeftHalf = findViewById(com.dodopress.R.id.btn_place_left_half);
        btnPlaceRightHalf = findViewById(com.dodopress.R.id.btn_place_right_half);
        btnPlaceQuadTl = findViewById(com.dodopress.R.id.btn_place_quad_tl);

        tvPrinterStatus = findViewById(com.dodopress.R.id.tv_printer_status);
        btnPickFile = findViewById(com.dodopress.R.id.btn_pick_file);
        tvFileInfo = findViewById(com.dodopress.R.id.tv_file_info);
        layoutPreviewContainer = findViewById(com.dodopress.R.id.layout_preview_container);
        ivPagePreview = findViewById(com.dodopress.R.id.iv_page_preview);
        tvPreviewModeBadge = findViewById(com.dodopress.R.id.tv_preview_mode_badge);
        btnTogglePreviewMode = findViewById(com.dodopress.R.id.btn_toggle_preview_mode);
        btnQuickZoom = findViewById(com.dodopress.R.id.btn_quick_zoom);
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
        btnDiagnostics.setOnClickListener(v -> DiagnosticLogManager.showDiagnosticDialog(this));

        btnClearRecents.setOnClickListener(v -> {
            RecentDocsManager.clearRecents(this);
            renderRecentsShelf();
        });

        // 5 Themes Switching
        btnThemeXp.setOnClickListener(v -> applyAppTheme(ThemeManager.AppTheme.WINDOWS_XP));
        btnThemeCyber.setOnClickListener(v -> applyAppTheme(ThemeManager.AppTheme.CYBERPUNK));
        btnThemeSlate.setOnClickListener(v -> applyAppTheme(ThemeManager.AppTheme.DEEP_SLATE));
        btnThemeMatrix.setOnClickListener(v -> applyAppTheme(ThemeManager.AppTheme.MATRIX));
        btnThemeStudio.setOnClickListener(v -> applyAppTheme(ThemeManager.AppTheme.STUDIO_LIGHT));

        // Paper Size Selection
        btnPaperA4.setOnClickListener(v -> setPaperSize(SheetGeometry.PaperSize.A4));
        btnPaperLetter.setOnClickListener(v -> setPaperSize(SheetGeometry.PaperSize.LETTER));
        btnPaperLegal.setOnClickListener(v -> setPaperSize(SheetGeometry.PaperSize.LEGAL));
        btnPaperA5.setOnClickListener(v -> setPaperSize(SheetGeometry.PaperSize.A5));

        // Placement Region Selection
        btnPlaceFull.setOnClickListener(v -> setPlacementRegion(SheetGeometry.PlacementRegion.FULL_PAGE));
        btnPlaceTopHalf.setOnClickListener(v -> setPlacementRegion(SheetGeometry.PlacementRegion.TOP_HALF));
        btnPlaceBottomHalf.setOnClickListener(v -> setPlacementRegion(SheetGeometry.PlacementRegion.BOTTOM_HALF));
        btnPlaceLeftHalf.setOnClickListener(v -> setPlacementRegion(SheetGeometry.PlacementRegion.LEFT_HALF));
        btnPlaceRightHalf.setOnClickListener(v -> setPlacementRegion(SheetGeometry.PlacementRegion.RIGHT_HALF));
        btnPlaceQuadTl.setOnClickListener(v -> setPlacementRegion(SheetGeometry.PlacementRegion.QUAD_TOP_LEFT));

        viewSheetGeometry.setOnRegionSelectedListener(this::setPlacementRegion);

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
            if (laserScanView != null) {
                laserScanView.triggerScan();
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

        // Universal Tactile Spring Micro-Interactions
        TouchFeedback.applyAll(
            btnPickFile, btnPrintNow, btnDiagnostics,
            btnTogglePreviewMode, btnQuickZoom, btnPrevPage, btnNextPage,
            btnPresetDraft, btnPresetNormal, btnPresetDeep, btnPresetUltra,
            btnCopiesMinus, btnCopiesPlus,
            btnThemeXp, btnThemeCyber, btnThemeSlate, btnThemeMatrix, btnThemeStudio,
            btnPaperA4, btnPaperLetter, btnPaperLegal, btnPaperA5,
            btnPlaceFull, btnPlaceTopHalf, btnPlaceBottomHalf, btnPlaceLeftHalf, btnPlaceRightHalf, btnPlaceQuadTl
        );
    }

    private void setPaperSize(SheetGeometry.PaperSize size) {
        this.currentPaperSize = size;
        viewSheetGeometry.setPaperSize(size);
        updatePaperSizeHighlight(size);
        tvGeometryBadge.setText(size.name() + " • " + currentPlacementRegion.title);
        updatePageSummary();
        renderPageThumb();
    }

    private void setPlacementRegion(SheetGeometry.PlacementRegion region) {
        this.currentPlacementRegion = region;
        viewSheetGeometry.setPlacementRegion(region);
        updatePlacementPresetHighlight(region);
        tvGeometryBadge.setText(currentPaperSize.name() + " • " + region.title);
        updatePageSummary();
        renderPageThumb();
    }

    private void renderRecentsShelf() {
        List<RecentDocsManager.RecentItem> recents = RecentDocsManager.getRecents(this);
        if (recents == null || recents.isEmpty()) {
            cardRecents.setVisibility(View.GONE);
            return;
        }

        cardRecents.setVisibility(View.VISIBLE);
        layoutRecentsContainer.removeAllViews();

        for (RecentDocsManager.RecentItem item : recents) {
            LinearLayout itemCard = new LinearLayout(this);
            itemCard.setOrientation(LinearLayout.VERTICAL);
            itemCard.setBackgroundResource(com.dodopress.R.drawable.card_recent_item);
            int pad = (int) (10 * getResources().getDisplayMetrics().density);
            itemCard.setPadding(pad, pad, pad, pad);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                (int) (140 * getResources().getDisplayMetrics().density),
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, (int) (8 * getResources().getDisplayMetrics().density), 0);
            itemCard.setLayoutParams(lp);

            TextView tvName = new TextView(this);
            tvName.setText(item.title);
            tvName.setTextSize(12);
            tvName.setTextColor(Color.parseColor("#F8FAFC"));
            tvName.setMaxLines(1);
            tvName.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tvName.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvInfo = new TextView(this);
            tvInfo.setText(item.getSubtitle());
            tvInfo.setTextSize(10);
            tvInfo.setTextColor(Color.parseColor("#94A3B8"));

            itemCard.addView(tvName);
            itemCard.addView(tvInfo);

            TouchFeedback.apply(itemCard);
            itemCard.setOnClickListener(v -> {
                try {
                    Uri uri = Uri.parse(item.uriString);
                    loadPdfFromUri(uri);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not open document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            layoutRecentsContainer.addView(itemCard);
        }
    }

    private void applyAppTheme(ThemeManager.AppTheme theme) {
        currentTheme = theme;
        ThemeManager.saveTheme(this, theme);
        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(theme);

        // 1. Root background
        rootScrollView.setBackgroundColor(p.bgColor);

        if (laserScanView != null) {
            laserScanView.setLaserColor(p.laserColor);
        }
        if (viewAnimatedLogo != null) {
            viewAnimatedLogo.setThemeColors(p.brandPrimaryColor, p.accentColor);
        }
        if (viewSheetGeometry != null) {
            viewSheetGeometry.setThemeColors(p.brandPrimaryColor, p.accentColor);
        }

        // 2. XP titlebar vs Modern header
        if (p.isXp) {
            layoutXpTitlebar.setVisibility(View.VISIBLE);
            layoutModernHeader.setVisibility(View.GONE);
            tvXpGroupDoc.setVisibility(View.VISIBLE);
            tvXpGroupDensity.setVisibility(View.VISIBLE);
            tvXpGroupGeometry.setVisibility(View.VISIBLE);
            tvXpGroupPages.setVisibility(View.VISIBLE);

            cardDocPreview.setBackgroundResource(com.dodopress.R.drawable.theme_card_xp);
            cardTonerDensity.setBackgroundResource(com.dodopress.R.drawable.theme_card_xp);
            cardGeometry.setBackgroundResource(com.dodopress.R.drawable.theme_card_xp);
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
            tvGeometryTitle.setTextColor(Color.parseColor("#000000"));
            tvGeometryDesc.setTextColor(Color.parseColor("#444444"));
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
            tvXpGroupGeometry.setVisibility(View.GONE);
            tvXpGroupPages.setVisibility(View.GONE);

            cardDocPreview.setBackgroundResource(p.cardDrawableRes);
            cardTonerDensity.setBackgroundResource(p.cardDrawableRes);
            cardGeometry.setBackgroundResource(p.cardDrawableRes);
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
            tvGeometryTitle.setTextColor(p.textHighColor);
            tvGeometryDesc.setTextColor(p.textMedColor);
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
        btnThemeCyber.setBackgroundResource(theme == ThemeManager.AppTheme.CYBERPUNK ? com.dodopress.R.drawable.btn_theme_chip_active : com.dodopress.R.drawable.btn_theme_chip);
        btnThemeSlate.setBackgroundResource(theme == ThemeManager.AppTheme.DEEP_SLATE ? com.dodopress.R.drawable.btn_theme_chip_active : com.dodopress.R.drawable.btn_theme_chip);
        btnThemeMatrix.setBackgroundResource(theme == ThemeManager.AppTheme.MATRIX ? com.dodopress.R.drawable.btn_theme_chip_active : com.dodopress.R.drawable.btn_theme_chip);
        btnThemeStudio.setBackgroundResource(theme == ThemeManager.AppTheme.STUDIO_LIGHT ? com.dodopress.R.drawable.btn_theme_chip_active : com.dodopress.R.drawable.btn_theme_chip);

        btnThemeXp.setTextColor(theme == ThemeManager.AppTheme.WINDOWS_XP ? Color.WHITE : Color.parseColor("#94A3B8"));
        btnThemeCyber.setTextColor(theme == ThemeManager.AppTheme.CYBERPUNK ? Color.WHITE : Color.parseColor("#94A3B8"));
        btnThemeSlate.setTextColor(theme == ThemeManager.AppTheme.DEEP_SLATE ? Color.WHITE : Color.parseColor("#94A3B8"));
        btnThemeMatrix.setTextColor(theme == ThemeManager.AppTheme.MATRIX ? Color.WHITE : Color.parseColor("#94A3B8"));
        btnThemeStudio.setTextColor(theme == ThemeManager.AppTheme.STUDIO_LIGHT ? Color.WHITE : Color.parseColor("#94A3B8"));

        updatePaperSizeHighlight(currentPaperSize);
        updatePlacementPresetHighlight(currentPlacementRegion);
        updatePresetChipHighlight(seekDarkness.getProgress());
        updatePageSummary();
    }

    private void updatePaperSizeHighlight(SheetGeometry.PaperSize size) {
        btnPaperA4.setBackgroundResource(size == SheetGeometry.PaperSize.A4 ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
        btnPaperLetter.setBackgroundResource(size == SheetGeometry.PaperSize.LETTER ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
        btnPaperLegal.setBackgroundResource(size == SheetGeometry.PaperSize.LEGAL ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
        btnPaperA5.setBackgroundResource(size == SheetGeometry.PaperSize.A5 ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);

        btnPaperA4.setTextColor(size == SheetGeometry.PaperSize.A4 ? Color.WHITE : Color.parseColor("#94A3B8"));
        btnPaperLetter.setTextColor(size == SheetGeometry.PaperSize.LETTER ? Color.WHITE : Color.parseColor("#94A3B8"));
        btnPaperLegal.setTextColor(size == SheetGeometry.PaperSize.LEGAL ? Color.WHITE : Color.parseColor("#94A3B8"));
        btnPaperA5.setTextColor(size == SheetGeometry.PaperSize.A5 ? Color.WHITE : Color.parseColor("#94A3B8"));
    }

    private void updatePlacementPresetHighlight(SheetGeometry.PlacementRegion region) {
        btnPlaceFull.setBackgroundResource(region == SheetGeometry.PlacementRegion.FULL_PAGE ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
        btnPlaceTopHalf.setBackgroundResource(region == SheetGeometry.PlacementRegion.TOP_HALF ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
        btnPlaceBottomHalf.setBackgroundResource(region == SheetGeometry.PlacementRegion.BOTTOM_HALF ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
        btnPlaceLeftHalf.setBackgroundResource(region == SheetGeometry.PlacementRegion.LEFT_HALF ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
        btnPlaceRightHalf.setBackgroundResource(region == SheetGeometry.PlacementRegion.RIGHT_HALF ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
        btnPlaceQuadTl.setBackgroundResource(region == SheetGeometry.PlacementRegion.QUAD_TOP_LEFT ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);

        btnPlaceFull.setTextColor(region == SheetGeometry.PlacementRegion.FULL_PAGE ? Color.WHITE : Color.parseColor("#94A3B8"));
        btnPlaceTopHalf.setTextColor(region == SheetGeometry.PlacementRegion.TOP_HALF ? Color.WHITE : Color.parseColor("#94A3B8"));
        btnPlaceBottomHalf.setTextColor(region == SheetGeometry.PlacementRegion.BOTTOM_HALF ? Color.WHITE : Color.parseColor("#94A3B8"));
        btnPlaceLeftHalf.setTextColor(region == SheetGeometry.PlacementRegion.LEFT_HALF ? Color.WHITE : Color.parseColor("#94A3B8"));
        btnPlaceRightHalf.setTextColor(region == SheetGeometry.PlacementRegion.RIGHT_HALF ? Color.WHITE : Color.parseColor("#94A3B8"));
        btnPlaceQuadTl.setTextColor(region == SheetGeometry.PlacementRegion.QUAD_TOP_LEFT ? Color.WHITE : Color.parseColor("#94A3B8"));
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

    private void scheduleDarknessPreview(int darkness) {
        if (rawPageBitmap == null || rawPageBitmap.isRecycled()) return;

        pendingDarkness.set(darkness);
        previewExecutor.execute(() -> {
            int d = pendingDarkness.getAndSet(-1);
            if (d >= 0 && rawPageBitmap != null && !rawPageBitmap.isRecycled()) {
                Bitmap updated = ToneEngine.processPreviewBitmap(rawPageBitmap, d);
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
            tvPrinterStatus.setText(displayName);
            if (viewBeacon != null) viewBeacon.setBeaconColor(Color.parseColor("#10B981"));
            layoutStatusCapsule.setBackgroundResource(com.dodopress.R.drawable.badge_status_online);
            tvPrinterStatus.setTextColor(Color.parseColor("#34D399"));
            DiagnosticLogManager.log("USB", "Connected: " + displayName);
        } else {
            tvPrinterStatus.setText("Connect USB OTG");
            if (viewBeacon != null) viewBeacon.setBeaconColor(Color.parseColor("#F59E0B"));
            layoutStatusCapsule.setBackgroundResource(com.dodopress.R.drawable.badge_status_offline);
            tvPrinterStatus.setTextColor(Color.parseColor("#FBBF24"));
            DiagnosticLogManager.log("USB", "Waiting for USB OTG cable connection...");
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
        closeCurrentPdf();

        String displayName = "document.pdf";
        long fileSize = 0;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIdx >= 0) displayName = cursor.getString(nameIdx);
                if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx);
            }
        } catch (Exception ignored) {}

        try {
            File cacheFile = new File(getCacheDir(), "print_job_" + System.currentTimeMillis() + ".pdf");
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(cacheFile)) {
                byte[] buf = new byte[16384];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }

            currentPdfFile = cacheFile;
            pdfPfd = ParcelFileDescriptor.open(currentPdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(pdfPfd);
            totalPages = pdfRenderer.getPageCount();
            currentPageIndex = 0;

            String sizeStr = fileSize > 0 ? " (" + (fileSize / 1024) + " KB)" : "";
            tvFileInfo.setText("📄 " + displayName + sizeStr + " • " + totalPages + " " + (totalPages == 1 ? "page" : "pages"));
            layoutPreviewContainer.setVisibility(View.VISIBLE);

            RecentDocsManager.addDoc(this, uri, displayName, totalPages, fileSize / 1024);
            renderRecentsShelf();

            DiagnosticLogManager.log("PDF", "Opened: " + displayName + ", pages=" + totalPages);

            renderPageThumb();
            updatePageRangeLabels();
            updatePageSummary();

        } catch (Exception e) {
            DiagnosticLogManager.logError("PDF", "Failed to load document", e);
            Toast.makeText(this, "Failed to read PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void renderPageThumb() {
        if (pdfRenderer == null || totalPages == 0) return;

        try {
            PdfRenderer.Page page = pdfRenderer.openPage(currentPageIndex);
            int pw = page.getWidth();
            int ph = page.getHeight();

            if (currentPlacementRegion == null || currentPlacementRegion == SheetGeometry.PlacementRegion.FULL_PAGE) {
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
            } else {
                int sheetPreviewW = 800;
                int sheetPreviewH = (int) (800.0f * (currentPaperSize.heightPts / (float) currentPaperSize.widthPts));

                if (rawPageBitmap != null && !rawPageBitmap.isRecycled()) {
                    rawPageBitmap.recycle();
                }
                rawPageBitmap = Bitmap.createBitmap(sheetPreviewW, sheetPreviewH, Bitmap.Config.ARGB_8888);
                rawPageBitmap.eraseColor(0xFFFFFFFF);

                Rect targetBox = SheetGeometry.getTargetBox(currentPlacementRegion, sheetPreviewW, sheetPreviewH);
                Rect destRect = SheetGeometry.calculateAspectFit(pw, ph, targetBox, 16);

                Bitmap docBm = Bitmap.createBitmap(destRect.width(), destRect.height(), Bitmap.Config.ARGB_8888);
                docBm.eraseColor(0xFFFFFFFF);
                page.render(docBm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();

                Canvas canvas = new Canvas(rawPageBitmap);
                canvas.drawBitmap(docBm, destRect.left, destRect.top, null);
                docBm.recycle();
            }

            tvPageIndicator.setText("Page " + (currentPageIndex + 1) + " of " + totalPages);
            btnPrevPage.setEnabled(currentPageIndex > 0);
            btnNextPage.setEnabled(currentPageIndex < totalPages - 1);
            btnQuickZoom.setText("🔍 Zoom");

            updatePageRangeLabels();
            scheduleDarknessPreview(seekDarkness.getProgress());

            if (laserScanView != null) {
                laserScanView.triggerScan();
            }

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
            for (int i = 0; i < totalPages; i++) pages.add(i);
        } else if (checkedId == com.dodopress.R.id.rb_pages_current) {
            pages.add(currentPageIndex);
        } else if (checkedId == com.dodopress.R.id.rb_pages_odd) {
            for (int i = 0; i < totalPages; i++) {
                if ((i + 1) % 2 != 0) pages.add(i);
            }
        } else if (checkedId == com.dodopress.R.id.rb_pages_even) {
            for (int i = 0; i < totalPages; i++) {
                if ((i + 1) % 2 == 0) pages.add(i);
            }
        } else if (checkedId == com.dodopress.R.id.rb_pages_custom) {
            String input = etCustomPages.getText().toString().trim();
            pages = parseCustomPageRange(input, totalPages);
        }

        if (cbReverseOrder.isChecked()) {
            Collections.reverse(pages);
        }
        return pages;
    }

    private List<Integer> parseCustomPageRange(String input, int maxPages) {
        Set<Integer> set = new HashSet<>();
        if (input == null || input.isEmpty()) return new ArrayList<>();

        String[] parts = input.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                if (range.length == 2) {
                    try {
                        int start = Integer.parseInt(range[0].trim());
                        int end = Integer.parseInt(range[1].trim());
                        int min = Math.max(1, Math.min(start, end));
                        int max = Math.min(maxPages, Math.max(start, end));
                        for (int i = min; i <= max; i++) {
                            set.add(i - 1);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                try {
                    int p = Integer.parseInt(part);
                    if (p >= 1 && p <= maxPages) {
                        set.add(p - 1);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        List<Integer> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }

    private void updatePageSummary() {
        if (totalPages <= 0) {
            tvPageSummary.setText("📄 No document loaded");
            tvPageSummary.setTextColor(Color.parseColor("#94A3B8"));
            return;
        }

        List<Integer> selected = resolveSelectedPages();
        int count = selected.size();
        if (count == 0) {
            tvPageSummary.setText("⚠️ No pages selected (check custom range)");
            tvPageSummary.setTextColor(Color.parseColor("#F87171"));
            return;
        }

        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);
        tvPageSummary.setTextColor(p.brandPrimaryColor);

        StringBuilder sb = new StringBuilder("📄 Ready: ");
        sb.append(count).append(count == 1 ? " page" : " pages");

        if (copies > 1) {
            sb.append(" • ").append(copies).append(" copies");
            if (cbCollate.isChecked()) {
                sb.append(" (Collated)");
            } else {
                sb.append(" (Grouped)");
            }
            sb.append(" • Total ").append(count * copies).append(" sheets");
        } else {
            sb.append(" • 1 copy");
        }

        sb.append(" • ").append(currentPaperSize.name());
        if (currentPlacementRegion != SheetGeometry.PlacementRegion.FULL_PAGE) {
            sb.append(" (").append(currentPlacementRegion.title).append(")");
        }

        if (cbReverseOrder.isChecked()) {
            sb.append(" • Reverse order");
        }

        tvPageSummary.setText(sb.toString());
    }

    private void closeCurrentPdf() {
        try {
            if (pdfRenderer != null) pdfRenderer.close();
            if (pdfPfd != null) pdfPfd.close();
            if (rawPageBitmap != null) rawPageBitmap.recycle();
            if (toneAdjustedBitmap != null) toneAdjustedBitmap.recycle();
        } catch (Exception ignored) {}
        pdfRenderer = null;
        pdfPfd = null;
        rawPageBitmap = null;
        toneAdjustedBitmap = null;
        totalPages = 0;
        currentPageIndex = 0;
    }

    private void startPrinting() {
        if (currentPdfFile == null || !currentPdfFile.exists()) {
            Toast.makeText(this, "Please select a PDF document first.", Toast.LENGTH_SHORT).show();
            return;
        }

        UsbDevice printer = usbPrinterConnection.findPrinter();
        if (printer == null) {
            DiagnosticLogManager.log("USB", "Cannot print: HP LaserJet M1005 not found.");
            Toast.makeText(this, "Printer not detected. Connect USB OTG cable.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!usbPrinterConnection.hasPermission(printer)) {
            usbPrinterConnection.requestPermission(printer, this::executePrintJob, () -> {
                DiagnosticLogManager.log("USB", "Permission denied by user.");
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

        DiagnosticLogManager.log("PRINT", "Initiating print job: " + pagesToPrint.size() + " pages, " + copies + " copies, darkness=" + darkness + "%, placement=" + currentPlacementRegion.title);

        btnPrintNow.setEnabled(false);
        pbPrintProgress.setVisibility(View.VISIBLE);
        tvPrintStatus.setVisibility(View.VISIBLE);
        pbPrintProgress.setProgress(0);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (viewAnimatedLogo != null) {
            viewAnimatedLogo.setPrintingActive(true);
        }
        if (laserScanView != null) {
            laserScanView.setVisibility(View.VISIBLE);
            laserScanView.startScan();
        }

        printEngine.printPdf(currentPdfFile, darkness, dpi, copies, collate, pagesToPrint, currentPaperSize, currentPlacementRegion, new PrintEngine.PrintProgressListener() {
            @Override
            public void onProgress(int currentPage, int total, String statusMessage) {
                DiagnosticLogManager.log("PRINT", statusMessage);
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
                DiagnosticLogManager.log("PRINT", "Print job finished successfully!");
                runOnUiThread(() -> {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    btnPrintNow.setEnabled(true);
                    pbPrintProgress.setVisibility(View.GONE);
                    tvPrintStatus.setText("Print job finished successfully!");
                    if (viewAnimatedLogo != null) {
                        viewAnimatedLogo.setPrintingActive(false);
                    }
                    if (laserScanView != null) {
                        laserScanView.stopScan();
                        laserScanView.setVisibility(View.GONE);
                    }
                    Toast.makeText(MainActivity.this, "Document printed!", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(Exception e) {
                DiagnosticLogManager.logError("PRINT", "Printing failed", e);
                runOnUiThread(() -> {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    btnPrintNow.setEnabled(true);
                    pbPrintProgress.setVisibility(View.GONE);
                    tvPrintStatus.setText("Printing error: " + e.getMessage());
                    if (viewAnimatedLogo != null) {
                        viewAnimatedLogo.setPrintingActive(false);
                    }
                    if (laserScanView != null) {
                        laserScanView.stopScan();
                        laserScanView.setVisibility(View.GONE);
                    }
                    Toast.makeText(MainActivity.this, "Printing failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
