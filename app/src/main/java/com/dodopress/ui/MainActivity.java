package com.dodopress.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.animation.ValueAnimator;
import android.view.Gravity;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
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
import android.widget.ArrayAdapter;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dodopress.engine.AiAssistantEngine;
import com.dodopress.engine.DocumentScannerEngine;
import com.dodopress.engine.IdCardEngine;
import com.dodopress.engine.ImagePrintEngine;
import com.dodopress.engine.PrintEngine;
import com.dodopress.engine.QrPrintEngine;
import com.dodopress.engine.SheetGeometry;
import com.dodopress.engine.SpoolManager;
import com.dodopress.engine.ToneEngine;
import com.dodopress.usb.UsbPrinterConnection;
import com.dodopress.util.DodoFileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

    // Multi-page navigation tabs (Material 3 PlayStore Style)
    private View pagePrintStudio;
    private View pageIdCopier;
    private View pageSpoolQueue;
    private View pageDeveloper;

    private View tabPrintStudio;
    private View tabIdCopier;
    private View tabSpoolQueue;
    private View tabDeveloper;

    private View pillPrintStudio;
    private View pillIdCopier;
    private View pillSpoolQueue;
    private View pillDeveloper;

    private ImageView iconPrintStudio;
    private ImageView iconIdCopier;
    private ImageView iconSpoolQueue;
    private ImageView iconDeveloper;

    private TextView labelPrintStudio;
    private TextView labelIdCopier;
    private TextView labelSpoolQueue;
    private TextView labelDeveloper;

    private View[] pillViews;
    private ImageView[] iconViews;
    private TextView[] labelViews;

    private int activePageIndex = 0;
    private int previousPageIndex = -1;

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

    private View btnThemeMenu;

    // Retractable Sliding Options Panel
    private View layoutSlidingPanel;
    private TextView tvPanelHandle;
    private Button btnTogglePanel;
    private Button btnTabTone;
    private Button btnTabGeometry;
    private Button btnTabMargins;
    private Button btnTabPages;
    private Button btnTabWatermark;
    private ScrollView scrollOptions;
    private View cardMargins;
    private View cardWatermark;

    // Watermark Presets
    private Button btnWmConfidential;
    private Button btnWmDraft;
    private Button btnWmPaid;
    private Button btnWmOriginal;
    private Button btnWmClear;

    private boolean isPanelCollapsed = false;

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

    private TextView tvAppTitle;
    private TextView tvAppSubtitle;
    private TextView tvProBadge;
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

    // N-Up Multi-Page Tiling Controls
    private Button btnTile1Up;
    private Button btnTile2Up;
    private Button btnTile4Up;
    private TextView tvTilingLabel;
    private SheetGeometry.TilingMode currentTilingMode = SheetGeometry.TilingMode.ONE_UP;

    private SheetGeometrySelectorView viewSheetGeometry;
    private Button btnPlaceFull;
    private Button btnPlaceTopHalf;
    private Button btnPlaceBottomHalf;
    private Button btnPlaceLeftHalf;
    private Button btnPlaceRightHalf;
    private Button btnPlaceQuadTl;

    // Margin Controls
    private TextView tvMarginTitle;
    private TextView tvMarginLabel;
    private TextView tvMarginDesc;
    private SeekBar seekMargin;
    private Button btnMarginZero;
    private Button btnMarginNarrow;
    private Button btnMarginNormal;
    private Button btnMarginWide;
    private int currentMarginMm = 12;

    private SheetGeometry.PaperSize currentPaperSize = SheetGeometry.PaperSize.A4;
    private SheetGeometry.PlacementRegion currentPlacementRegion = SheetGeometry.PlacementRegion.FULL_PAGE;

    // Preview Controls
    private TextView tvPrinterStatus;
    private Button btnPickFile;
    private Button btnScanDocument;
    private Button btnQrLabel;
    private Button btnAiAssistant;
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
    private Button btnPrintUpiQr;
    private Button btnFeedbackRequest;
    private CheckBox cbSoundEffects;
    private int devAvatarTapCount = 0;

    // BYOK AI Assistant Views
    private View cardAiByok;
    private EditText etGeminiApiKey;
    private Button btnSaveAiKey;
    private Button btnTestAiKey;
    private TextView tvAiKeyStatus;

    private File currentPdfFile;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor pdfPfd;
    private int totalPages = 0;
    private int currentPageIndex = 0;
    private Bitmap rawPageBitmap;
    private int copies = 1;

    private ThemeManager.AppTheme currentTheme = ThemeManager.AppTheme.MATRIX_CRT;

    // Background thread for real-time darkness rendering & debounced preview rasterization
    private final ExecutorService previewExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicInteger pendingDarkness = new AtomicInteger(-1);
    private final AtomicInteger previewRenderToken = new AtomicInteger(0);
    private final Object pdfRendererLock = new Object();

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
                processIncomingDocumentUri(uri);
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

        // Tabs (Material 3 PlayStore Navigation Structure)
        tabPrintStudio = findViewById(com.dodopress.R.id.tab_print_studio);
        tabIdCopier = findViewById(com.dodopress.R.id.tab_id_copier);
        tabSpoolQueue = findViewById(com.dodopress.R.id.tab_spool_queue);
        tabDeveloper = findViewById(com.dodopress.R.id.tab_developer);

        pillPrintStudio = findViewById(com.dodopress.R.id.pill_print_studio);
        pillIdCopier = findViewById(com.dodopress.R.id.pill_id_copier);
        pillSpoolQueue = findViewById(com.dodopress.R.id.pill_spool_queue);
        pillDeveloper = findViewById(com.dodopress.R.id.pill_developer);

        iconPrintStudio = findViewById(com.dodopress.R.id.icon_print_studio);
        iconIdCopier = findViewById(com.dodopress.R.id.icon_id_copier);
        iconSpoolQueue = findViewById(com.dodopress.R.id.icon_spool_queue);
        iconDeveloper = findViewById(com.dodopress.R.id.icon_developer);

        labelPrintStudio = findViewById(com.dodopress.R.id.label_print_studio);
        labelIdCopier = findViewById(com.dodopress.R.id.label_id_copier);
        labelSpoolQueue = findViewById(com.dodopress.R.id.label_spool_queue);
        labelDeveloper = findViewById(com.dodopress.R.id.label_developer);

        pillViews = new View[]{pillPrintStudio, pillIdCopier, pillSpoolQueue, pillDeveloper};
        iconViews = new ImageView[]{iconPrintStudio, iconIdCopier, iconSpoolQueue, iconDeveloper};
        labelViews = new TextView[]{labelPrintStudio, labelIdCopier, labelSpoolQueue, labelDeveloper};

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

        // Theme Studio Menu
        btnThemeMenu = findViewById(com.dodopress.R.id.btn_theme_menu);

        // Retractable Sliding Panel
        layoutSlidingPanel = findViewById(com.dodopress.R.id.layout_sliding_panel);
        tvPanelHandle = findViewById(com.dodopress.R.id.tv_panel_handle);
        btnTogglePanel = findViewById(com.dodopress.R.id.btn_toggle_panel);
        btnTabTone = findViewById(com.dodopress.R.id.btn_tab_tone);
        btnTabGeometry = findViewById(com.dodopress.R.id.btn_tab_geometry);
        btnTabMargins = findViewById(com.dodopress.R.id.btn_tab_margins);
        btnTabPages = findViewById(com.dodopress.R.id.btn_tab_pages);
        btnTabWatermark = findViewById(com.dodopress.R.id.btn_tab_watermark);
        scrollOptions = findViewById(com.dodopress.R.id.scroll_options);

        // Recents
        cardRecents = findViewById(com.dodopress.R.id.card_recents);
        layoutRecentsContainer = findViewById(com.dodopress.R.id.layout_recents_container);
        tvRecentsTitle = findViewById(com.dodopress.R.id.tv_recents_title);
        btnClearRecents = findViewById(com.dodopress.R.id.btn_clear_recents);

        // Cards
        cardDocPreview = findViewById(com.dodopress.R.id.card_doc_preview);
        cardTonerDensity = findViewById(com.dodopress.R.id.card_toner_density);
        cardGeometry = findViewById(com.dodopress.R.id.card_geometry);
        cardMargins = findViewById(com.dodopress.R.id.card_margins);
        cardPageConfig = findViewById(com.dodopress.R.id.card_page_config);
        cardWatermark = findViewById(com.dodopress.R.id.card_watermark);

        // Watermark Preset Chips
        btnWmConfidential = findViewById(com.dodopress.R.id.btn_wm_confidential);
        btnWmDraft = findViewById(com.dodopress.R.id.btn_wm_draft);
        btnWmPaid = findViewById(com.dodopress.R.id.btn_wm_paid);
        btnWmOriginal = findViewById(com.dodopress.R.id.btn_wm_original);
        btnWmClear = findViewById(com.dodopress.R.id.btn_wm_clear);

        tvAppTitle = findViewById(com.dodopress.R.id.tv_app_title);
        tvAppSubtitle = findViewById(com.dodopress.R.id.tv_app_subtitle);
        tvProBadge = findViewById(com.dodopress.R.id.tv_pro_badge);
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

        // N-Up Tiling Views
        btnTile1Up = findViewById(com.dodopress.R.id.btn_tile_1up);
        btnTile2Up = findViewById(com.dodopress.R.id.btn_tile_2up);
        btnTile4Up = findViewById(com.dodopress.R.id.btn_tile_4up);
        tvTilingLabel = findViewById(com.dodopress.R.id.tv_tiling_label);

        viewSheetGeometry = findViewById(com.dodopress.R.id.view_sheet_geometry);
        btnPlaceFull = findViewById(com.dodopress.R.id.btn_place_full);
        btnPlaceTopHalf = findViewById(com.dodopress.R.id.btn_place_top_half);
        btnPlaceBottomHalf = findViewById(com.dodopress.R.id.btn_place_bottom_half);
        btnPlaceLeftHalf = findViewById(com.dodopress.R.id.btn_place_left_half);
        btnPlaceRightHalf = findViewById(com.dodopress.R.id.btn_place_right_half);
        btnPlaceQuadTl = findViewById(com.dodopress.R.id.btn_place_quad_tl);

        // Margin Controls
        tvMarginTitle = findViewById(com.dodopress.R.id.tv_margin_title);
        tvMarginLabel = findViewById(com.dodopress.R.id.tv_margin_label);
        tvMarginDesc = findViewById(com.dodopress.R.id.tv_margin_desc);
        seekMargin = findViewById(com.dodopress.R.id.seek_margin);
        btnMarginZero = findViewById(com.dodopress.R.id.btn_margin_zero);
        btnMarginNarrow = findViewById(com.dodopress.R.id.btn_margin_narrow);
        btnMarginNormal = findViewById(com.dodopress.R.id.btn_margin_normal);
        btnMarginWide = findViewById(com.dodopress.R.id.btn_margin_wide);

        tvPrinterStatus = findViewById(com.dodopress.R.id.tv_printer_status);
        btnPickFile = findViewById(com.dodopress.R.id.btn_pick_file);
        btnScanDocument = findViewById(com.dodopress.R.id.btn_scan_document);
        btnQrLabel = findViewById(com.dodopress.R.id.btn_qr_label);
        btnAiAssistant = findViewById(com.dodopress.R.id.btn_ai_assistant);
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
        btnPrintUpiQr = findViewById(com.dodopress.R.id.btn_print_upi_qr);
        btnFeedbackRequest = findViewById(com.dodopress.R.id.btn_feedback_request);
        cbSoundEffects = findViewById(com.dodopress.R.id.cb_sound_effects);

        // BYOK AI Views
        cardAiByok = findViewById(com.dodopress.R.id.card_ai_byok);
        etGeminiApiKey = findViewById(com.dodopress.R.id.et_gemini_api_key);
        btnSaveAiKey = findViewById(com.dodopress.R.id.btn_save_ai_key);
        btnTestAiKey = findViewById(com.dodopress.R.id.btn_test_ai_key);
        tvAiKeyStatus = findViewById(com.dodopress.R.id.tv_ai_key_status);
    }

    private void showPage(int index) {
        activePageIndex = index;
        pagePrintStudio.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        pageIdCopier.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        pageSpoolQueue.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        pageDeveloper.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        floatingActionDock.setVisibility((index == 0 || index == 1) ? View.VISIBLE : View.GONE);

        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);
        int activeColor = p.brandPrimaryColor;
        int inactiveColor = Color.parseColor("#94A3B8");

        boolean isInitial = (previousPageIndex == -1);

        for (int i = 0; i < 4; i++) {
            final boolean isActive = (i == index);
            final View pill = pillViews[i];
            final ImageView icon = iconViews[i];
            final TextView label = labelViews[i];

            if (isActive) {
                label.setTypeface(null, android.graphics.Typeface.BOLD);
                pill.setBackgroundResource(com.dodopress.R.drawable.bg_nav_pill_active);

                // Apply elastic bounce scale animation to the active tab pill/icon
                pill.animate().cancel();
                pill.setScaleX(1.0f);
                pill.setScaleY(1.0f);
                pill.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .setDuration(120)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        pill.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(160)
                            .setInterpolator(new OvershootInterpolator(2.2f))
                            .start();
                    })
                    .start();

                if (!isInitial && previousPageIndex != index) {
                    // Animate active pill alpha and color tinting between active brand primary and inactive muted slate (#94A3B8)
                    pill.setAlpha(0.2f);
                    pill.animate().alpha(1.0f).setDuration(180).start();

                    ValueAnimator colorAnim = ValueAnimator.ofArgb(inactiveColor, activeColor);
                    colorAnim.setDuration(180);
                    colorAnim.addUpdateListener(anim -> {
                        int c = (int) anim.getAnimatedValue();
                        icon.setColorFilter(c, PorterDuff.Mode.SRC_IN);
                        label.setTextColor(c);
                    });
                    colorAnim.start();
                } else {
                    pill.setAlpha(1.0f);
                    icon.setColorFilter(activeColor, PorterDuff.Mode.SRC_IN);
                    label.setTextColor(activeColor);
                }
            } else {
                label.setTypeface(null, android.graphics.Typeface.NORMAL);

                if (!isInitial && previousPageIndex == i && previousPageIndex != index) {
                    // Previously active tab transitioning to inactive
                    pill.animate().cancel();
                    pill.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(0.0f)
                        .setDuration(120)
                        .withEndAction(() -> {
                            pill.setBackgroundResource(com.dodopress.R.drawable.bg_nav_pill_inactive);
                            pill.setAlpha(1.0f);
                        })
                        .start();

                    ValueAnimator colorAnim = ValueAnimator.ofArgb(activeColor, inactiveColor);
                    colorAnim.setDuration(180);
                    colorAnim.addUpdateListener(anim -> {
                        int c = (int) anim.getAnimatedValue();
                        icon.setColorFilter(c, PorterDuff.Mode.SRC_IN);
                        label.setTextColor(c);
                    });
                    colorAnim.start();
                } else {
                    // Inactive tab static reset
                    pill.animate().cancel();
                    pill.setScaleX(1.0f);
                    pill.setScaleY(1.0f);
                    pill.setAlpha(1.0f);
                    pill.setBackgroundResource(com.dodopress.R.drawable.bg_nav_pill_inactive);
                    icon.setColorFilter(inactiveColor, PorterDuff.Mode.SRC_IN);
                    label.setTextColor(inactiveColor);
                }
            }
        }

        previousPageIndex = index;

        if (index == 2) {
            renderSpoolQueue();
        } else if (index == 3) {
            updateAiKeyDisplay();
        }
    }

    private void setupListeners() {
        // Tab Clicks & Tactile Micro-Interactions
        TouchFeedback.applyAll(tabPrintStudio, tabIdCopier, tabSpoolQueue, tabDeveloper);
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

        // Theme Studio
        if (btnThemeMenu != null) {
            btnThemeMenu.setOnClickListener(v -> {
                SoundManager.playClick();
                showThemeSelectionDialog();
            });
        }

        // Sliding Panel Expand/Retract
        if (btnTogglePanel != null) {
            btnTogglePanel.setOnClickListener(v -> toggleOptionsPanel());
        }
        if (tvPanelHandle != null) {
            tvPanelHandle.setOnClickListener(v -> toggleOptionsPanel());
        }

        // Category Fast-Scroll Tabs
        if (btnTabTone != null) btnTabTone.setOnClickListener(v -> scrollToCard(cardTonerDensity, btnTabTone));
        if (btnTabGeometry != null) btnTabGeometry.setOnClickListener(v -> scrollToCard(cardGeometry, btnTabGeometry));
        if (btnTabMargins != null) btnTabMargins.setOnClickListener(v -> scrollToCard(cardMargins, btnTabMargins));
        if (btnTabPages != null) btnTabPages.setOnClickListener(v -> scrollToCard(cardPageConfig, btnTabPages));
        if (btnTabWatermark != null) btnTabWatermark.setOnClickListener(v -> scrollToCard(cardWatermark, btnTabWatermark));

        // Watermark live update & presets
        if (etWatermark != null) {
            etWatermark.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    renderPageThumb();
                }
            });
        }
        if (btnWmConfidential != null) btnWmConfidential.setOnClickListener(v -> setWatermarkPreset("CONFIDENTIAL"));
        if (btnWmDraft != null) btnWmDraft.setOnClickListener(v -> setWatermarkPreset("DRAFT"));
        if (btnWmPaid != null) btnWmPaid.setOnClickListener(v -> setWatermarkPreset("PAID / VERIFIED"));
        if (btnWmOriginal != null) btnWmOriginal.setOnClickListener(v -> setWatermarkPreset("ORIGINAL"));
        if (btnWmClear != null) btnWmClear.setOnClickListener(v -> setWatermarkPreset(""));

        // Paper Size Selection
        btnPaperA4.setOnClickListener(v -> { SoundManager.playClick(); setPaperSize(SheetGeometry.PaperSize.A4); });
        btnPaperLetter.setOnClickListener(v -> { SoundManager.playClick(); setPaperSize(SheetGeometry.PaperSize.LETTER); });
        btnPaperLegal.setOnClickListener(v -> { SoundManager.playClick(); setPaperSize(SheetGeometry.PaperSize.LEGAL); });
        btnPaperA5.setOnClickListener(v -> { SoundManager.playClick(); setPaperSize(SheetGeometry.PaperSize.A5); });

        // Multi-Page Sheet Tiling Selection
        btnTile1Up.setOnClickListener(v -> { SoundManager.playClick(); setTilingMode(SheetGeometry.TilingMode.ONE_UP); });
        btnTile2Up.setOnClickListener(v -> { SoundManager.playClick(); setTilingMode(SheetGeometry.TilingMode.TWO_UP); });
        btnTile4Up.setOnClickListener(v -> { SoundManager.playClick(); setTilingMode(SheetGeometry.TilingMode.FOUR_UP); });

        // Placement Region Selection
        btnPlaceFull.setOnClickListener(v -> { SoundManager.playClick(); setPlacementRegion(SheetGeometry.PlacementRegion.FULL_PAGE); });
        btnPlaceTopHalf.setOnClickListener(v -> { SoundManager.playClick(); setPlacementRegion(SheetGeometry.PlacementRegion.TOP_HALF); });
        btnPlaceBottomHalf.setOnClickListener(v -> { SoundManager.playClick(); setPlacementRegion(SheetGeometry.PlacementRegion.BOTTOM_HALF); });
        btnPlaceLeftHalf.setOnClickListener(v -> { SoundManager.playClick(); setPlacementRegion(SheetGeometry.PlacementRegion.LEFT_HALF); });
        btnPlaceRightHalf.setOnClickListener(v -> { SoundManager.playClick(); setPlacementRegion(SheetGeometry.PlacementRegion.RIGHT_HALF); });
        btnPlaceQuadTl.setOnClickListener(v -> { SoundManager.playClick(); setPlacementRegion(SheetGeometry.PlacementRegion.QUAD_TOP_LEFT); });

        // Print Margin Listeners
        seekMargin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentMarginMm = progress;
                updateMarginDisplay(progress);
                if (fromUser) {
                    renderPageThumb();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                renderPageThumb();
            }
        });

        btnMarginZero.setOnClickListener(v -> setMarginPreset(0));
        btnMarginNarrow.setOnClickListener(v -> setMarginPreset(5));
        btnMarginNormal.setOnClickListener(v -> setMarginPreset(12));
        btnMarginWide.setOnClickListener(v -> setMarginPreset(24));

        viewSheetGeometry.setOnRegionSelectedListener(this::setPlacementRegion);

        btnPickFile.setOnClickListener(v -> {
            SoundManager.playClick();
            pickPdf();
        });

        btnScanDocument.setOnClickListener(v -> {
            SoundManager.playClick();
            launchCameraScanner(false);
        });

        if (btnQrLabel != null) {
            btnQrLabel.setOnClickListener(v -> {
                SoundManager.playClick();
                showQrGeneratorDialog();
            });
        }

        if (btnAiAssistant != null) {
            btnAiAssistant.setOnClickListener(v -> {
                SoundManager.playClick();
                v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                showAiAssistantDialog();
            });
        }

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
            int nUp = (currentTilingMode != null) ? currentTilingMode.pagesPerSheet : 1;
            int currentSheet = currentPageIndex / nUp;
            if (currentSheet > 0) {
                currentPageIndex = (currentSheet - 1) * nUp;
                renderPageThumb();
            }
        });

        btnNextPage.setOnClickListener(v -> {
            int nUp = (currentTilingMode != null) ? currentTilingMode.pagesPerSheet : 1;
            int totalSheets = (totalPages + nUp - 1) / nUp;
            int currentSheet = currentPageIndex / nUp;
            if (currentSheet < totalSheets - 1) {
                currentPageIndex = (currentSheet + 1) * nUp;
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

        if (btnPrintUpiQr != null) {
            btnPrintUpiQr.setOnClickListener(v -> {
                SoundManager.playClick();
                generateAndLoadUpiQr();
            });
        }

        if (btnSaveAiKey != null) {
            btnSaveAiKey.setOnClickListener(v -> {
                SoundManager.playClick();
                saveAiKey();
            });
        }

        if (btnTestAiKey != null) {
            btnTestAiKey.setOnClickListener(v -> {
                SoundManager.playClick();
                testAiKey();
            });
        }

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
            btnPickFile, btnScanDocument, btnQrLabel, btnAiAssistant, btnPrintNow, dockBtnPrint, btnDiagnostics,
            btnTogglePreviewMode, btnQuickZoom, btnRotatePage, btnPrevPage, btnNextPage,
            btnPresetDraft, btnPresetNormal, btnPresetDeep, btnPresetUltra,
            btnCopiesMinus, btnCopiesPlus, btnQueueJob,
            btnThemeMenu, btnTogglePanel, btnTabTone, btnTabGeometry, btnTabMargins, btnTabPages, btnTabWatermark,
            btnWmConfidential, btnWmDraft, btnWmPaid, btnWmOriginal, btnWmClear,
            btnPaperA4, btnPaperLetter, btnPaperLegal, btnPaperA5,
            btnTile1Up, btnTile2Up, btnTile4Up,
            btnPlaceFull, btnPlaceTopHalf, btnPlaceBottomHalf, btnPlaceLeftHalf, btnPlaceRightHalf, btnPlaceQuadTl,
            btnMarginZero, btnMarginNarrow, btnMarginNormal, btnMarginWide,
            btnIdPickFront, btnIdPickBack, btnIdPrintNow, btnCopyUpi, btnPrintUpiQr, btnSaveAiKey, btnTestAiKey, btnFeedbackRequest
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
        updateGeometryBadge();
        updatePageSummary();
        renderPageThumb();
    }

    private void setPlacementRegion(SheetGeometry.PlacementRegion region) {
        this.currentPlacementRegion = region;
        this.currentTilingMode = SheetGeometry.TilingMode.ONE_UP;
        updateTilingHighlight(currentTilingMode);
        if (tvTilingLabel != null) {
            tvTilingLabel.setText(currentTilingMode.title);
        }
        viewSheetGeometry.setTilingMode(SheetGeometry.TilingMode.ONE_UP);
        viewSheetGeometry.setPlacementRegion(region);
        updatePlacementPresetHighlight(region);
        updateGeometryBadge();
        updatePageSummary();
        renderPageThumb();
    }

    private void setTilingMode(SheetGeometry.TilingMode mode) {
        this.currentTilingMode = mode;
        if (mode != SheetGeometry.TilingMode.ONE_UP) {
            this.currentPlacementRegion = SheetGeometry.PlacementRegion.FULL_PAGE;
            viewSheetGeometry.setPlacementRegion(SheetGeometry.PlacementRegion.FULL_PAGE);
            updatePlacementPresetHighlight(SheetGeometry.PlacementRegion.FULL_PAGE);
        }
        viewSheetGeometry.setTilingMode(mode);
        updateTilingHighlight(mode);
        if (tvTilingLabel != null) {
            tvTilingLabel.setText(mode.title);
        }
        updateGeometryBadge();
        updatePageSummary();
        renderPageThumb();
    }

    private void updateGeometryBadge() {
        if (tvGeometryBadge == null) return;
        String modeDesc = (currentTilingMode != SheetGeometry.TilingMode.ONE_UP)
            ? currentTilingMode.title
            : currentPlacementRegion.title;
        tvGeometryBadge.setText(currentPaperSize.name() + " • " + modeDesc);
    }

    private void updateTilingHighlight(SheetGeometry.TilingMode mode) {
        int activeBg = com.dodopress.R.drawable.btn_primary;
        int normalBg = com.dodopress.R.drawable.btn_chip;

        if (btnTile1Up != null) {
            btnTile1Up.setBackgroundResource(mode == SheetGeometry.TilingMode.ONE_UP ? activeBg : normalBg);
            btnTile1Up.setTextColor(mode == SheetGeometry.TilingMode.ONE_UP ? Color.WHITE : Color.parseColor("#94A3B8"));
        }
        if (btnTile2Up != null) {
            btnTile2Up.setBackgroundResource(mode == SheetGeometry.TilingMode.TWO_UP ? activeBg : normalBg);
            btnTile2Up.setTextColor(mode == SheetGeometry.TilingMode.TWO_UP ? Color.WHITE : Color.parseColor("#94A3B8"));
        }
        if (btnTile4Up != null) {
            btnTile4Up.setBackgroundResource(mode == SheetGeometry.TilingMode.FOUR_UP ? activeBg : normalBg);
            btnTile4Up.setTextColor(mode == SheetGeometry.TilingMode.FOUR_UP ? Color.WHITE : Color.parseColor("#94A3B8"));
        }
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
        if (rootMainContainer != null) {
            rootMainContainer.setBackgroundColor(p.bgColor);
        }

        if (laserScanView != null) {
            laserScanView.setLaserColor(p.laserColor);
        }
        if (viewAnimatedLogo != null) {
            viewAnimatedLogo.setThemeColors(p.brandPrimaryColor, p.accentColor);
        }
        if (viewSheetGeometry != null) {
            viewSheetGeometry.setThemeColors(p.brandPrimaryColor, p.accentColor);
        }
        if (viewBeacon != null) {
            viewBeacon.setBeaconColor(p.beaconColor);
        }

        // Cards background
        if (cardDocPreview != null) cardDocPreview.setBackgroundResource(p.cardDrawableRes);
        if (cardTonerDensity != null) cardTonerDensity.setBackgroundResource(p.cardDrawableRes);
        if (cardGeometry != null) cardGeometry.setBackgroundResource(p.cardDrawableRes);
        if (cardMargins != null) cardMargins.setBackgroundResource(p.cardDrawableRes);
        if (cardPageConfig != null) cardPageConfig.setBackgroundResource(p.cardDrawableRes);
        if (cardWatermark != null) cardWatermark.setBackgroundResource(p.cardDrawableRes);
        if (cardRecents != null) cardRecents.setBackgroundResource(p.cardDrawableRes);

        // Primary Buttons
        int primaryBg = com.dodopress.R.drawable.btn_primary;
        int secondaryBg = com.dodopress.R.drawable.btn_secondary;
        int chipBg = com.dodopress.R.drawable.btn_chip;

        if (btnPickFile != null) {
            btnPickFile.setBackgroundResource(primaryBg);
            btnPickFile.setTextColor(Color.WHITE);
        }
        if (btnScanDocument != null) {
            btnScanDocument.setBackgroundResource(secondaryBg);
            btnScanDocument.setTextColor(p.textHighColor);
        }
        if (btnQrLabel != null) {
            btnQrLabel.setBackgroundResource(secondaryBg);
            btnQrLabel.setTextColor(p.textHighColor);
        }
        if (btnAiAssistant != null) {
            btnAiAssistant.setBackgroundResource(secondaryBg);
            btnAiAssistant.setTextColor(p.textHighColor);
        }
        if (btnPrintNow != null) {
            btnPrintNow.setBackgroundResource(primaryBg);
            btnPrintNow.setTextColor(Color.WHITE);
        }
        if (dockBtnPrint != null) {
            dockBtnPrint.setBackgroundResource(primaryBg);
            dockBtnPrint.setTextColor(Color.WHITE);
        }
        if (btnCopiesMinus != null) {
            btnCopiesMinus.setBackgroundResource(secondaryBg);
            btnCopiesMinus.setTextColor(p.textHighColor);
        }
        if (btnCopiesPlus != null) {
            btnCopiesPlus.setBackgroundResource(secondaryBg);
            btnCopiesPlus.setTextColor(p.textHighColor);
        }
        if (btnPrevPage != null) {
            btnPrevPage.setBackgroundResource(secondaryBg);
            btnPrevPage.setTextColor(p.textHighColor);
        }
        if (btnNextPage != null) {
            btnNextPage.setBackgroundResource(secondaryBg);
            btnNextPage.setTextColor(p.textHighColor);
        }
        if (btnTogglePreviewMode != null) {
            btnTogglePreviewMode.setBackgroundResource(chipBg);
            btnTogglePreviewMode.setTextColor(p.textHighColor);
        }
        if (btnRotatePage != null) {
            btnRotatePage.setBackgroundResource(chipBg);
            btnRotatePage.setTextColor(p.textHighColor);
        }
        if (btnQuickZoom != null) {
            btnQuickZoom.setBackgroundResource(chipBg);
            btnQuickZoom.setTextColor(p.brandPrimaryColor);
        }

        // Text Colors
        if (tvAppTitle != null) tvAppTitle.setTextColor(p.textHighColor);
        if (tvDensityTitle != null) tvDensityTitle.setTextColor(p.textHighColor);
        if (tvDensityDesc != null) tvDensityDesc.setTextColor(p.textMedColor);
        if (tvGeometryTitle != null) tvGeometryTitle.setTextColor(p.textHighColor);
        if (tvGeometryDesc != null) tvGeometryDesc.setTextColor(p.textMedColor);
        if (tvMarginTitle != null) tvMarginTitle.setTextColor(p.textHighColor);
        if (tvMarginDesc != null) tvMarginDesc.setTextColor(p.textMedColor);
        if (tvCopiesTitle != null) tvCopiesTitle.setTextColor(p.textHighColor);
        if (tvPagesTitle != null) tvPagesTitle.setTextColor(p.textHighColor);

        if (rbPagesAll != null) rbPagesAll.setTextColor(p.textHighColor);
        if (rbPagesCurrent != null) rbPagesCurrent.setTextColor(p.textHighColor);
        if (rbPagesOdd != null) rbPagesOdd.setTextColor(p.textHighColor);
        if (rbPagesEven != null) rbPagesEven.setTextColor(p.textHighColor);
        if (rbPagesCustom != null) rbPagesCustom.setTextColor(p.textHighColor);
        if (cbCollate != null) cbCollate.setTextColor(p.textMedColor);
        if (cbReverseOrder != null) cbReverseOrder.setTextColor(p.textHighColor);
        if (cbInvertDark != null) cbInvertDark.setTextColor(p.textHighColor);

        // Highlight active sub-controls
        updatePaperSizeHighlight(currentPaperSize);
        updatePlacementPresetHighlight(currentPlacementRegion);
        updateTilingHighlight(currentTilingMode);
        if (seekDarkness != null) {
            updatePresetChipHighlight(seekDarkness.getProgress());
        }
        updateMarginButtonsHighlight(currentMarginMm);
        updateTabHighlight(btnTabTone);
        updatePageSummary();
        showPage(activePageIndex);
    }

    private void toggleOptionsPanel() {
        SoundManager.playClick();
        if (scrollOptions == null || btnTogglePanel == null) return;
        isPanelCollapsed = !isPanelCollapsed;
        if (isPanelCollapsed) {
            scrollOptions.setVisibility(View.GONE);
            btnTogglePanel.setText("▲ EXPAND");
        } else {
            scrollOptions.setVisibility(View.VISIBLE);
            btnTogglePanel.setText("▼ RETRACT");
        }
    }

    private void scrollToCard(View card, Button activeTab) {
        SoundManager.playClick();
        if (card == null || scrollOptions == null) return;
        if (isPanelCollapsed) {
            toggleOptionsPanel();
        }
        scrollOptions.smoothScrollTo(0, card.getTop());
        updateTabHighlight(activeTab);
    }

    private void updateTabHighlight(Button activeTab) {
        Button[] tabs = {btnTabTone, btnTabGeometry, btnTabMargins, btnTabPages, btnTabWatermark};
        for (Button b : tabs) {
            if (b == null) continue;
            boolean isAct = (b == activeTab);
            b.setTextColor(isAct ? Color.WHITE : Color.parseColor("#94A3B8"));
            b.setBackgroundResource(isAct ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
        }
    }

    private void setWatermarkPreset(String text) {
        SoundManager.playClick();
        if (etWatermark != null) {
            etWatermark.setText(text);
            etWatermark.setSelection(text.length());
        }
        renderPageThumb();
    }

    private void showThemeSelectionDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#080D1A"));
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("🎨 HARDWARE THEME STUDIO");
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(Color.WHITE);
        root.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("Select high-contrast phosphor or precision engineering palette");
        tvSub.setTextSize(11);
        tvSub.setTextColor(Color.parseColor("#94A3B8"));
        tvSub.setPadding(0, 4, 0, (int) (14 * getResources().getDisplayMetrics().density));
        root.addView(tvSub);

        final android.app.AlertDialog dialog = builder.setView(root).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        for (final ThemeManager.AppTheme t : ThemeManager.AppTheme.values()) {
            final boolean isActive = (t == currentTheme);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int rowPadH = (int) (12 * getResources().getDisplayMetrics().density);
            int rowPadV = (int) (10 * getResources().getDisplayMetrics().density);
            row.setPadding(rowPadH, rowPadV, rowPadH, rowPadV);

            android.graphics.drawable.GradientDrawable rowBg = new android.graphics.drawable.GradientDrawable();
            rowBg.setCornerRadius(10 * getResources().getDisplayMetrics().density);
            if (isActive) {
                rowBg.setColor(Color.parseColor("#172554"));
                rowBg.setStroke((int) (1.5f * getResources().getDisplayMetrics().density), Color.parseColor(t.previewAccentHex));
            } else {
                rowBg.setColor(Color.parseColor("#0F172A"));
                rowBg.setStroke((int) (1 * getResources().getDisplayMetrics().density), Color.parseColor("#1E293B"));
            }
            row.setBackground(rowBg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
            row.setLayoutParams(lp);

            View swatch = new View(this);
            int swatchSize = (int) (22 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams swatchLp = new LinearLayout.LayoutParams(swatchSize, swatchSize);
            swatchLp.rightMargin = (int) (12 * getResources().getDisplayMetrics().density);
            swatch.setLayoutParams(swatchLp);
            android.graphics.drawable.GradientDrawable swatchBg = new android.graphics.drawable.GradientDrawable();
            swatchBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            swatchBg.setColor(Color.parseColor(t.previewAccentHex));
            swatch.setBackground(swatchBg);
            row.addView(swatch);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            textCol.setLayoutParams(colLp);

            TextView tvItemTitle = new TextView(this);
            tvItemTitle.setText(t.title);
            tvItemTitle.setTextSize(13);
            tvItemTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvItemTitle.setTextColor(isActive ? Color.parseColor(t.previewAccentHex) : Color.WHITE);
            textCol.addView(tvItemTitle);

            TextView tvItemSub = new TextView(this);
            tvItemSub.setText(t.subtitle);
            tvItemSub.setTextSize(10);
            tvItemSub.setTextColor(Color.parseColor("#94A3B8"));
            textCol.addView(tvItemSub);
            row.addView(textCol);

            if (isActive) {
                TextView activeBadge = new TextView(this);
                activeBadge.setText("ACTIVE");
                activeBadge.setTextSize(9);
                activeBadge.setTypeface(null, android.graphics.Typeface.BOLD);
                activeBadge.setTextColor(Color.parseColor(t.previewAccentHex));
                activeBadge.setPadding((int) (8 * getResources().getDisplayMetrics().density),
                    (int) (3 * getResources().getDisplayMetrics().density),
                    (int) (8 * getResources().getDisplayMetrics().density),
                    (int) (3 * getResources().getDisplayMetrics().density));
                android.graphics.drawable.GradientDrawable badgeBg = new android.graphics.drawable.GradientDrawable();
                badgeBg.setCornerRadius(6 * getResources().getDisplayMetrics().density);
                badgeBg.setColor(Color.parseColor("#0F172A"));
                badgeBg.setStroke(1, Color.parseColor(t.previewAccentHex));
                activeBadge.setBackground(badgeBg);
                row.addView(activeBadge);
            }

            row.setOnClickListener(v -> {
                SoundManager.playClick();
                row.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                applyAppTheme(t);
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "Theme activated: " + t.title, Toast.LENGTH_SHORT).show();
            });

            root.addView(row);
        }

        dialog.show();
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

    private void setMarginPreset(int mm) {
        SoundManager.playClick();
        if (seekMargin != null) {
            seekMargin.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            seekMargin.setProgress(mm);
        }
        currentMarginMm = mm;
        updateMarginDisplay(mm);
        renderPageThumb();
    }

    private void updateMarginDisplay(int mm) {
        if (tvMarginLabel != null) {
            String desc;
            if (mm == 0) desc = "0 mm (Edge-to-Edge / Borderless)";
            else if (mm == 5) desc = "5 mm (Narrow)";
            else if (mm == 12) desc = "12 mm (Normal)";
            else if (mm == 24) desc = "24 mm (Wide)";
            else desc = mm + " mm (Custom)";
            tvMarginLabel.setText(desc);
        }
        updateMarginButtonsHighlight(mm);
        updatePageSummary();
    }

    private void updateMarginButtonsHighlight(int mm) {
        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);
        int activeBg = com.dodopress.R.drawable.btn_primary;
        int normalBg = com.dodopress.R.drawable.btn_chip;
        int activeText = Color.WHITE;
        int normalText = p.textMedColor;

        if (btnMarginZero != null) {
            btnMarginZero.setBackgroundResource(mm == 0 ? activeBg : normalBg);
            btnMarginZero.setTextColor(mm == 0 ? activeText : normalText);
        }
        if (btnMarginNarrow != null) {
            btnMarginNarrow.setBackgroundResource(mm == 5 ? activeBg : normalBg);
            btnMarginNarrow.setTextColor(mm == 5 ? activeText : normalText);
        }
        if (btnMarginNormal != null) {
            btnMarginNormal.setBackgroundResource(mm == 12 ? activeBg : normalBg);
            btnMarginNormal.setTextColor(mm == 12 ? activeText : normalText);
        }
        if (btnMarginWide != null) {
            btnMarginWide.setBackgroundResource(mm == 24 ? activeBg : normalBg);
            btnMarginWide.setTextColor(mm == 24 ? activeText : normalText);
        }
    }

    private void setDarkness(int value) {
        seekDarkness.setProgress(value);
        updatePresetChipHighlight(value);
    }

    private void updatePresetChipHighlight(int progress) {
        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);
        if (btnPresetDraft != null) {
            btnPresetDraft.setBackgroundResource(progress == ToneEngine.PRESET_DRAFT ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
            btnPresetDraft.setTextColor(progress == ToneEngine.PRESET_DRAFT ? Color.WHITE : p.textMedColor);
        }
        if (btnPresetNormal != null) {
            btnPresetNormal.setBackgroundResource(progress == ToneEngine.PRESET_NORMAL ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
            btnPresetNormal.setTextColor(progress == ToneEngine.PRESET_NORMAL ? Color.WHITE : p.textMedColor);
        }
        if (btnPresetDeep != null) {
            btnPresetDeep.setBackgroundResource(progress == ToneEngine.PRESET_DEEP ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
            btnPresetDeep.setTextColor(progress == ToneEngine.PRESET_DEEP ? Color.WHITE : p.textMedColor);
        }
        if (btnPresetUltra != null) {
            btnPresetUltra.setBackgroundResource(progress == ToneEngine.PRESET_ULTRA ? com.dodopress.R.drawable.btn_primary : com.dodopress.R.drawable.btn_chip);
            btnPresetUltra.setTextColor(progress == ToneEngine.PRESET_ULTRA ? Color.WHITE : p.textMedColor);
        }
    }

    private void scheduleDarknessPreview(int darkness) {
        if (rawPageBitmap == null || rawPageBitmap.isRecycled()) return;

        pendingDarkness.set(darkness);
        final int token = previewRenderToken.get();
        previewExecutor.execute(() -> {
            int d = pendingDarkness.getAndSet(-1);
            if (d >= 0 && token == previewRenderToken.get()) {
                Bitmap source;
                synchronized (pdfRendererLock) {
                    if (rawPageBitmap == null || rawPageBitmap.isRecycled()) return;
                    source = rawPageBitmap.copy(Bitmap.Config.ARGB_8888, false);
                }
                if (source == null) return;
                Bitmap updated = ToneEngine.processPreviewBitmap(source, d);
                source.recycle();
                if (updated != null) {
                    mainHandler.post(() -> {
                        if (token != previewRenderToken.get()) {
                            updated.recycle();
                            return;
                        }
                        if (toneAdjustedBitmap != null && toneAdjustedBitmap != rawPageBitmap && !toneAdjustedBitmap.isRecycled()) {
                            toneAdjustedBitmap.recycle();
                        }
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
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "image/*"});
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
                processIncomingDocumentUri(uri);
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

    private boolean isImageUri(Uri uri) {
        if (uri == null) return false;
        try {
            String mime = getContentResolver().getType(uri);
            if (mime != null) {
                if (mime.startsWith("image/")) return true;
                if (mime.equalsIgnoreCase("application/pdf")) return false;
            }
        } catch (Exception ignored) {}

        String name = getDisplayName(uri).toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
               name.endsWith(".webp") || name.endsWith(".bmp") || name.endsWith(".heic") ||
               name.endsWith(".heif") || name.endsWith(".gif");
    }

    private String getDisplayName(Uri uri) {
        if (uri == null) return "document";
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIdx >= 0) {
                    String name = cursor.getString(nameIdx);
                    if (name != null) return name;
                }
            }
        } catch (Exception ignored) {}
        String path = uri.getPath();
        if (path != null) {
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                return path.substring(lastSlash + 1);
            }
            return path;
        }
        return "document";
    }

    private void processIncomingDocumentUri(Uri uri) {
        if (uri == null) return;
        if (isImageUri(uri)) {
            String originalName = getDisplayName(uri);
            Toast.makeText(this, "Converting image to print-ready PDF...", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                try {
                    File pdf = ImagePrintEngine.convertImageToPdf(this, uri, currentPaperSize);
                    runOnUiThread(() -> {
                        loadPdfFromFile(pdf, originalName + " (Image)");
                        Toast.makeText(this, "Photo converted! Ready to print.", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    DiagnosticLogManager.logError("IMAGE_INGEST", "Failed to convert image to PDF", e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        } else {
            loadPdfFromUri(uri);
        }
    }

    private void loadPdfFromFile(File pdfFile, String displayName) {
        if (pdfFile == null || !pdfFile.exists()) return;
        loadPdfFromUri(Uri.fromFile(pdfFile), displayName);
    }

    private InputStream openUriInputStream(Uri uri) throws IOException {
        if ("file".equalsIgnoreCase(uri.getScheme()) && uri.getPath() != null) {
            return new FileInputStream(new File(uri.getPath()));
        }
        return getContentResolver().openInputStream(uri);
    }

    private void loadPdfFromUri(Uri uri) {
        loadPdfFromUri(uri, null);
    }

    private void loadPdfFromUri(Uri uri, String customDisplayName) {
        closeCurrentPdf();

        String displayName = (customDisplayName != null && !customDisplayName.isEmpty()) ? customDisplayName : "document.pdf";
        long fileSize = 0;

        if ("file".equalsIgnoreCase(uri.getScheme()) && uri.getPath() != null) {
            File f = new File(uri.getPath());
            if (f.exists()) {
                if (customDisplayName == null) displayName = f.getName();
                fileSize = f.length();
            }
        } else {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    int sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (nameIdx >= 0 && customDisplayName == null) displayName = cursor.getString(nameIdx);
                    if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx);
                }
            } catch (Exception ignored) {}
        }

        try {
            File cacheFile = new File(getCacheDir(), "print_job_" + System.currentTimeMillis() + ".pdf");
            try (InputStream in = openUriInputStream(uri);
                 FileOutputStream out = new FileOutputStream(cacheFile)) {
                byte[] buf = new byte[16384];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }

            synchronized (pdfRendererLock) {
                currentPdfFile = cacheFile;
                pdfPfd = ParcelFileDescriptor.open(currentPdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
                pdfRenderer = new PdfRenderer(pdfPfd);
                totalPages = pdfRenderer.getPageCount();
                currentPageIndex = 0;
                currentPageRotation = 0f;
            }

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

    private void generateAndLoadUpiQr() {
        try {
            String upiString = QrPrintEngine.formatUpi("killindodo@upi", "killindodo", null, "Support DodoPress Developer");
            File qrPdf = QrPrintEngine.renderQrToPdfFile(this, upiString, "☕ SUPPORT THE DEVELOPER",
                                                        "UPI ID: killindodo@upi • Offline Printing Champion",
                                                        currentPaperSize, QrPrintEngine.LabelLayout.SINGLE_CARD);
            showPage(0);
            loadPdfFromFile(qrPdf, "Support_killindodo_UPI.pdf");
            Toast.makeText(this, "UPI QR placard ready to print!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to generate UPI QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showQrGeneratorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🏁 Native QR Code & Label Studio");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 24, 36, 24);

        // QR Type Selection
        TextView tvType = new TextView(this);
        tvType.setText("Select QR Type:");
        tvType.setTextSize(14f);
        tvType.setTextColor(Color.WHITE);
        tvType.setPadding(0, 0, 0, 8);
        layout.addView(tvType);

        RadioGroup rgType = new RadioGroup(this);
        rgType.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbWifi = new RadioButton(this);
        rbWifi.setText("📶 Wi-Fi");
        rbWifi.setTextColor(Color.WHITE);
        rbWifi.setId(View.generateViewId());

        RadioButton rbUrl = new RadioButton(this);
        rbUrl.setText("🔗 URL");
        rbUrl.setTextColor(Color.WHITE);
        rbUrl.setId(View.generateViewId());

        RadioButton rbText = new RadioButton(this);
        rbText.setText("📝 Text");
        rbText.setTextColor(Color.WHITE);
        rbText.setId(View.generateViewId());

        RadioButton rbUpi = new RadioButton(this);
        rbUpi.setText("⚡ UPI");
        rbUpi.setTextColor(Color.WHITE);
        rbUpi.setId(View.generateViewId());

        rgType.addView(rbWifi);
        rgType.addView(rbUrl);
        rgType.addView(rbText);
        rgType.addView(rbUpi);
        layout.addView(rgType);

        // Inputs
        EditText etTitle = new EditText(this);
        etTitle.setHint("Placard Title (e.g. GUEST WI-FI)");
        etTitle.setTextColor(Color.WHITE);
        etTitle.setHintTextColor(0xFF888888);
        layout.addView(etTitle);

        EditText etPrimary = new EditText(this);
        etPrimary.setHint("Wi-Fi Network Name (SSID)");
        etPrimary.setTextColor(Color.WHITE);
        etPrimary.setHintTextColor(0xFF888888);
        layout.addView(etPrimary);

        EditText etSecondary = new EditText(this);
        etSecondary.setHint("Wi-Fi Password");
        etSecondary.setTextColor(Color.WHITE);
        etSecondary.setHintTextColor(0xFF888888);
        layout.addView(etSecondary);

        // Layout Selection
        TextView tvLayout = new TextView(this);
        tvLayout.setText("Sheet Layout:");
        tvLayout.setTextSize(14f);
        tvLayout.setTextColor(Color.WHITE);
        tvLayout.setPadding(0, 16, 0, 8);
        layout.addView(tvLayout);

        RadioGroup rgLayout = new RadioGroup(this);
        RadioButton rbSingle = new RadioButton(this);
        rbSingle.setText("Single Sign / Placard (1 per sheet)");
        rbSingle.setTextColor(Color.WHITE);
        rbSingle.setChecked(true);

        RadioButton rbGrid2 = new RadioButton(this);
        rbGrid2.setText("Quad Label Sheet (4 per sheet - 2×2)");
        rbGrid2.setTextColor(Color.WHITE);

        RadioButton rbGrid3 = new RadioButton(this);
        rbGrid3.setText("Sticker Sheet Medium (9 per sheet - 3×3)");
        rbGrid3.setTextColor(Color.WHITE);

        RadioButton rbGrid4 = new RadioButton(this);
        rbGrid4.setText("Mini Stickers (16 per sheet - 4×4)");
        rbGrid4.setTextColor(Color.WHITE);

        rgLayout.addView(rbSingle);
        rgLayout.addView(rbGrid2);
        rgLayout.addView(rbGrid3);
        rgLayout.addView(rbGrid4);
        layout.addView(rgLayout);

        scrollView.addView(layout);
        builder.setView(scrollView);

        // Toggle hints when radio changes
        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == rbWifi.getId()) {
                etTitle.setText("📶 GUEST WI-FI ACCESS");
                etPrimary.setHint("Wi-Fi SSID");
                etSecondary.setVisibility(View.VISIBLE);
                etSecondary.setHint("Wi-Fi Password (leave empty if open)");
            } else if (checkedId == rbUrl.getId()) {
                etTitle.setText("🔗 SCAN TO VISIT WEBSITE");
                etPrimary.setHint("https://your-website.com");
                etSecondary.setVisibility(View.VISIBLE);
                etSecondary.setHint("Subtitle / Description");
            } else if (checkedId == rbUpi.getId()) {
                etTitle.setText("⚡ SCAN TO PAY VIA UPI");
                etPrimary.setHint("UPI VPA (e.g. username@upi)");
                etSecondary.setVisibility(View.VISIBLE);
                etSecondary.setHint("Payee Name / Note");
            } else {
                etTitle.setText("SCAN QR CODE");
                etPrimary.setHint("Text payload / message");
                etSecondary.setVisibility(View.VISIBLE);
                etSecondary.setHint("Subtitle / Description");
            }
        });
        rbWifi.setChecked(true);

        builder.setPositiveButton("⚡ Generate & Preview", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String p1 = etPrimary.getText().toString().trim();
            String p2 = etSecondary.getText().toString().trim();

            if (p1.isEmpty()) {
                Toast.makeText(this, "Please enter content for the QR code.", Toast.LENGTH_SHORT).show();
                return;
            }

            String content;
            String subtitle;
            int checkedType = rgType.getCheckedRadioButtonId();
            if (checkedType == rbWifi.getId()) {
                content = QrPrintEngine.formatWifi(p1, p2, p2.isEmpty() ? "nopass" : "WPA", false);
                subtitle = "Network: " + p1 + (p2.isEmpty() ? " • Open" : " • Password: " + p2);
            } else if (checkedType == rbUpi.getId()) {
                content = QrPrintEngine.formatUpi(p1, p2, null, "DodoPress QR Payment");
                subtitle = "Payee: " + (p2.isEmpty() ? p1 : p2) + " • UPI: " + p1;
            } else {
                content = p1;
                subtitle = p2;
            }

            QrPrintEngine.LabelLayout selectedLayout = QrPrintEngine.LabelLayout.SINGLE_CARD;
            if (rbGrid2.isChecked()) selectedLayout = QrPrintEngine.LabelLayout.GRID_2X2;
            else if (rbGrid3.isChecked()) selectedLayout = QrPrintEngine.LabelLayout.GRID_3X3;
            else if (rbGrid4.isChecked()) selectedLayout = QrPrintEngine.LabelLayout.GRID_4X4;

            try {
                File qrPdf = QrPrintEngine.renderQrToPdfFile(this, content, title, subtitle, currentPaperSize, selectedLayout);
                showPage(0);
                loadPdfFromFile(qrPdf, "QR_" + (title.isEmpty() ? "Label" : title.replace(" ", "_")) + ".pdf");
                Toast.makeText(this, "QR Sheet generated! Ready to print.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "QR generation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void renderPageThumb() {
        if (totalPages <= 0) return;

        final SheetGeometry.TilingMode targetTiling = (currentTilingMode != null) ? currentTilingMode : SheetGeometry.TilingMode.ONE_UP;
        int nUp = targetTiling.pagesPerSheet;
        int totalSheets = (totalPages + nUp - 1) / nUp;
        int currentSheet = currentPageIndex / nUp;

        if (targetTiling == SheetGeometry.TilingMode.ONE_UP) {
            tvPageIndicator.setText("Page " + (currentPageIndex + 1) + " of " + totalPages);
        } else {
            int startP = currentSheet * nUp + 1;
            int endP = Math.min((currentSheet + 1) * nUp, totalPages);
            if (startP == endP) {
                tvPageIndicator.setText("Sheet " + (currentSheet + 1) + " of " + totalSheets + " (Page " + startP + ")");
            } else {
                tvPageIndicator.setText("Sheet " + (currentSheet + 1) + " of " + totalSheets + " (Pages " + startP + "–" + endP + ")");
            }
        }
        btnPrevPage.setEnabled(currentSheet > 0);
        btnNextPage.setEnabled(currentSheet < totalSheets - 1);
        btnQuickZoom.setText("🔍 Zoom");
        updatePageRangeLabels();
        updatePageSummary();

        final int token = previewRenderToken.incrementAndGet();
        final int targetPageIndex = currentPageIndex;
        final int targetMarginMm = currentMarginMm;
        final SheetGeometry.PaperSize targetPaper = (currentPaperSize != null) ? currentPaperSize : SheetGeometry.PaperSize.A4;
        final SheetGeometry.PlacementRegion targetRegion = currentPlacementRegion;
        final SheetGeometry.TilingMode targetTilingMode = currentTilingMode;
        final float targetRotation = currentPageRotation;
        final boolean targetInvertDark = (cbInvertDark != null && cbInvertDark.isChecked());
        final int targetDarkness = (seekDarkness != null) ? seekDarkness.getProgress() : 75;

        previewExecutor.execute(() -> {
            if (token != previewRenderToken.get()) return;

            Bitmap baseBm = null;
            synchronized (pdfRendererLock) {
                if (pdfRenderer == null || totalPages == 0 || targetPageIndex < 0 || targetPageIndex >= totalPages) {
                    return;
                }
                try {
                    int sheetPreviewW = 800;
                    int sheetPreviewH = (int) (800.0f * (targetPaper.heightPts / (float) targetPaper.widthPts));
                    int marginPx = (targetMarginMm > 0) ? (int) Math.round((targetMarginMm / 210.0f) * sheetPreviewW) : 0;

                    if (targetTilingMode == null || targetTilingMode == SheetGeometry.TilingMode.ONE_UP) {
                        PdfRenderer.Page page = pdfRenderer.openPage(targetPageIndex);
                        int pw = page.getWidth();
                        int ph = page.getHeight();

                        if (targetRegion == null || targetRegion == SheetGeometry.PlacementRegion.FULL_PAGE) {
                            if (targetMarginMm <= 0) {
                                // Full-bleed borderless
                                float scale = 800.0f / (float) pw;
                                int previewW = 800;
                                int previewH = (int) (ph * scale);

                                baseBm = Bitmap.createBitmap(previewW, previewH, Bitmap.Config.ARGB_8888);
                                baseBm.eraseColor(0xFFFFFFFF);
                                page.render(baseBm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                                page.close();
                            } else {
                                // Full page with margin padding
                                baseBm = Bitmap.createBitmap(sheetPreviewW, sheetPreviewH, Bitmap.Config.ARGB_8888);
                                baseBm.eraseColor(0xFFFFFFFF);

                                Rect targetBox = new Rect(0, 0, sheetPreviewW, sheetPreviewH);
                                Rect destRect = SheetGeometry.calculateAspectFit(pw, ph, targetBox, marginPx);

                                Bitmap docBm = Bitmap.createBitmap(destRect.width(), destRect.height(), Bitmap.Config.ARGB_8888);
                                docBm.eraseColor(0xFFFFFFFF);
                                page.render(docBm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                                page.close();

                                Canvas canvas = new Canvas(baseBm);
                                canvas.drawBitmap(docBm, destRect.left, destRect.top, null);
                                docBm.recycle();
                            }
                        } else {
                            // Partial-page placement (top half, bottom half, quadrant...)
                            baseBm = Bitmap.createBitmap(sheetPreviewW, sheetPreviewH, Bitmap.Config.ARGB_8888);
                            baseBm.eraseColor(0xFFFFFFFF);

                            Rect targetBox = SheetGeometry.getTargetBox(targetRegion, sheetPreviewW, sheetPreviewH);
                            int effectiveMargin = Math.max(marginPx, 16);
                            Rect destRect = SheetGeometry.calculateAspectFit(pw, ph, targetBox, effectiveMargin);

                            Bitmap docBm = Bitmap.createBitmap(destRect.width(), destRect.height(), Bitmap.Config.ARGB_8888);
                            docBm.eraseColor(0xFFFFFFFF);
                            page.render(docBm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                            page.close();

                            Canvas canvas = new Canvas(baseBm);
                            canvas.drawBitmap(docBm, destRect.left, destRect.top, null);
                            docBm.recycle();
                        }
                    } else {
                        // Tiled Multi-Page Preview (2-Up or 4-Up)
                        baseBm = Bitmap.createBitmap(sheetPreviewW, sheetPreviewH, Bitmap.Config.ARGB_8888);
                        baseBm.eraseColor(0xFFFFFFFF);
                        Canvas canvas = new Canvas(baseBm);

                        Rect[] tileBoxes = SheetGeometry.calculateNUpBoxes(targetTilingMode, sheetPreviewW, sheetPreviewH, marginPx);
                        int tileNUp = targetTilingMode.pagesPerSheet;
                        int startPage = (targetPageIndex / tileNUp) * tileNUp;

                        android.graphics.Paint borderPaint = new android.graphics.Paint();
                        borderPaint.setStyle(android.graphics.Paint.Style.STROKE);
                        borderPaint.setColor(0x22000000);
                        borderPaint.setStrokeWidth(1.5f);

                        for (int i = 0; i < tileNUp; i++) {
                            int pIdx = startPage + i;
                            if (pIdx < totalPages && i < tileBoxes.length) {
                                PdfRenderer.Page page = pdfRenderer.openPage(pIdx);
                                int pw = page.getWidth();
                                int ph = page.getHeight();

                                Rect tileBox = tileBoxes[i];
                                Rect destRect = SheetGeometry.calculateAspectFit(pw, ph, tileBox, 0);

                                if (destRect.width() > 0 && destRect.height() > 0) {
                                    Bitmap docBm = Bitmap.createBitmap(destRect.width(), destRect.height(), Bitmap.Config.ARGB_8888);
                                    docBm.eraseColor(0xFFFFFFFF);
                                    page.render(docBm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                                    canvas.drawBitmap(docBm, destRect.left, destRect.top, null);
                                    canvas.drawRect(destRect, borderPaint);
                                    docBm.recycle();
                                }
                                page.close();
                            }
                        }
                    }
                } catch (Exception e) {
                    DiagnosticLogManager.logError("PREVIEW", "Page render error", e);
                    if (baseBm != null && !baseBm.isRecycled()) {
                        baseBm.recycle();
                        baseBm = null;
                    }
                    return;
                }
            }

            if (token != previewRenderToken.get() || baseBm == null) {
                if (baseBm != null && !baseBm.isRecycled()) baseBm.recycle();
                return;
            }

            // 1. Rotation
            if (targetRotation != 0) {
                Bitmap rot = ToneEngine.rotateBitmap(baseBm, targetRotation);
                if (rot != baseBm) {
                    baseBm.recycle();
                    baseBm = rot;
                }
            }

            if (token != previewRenderToken.get()) {
                baseBm.recycle();
                return;
            }

            // 2. Toner Shield (Dark PDF Inversion)
            if (targetInvertDark) {
                Bitmap inv = ToneEngine.invertDarkDocument(baseBm);
                if (inv != baseBm) {
                    baseBm.recycle();
                    baseBm = inv;
                }
            }

            if (token != previewRenderToken.get()) {
                baseBm.recycle();
                return;
            }

            // 2.5 Watermark Real-Time Overlay
            String wmText = (etWatermark != null && etWatermark.getText() != null) ? etWatermark.getText().toString().trim() : "";
            if (!wmText.isEmpty()) {
                Bitmap wmBm = baseBm.copy(Bitmap.Config.ARGB_8888, true);
                baseBm.recycle();
                baseBm = wmBm;
                Canvas wmCanvas = new Canvas(baseBm);
                ToneEngine.applyWatermark(wmCanvas, baseBm.getWidth(), baseBm.getHeight(), wmText);
            }

            if (token != previewRenderToken.get()) {
                baseBm.recycle();
                return;
            }

            // 3. Auto-blank page detection
            final boolean isBlank = ToneEngine.isPageBlank(baseBm);

            // 4. Tone adjusted bitmap for real-time laser preview
            final Bitmap toneAdjusted = ToneEngine.processPreviewBitmap(baseBm, targetDarkness);

            final Bitmap finalRaw = baseBm;

            mainHandler.post(() -> {
                if (token != previewRenderToken.get()) {
                    finalRaw.recycle();
                    if (toneAdjusted != null && toneAdjusted != finalRaw && !toneAdjusted.isRecycled()) {
                        toneAdjusted.recycle();
                    }
                    return;
                }

                if (rawPageBitmap != null && !rawPageBitmap.isRecycled()) {
                    rawPageBitmap.recycle();
                }
                if (toneAdjustedBitmap != null && toneAdjustedBitmap != rawPageBitmap && !toneAdjustedBitmap.isRecycled()) {
                    toneAdjustedBitmap.recycle();
                }

                rawPageBitmap = finalRaw;
                toneAdjustedBitmap = (toneAdjusted != null) ? toneAdjusted : finalRaw;

                if (isShowingOriginal) {
                    ivPagePreview.setImageBitmap(rawPageBitmap);
                } else {
                    ivPagePreview.setImageBitmap(toneAdjustedBitmap);
                }

                btnQuickZoom.setText("🔍 Zoom");

                if (isBlank) {
                    layoutBlankPageBanner.setVisibility(View.VISIBLE);
                    tvBlankPageText.setText("⚠️ Page " + (targetPageIndex + 1) + " appears blank. Skip printing?");
                    btnSkipBlankPage.setOnClickListener(v -> {
                        SoundManager.playClick();
                        excludePageFromPrint(targetPageIndex);
                        layoutBlankPageBanner.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, "Page " + (targetPageIndex + 1) + " excluded from print run.", Toast.LENGTH_SHORT).show();
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
            });
        });
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

        int nUp = (currentTilingMode != null) ? currentTilingMode.pagesPerSheet : 1;
        int physicalSheetsPerCopy = (count + nUp - 1) / nUp;
        int totalPhysicalSheets = physicalSheetsPerCopy * copies;

        if (copies > 1) {
            sb.append(" • ").append(copies).append(" copies");
            if (cbCollate.isChecked()) {
                sb.append(" (Collated)");
            } else {
                sb.append(" (Grouped)");
            }
            sb.append(" • Total ").append(totalPhysicalSheets).append(totalPhysicalSheets == 1 ? " sheet" : " sheets");
        } else {
            sb.append(" • 1 copy");
            if (currentTilingMode != SheetGeometry.TilingMode.ONE_UP) {
                sb.append(" • Total ").append(totalPhysicalSheets).append(totalPhysicalSheets == 1 ? " sheet" : " sheets");
            }
        }

        if (currentTilingMode == SheetGeometry.TilingMode.TWO_UP) {
            sb.append(" (2-in-1 Saves 50% Paper)");
        } else if (currentTilingMode == SheetGeometry.TilingMode.FOUR_UP) {
            sb.append(" (4-in-1 Saves 75% Paper)");
        }

        sb.append(" • ").append(currentPaperSize.name());
        if (currentTilingMode != SheetGeometry.TilingMode.ONE_UP) {
            sb.append(" (").append(currentTilingMode.title).append(")");
        } else if (currentPlacementRegion != SheetGeometry.PlacementRegion.FULL_PAGE) {
            sb.append(" (").append(currentPlacementRegion.title).append(")");
        }

        if (currentMarginMm == 0) {
            sb.append(" • 0mm (Border)");
        } else {
            sb.append(" • ").append(currentMarginMm).append("mm margin");
        }

        if (cbReverseOrder.isChecked()) {
            sb.append(" • Reverse order");
        }

        tvPageSummary.setText(sb.toString());
    }

    private void closeCurrentPdf() {
        previewRenderToken.incrementAndGet();
        synchronized (pdfRendererLock) {
            try {
                if (pdfRenderer != null) pdfRenderer.close();
                if (pdfPfd != null) pdfPfd.close();
                if (rawPageBitmap != null && !rawPageBitmap.isRecycled()) rawPageBitmap.recycle();
                if (toneAdjustedBitmap != null && toneAdjustedBitmap != rawPageBitmap && !toneAdjustedBitmap.isRecycled()) {
                    toneAdjustedBitmap.recycle();
                }
            } catch (Exception ignored) {}
            pdfRenderer = null;
            pdfPfd = null;
            rawPageBitmap = null;
            toneAdjustedBitmap = null;
            totalPages = 0;
            currentPageIndex = 0;
        }
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

        DiagnosticLogManager.log("PRINT", "Initiating print job: " + pagesToPrint.size() + " pages, " + copies + " copies, darkness=" + darkness + "%, placement=" + currentPlacementRegion.title + ", tiling=" + currentTilingMode.title);

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

        printEngine.printPdf(currentPdfFile, darkness, dpi, copies, collate, pagesToPrint, currentPaperSize, currentPlacementRegion, currentTilingMode, invertDark, watermark, rotation, currentMarginMm, new PrintEngine.PrintProgressListener() {
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

    // =========================================================================
    // BYOK (Bring Your Own Key) AI Print Suite
    // =========================================================================

    private void updateAiKeyDisplay() {
        if (etGeminiApiKey == null || tvAiKeyStatus == null) return;
        if (AiAssistantEngine.hasApiKey(this)) {
            String key = AiAssistantEngine.getApiKey(this);
            etGeminiApiKey.setText(key);
            tvAiKeyStatus.setText("✓ Gemini API Key Configured (100% Free Tier 15 RPM Active)");
            tvAiKeyStatus.setTextColor(Color.parseColor("#10B981"));
        } else {
            etGeminiApiKey.setText("");
            tvAiKeyStatus.setText("No key configured. Get your free key at aistudio.google.com");
            tvAiKeyStatus.setTextColor(Color.parseColor("#94A3B8"));
        }
    }

    private void saveAiKey() {
        if (etGeminiApiKey == null) return;
        String key = etGeminiApiKey.getText().toString().trim();
        AiAssistantEngine.saveApiKey(this, key);
        updateAiKeyDisplay();
        SoundManager.playClick();
        if (!key.isEmpty()) {
            Toast.makeText(this, "Gemini API Key saved! AI Print Suite ready.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Gemini API Key cleared.", Toast.LENGTH_SHORT).show();
        }
    }

    private void testAiKey() {
        if (etGeminiApiKey == null || tvAiKeyStatus == null) return;
        String key = etGeminiApiKey.getText().toString().trim();
        if (key.isEmpty()) {
            key = AiAssistantEngine.getApiKey(this);
        }
        if (key.isEmpty()) {
            Toast.makeText(this, "Please enter an API key first.", Toast.LENGTH_SHORT).show();
            tvAiKeyStatus.setText("Please enter an API key to test.");
            tvAiKeyStatus.setTextColor(Color.parseColor("#F59E0B"));
            return;
        }

        tvAiKeyStatus.setText("⚡ Testing connection to Gemini API...");
        tvAiKeyStatus.setTextColor(Color.parseColor("#38BDF8"));
        if (btnTestAiKey != null) btnTestAiKey.setEnabled(false);

        final String testKey = key;
        AiAssistantEngine.testApiKey(this, testKey, new AiAssistantEngine.AiCallback<String>() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    if (btnTestAiKey != null) btnTestAiKey.setEnabled(true);
                    tvAiKeyStatus.setText("✓ " + result);
                    tvAiKeyStatus.setTextColor(Color.parseColor("#10B981"));
                    SoundManager.playXpPrintChime();
                    Toast.makeText(MainActivity.this, "Gemini API ping successful!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    if (btnTestAiKey != null) btnTestAiKey.setEnabled(true);
                    tvAiKeyStatus.setText("✗ Ping failed: " + e.getMessage());
                    tvAiKeyStatus.setTextColor(Color.parseColor("#EF4444"));
                    SoundManager.playAlert();
                    Toast.makeText(MainActivity.this, "Test failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showAiAssistantDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✨ AI Print Suite (100% Free BYOK)");

        String[] options = new String[] {
            "🧾 Chat / WhatsApp Text → Clean Invoice PDF",
            "📝 Lecture / Notes → 1-Page Executive Cheat Sheet",
            "🌐 Document / Text Translator & Placard",
            "⚙️ Configure Gemini API Key (Dev Tab)"
        };

        builder.setItems(options, (dialog, which) -> {
            SoundManager.playClick();
            if (which == 0) {
                if (!ensureAiKeyConfigured()) return;
                promptChatToInvoice();
            } else if (which == 1) {
                if (!ensureAiKeyConfigured()) return;
                promptSummarize();
            } else if (which == 2) {
                if (!ensureAiKeyConfigured()) return;
                promptTranslate();
            } else if (which == 3) {
                showPage(3);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private boolean ensureAiKeyConfigured() {
        if (!AiAssistantEngine.hasApiKey(this)) {
            new AlertDialog.Builder(this)
                .setTitle("🔑 Gemini API Key Required")
                .setMessage("DodoPress uses Bring Your Own Key (BYOK) architecture with Google Gemini (Free Tier: 15 requests/minute forever).\n\nNo credit card required at aistudio.google.com.\n\nWould you like to configure your key now in the Dev tab?")
                .setPositiveButton("Configure Key", (d, w) -> {
                    SoundManager.playClick();
                    showPage(3);
                })
                .setNegativeButton("Later", (d, w) -> d.dismiss())
                .show();
            return false;
        }
        return true;
    }

    private void promptChatToInvoice() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🧾 Chat to Laser Invoice");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 24, 36, 24);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("Paste an informal WhatsApp message, email order, or rough notes with client, items, and prices:");
        tvDesc.setTextColor(Color.WHITE);
        tvDesc.setTextSize(13f);
        tvDesc.setPadding(0, 0, 0, 12);
        layout.addView(tvDesc);

        EditText etChat = new EditText(this);
        etChat.setLines(8);
        etChat.setGravity(Gravity.TOP | Gravity.START);
        etChat.setTextColor(Color.WHITE);
        etChat.setHintTextColor(0xFF888888);
        etChat.setHint("e.g.\nClient: Rajesh Kumar, Ph: 9876543210\n1x LaserJet Cartridge Refill - ₹450\n2x A4 Paper Reams (JK Copier) - ₹720\nAdvance paid: ₹500, Balance due: ₹670\nThank you for choosing us!");
        layout.addView(etChat);
        scrollView.addView(layout);
        builder.setView(scrollView);

        builder.setPositiveButton("⚡ Generate Invoice PDF", (dialog, which) -> {
            String text = etChat.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Please enter or paste order details.", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog progress = new ProgressDialog(this);
            progress.setMessage("AI is formatting order into 300 DPI vector invoice...");
            progress.setCancelable(false);
            progress.show();

            AiAssistantEngine.chatToInvoice(this, text, currentPaperSize, new AiAssistantEngine.AiCallback<File>() {
                @Override
                public void onSuccess(File invoicePdf) {
                    runOnUiThread(() -> {
                        progress.dismiss();
                        SoundManager.playXpPrintChime();
                        showPage(0);
                        loadPdfFromFile(invoicePdf, "AI_Invoice_" + System.currentTimeMillis() + ".pdf");
                        Toast.makeText(MainActivity.this, "Invoice generated! Ready to print.", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        progress.dismiss();
                        SoundManager.playAlert();
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Invoice Generation Failed")
                            .setMessage(e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void promptSummarize() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📝 1-Page Executive / Cheat Sheet");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 24, 36, 24);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("Paste lengthy lecture notes, article, meeting transcript, or contract to condense into a 1-page high-contrast laser printout:");
        tvDesc.setTextColor(Color.WHITE);
        tvDesc.setTextSize(13f);
        tvDesc.setPadding(0, 0, 0, 12);
        layout.addView(tvDesc);

        EditText etTitle = new EditText(this);
        etTitle.setHint("Document Title (e.g. Operating Systems Notes)");
        etTitle.setTextColor(Color.WHITE);
        etTitle.setHintTextColor(0xFF888888);
        layout.addView(etTitle);

        EditText etNotes = new EditText(this);
        etNotes.setLines(8);
        etNotes.setGravity(Gravity.TOP | Gravity.START);
        etNotes.setTextColor(Color.WHITE);
        etNotes.setHintTextColor(0xFF888888);
        etNotes.setHint("Paste full lecture or meeting text here...");
        layout.addView(etNotes);

        scrollView.addView(layout);
        builder.setView(scrollView);

        builder.setPositiveButton("⚡ Generate Cheat Sheet PDF", (dialog, which) -> {
            String text = etNotes.getText().toString().trim();
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) title = "Executive Cheat Sheet";
            if (text.isEmpty()) {
                Toast.makeText(this, "Please enter or paste document text.", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog progress = new ProgressDialog(this);
            progress.setMessage("AI is condensing and formatting into 1-page laser cheat sheet...");
            progress.setCancelable(false);
            progress.show();

            AiAssistantEngine.summarizeToCheatSheet(this, text, title, currentPaperSize, new AiAssistantEngine.AiCallback<File>() {
                @Override
                public void onSuccess(File summaryPdf) {
                    runOnUiThread(() -> {
                        progress.dismiss();
                        SoundManager.playXpPrintChime();
                        showPage(0);
                        loadPdfFromFile(summaryPdf, "AI_CheatSheet_" + System.currentTimeMillis() + ".pdf");
                        Toast.makeText(MainActivity.this, "1-Page Cheat Sheet generated! Ready to print.", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        progress.dismiss();
                        SoundManager.playAlert();
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Cheat Sheet Generation Failed")
                            .setMessage(e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void promptTranslate() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🌐 Document Translator & Placard");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 24, 36, 24);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("Select target language and paste content to generate a clean translated printable document:");
        tvDesc.setTextColor(Color.WHITE);
        tvDesc.setTextSize(13f);
        tvDesc.setPadding(0, 0, 0, 12);
        layout.addView(tvDesc);

        final String[] languages = new String[] {
            "Hindi (हिन्दी)", "Spanish (Español)", "French (Français)", "German (Deutsch)",
            "Japanese (日本語)", "Arabic (العربية)", "Russian (Русский)", "Portuguese (Português)",
            "Bengali (বাংলা)", "Telugu (తెలుగు)", "Tamil (தமிழ்)", "Marathi (मराठी)", "Gujarati (ગુજરાતી)"
        };
        final String[] langCodes = new String[] {
            "Hindi", "Spanish", "French", "German",
            "Japanese", "Arabic", "Russian", "Portuguese",
            "Bengali", "Telugu", "Tamil", "Marathi", "Gujarati"
        };

        Spinner spLang = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        spLang.setAdapter(adapter);
        layout.addView(spLang);

        EditText etText = new EditText(this);
        etText.setLines(7);
        etText.setGravity(Gravity.TOP | Gravity.START);
        etText.setTextColor(Color.WHITE);
        etText.setHintTextColor(0xFF888888);
        etText.setHint("Paste notice, guidelines, contract, or placard text to translate...");
        layout.addView(etText);

        scrollView.addView(layout);
        builder.setView(scrollView);

        builder.setPositiveButton("⚡ Translate & Generate PDF", (dialog, which) -> {
            String text = etText.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Please enter or paste text to translate.", Toast.LENGTH_SHORT).show();
                return;
            }
            int selectedIdx = spLang.getSelectedItemPosition();
            String targetLang = (selectedIdx >= 0 && selectedIdx < langCodes.length) ? langCodes[selectedIdx] : "Hindi";

            ProgressDialog progress = new ProgressDialog(this);
            progress.setMessage("AI is translating into " + targetLang + " and formatting PDF...");
            progress.setCancelable(false);
            progress.show();

            AiAssistantEngine.translateAndFormat(this, text, targetLang, currentPaperSize, new AiAssistantEngine.AiCallback<File>() {
                @Override
                public void onSuccess(File transPdf) {
                    runOnUiThread(() -> {
                        progress.dismiss();
                        SoundManager.playXpPrintChime();
                        showPage(0);
                        loadPdfFromFile(transPdf, "AI_Translated_" + targetLang + ".pdf");
                        Toast.makeText(MainActivity.this, "Translated document ready to print!", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        progress.dismiss();
                        SoundManager.playAlert();
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Translation Failed")
                            .setMessage(e.getMessage())
                            .setPositiveButton("OK", null)
                            .show();
                    });
                }
            });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
