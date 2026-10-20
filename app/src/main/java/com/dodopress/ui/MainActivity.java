package com.dodopress.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dodopress.engine.DocumentScannerEngine;
import com.dodopress.engine.IdCardEngine;
import com.dodopress.engine.PrintEngine;
import com.dodopress.engine.SheetGeometry;
import com.dodopress.engine.SpoolManager;
import com.dodopress.engine.ToneEngine;
import com.dodopress.usb.UsbPrinterConnection;
import com.dodopress.util.DodoFileProvider;

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
    private static final int REQUEST_PICK_ID_FRONT = 2002;
    private static final int REQUEST_PICK_ID_BACK = 2003;
    private static final int REQUEST_CAMERA_SCAN = 2004;
    private static final int PERMISSION_CAMERA_REQ = 3001;

    private UsbPrinterConnection usbPrinterConnection;
    private PrintEngine printEngine;
    private BroadcastReceiver usbHotplugReceiver;

    // Camera document scanner & Quick Settings shortcut state
    private boolean isDirectInstantPhotocopy = false;
    private File tempScanFile;
    private PrinterCutawayView.PrinterState currentPrinterCutawayState = PrinterCutawayView.PrinterState.OFFLINE;

    // Multi-page navigation tabs
    private View pagePrintStudio;
    private View pageIdCopier;
    private View pageSpoolQueue;
    private View pageDeveloper;

    private Button tabPrintStudio;
    private Button tabIdCopier;
    private Button tabSpoolQueue;
    private Button tabDeveloper;
    private int activePageIndex = 0;

    // Floating Frosted Glass Action Dock
    private View floatingActionDock;
    private TextView dockTvStatus;
    private Button dockBtnPrint;

    // Theming & Navigation views
    private View rootMainContainer;
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
    private TextView tvCopiesTitle;
    private TextView tvPagesTitle;

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
    private Button btnScanDocument;
    private TextView tvFileInfo;
    private LinearLayout layoutPreviewContainer;
    private ZoomableImageView ivPagePreview;
    private TextView tvPreviewModeBadge;
    private Button btnRotatePage;
    private Button btnTogglePreviewMode;
    private Button btnQuickZoom;
    private Button btnPrevPage;
    private Button btnNextPage;
    private TextView tvPageIndicator;
    private SeekBar seekDarkness;
    private TextView tvDarknessLabel;
    private TextView tvDarknessPct;

    // Blank Page Remover Banner
    private LinearLayout layoutBlankPageBanner;
    private TextView tvBlankPageText;
    private Button btnSkipBlankPage;
    private TextView btnDismissBlankBanner;

    private Button btnPresetDraft;
    private Button btnPresetNormal;
    private Button btnPresetDeep;
    private Button btnPresetUltra;
    private CheckBox cbInvertDark;

    private Bitmap toneAdjustedBitmap;
    private boolean isShowingOriginal = false;
    private float currentPageRotation = 0f;

    private RadioGroup rgPages;
    private RadioButton rbPagesAll;
    private RadioButton rbPagesCurrent;
    private RadioButton rbPagesOdd;
    private RadioButton rbPagesEven;
    private RadioButton rbPagesCustom;
    private View layoutCustomRange;
    private EditText etCustomPages;
    private EditText etWatermark;
    private CheckBox cbReverseOrder;
    private CheckBox cbCollate;
    private TextView tvPageSummary;
    private Button btnQueueJob;

    private Button btnCopiesMinus;
    private Button btnCopiesPlus;
    private TextView tvCopiesCount;

    private ProgressBar pbPrintProgress;
    private TextView tvPrintStatus;
    private Button btnPrintNow;

    // ID Copier Tab Views
    private ImageView ivIdFrontPreview;
    private ImageView ivIdBackPreview;
    private Button btnIdPickFront;
    private Button btnIdPickBack;
    private Button btnIdPrintNow;
    private Bitmap idFrontBitmap;
    private Bitmap idBackBitmap;

    // Spool Queue Tab Views
    private Button btnSpoolClearAll;
    private LinearLayout layoutSpoolItemsContainer;
    private TextView tvSpoolEmpty;

    // Developer Tab Views
    private FrameLayout frameDevAvatar;
    private TextView tvDevName;
    private TextView tvUpiId;
    private Button btnCopyUpi;
    private Button btnFeedbackRequest;
    private CheckBox cbSoundEffects;
    private int devAvatarTapCount = 0;

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

        SoundManager.init(this);
        usbPrinterConnection = new UsbPrinterConnection(this);
        printEngine = new PrintEngine(this, usbPrinterConnection);

        initViews();
        setupListeners();
        registerUsbHotplugReceiver();

        currentTheme = ThemeManager.getTheme(this);
        applyAppTheme(currentTheme);

        showPage(0); // Default to Print Studio
        renderRecentsShelf();
        renderSpoolQueue();
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

        // 1-Touch Instant Photocopy trigger from Quick Settings Tile or Launcher Shortcut
        if (intent.getBooleanExtra(InstantCopyTileService.EXTRA_INSTANT_PHOTOCOPY, false)
            || InstantCopyTileService.ACTION_INSTANT_PHOTOCOPY.equals(action)) {
            showPage(0);
            launchCameraScanner(true);
            return;
        }

        if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_SEND.equals(action)) {
            Uri uri = null;
            if (Intent.ACTION_SEND.equals(action)) {
                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            } else {
                uri = intent.getData();
            }
            if (uri != null) {
                DiagnosticLogManager.log("INTENT", "Opened via external Share/View intent: " + uri);
                showPage(0);
                loadPdfFromUri(uri);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPrinterStatus();
        renderSpoolQueue();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterUsbHotplugReceiver();
        closeCurrentPdf();
        if (idFrontBitmap != null) idFrontBitmap.recycle();
        if (idBackBitmap != null) idBackBitmap.recycle();
        previewExecutor.shutdownNow();
    }

    private void initViews() {
        rootMainContainer = findViewById(com.dodopress.R.id.root_main_container);
        layoutXpTitlebar = findViewById(com.dodopress.R.id.layout_xp_titlebar);
        layoutModernHeader = findViewById(com.dodopress.R.id.layout_modern_header);

        // Pages
        pagePrintStudio = findViewById(com.dodopress.R.id.page_print_studio);
        pageIdCopier = findViewById(com.dodopress.R.id.page_id_copier);
        pageSpoolQueue = findViewById(com.dodopress.R.id.page_spool_queue);
        pageDeveloper = findViewById(com.dodopress.R.id.page_developer);

        // Tabs
        tabPrintStudio = findViewById(com.dodopress.R.id.tab_print_studio);
        tabIdCopier = findViewById(com.dodopress.R.id.tab_id_copier);
        tabSpoolQueue = findViewById(com.dodopress.R.id.tab_spool_queue);
        tabDeveloper = findViewById(com.dodopress.R.id.tab_developer);

        // Floating Action Dock
        floatingActionDock = findViewById(com.dodopress.R.id.floating_action_dock);
        dockTvStatus = findViewById(com.dodopress.R.id.dock_tv_status);
        dockBtnPrint = findViewById(com.dodopress.R.id.dock_btn_print);

        // Motion Views
        viewAnimatedLogo = findViewById(com.dodopress.R.id.view_animated_logo);
        btnDiagnostics = findViewById(com.dodopress.R.id.btn_diagnostics);
        layoutStatusCapsule = findViewById(com.dodopress.R.id.layout_status_capsule);
        viewBeacon = findViewById(com.dodopress.R.id.view_beacon);
        laserScanView = findViewById(com.dodopress.R.id.laser_scan_view);

        // Theme Chips
        btnThemeXp = findViewById(com.dodopress.R.id.btn_theme_xp);
        btnThemeCyber = findViewById(com.dodopress.R.id.btn_theme_cyber);
        btnThemeSlate = findViewById(com.dodopress.R.id.btn_theme_slate);
        btnThemeMatrix = findViewById(com.dodopress.R.id.btn_theme_matrix);
        btnThemeStudio = findViewById(com.dodopress.R.id.btn_theme_studio);

        // Recents
        cardRecents = findViewById(com.dodopress.R.id.card_recents);
        layoutRecentsContainer = findViewById(com.dodopress.R.id.layout_recents_container);
        tvRecentsTitle = findViewById(com.dodopress.R.id.tv_recents_title);
        btnClearRecents = findViewById(com.dodopress.R.id.btn_clear_recents);

        // Cards
        cardDocPreview = findViewById(com.dodopress.R.id.card_doc_preview);
        cardTonerDensity = findViewById(com.dodopress.R.id.card_toner_density);
        cardGeometry = findViewById(com.dodopress.R.id.card_geometry);
        cardPageConfig = findViewById(com.dodopress.R.id.card_page_config);

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
        tvCopiesTitle = findViewById(com.dodopress.R.id.tv_copies_title);
        tvPagesTitle = findViewById(com.dodopress.R.id.tv_pages_title);

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
        btnScanDocument = findViewById(com.dodopress.R.id.btn_scan_document);
        tvFileInfo = findViewById(com.dodopress.R.id.tv_file_info);
        layoutPreviewContainer = findViewById(com.dodopress.R.id.layout_preview_container);
        ivPagePreview = findViewById(com.dodopress.R.id.iv_page_preview);
        layoutBlankPageBanner = findViewById(com.dodopress.R.id.layout_blank_page_banner);
        tvBlankPageText = findViewById(com.dodopress.R.id.tv_blank_page_text);
        btnSkipBlankPage = findViewById(com.dodopress.R.id.btn_skip_blank_page);
        btnDismissBlankBanner = findViewById(com.dodopress.R.id.btn_dismiss_blank_banner);
        tvPreviewModeBadge = findViewById(com.dodopress.R.id.tv_preview_mode_badge);
        btnRotatePage = findViewById(com.dodopress.R.id.btn_rotate_page);
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
        cbInvertDark = findViewById(com.dodopress.R.id.cb_invert_dark);

        rgPages = findViewById(com.dodopress.R.id.rg_pages);
        rbPagesAll = findViewById(com.dodopress.R.id.rb_pages_all);
        rbPagesCurrent = findViewById(com.dodopress.R.id.rb_pages_current);
        rbPagesOdd = findViewById(com.dodopress.R.id.rb_pages_odd);
        rbPagesEven = findViewById(com.dodopress.R.id.rb_pages_even);
        rbPagesCustom = findViewById(com.dodopress.R.id.rb_pages_custom);
        layoutCustomRange = findViewById(com.dodopress.R.id.layout_custom_range);
        etCustomPages = findViewById(com.dodopress.R.id.et_custom_pages);
        etWatermark = findViewById(com.dodopress.R.id.et_watermark);
        cbReverseOrder = findViewById(com.dodopress.R.id.cb_reverse_order);
        cbCollate = findViewById(com.dodopress.R.id.cb_collate);
        tvPageSummary = findViewById(com.dodopress.R.id.tv_page_summary);
        btnQueueJob = findViewById(com.dodopress.R.id.btn_queue_job);

        btnCopiesMinus = findViewById(com.dodopress.R.id.btn_copies_minus);
        btnCopiesPlus = findViewById(com.dodopress.R.id.btn_copies_plus);
        tvCopiesCount = findViewById(com.dodopress.R.id.tv_copies_count);

        pbPrintProgress = findViewById(com.dodopress.R.id.pb_print_progress);
        tvPrintStatus = findViewById(com.dodopress.R.id.tv_print_status);
        btnPrintNow = findViewById(com.dodopress.R.id.btn_print_now);

        // ID Card Copier
        ivIdFrontPreview = findViewById(com.dodopress.R.id.iv_id_front_preview);
        ivIdBackPreview = findViewById(com.dodopress.R.id.iv_id_back_preview);
        btnIdPickFront = findViewById(com.dodopress.R.id.btn_id_pick_front);
        btnIdPickBack = findViewById(com.dodopress.R.id.btn_id_pick_back);
        btnIdPrintNow = findViewById(com.dodopress.R.id.btn_id_print_now);

        // Spool Queue
        btnSpoolClearAll = findViewById(com.dodopress.R.id.btn_spool_clear_all);
        layoutSpoolItemsContainer = findViewById(com.dodopress.R.id.layout_spool_items_container);
        tvSpoolEmpty = findViewById(com.dodopress.R.id.tv_spool_empty);

        // Developer Page
        frameDevAvatar = findViewById(com.dodopress.R.id.frame_dev_avatar);
        tvDevName = findViewById(com.dodopress.R.id.tv_dev_name);
        tvUpiId = findViewById(com.dodopress.R.id.tv_upi_id);
        btnCopyUpi = findViewById(com.dodopress.R.id.btn_copy_upi);
        btnFeedbackRequest = findViewById(com.dodopress.R.id.btn_feedback_request);
        cbSoundEffects = findViewById(com.dodopress.R.id.cb_sound_effects);
    }

    private void showPage(int index) {
        activePageIndex = index;
        pagePrintStudio.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        pageIdCopier.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        pageSpoolQueue.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        pageDeveloper.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        floatingActionDock.setVisibility((index == 0 || index == 1) ? View.VISIBLE : View.GONE);

        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);
        tabPrintStudio.setTextColor(index == 0 ? p.brandPrimaryColor : p.textMedColor);
        tabIdCopier.setTextColor(index == 1 ? p.brandPrimaryColor : p.textMedColor);
        tabSpoolQueue.setTextColor(index == 2 ? p.brandPrimaryColor : p.textMedColor);
        tabDeveloper.setTextColor(index == 3 ? p.brandPrimaryColor : p.textMedColor);

        if (index == 2) {
            renderSpoolQueue();
        }
    }

    private void setupListeners() {
        // Tab Clicks
        tabPrintStudio.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            showPage(0);
        });
        tabIdCopier.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            showPage(1);
        });
        tabSpoolQueue.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            showPage(2);
        });
        tabDeveloper.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            showPage(3);
        });

        // Floating Action Dock Print Button
        dockBtnPrint.setOnClickListener(v -> {
            SoundManager.playClick();
            v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
            startPrinting();
        });

        btnDiagnostics.setOnClickListener(v -> {
            SoundManager.playClick();
            DiagnosticLogManager.showDiagnosticDialog(this, getCurrentPrinterCutawayState());
        });

        btnClearRecents.setOnClickListener(v -> {
            SoundManager.playClick();
            RecentDocsManager.clearRecents(this);
            renderRecentsShelf();
        });

        // 5 Themes Switching
        btnThemeXp.setOnClickListener(v -> { SoundManager.playClick(); applyAppTheme(ThemeManager.AppTheme.WINDOWS_XP); });
        btnThemeCyber.setOnClickListener(v -> { SoundManager.playClick(); applyAppTheme(ThemeManager.AppTheme.CYBERPUNK); });
        btnThemeSlate.setOnClickListener(v -> { SoundManager.playClick(); applyAppTheme(ThemeManager.AppTheme.DEEP_SLATE); });
        btnThemeMatrix.setOnClickListener(v -> { SoundManager.playClick(); applyAppTheme(ThemeManager.AppTheme.MATRIX); });
        btnThemeStudio.setOnClickListener(v -> { SoundManager.playClick(); applyAppTheme(ThemeManager.AppTheme.STUDIO_LIGHT); });

        // Paper Size Selection
        btnPaperA4.setOnClickListener(v -> { SoundManager.playClick(); setPaperSize(SheetGeometry.PaperSize.A4); });
        btnPaperLetter.setOnClickListener(v -> { SoundManager.playClick(); setPaperSize(SheetGeometry.PaperSize.LETTER); });
        btnPaperLegal.setOnClickListener(v -> { SoundManager.playClick(); setPaperSize(SheetGeometry.PaperSize.LEGAL); });
        btnPaperA5.setOnClickListener(v -> { SoundManager.playClick(); setPaperSize(SheetGeometry.PaperSize.A5); });

        // Placement Region Selection
        btnPlaceFull.setOnClickListener(v -> { SoundManager.playClick(); setPlacementRegion(SheetGeometry.PlacementRegion.FULL_PAGE); });
        btnPlaceTopHalf.setOnClickListener(v -> { SoundManager.playClick(); setPlacementRegion(SheetGeometry.PlacementRegion.TOP_HALF); });
        btnPlaceBottomHalf.setOnClickListener(v -> { SoundManager.playClick(); setPlacementRegion(SheetGeometry.PlacementRegion.BOTTOM_HALF); });
        btnPlaceLeftHalf.setOnClickListener(v -> { SoundManager.playClick(); setPlacementRegion(SheetGeometry.PlacementRegion.LEFT_HALF); });
        btnPlaceRightHalf.setOnClickListener(v -> { SoundManager.playClick(); setPlacementRegion(SheetGeometry.PlacementRegion.RIGHT_HALF); });
        btnPlaceQuadTl.setOnClickListener(v -> { SoundManager.playClick(); setPlacementRegion(SheetGeometry.PlacementRegion.QUAD_TOP_LEFT); });

        viewSheetGeometry.setOnRegionSelectedListener(this::setPlacementRegion);

        btnPickFile.setOnClickListener(v -> {
            SoundManager.playClick();
            pickPdf();
        });

        btnScanDocument.setOnClickListener(v -> {
            SoundManager.playClick();
            launchCameraScanner(false);
        });

        // 1-Tap 90° Page Rotation
        btnRotatePage.setOnClickListener(v -> {
            SoundManager.playClick();
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            currentPageRotation = (currentPageRotation + 90f) % 360f;
            renderPageThumb();
        });

        // Smart Toner Shield (Dark PDF Invert)
        cbInvertDark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SoundManager.playClick();
            buttonView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            renderPageThumb();
        });

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
                btnTogglePreviewMode.setText("Compare");
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
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }
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

        // Queue Job for Offline Printing
        btnQueueJob.setOnClickListener(v -> {
            if (currentPdfFile == null || !currentPdfFile.exists()) {
                Toast.makeText(this, "Please select a PDF document first.", Toast.LENGTH_SHORT).show();
                return;
            }
            SpoolManager.SpoolJob job = SpoolManager.enqueueJob(
                this, currentPdfFile, tvFileInfo.getText().toString(), totalPages, copies, seekDarkness.getProgress(), currentPaperSize, currentPlacementRegion
            );
            if (job != null) {
                Toast.makeText(this, "Document queued! Will prompt to print once USB is connected.", Toast.LENGTH_LONG).show();
                renderSpoolQueue();
            }
        });

        btnPrintNow.setOnClickListener(v -> startPrinting());

        // ID Card Copier Listeners
        btnIdPickFront.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_ID_FRONT);
        });

        btnIdPickBack.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_ID_BACK);
        });

        btnIdPrintNow.setOnClickListener(v -> printIdCardDirect());

        // Spool Queue Listeners
        btnSpoolClearAll.setOnClickListener(v -> {
            SpoolManager.clearQueue(this);
            renderSpoolQueue();
        });

        // Developer Page & Easter Egg Listeners
        btnCopyUpi.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("UPI ID", "killindodo@upi");
            if (cm != null) cm.setPrimaryClip(clip);
            Toast.makeText(this, "UPI ID copied! Thank you for supporting DodoPress ❤️", Toast.LENGTH_LONG).show();
        });

        btnFeedbackRequest.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:support@dodopress.org"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "DodoPress Pro - Feature / Support Request");
            try {
                startActivity(Intent.createChooser(emailIntent, "Send Request"));
            } catch (Exception e) {
                Toast.makeText(this, "No email client installed.", Toast.LENGTH_SHORT).show();
            }
        });

        // Sound Effects Toggle
        cbSoundEffects.setChecked(SoundManager.isSoundEnabled());
        cbSoundEffects.setOnCheckedChangeListener((btn, isChecked) -> {
            SoundManager.setSoundEnabled(this, isChecked);
            if (isChecked) SoundManager.playClick();
        });

        // Secret Easter Egg: Tap avatar 7 times
        frameDevAvatar.setOnClickListener(v -> {
            devAvatarTapCount++;
            SoundManager.playClick();
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            if (devAvatarTapCount == 7) {
                devAvatarTapCount = 0;
                SoundManager.playXpPrintChime();
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                new AlertDialog.Builder(this)
                    .setTitle("🎉 Easter Egg Unlocked!")
                    .setMessage("⚡ Dodo Quantum Print Mode Activated!\n\nEngineered with love and relentless passion by killindodo.\nKeep on hacking and printing offline!")
                    .setPositiveButton("Awesome!", (dialog, which) -> dialog.dismiss())
                    .show();
            }
        });

        // Universal Tactile Spring Micro-Interactions
        TouchFeedback.applyAll(
            btnPickFile, btnScanDocument, btnPrintNow, dockBtnPrint, btnDiagnostics,
            btnTogglePreviewMode, btnQuickZoom, btnRotatePage, btnPrevPage, btnNextPage,
            btnPresetDraft, btnPresetNormal, btnPresetDeep, btnPresetUltra,
            btnCopiesMinus, btnCopiesPlus, btnQueueJob,
            btnThemeXp, btnThemeCyber, btnThemeSlate, btnThemeMatrix, btnThemeStudio,
            btnPaperA4, btnPaperLetter, btnPaperLegal, btnPaperA5,
            btnPlaceFull, btnPlaceTopHalf, btnPlaceBottomHalf, btnPlaceLeftHalf, btnPlaceRightHalf, btnPlaceQuadTl,
            btnIdPickFront, btnIdPickBack, btnIdPrintNow, btnCopyUpi, btnFeedbackRequest
        );
    }

    private void printIdCardDirect() {
        if (idFrontBitmap == null && idBackBitmap == null) {
            Toast.makeText(this, "Please pick Front or Back of ID Card first.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Compose onto A4 sheet top half (2480 x 3508 px @ 300 DPI)
            Bitmap composedSheet = IdCardEngine.composeIdCardSheet(idFrontBitmap, idBackBitmap, 2480, 3508);
            File idCardPdf = new File(getCacheDir(), "id_card_" + System.currentTimeMillis() + ".pdf");

            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Rect dest = new Rect(0, 0, 595, 842);
            canvas.drawBitmap(composedSheet, null, dest, null);
            document.finishPage(page);

            try (FileOutputStream fos = new FileOutputStream(idCardPdf)) {
                document.writeTo(fos);
            }
            document.close();
            composedSheet.recycle();

            // Load into Print Studio and start print
            showPage(0);
            loadPdfFromUri(Uri.fromFile(idCardPdf));
            Toast.makeText(this, "ID Card ready on A4 top half!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Failed to compose ID card: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void renderSpoolQueue() {
        List<SpoolManager.SpoolJob> jobs = SpoolManager.getQueuedJobs(this);
        layoutSpoolItemsContainer.removeAllViews();

        if (jobs.isEmpty()) {
            tvSpoolEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvSpoolEmpty.setVisibility(View.GONE);
        for (SpoolManager.SpoolJob job : jobs) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setBackgroundResource(com.dodopress.R.drawable.card_bg);
            int pad = (int) (12 * getResources().getDisplayMetrics().density);
            item.setPadding(pad, pad, pad, pad);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, 0, (int) (10 * getResources().getDisplayMetrics().density));
            item.setLayoutParams(lp);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(job.title);
            tvTitle.setTextColor(Color.parseColor("#F8FAFC"));
            tvTitle.setTextSize(13);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvSub = new TextView(this);
            tvSub.setText(job.getSubtitle());
            tvSub.setTextColor(Color.parseColor("#94A3B8"));
            tvSub.setTextSize(11);
            tvSub.setPadding(0, 4, 0, 8);

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);

            Button btnPrintJob = new Button(this);
            btnPrintJob.setText("🖨️ Print Now");
            btnPrintJob.setBackgroundResource(com.dodopress.R.drawable.btn_primary);
            btnPrintJob.setTextColor(Color.WHITE);
            btnPrintJob.setTextSize(11);
            LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, (int) (36 * getResources().getDisplayMetrics().density), 1f);
            pLp.setMargins(0, 0, 8, 0);
            btnPrintJob.setLayoutParams(pLp);
            btnPrintJob.setOnClickListener(v -> {
                File f = new File(job.filePath);
                if (f.exists()) {
                    showPage(0);
                    loadPdfFromUri(Uri.fromFile(f));
                }
            });

            Button btnDel = new Button(this);
            btnDel.setText("🗑️ Delete");
            btnDel.setBackgroundResource(com.dodopress.R.drawable.btn_chip);
            btnDel.setTextColor(Color.parseColor("#EF4444"));
            btnDel.setTextSize(11);
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(0, (int) (36 * getResources().getDisplayMetrics().density), 1f);
            btnDel.setLayoutParams(dLp);
            btnDel.setOnClickListener(v -> {
                SpoolManager.removeJob(this, job.id);
                renderSpoolQueue();
            });

            actions.addView(btnPrintJob);
            actions.addView(btnDel);

            item.addView(tvTitle);
            item.addView(tvSub);
            item.addView(actions);

            layoutSpoolItemsContainer.addView(item);
        }
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
        rootMainContainer.setBackgroundColor(p.bgColor);

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
            btnScanDocument.setBackgroundResource(com.dodopress.R.drawable.theme_btn_xp);
            btnScanDocument.setTextColor(Color.parseColor("#000000"));

            btnPrintNow.setBackgroundResource(com.dodopress.R.drawable.theme_btn_xp_primary);
            btnPrintNow.setTextColor(Color.parseColor("#000000"));
            dockBtnPrint.setBackgroundResource(com.dodopress.R.drawable.theme_btn_xp_primary);
            dockBtnPrint.setTextColor(Color.parseColor("#000000"));

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
            btnRotatePage.setBackgroundResource(com.dodopress.R.drawable.theme_btn_xp);
            btnRotatePage.setTextColor(Color.parseColor("#000000"));
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
            cbInvertDark.setTextColor(Color.parseColor("#333333"));
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
            btnScanDocument.setBackgroundResource(com.dodopress.R.drawable.btn_secondary);
            btnScanDocument.setTextColor(p.textHighColor);

            btnPrintNow.setBackgroundResource(com.dodopress.R.drawable.btn_primary);
            btnPrintNow.setTextColor(Color.parseColor("#FFFFFF"));
            dockBtnPrint.setBackgroundResource(com.dodopress.R.drawable.btn_primary);
            dockBtnPrint.setTextColor(Color.parseColor("#FFFFFF"));

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
            btnRotatePage.setBackgroundResource(com.dodopress.R.drawable.btn_chip);
            btnRotatePage.setTextColor(p.textHighColor);
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
            cbInvertDark.setTextColor(p.textHighColor);
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
        showPage(activePageIndex);
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
            currentPrinterCutawayState = PrinterCutawayView.PrinterState.READY;
            String displayName = usbPrinterConnection.getPrinterDisplayName(printer);
            tvPrinterStatus.setText(displayName);
            dockTvStatus.setText(displayName + " (Online)");
            if (viewBeacon != null) viewBeacon.setBeaconColor(Color.parseColor("#10B981"));
            layoutStatusCapsule.setBackgroundResource(com.dodopress.R.drawable.badge_status_online);
            tvPrinterStatus.setTextColor(Color.parseColor("#34D399"));
            DiagnosticLogManager.log("USB", "Connected: " + displayName);

            // Prompt if offline queue has items waiting
            List<SpoolManager.SpoolJob> queued = SpoolManager.getQueuedJobs(this);
            if (!queued.isEmpty()) {
                Toast.makeText(this, "HP LaserJet connected: " + queued.size() + " queued job(s) waiting in Queue tab!", Toast.LENGTH_LONG).show();
            }
        } else {
            currentPrinterCutawayState = PrinterCutawayView.PrinterState.OFFLINE;
            tvPrinterStatus.setText("Connect USB OTG");
            dockTvStatus.setText("Printer Offline (OTG Cable Needed)");
            if (viewBeacon != null) viewBeacon.setBeaconColor(Color.parseColor("#F59E0B"));
            layoutStatusCapsule.setBackgroundResource(com.dodopress.R.drawable.badge_status_offline);
            tvPrinterStatus.setTextColor(Color.parseColor("#FBBF24"));
            DiagnosticLogManager.log("USB", "Waiting for USB OTG cable connection...");
        }
    }

    private PrinterCutawayView.PrinterState getCurrentPrinterCutawayState() {
        UsbDevice printer = usbPrinterConnection.findPrinter();
        if (printer == null) {
            return PrinterCutawayView.PrinterState.OFFLINE;
        }
        return currentPrinterCutawayState;
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

    private void launchCameraScanner(boolean directPrint) {
        this.isDirectInstantPhotocopy = directPrint;
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA_REQ);
            return;
        }

        try {
            tempScanFile = new File(getCacheDir(), "scan_capture.jpg");
            if (tempScanFile.exists()) tempScanFile.delete();

            Uri photoUri = Uri.parse("content://" + DodoFileProvider.AUTHORITY + "/scan_capture.jpg");
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(takePictureIntent, REQUEST_CAMERA_SCAN);
        } catch (Exception e) {
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CAMERA_REQ) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCameraScanner(isDirectInstantPhotocopy);
            } else {
                Toast.makeText(this, "Camera permission needed for document scanner.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void processScannedDocument() {
        new Thread(() -> {
            try {
                if (tempScanFile == null || !tempScanFile.exists() || tempScanFile.length() == 0) {
                    runOnUiThread(() -> Toast.makeText(this, "No photo captured.", Toast.LENGTH_SHORT).show());
                    return;
                }

                runOnUiThread(() -> Toast.makeText(this, "Enhancing document & removing shadows...", Toast.LENGTH_SHORT).show());

                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap raw = BitmapFactory.decodeFile(tempScanFile.getAbsolutePath(), opts);
                if (raw == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to decode photo.", Toast.LENGTH_SHORT).show());
                    return;
                }

                Bitmap enhanced = DocumentScannerEngine.enhanceDocument(raw);
                File scanPdf = new File(getCacheDir(), "scan_" + System.currentTimeMillis() + ".pdf");
                DocumentScannerEngine.saveBitmapAsA4Pdf(enhanced, scanPdf);

                enhanced.recycle();
                raw.recycle();

                runOnUiThread(() -> {
                    SoundManager.playClick();
                    showPage(0);
                    loadPdfFromUri(Uri.fromFile(scanPdf));
                    Toast.makeText(this, "Document enhanced! Ready to print.", Toast.LENGTH_SHORT).show();

                    if (isDirectInstantPhotocopy) {
                        isDirectInstantPhotocopy = false;
                        startPrinting();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Scan processing error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_CAMERA_SCAN) {
            processScannedDocument();
            return;
        }

        if (data == null) return;

        if (requestCode == REQUEST_PICK_PDF) {
            Uri uri = data.getData();
            if (uri != null) {
                loadPdfFromUri(uri);
            }
        } else if (requestCode == REQUEST_PICK_ID_FRONT) {
            Uri uri = data.getData();
            if (uri != null) {
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    idFrontBitmap = BitmapFactory.decodeStream(in);
                    ivIdFrontPreview.setImageBitmap(idFrontBitmap);
                    Toast.makeText(this, "ID Front loaded!", Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {}
            }
        } else if (requestCode == REQUEST_PICK_ID_BACK) {
            Uri uri = data.getData();
            if (uri != null) {
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    idBackBitmap = BitmapFactory.decodeStream(in);
                    ivIdBackPreview.setImageBitmap(idBackBitmap);
                    Toast.makeText(this, "ID Back loaded!", Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {}
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
            currentPageRotation = 0f;

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

            Bitmap baseBm;
            if (currentPlacementRegion == null || currentPlacementRegion == SheetGeometry.PlacementRegion.FULL_PAGE) {
                float scale = 800.0f / (float) pw;
                int previewW = 800;
                int previewH = (int) (ph * scale);

                baseBm = Bitmap.createBitmap(previewW, previewH, Bitmap.Config.ARGB_8888);
                baseBm.eraseColor(0xFFFFFFFF);
                page.render(baseBm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
            } else {
                int sheetPreviewW = 800;
                int sheetPreviewH = (int) (800.0f * (currentPaperSize.heightPts / (float) currentPaperSize.widthPts));

                baseBm = Bitmap.createBitmap(sheetPreviewW, sheetPreviewH, Bitmap.Config.ARGB_8888);
                baseBm.eraseColor(0xFFFFFFFF);

                Rect targetBox = SheetGeometry.getTargetBox(currentPlacementRegion, sheetPreviewW, sheetPreviewH);
                Rect destRect = SheetGeometry.calculateAspectFit(pw, ph, targetBox, 16);

                Bitmap docBm = Bitmap.createBitmap(destRect.width(), destRect.height(), Bitmap.Config.ARGB_8888);
                docBm.eraseColor(0xFFFFFFFF);
                page.render(docBm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();

                Canvas canvas = new Canvas(baseBm);
                canvas.drawBitmap(docBm, destRect.left, destRect.top, null);
                docBm.recycle();
            }

            // Apply rotation if any
            if (currentPageRotation != 0) {
                Bitmap rot = ToneEngine.rotateBitmap(baseBm, currentPageRotation);
                if (rot != baseBm) {
                    baseBm.recycle();
                    baseBm = rot;
                }
            }

            // Apply Toner Shield (Invert Dark Background)
            if (cbInvertDark.isChecked()) {
                Bitmap inv = ToneEngine.invertDarkDocument(baseBm);
                if (inv != baseBm) {
                    baseBm.recycle();
                    baseBm = inv;
                }
            }

            if (rawPageBitmap != null && !rawPageBitmap.isRecycled()) {
                rawPageBitmap.recycle();
            }
            rawPageBitmap = baseBm;

            tvPageIndicator.setText("Page " + (currentPageIndex + 1) + " of " + totalPages);
            btnPrevPage.setEnabled(currentPageIndex > 0);
            btnNextPage.setEnabled(currentPageIndex < totalPages - 1);
            btnQuickZoom.setText("🔍 Zoom");

            updatePageRangeLabels();
            scheduleDarknessPreview(seekDarkness.getProgress());

            // Auto-Blank Page Detection
            boolean isBlank = ToneEngine.isPageBlank(rawPageBitmap);
            if (isBlank) {
                layoutBlankPageBanner.setVisibility(View.VISIBLE);
                tvBlankPageText.setText("⚠️ Page " + (currentPageIndex + 1) + " appears blank. Skip printing?");
                btnSkipBlankPage.setOnClickListener(v -> {
                    SoundManager.playClick();
                    excludePageFromPrint(currentPageIndex);
                    layoutBlankPageBanner.setVisibility(View.GONE);
                    Toast.makeText(this, "Page " + (currentPageIndex + 1) + " excluded from print run.", Toast.LENGTH_SHORT).show();
                });
                btnDismissBlankBanner.setOnClickListener(v -> {
                    SoundManager.playClick();
                    layoutBlankPageBanner.setVisibility(View.GONE);
                });
            } else {
                layoutBlankPageBanner.setVisibility(View.GONE);
            }

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

    private void excludePageFromPrint(int pageIdx) {
        List<Integer> currentSelected = resolveSelectedPages();
        currentSelected.remove(Integer.valueOf(pageIdx));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentSelected.size(); i++) {
            int pageNum = currentSelected.get(i) + 1;
            if (i > 0) sb.append(", ");
            sb.append(pageNum);
        }
        rgPages.check(com.dodopress.R.id.rb_pages_custom);
        layoutCustomRange.setVisibility(View.VISIBLE);
        etCustomPages.setText(sb.toString());
        updatePageSummary();
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
            Toast.makeText(this, "Printer not detected. Connect USB OTG cable, or tap 'Queue to Offline Spool'.", Toast.LENGTH_LONG).show();
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
        boolean invertDark = cbInvertDark.isChecked();
        String watermark = etWatermark.getText().toString().trim();
        float rotation = currentPageRotation;

        DiagnosticLogManager.log("PRINT", "Initiating print job: " + pagesToPrint.size() + " pages, " + copies + " copies, darkness=" + darkness + "%, placement=" + currentPlacementRegion.title);

        currentPrinterCutawayState = PrinterCutawayView.PrinterState.PRINTING;
        btnPrintNow.setEnabled(false);
        dockBtnPrint.setEnabled(false);
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

        printEngine.printPdf(currentPdfFile, darkness, dpi, copies, collate, pagesToPrint, currentPaperSize, currentPlacementRegion, invertDark, watermark, rotation, new PrintEngine.PrintProgressListener() {
            @Override
            public void onProgress(int currentPage, int total, String statusMessage) {
                DiagnosticLogManager.log("PRINT", statusMessage);
                runOnUiThread(() -> {
                    tvPrintStatus.setText(statusMessage);
                    dockTvStatus.setText(statusMessage);
                    if (total > 0) {
                        pbPrintProgress.setMax(total);
                        pbPrintProgress.setProgress(currentPage);
                    }
                });
            }

            @Override
            public void onComplete() {
                DiagnosticLogManager.log("PRINT", "Print job finished successfully!");
                currentPrinterCutawayState = PrinterCutawayView.PrinterState.READY;
                runOnUiThread(() -> {
                    SoundManager.playXpPrintChime();
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    btnPrintNow.setEnabled(true);
                    dockBtnPrint.setEnabled(true);
                    pbPrintProgress.setVisibility(View.GONE);
                    tvPrintStatus.setText("Print job finished successfully!");
                    dockTvStatus.setText("Print Complete!");
                    if (viewAnimatedLogo != null) {
                        viewAnimatedLogo.setPrintingActive(false);
                    }
                    if (laserScanView != null) {
                        laserScanView.stopScan();
                        laserScanView.setVisibility(View.GONE);
                    }
                    Toast.makeText(MainActivity.this, "Document printed successfully!", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(Exception e) {
                DiagnosticLogManager.logError("PRINT", "Printing failed", e);
                currentPrinterCutawayState = PrinterCutawayView.PrinterState.PAPER_EMPTY;
                runOnUiThread(() -> {
                    SoundManager.playAlert();
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    btnPrintNow.setEnabled(true);
                    dockBtnPrint.setEnabled(true);
                    pbPrintProgress.setVisibility(View.GONE);
                    tvPrintStatus.setText("Printing error: " + e.getMessage());
                    dockTvStatus.setText("Error: " + e.getMessage());
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
