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
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.animation.ValueAnimator;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowInsetsController;
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
import android.widget.HorizontalScrollView;
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
import com.dodopress.driver.DriverManager;
import com.dodopress.driver.PrinterModel;
import com.dodopress.driver.PrinterProtocol;
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
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {

    private static final int REQUEST_PICK_PDF = 2001;
    private static final int REQUEST_PICK_ID_FRONT = 2002;
    private static final int REQUEST_PICK_ID_BACK = 2003;
    private static final int REQUEST_CAMERA_SCAN = 2004;
    private static final int REQUEST_SCAN_ID_FRONT = 2005;
    private static final int REQUEST_SCAN_ID_BACK = 2006;
    private static final int REQUEST_PICK_ID2_FRONT = 2007;
    private static final int REQUEST_PICK_ID2_BACK = 2008;
    private static final int REQUEST_SCAN_ID2_FRONT = 2009;
    private static final int REQUEST_SCAN_ID2_BACK = 2010;
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
    private TextView tvXpTitle;
    private View layoutModernHeader;

    private AnimatedLogoView viewAnimatedLogo;
    private Button btnDiagnostics;
    private View layoutStatusCapsule;
    private PulsingBeaconView viewBeacon;
    private LaserScanView laserScanView;

    private View btnThemeMenu;
    private AuroraBackgroundView viewAuroraBg;
    private View cardIdSheetPreview;
    private View cardGithubStars;
    private View cardTelegramFeedback;

    // Retractable Sliding Options Panel & Section Control
    public enum ControlSection {
        RECENTS,
        TONE,
        GEOMETRY,
        MARGINS,
        PAGES,
        WATERMARK
    }

    private ControlSection currentControlSection = ControlSection.TONE;

    private View layoutSlidingPanel;
    private TextView tvPanelHandle;
    private Button btnTogglePanel;
    private HorizontalScrollView scrollCategoryTabs;
    private Button btnTabRecents;
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

    // Margin Controls & Mathematical Readout
    private TextView tvMarginTitle;
    private TextView tvMarginLabel;
    private TextView tvMarginDesc;
    private SeekBar seekMargin;
    private Button btnMarginMinus;
    private Button btnMarginPlus;
    private Button btnMarginZero;
    private Button btnMarginHwMin;
    private Button btnMarginNarrow;
    private Button btnMarginNormal;
    private Button btnMarginWide;
    private TextView tvMarginCalcSheet;
    private TextView tvMarginCalcPrintable;
    private TextView tvMarginCalcHardware;
    private TextView tvMarginHwStatus;
    private float currentMarginMm = 12.0f;

    // Sheet Geometry & Orientation
    private SheetGeometry.PaperSize currentPaperSize = SheetGeometry.PaperSize.A4;
    private SheetGeometry.Orientation currentOrientation = SheetGeometry.Orientation.PORTRAIT;
    private SheetGeometry.PlacementRegion currentPlacementRegion = SheetGeometry.PlacementRegion.FULL_PAGE;
    private Button btnQuickOrient;
    private TextView tvOrientLabel;
    private Button btnOrientPortrait;
    private Button btnOrientLandscape;

    // Preview & Header Controls
    private TextView tvPrinterStatus;
    private TextView tvConnectionSymbol;
    private Button btnPickFile;
    private Button btnScanDocument;
    private Button btnQrLabel;
    private Button btnAiAssistant;
    private TextView tvFileInfo;
    private FrameLayout layoutPreviewContainer;
    private ZoomableImageView ivPagePreview;
    private TextView tvPreviewModeBadge;
    private Button btnRotatePage;
    private Button btnTogglePreviewMode;
    private Button btnQuickZoom;
    private Button btnStageExpand;
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
    private Bitmap cleanPreviewBitmap;
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

    // ID Copier Tab Views & Config
    public enum IdControlSection {
        PHOTOS,
        LAYOUT,
        PAPER,
        MARGINS,
        TONE,
        WATERMARK,
        COPIES
    }

    private IdControlSection currentIdControlSection = IdControlSection.PHOTOS;
    private boolean isIdPanelCollapsed = false;

    // Header & Preview Card
    private Button btnIdClearPhotos;
    private Button btnIdQuickPrint;
    private TextView tvIdPreviewBadge;
    private Button btnIdQuickOrient;
    private Button btnIdQuickGuides;
    private Button btnIdStageExpand;
    private View layoutIdPreviewContainer;

    // Sliding Panel & Category Fast-Switch Tabs
    private View layoutIdSlidingPanel;
    private TextView tvIdPanelHandle;
    private Button btnIdTogglePanel;
    private HorizontalScrollView scrollIdCategoryTabs;
    private Button btnIdTabPhotos;
    private Button btnIdTabLayout;
    private Button btnIdTabPaper;
    private Button btnIdTabMargins;
    private Button btnIdTabTone;
    private Button btnIdTabWatermark;
    private Button btnIdTabCopies;
    private ScrollView scrollIdOptions;

    // Section Cards
    private View cardIdPhotos;
    private View cardIdLayout;
    private View cardIdPaper;
    private View cardIdMargins;
    private View cardIdTone;
    private View cardIdWatermark;
    private View cardIdCopies;

    // Layout & Spacing Controls
    private Button btnIdCount1, btnIdCount2;
    private Button btnIdPlaceTop, btnIdPlaceCenter, btnIdPlaceFull, btnIdPlaceGrid;
    private Button btnIdToggleCutGuides;
    private TextView tvIdSpacingVal;
    private SeekBar seekIdSpacing;
    private Button btnIdSpace5, btnIdSpace10, btnIdSpace20, btnIdSpace30, btnIdSpace40;
    private TextView tvIdScaleVal;
    private SeekBar seekIdScale;
    private Button btnIdScale85, btnIdScale100, btnIdScale115, btnIdScale130;

    // Paper & Orientation Controls
    private Button btnIdPaperA4, btnIdPaperLetter, btnIdPaperLegal, btnIdPaperA5;
    private Button btnIdOrientPortrait, btnIdOrientLandscape;

    // Margins Controls
    private TextView tvIdMarginVal;
    private View layoutIdDrumWarning;
    private SeekBar seekIdMargin;
    private Button btnIdMarginZero, btnIdMarginHwMin, btnIdMarginNarrow, btnIdMarginNormal, btnIdMarginWide;

    // Toner Density Controls
    private TextView tvIdToneVal;
    private SeekBar seekIdDarkness;
    private Button btnIdToneDraft, btnIdToneNormal, btnIdToneDeep, btnIdToneUltra;

    // Watermark Controls
    private Button btnIdWmConfidential, btnIdWmCopy, btnIdWmOfficial, btnIdWmClear;
    private EditText etIdWatermark;

    // Copies Controls
    private Button btnIdCopiesMinus, btnIdCopiesPlus;
    private TextView tvIdCopiesCount;
    private Button btnIdCopies1, btnIdCopies2, btnIdCopies5, btnIdCopies10;

    // Photos
    private ImageView ivIdSheetPreview;
    private TextView tvIdPreviewInfo;
    private ImageView ivIdFrontPreview;
    private ImageView ivIdBackPreview;
    private Button btnIdScanFront, btnIdPickFront;
    private Button btnIdScanBack, btnIdPickBack;
    private View layoutId2Container;
    private ImageView ivId2FrontPreview, ivId2BackPreview;
    private Button btnId2ScanFront, btnId2PickFront;
    private Button btnId2ScanBack, btnId2PickBack;
    private Button btnIdPrintNow;
    private Button btnIdSendStudio;

    private Bitmap idFrontBitmap;
    private Bitmap idBackBitmap;
    private Bitmap id2FrontBitmap;
    private Bitmap id2BackBitmap;
    private IdCardEngine.IdCardConfig idConfig = new IdCardEngine.IdCardConfig();

    // Spool Queue Tab Views
    private Button btnSpoolClearAll;
    private LinearLayout layoutSpoolItemsContainer;
    private TextView tvSpoolEmpty;

    // Developer & Support Tab Views
    private FrameLayout frameDevAvatar;
    private TextView tvDevName;
    private Button btnStarGithub;
    private Button btnOpenTelegram;
    private Button btnCopyTelegram;
    private Button btnFeedbackRequest;
    private CheckBox cbSoundEffects;
    private int devAvatarTapCount = 0;

    // BYOK AI Assistant Views
    private View cardAiByok;
    private EditText etGeminiApiKey;
    private Button btnSaveAiKey;
    private Button btnTestAiKey;
    private TextView tvAiKeyStatus;

    // Universal Driver Hub (PrintShare Architecture)
    private DriverManager driverManager;
    private View cardDevUniversalDriver;
    private TextView tvDriverProtocolBadge;
    private TextView tvActivePrinterName;
    private TextView tvActivePrinterSpecs;
    private TextView tvActiveDriverStatus;
    private ProgressBar pbDriverDownload;
    private Button btnDevBrowseCatalog;
    private Button btnDevScanUsb;

    private File currentPdfFile;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor pdfPfd;
    private int totalPages = 0;
    private int currentPageIndex = 0;
    private Bitmap rawPageBitmap;
    private int copies = 1;

    private ThemeManager.AppTheme currentTheme = ThemeManager.AppTheme.PAPER_STUDIO;

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
        driverManager = DriverManager.getInstance(this);

        initViews();
        setupListeners();
        registerUsbHotplugReceiver();
        refreshActivePrinterDisplay();

        currentTheme = ThemeManager.getTheme(this);
        applyAppTheme(currentTheme);
        updateOrientationButtonsHighlight();
        updateMarginDisplay(currentMarginMm);
        renderPageThumb();

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
        if (viewAuroraBg != null && currentTheme == ThemeManager.AppTheme.AETHER_AURORA) {
            viewAuroraBg.resumeAnimation();
        }
        refreshPrinterStatus();
        renderSpoolQueue();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (viewAuroraBg != null) {
            viewAuroraBg.pauseAnimation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (viewAuroraBg != null) {
            viewAuroraBg.stopAnimation();
        }
        unregisterUsbHotplugReceiver();
        closeCurrentPdf();
        if (idFrontBitmap != null && !idFrontBitmap.isRecycled()) idFrontBitmap.recycle();
        if (idBackBitmap != null && !idBackBitmap.isRecycled()) idBackBitmap.recycle();
        if (id2FrontBitmap != null && !id2FrontBitmap.isRecycled()) id2FrontBitmap.recycle();
        if (id2BackBitmap != null && !id2BackBitmap.isRecycled()) id2BackBitmap.recycle();
        previewExecutor.shutdownNow();
    }

    private void initViews() {
        rootMainContainer = findViewById(com.dodopress.R.id.root_main_container);
        layoutXpTitlebar = findViewById(com.dodopress.R.id.layout_xp_titlebar);
        tvXpTitle = findViewById(com.dodopress.R.id.tv_xp_title);
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

        // Dynamic Aurora View
        viewAuroraBg = findViewById(com.dodopress.R.id.view_aurora_bg);

        // Theme Studio Menu
        btnThemeMenu = findViewById(com.dodopress.R.id.btn_theme_menu);

        // Retractable Sliding Panel
        layoutSlidingPanel = findViewById(com.dodopress.R.id.layout_sliding_panel);
        tvPanelHandle = findViewById(com.dodopress.R.id.tv_panel_handle);
        btnTogglePanel = findViewById(com.dodopress.R.id.btn_toggle_panel);
        scrollCategoryTabs = findViewById(com.dodopress.R.id.scroll_category_tabs);
        btnTabRecents = findViewById(com.dodopress.R.id.btn_tab_recents);
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

        // Margin Controls & Mathematical Readout
        tvMarginTitle = findViewById(com.dodopress.R.id.tv_margin_title);
        tvMarginLabel = findViewById(com.dodopress.R.id.tv_margin_label);
        tvMarginDesc = findViewById(com.dodopress.R.id.tv_margin_desc);
        seekMargin = findViewById(com.dodopress.R.id.seek_margin);
        btnMarginMinus = findViewById(com.dodopress.R.id.btn_margin_minus);
        btnMarginPlus = findViewById(com.dodopress.R.id.btn_margin_plus);
        btnMarginZero = findViewById(com.dodopress.R.id.btn_margin_zero);
        btnMarginHwMin = findViewById(com.dodopress.R.id.btn_margin_hw_min);
        btnMarginNarrow = findViewById(com.dodopress.R.id.btn_margin_narrow);
        btnMarginNormal = findViewById(com.dodopress.R.id.btn_margin_normal);
        btnMarginWide = findViewById(com.dodopress.R.id.btn_margin_wide);
        tvMarginCalcSheet = findViewById(com.dodopress.R.id.tv_margin_calc_sheet);
        tvMarginCalcPrintable = findViewById(com.dodopress.R.id.tv_margin_calc_printable);
        tvMarginCalcHardware = findViewById(com.dodopress.R.id.tv_margin_calc_hardware);
        tvMarginHwStatus = findViewById(com.dodopress.R.id.tv_margin_hw_status);

        // Sheet Orientation Controls
        btnQuickOrient = findViewById(com.dodopress.R.id.btn_quick_orient);
        tvOrientLabel = findViewById(com.dodopress.R.id.tv_orient_label);
        btnOrientPortrait = findViewById(com.dodopress.R.id.btn_orient_portrait);
        btnOrientLandscape = findViewById(com.dodopress.R.id.btn_orient_landscape);

        tvPrinterStatus = findViewById(com.dodopress.R.id.tv_printer_status);
        tvConnectionSymbol = findViewById(com.dodopress.R.id.tv_connection_symbol);
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
        btnStageExpand = findViewById(com.dodopress.R.id.btn_stage_expand);
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
        tvPrintStatus = null;
        btnPrintNow = null;

        // ID Card Copier
        cardIdSheetPreview = findViewById(com.dodopress.R.id.card_id_sheet_preview);
        ivIdSheetPreview = findViewById(com.dodopress.R.id.iv_id_sheet_preview);
        tvIdPreviewInfo = findViewById(com.dodopress.R.id.tv_id_preview_info);
        btnIdClearPhotos = findViewById(com.dodopress.R.id.btn_id_clear_photos);
        btnIdQuickPrint = findViewById(com.dodopress.R.id.btn_id_quick_print);
        tvIdPreviewBadge = findViewById(com.dodopress.R.id.tv_id_preview_badge);
        btnIdQuickOrient = findViewById(com.dodopress.R.id.btn_id_quick_orient);
        btnIdQuickGuides = findViewById(com.dodopress.R.id.btn_id_quick_guides);
        btnIdStageExpand = findViewById(com.dodopress.R.id.btn_id_stage_expand);
        layoutIdPreviewContainer = findViewById(com.dodopress.R.id.layout_id_preview_container);

        layoutIdSlidingPanel = findViewById(com.dodopress.R.id.layout_id_sliding_panel);
        tvIdPanelHandle = findViewById(com.dodopress.R.id.tv_id_panel_handle);
        btnIdTogglePanel = findViewById(com.dodopress.R.id.btn_id_toggle_panel);
        scrollIdCategoryTabs = findViewById(com.dodopress.R.id.scroll_id_category_tabs);
        btnIdTabPhotos = findViewById(com.dodopress.R.id.btn_id_tab_photos);
        btnIdTabLayout = findViewById(com.dodopress.R.id.btn_id_tab_layout);
        btnIdTabPaper = findViewById(com.dodopress.R.id.btn_id_tab_paper);
        btnIdTabMargins = findViewById(com.dodopress.R.id.btn_id_tab_margins);
        btnIdTabTone = findViewById(com.dodopress.R.id.btn_id_tab_tone);
        btnIdTabWatermark = findViewById(com.dodopress.R.id.btn_id_tab_watermark);
        btnIdTabCopies = findViewById(com.dodopress.R.id.btn_id_tab_copies);
        scrollIdOptions = findViewById(com.dodopress.R.id.scroll_id_options);

        cardIdPhotos = findViewById(com.dodopress.R.id.card_id_photos);
        cardIdLayout = findViewById(com.dodopress.R.id.card_id_layout);
        cardIdPaper = findViewById(com.dodopress.R.id.card_id_paper);
        cardIdMargins = findViewById(com.dodopress.R.id.card_id_margins);
        cardIdTone = findViewById(com.dodopress.R.id.card_id_tone);
        cardIdWatermark = findViewById(com.dodopress.R.id.card_id_watermark);
        cardIdCopies = findViewById(com.dodopress.R.id.card_id_copies);

        btnIdCount1 = findViewById(com.dodopress.R.id.btn_id_count_1);
        btnIdCount2 = findViewById(com.dodopress.R.id.btn_id_count_2);
        btnIdPlaceTop = findViewById(com.dodopress.R.id.btn_id_place_top);
        btnIdPlaceCenter = findViewById(com.dodopress.R.id.btn_id_place_center);
        btnIdPlaceFull = findViewById(com.dodopress.R.id.btn_id_place_full);
        btnIdPlaceGrid = findViewById(com.dodopress.R.id.btn_id_place_grid);
        btnIdToggleCutGuides = findViewById(com.dodopress.R.id.btn_id_toggle_cut_guides);

        tvIdSpacingVal = findViewById(com.dodopress.R.id.tv_id_spacing_val);
        seekIdSpacing = findViewById(com.dodopress.R.id.seek_id_spacing);
        btnIdSpace5 = findViewById(com.dodopress.R.id.btn_id_space_5);
        btnIdSpace10 = findViewById(com.dodopress.R.id.btn_id_space_10);
        btnIdSpace20 = findViewById(com.dodopress.R.id.btn_id_space_20);
        btnIdSpace30 = findViewById(com.dodopress.R.id.btn_id_space_30);
        btnIdSpace40 = findViewById(com.dodopress.R.id.btn_id_space_40);

        tvIdScaleVal = findViewById(com.dodopress.R.id.tv_id_scale_val);
        seekIdScale = findViewById(com.dodopress.R.id.seek_id_scale);
        btnIdScale85 = findViewById(com.dodopress.R.id.btn_id_scale_85);
        btnIdScale100 = findViewById(com.dodopress.R.id.btn_id_scale_100);
        btnIdScale115 = findViewById(com.dodopress.R.id.btn_id_scale_115);
        btnIdScale130 = findViewById(com.dodopress.R.id.btn_id_scale_130);

        btnIdPaperA4 = findViewById(com.dodopress.R.id.btn_id_paper_a4);
        btnIdPaperLetter = findViewById(com.dodopress.R.id.btn_id_paper_letter);
        btnIdPaperLegal = findViewById(com.dodopress.R.id.btn_id_paper_legal);
        btnIdPaperA5 = findViewById(com.dodopress.R.id.btn_id_paper_a5);
        btnIdOrientPortrait = findViewById(com.dodopress.R.id.btn_id_orient_portrait);
        btnIdOrientLandscape = findViewById(com.dodopress.R.id.btn_id_orient_landscape);

        tvIdMarginVal = findViewById(com.dodopress.R.id.tv_id_margin_val);
        layoutIdDrumWarning = findViewById(com.dodopress.R.id.layout_id_drum_warning);
        seekIdMargin = findViewById(com.dodopress.R.id.seek_id_margin);
        btnIdMarginZero = findViewById(com.dodopress.R.id.btn_id_margin_zero);
        btnIdMarginHwMin = findViewById(com.dodopress.R.id.btn_id_margin_hw_min);
        btnIdMarginNarrow = findViewById(com.dodopress.R.id.btn_id_margin_narrow);
        btnIdMarginNormal = findViewById(com.dodopress.R.id.btn_id_margin_normal);
        btnIdMarginWide = findViewById(com.dodopress.R.id.btn_id_margin_wide);

        tvIdToneVal = findViewById(com.dodopress.R.id.tv_id_tone_val);
        seekIdDarkness = findViewById(com.dodopress.R.id.seek_id_darkness);
        btnIdToneDraft = findViewById(com.dodopress.R.id.btn_id_tone_draft);
        btnIdToneNormal = findViewById(com.dodopress.R.id.btn_id_tone_normal);
        btnIdToneDeep = findViewById(com.dodopress.R.id.btn_id_tone_deep);
        btnIdToneUltra = findViewById(com.dodopress.R.id.btn_id_tone_ultra);

        btnIdWmConfidential = findViewById(com.dodopress.R.id.btn_id_wm_confidential);
        btnIdWmCopy = findViewById(com.dodopress.R.id.btn_id_wm_copy);
        btnIdWmOfficial = findViewById(com.dodopress.R.id.btn_id_wm_official);
        btnIdWmClear = findViewById(com.dodopress.R.id.btn_id_wm_clear);
        etIdWatermark = findViewById(com.dodopress.R.id.et_id_watermark);

        btnIdCopiesMinus = findViewById(com.dodopress.R.id.btn_id_copies_minus);
        tvIdCopiesCount = findViewById(com.dodopress.R.id.tv_id_copies_count);
        btnIdCopiesPlus = findViewById(com.dodopress.R.id.btn_id_copies_plus);
        btnIdCopies1 = findViewById(com.dodopress.R.id.btn_id_copies_1);
        btnIdCopies2 = findViewById(com.dodopress.R.id.btn_id_copies_2);
        btnIdCopies5 = findViewById(com.dodopress.R.id.btn_id_copies_5);
        btnIdCopies10 = findViewById(com.dodopress.R.id.btn_id_copies_10);

        ivIdFrontPreview = findViewById(com.dodopress.R.id.iv_id_front_preview);
        ivIdBackPreview = findViewById(com.dodopress.R.id.iv_id_back_preview);
        btnIdScanFront = findViewById(com.dodopress.R.id.btn_id_scan_front);
        btnIdPickFront = findViewById(com.dodopress.R.id.btn_id_pick_front);
        btnIdScanBack = findViewById(com.dodopress.R.id.btn_id_scan_back);
        btnIdPickBack = findViewById(com.dodopress.R.id.btn_id_pick_back);

        layoutId2Container = findViewById(com.dodopress.R.id.layout_id2_container);
        ivId2FrontPreview = findViewById(com.dodopress.R.id.iv_id2_front_preview);
        ivId2BackPreview = findViewById(com.dodopress.R.id.iv_id2_back_preview);
        btnId2ScanFront = findViewById(com.dodopress.R.id.btn_id2_scan_front);
        btnId2PickFront = findViewById(com.dodopress.R.id.btn_id2_pick_front);
        btnId2ScanBack = findViewById(com.dodopress.R.id.btn_id2_scan_back);
        btnId2PickBack = findViewById(com.dodopress.R.id.btn_id2_pick_back);

        btnIdPrintNow = findViewById(com.dodopress.R.id.btn_id_print_now);
        btnIdSendStudio = findViewById(com.dodopress.R.id.btn_id_send_studio);

        // Spool Queue
        btnSpoolClearAll = findViewById(com.dodopress.R.id.btn_spool_clear_all);
        layoutSpoolItemsContainer = findViewById(com.dodopress.R.id.layout_spool_items_container);
        tvSpoolEmpty = findViewById(com.dodopress.R.id.tv_spool_empty);

        // Developer & Support Page
        cardGithubStars = findViewById(com.dodopress.R.id.card_github_stars);
        cardTelegramFeedback = findViewById(com.dodopress.R.id.card_telegram_feedback);
        frameDevAvatar = findViewById(com.dodopress.R.id.frame_dev_avatar);
        tvDevName = findViewById(com.dodopress.R.id.tv_dev_name);
        btnStarGithub = findViewById(com.dodopress.R.id.btn_star_github);
        btnOpenTelegram = findViewById(com.dodopress.R.id.btn_open_telegram);
        btnCopyTelegram = findViewById(com.dodopress.R.id.btn_copy_telegram);
        btnFeedbackRequest = findViewById(com.dodopress.R.id.btn_feedback_request);
        cbSoundEffects = findViewById(com.dodopress.R.id.cb_sound_effects);

        // BYOK AI Views
        cardAiByok = findViewById(com.dodopress.R.id.card_ai_byok);
        etGeminiApiKey = findViewById(com.dodopress.R.id.et_gemini_api_key);
        btnSaveAiKey = findViewById(com.dodopress.R.id.btn_save_ai_key);
        btnTestAiKey = findViewById(com.dodopress.R.id.btn_test_ai_key);
        tvAiKeyStatus = findViewById(com.dodopress.R.id.tv_ai_key_status);

        // Universal Driver Hub Views
        cardDevUniversalDriver = findViewById(com.dodopress.R.id.card_dev_universal_driver);
        tvDriverProtocolBadge = findViewById(com.dodopress.R.id.tv_driver_protocol_badge);
        tvActivePrinterName = findViewById(com.dodopress.R.id.tv_active_printer_name);
        tvActivePrinterSpecs = findViewById(com.dodopress.R.id.tv_active_printer_specs);
        tvActiveDriverStatus = findViewById(com.dodopress.R.id.tv_active_driver_status);
        pbDriverDownload = findViewById(com.dodopress.R.id.pb_driver_download);
        btnDevBrowseCatalog = findViewById(com.dodopress.R.id.btn_dev_browse_catalog);
        btnDevScanUsb = findViewById(com.dodopress.R.id.btn_dev_scan_usb);
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
        int inactiveColor = p.tabInactiveTextColor;
        float density = getResources().getDisplayMetrics().density;

        boolean isInitial = (previousPageIndex == -1);

        for (int i = 0; i < 4; i++) {
            final boolean isActive = (i == index);
            final View pill = pillViews[i];
            final ImageView icon = iconViews[i];
            final TextView label = labelViews[i];

            if (isActive) {
                label.setTypeface(null, android.graphics.Typeface.BOLD);
                GradientDrawable pillBg = new GradientDrawable();
                pillBg.setShape(GradientDrawable.RECTANGLE);
                pillBg.setCornerRadius(16 * density);
                pillBg.setColor(Color.argb(38, Color.red(p.tabActiveBgColor), Color.green(p.tabActiveBgColor), Color.blue(p.tabActiveBgColor)));
                pillBg.setStroke((int) (1.5f * density), p.tabActiveBgColor);
                pill.setBackground(pillBg);

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

        if (index == 0) {
            updatePageSummary();
            if (dockBtnPrint != null) dockBtnPrint.setText("⚡ PRINT NOW");
        } else if (index == 1) {
            renderIdCardPreview();
            updateIdCardSummary();
        } else if (index == 2) {
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
            UsbDevice printer = usbPrinterConnection.findPrinter();
            if (printer == null) {
                showOfflinePrinterDialog();
                return;
            }
            if (activePageIndex == 1) {
                printIdCardDirect();
            } else {
                startPrinting();
            }
        });

        if (layoutStatusCapsule != null) {
            layoutStatusCapsule.setOnClickListener(v -> {
                SoundManager.playClick();
                DiagnosticLogManager.showDiagnosticDialog(this, getCurrentPrinterCutawayState());
            });
        }

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

        // Category Section Switch Tabs (Mutually exclusive sections: only selected section shown)
        if (btnTabRecents != null) btnTabRecents.setOnClickListener(v -> showControlSection(ControlSection.RECENTS));
        if (btnTabTone != null) btnTabTone.setOnClickListener(v -> showControlSection(ControlSection.TONE));
        if (btnTabGeometry != null) btnTabGeometry.setOnClickListener(v -> showControlSection(ControlSection.GEOMETRY));
        if (btnTabMargins != null) btnTabMargins.setOnClickListener(v -> showControlSection(ControlSection.MARGINS));
        if (btnTabPages != null) btnTabPages.setOnClickListener(v -> showControlSection(ControlSection.PAGES));
        if (btnTabWatermark != null) btnTabWatermark.setOnClickListener(v -> showControlSection(ControlSection.WATERMARK));

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

        // Sheet Orientation Listeners
        if (btnQuickOrient != null) {
            btnQuickOrient.setOnClickListener(v -> {
                SoundManager.playClick();
                toggleOrientation();
            });
        }
        if (btnOrientPortrait != null) {
            btnOrientPortrait.setOnClickListener(v -> {
                SoundManager.playClick();
                setOrientation(SheetGeometry.Orientation.PORTRAIT);
            });
        }
        if (btnOrientLandscape != null) {
            btnOrientLandscape.setOnClickListener(v -> {
                SoundManager.playClick();
                setOrientation(SheetGeometry.Orientation.LANDSCAPE);
            });
        }

        // Print Margin Precision Listeners
        seekMargin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentMarginMm = (float) progress;
                    updateMarginDisplay(currentMarginMm);
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

        if (btnMarginMinus != null) {
            btnMarginMinus.setOnClickListener(v -> {
                SoundManager.playClick();
                setMarginPreset(Math.max(0f, currentMarginMm - 1.0f));
            });
        }
        if (btnMarginPlus != null) {
            btnMarginPlus.setOnClickListener(v -> {
                SoundManager.playClick();
                setMarginPreset(Math.min(40f, currentMarginMm + 1.0f));
            });
        }

        btnMarginZero.setOnClickListener(v -> setMarginPreset(0f));
        if (btnMarginHwMin != null) {
            btnMarginHwMin.setOnClickListener(v -> setMarginPreset(SheetGeometry.HARDWARE_MIN_MARGIN_MM));
        }
        btnMarginNarrow.setOnClickListener(v -> setMarginPreset(8f));
        btnMarginNormal.setOnClickListener(v -> setMarginPreset(12f));
        btnMarginWide.setOnClickListener(v -> setMarginPreset(20f));

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

        if (btnStageExpand != null) {
            btnStageExpand.setOnClickListener(v -> toggleOptionsPanel());
        }

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
                updatePresetChipHighlight(progress);
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

        if (btnPrintNow != null) {
            btnPrintNow.setOnClickListener(v -> startPrinting());
        }

        // ID Card Copier Category Fast-Switch Tabs
        if (btnIdTabPhotos != null) btnIdTabPhotos.setOnClickListener(v -> showIdControlSection(IdControlSection.PHOTOS));
        if (btnIdTabLayout != null) btnIdTabLayout.setOnClickListener(v -> showIdControlSection(IdControlSection.LAYOUT));
        if (btnIdTabPaper != null) btnIdTabPaper.setOnClickListener(v -> showIdControlSection(IdControlSection.PAPER));
        if (btnIdTabMargins != null) btnIdTabMargins.setOnClickListener(v -> showIdControlSection(IdControlSection.MARGINS));
        if (btnIdTabTone != null) btnIdTabTone.setOnClickListener(v -> showIdControlSection(IdControlSection.TONE));
        if (btnIdTabWatermark != null) btnIdTabWatermark.setOnClickListener(v -> showIdControlSection(IdControlSection.WATERMARK));
        if (btnIdTabCopies != null) btnIdTabCopies.setOnClickListener(v -> showIdControlSection(IdControlSection.COPIES));

        // Panel toggle & Stage expand
        if (btnIdTogglePanel != null) btnIdTogglePanel.setOnClickListener(v -> toggleIdOptionsPanel());
        if (tvIdPanelHandle != null) tvIdPanelHandle.setOnClickListener(v -> toggleIdOptionsPanel());
        if (btnIdStageExpand != null) btnIdStageExpand.setOnClickListener(v -> toggleIdOptionsPanel());

        // Header & Quick Preview Actions
        if (btnIdClearPhotos != null) btnIdClearPhotos.setOnClickListener(v -> clearIdPhotos());
        if (btnIdQuickPrint != null) btnIdQuickPrint.setOnClickListener(v -> printIdCardDirect());
        if (btnIdQuickOrient != null) btnIdQuickOrient.setOnClickListener(v -> toggleIdOrientation());
        if (btnIdQuickGuides != null) btnIdQuickGuides.setOnClickListener(v -> toggleIdCutGuides());

        // ID Count & Placements
        if (btnIdCount1 != null) btnIdCount1.setOnClickListener(v -> updateIdCount(1));
        if (btnIdCount2 != null) btnIdCount2.setOnClickListener(v -> updateIdCount(2));
        if (btnIdPlaceTop != null) btnIdPlaceTop.setOnClickListener(v -> updateIdPlacement(IdCardEngine.IdPlacement.TOP_HALF));
        if (btnIdPlaceCenter != null) btnIdPlaceCenter.setOnClickListener(v -> updateIdPlacement(IdCardEngine.IdPlacement.CENTERED));
        if (btnIdPlaceFull != null) btnIdPlaceFull.setOnClickListener(v -> updateIdPlacement(IdCardEngine.IdPlacement.TOP_AND_BOTTOM));
        if (btnIdPlaceGrid != null) btnIdPlaceGrid.setOnClickListener(v -> updateIdPlacement(IdCardEngine.IdPlacement.FOUR_UP));
        if (btnIdToggleCutGuides != null) btnIdToggleCutGuides.setOnClickListener(v -> toggleIdCutGuides());

        // Spacing Slider & Preset Chips
        if (seekIdSpacing != null) {
            seekIdSpacing.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    updateIdSpacing(progress);
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    seekBar.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                }
            });
        }
        if (btnIdSpace5 != null) btnIdSpace5.setOnClickListener(v -> updateIdSpacing(5));
        if (btnIdSpace10 != null) btnIdSpace10.setOnClickListener(v -> updateIdSpacing(10));
        if (btnIdSpace20 != null) btnIdSpace20.setOnClickListener(v -> updateIdSpacing(20));
        if (btnIdSpace30 != null) btnIdSpace30.setOnClickListener(v -> updateIdSpacing(30));
        if (btnIdSpace40 != null) btnIdSpace40.setOnClickListener(v -> updateIdSpacing(40));

        // Scale Slider & Preset Chips
        if (seekIdScale != null) {
            seekIdScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float scale = (70 + progress) / 100.0f;
                    updateIdScale(scale);
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    seekBar.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                }
            });
        }
        if (btnIdScale85 != null) btnIdScale85.setOnClickListener(v -> updateIdScale(0.85f));
        if (btnIdScale100 != null) btnIdScale100.setOnClickListener(v -> updateIdScale(1.0f));
        if (btnIdScale115 != null) btnIdScale115.setOnClickListener(v -> updateIdScale(1.15f));
        if (btnIdScale130 != null) btnIdScale130.setOnClickListener(v -> updateIdScale(1.30f));

        // Paper Sizing & Orientation
        if (btnIdPaperA4 != null) btnIdPaperA4.setOnClickListener(v -> updateIdPaperSize(SheetGeometry.PaperSize.A4));
        if (btnIdPaperLetter != null) btnIdPaperLetter.setOnClickListener(v -> updateIdPaperSize(SheetGeometry.PaperSize.LETTER));
        if (btnIdPaperLegal != null) btnIdPaperLegal.setOnClickListener(v -> updateIdPaperSize(SheetGeometry.PaperSize.LEGAL));
        if (btnIdPaperA5 != null) btnIdPaperA5.setOnClickListener(v -> updateIdPaperSize(SheetGeometry.PaperSize.A5));
        if (btnIdOrientPortrait != null) btnIdOrientPortrait.setOnClickListener(v -> updateIdOrientation(SheetGeometry.Orientation.PORTRAIT));
        if (btnIdOrientLandscape != null) btnIdOrientLandscape.setOnClickListener(v -> updateIdOrientation(SheetGeometry.Orientation.LANDSCAPE));

        // Print Margins Slider & Presets
        if (seekIdMargin != null) {
            seekIdMargin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    updateIdMargin(progress);
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    seekBar.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                }
            });
        }
        if (btnIdMarginZero != null) btnIdMarginZero.setOnClickListener(v -> updateIdMargin(0f));
        if (btnIdMarginHwMin != null) btnIdMarginHwMin.setOnClickListener(v -> updateIdMargin(SheetGeometry.HARDWARE_MIN_MARGIN_MM));
        if (btnIdMarginNarrow != null) btnIdMarginNarrow.setOnClickListener(v -> updateIdMargin(8f));
        if (btnIdMarginNormal != null) btnIdMarginNormal.setOnClickListener(v -> updateIdMargin(12f));
        if (btnIdMarginWide != null) btnIdMarginWide.setOnClickListener(v -> updateIdMargin(20f));

        // Toner Density Slider & Presets
        if (seekIdDarkness != null) {
            seekIdDarkness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    updateIdDarkness(progress);
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    seekBar.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                }
            });
        }
        if (btnIdToneDraft != null) btnIdToneDraft.setOnClickListener(v -> updateIdDarkness(ToneEngine.PRESET_DRAFT));
        if (btnIdToneNormal != null) btnIdToneNormal.setOnClickListener(v -> updateIdDarkness(ToneEngine.PRESET_NORMAL));
        if (btnIdToneDeep != null) btnIdToneDeep.setOnClickListener(v -> updateIdDarkness(ToneEngine.PRESET_DEEP));
        if (btnIdToneUltra != null) btnIdToneUltra.setOnClickListener(v -> updateIdDarkness(ToneEngine.PRESET_ULTRA));

        // Watermark Presets & Live Text Watcher
        if (btnIdWmConfidential != null) btnIdWmConfidential.setOnClickListener(v -> updateIdWatermark("CONFIDENTIAL"));
        if (btnIdWmCopy != null) btnIdWmCopy.setOnClickListener(v -> updateIdWatermark("COPY ONLY"));
        if (btnIdWmOfficial != null) btnIdWmOfficial.setOnClickListener(v -> updateIdWatermark("OFFICIAL"));
        if (btnIdWmClear != null) btnIdWmClear.setOnClickListener(v -> updateIdWatermark(""));
        if (etIdWatermark != null) {
            etIdWatermark.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    idConfig.watermarkText = s.toString();
                    scheduleIdPreviewRender();
                }
            });
        }

        // Copies Controls
        if (btnIdCopiesMinus != null) btnIdCopiesMinus.setOnClickListener(v -> {
            if (idConfig.copies > 1) updateIdCopies(idConfig.copies - 1);
        });
        if (btnIdCopiesPlus != null) btnIdCopiesPlus.setOnClickListener(v -> {
            if (idConfig.copies < 99) updateIdCopies(idConfig.copies + 1);
        });
        if (btnIdCopies1 != null) btnIdCopies1.setOnClickListener(v -> updateIdCopies(1));
        if (btnIdCopies2 != null) btnIdCopies2.setOnClickListener(v -> updateIdCopies(2));
        if (btnIdCopies5 != null) btnIdCopies5.setOnClickListener(v -> updateIdCopies(5));
        if (btnIdCopies10 != null) btnIdCopies10.setOnClickListener(v -> updateIdCopies(10));

        // ID #1 Scan & Pick
        if (btnIdScanFront != null) btnIdScanFront.setOnClickListener(v -> launchCameraScanner(false, REQUEST_SCAN_ID_FRONT));
        if (btnIdPickFront != null) btnIdPickFront.setOnClickListener(v -> pickImage(REQUEST_PICK_ID_FRONT));
        if (btnIdScanBack != null) btnIdScanBack.setOnClickListener(v -> launchCameraScanner(false, REQUEST_SCAN_ID_BACK));
        if (btnIdPickBack != null) btnIdPickBack.setOnClickListener(v -> pickImage(REQUEST_PICK_ID_BACK));

        // ID #2 Scan & Pick
        if (btnId2ScanFront != null) btnId2ScanFront.setOnClickListener(v -> launchCameraScanner(false, REQUEST_SCAN_ID2_FRONT));
        if (btnId2PickFront != null) btnId2PickFront.setOnClickListener(v -> pickImage(REQUEST_PICK_ID2_FRONT));
        if (btnId2ScanBack != null) btnId2ScanBack.setOnClickListener(v -> launchCameraScanner(false, REQUEST_SCAN_ID2_BACK));
        if (btnId2PickBack != null) btnId2PickBack.setOnClickListener(v -> pickImage(REQUEST_PICK_ID2_BACK));

        // ID Actions
        if (btnIdPrintNow != null) btnIdPrintNow.setOnClickListener(v -> printIdCardDirect());
        if (btnIdSendStudio != null) btnIdSendStudio.setOnClickListener(v -> sendIdCardToStudio());

        // Spool Queue Listeners
        if (btnSpoolClearAll != null) {
            btnSpoolClearAll.setOnClickListener(v -> {
                SpoolManager.clearQueue(this);
                renderSpoolQueue();
            });
        }

        // Support & Developer Page
        if (btnStarGithub != null) {
            btnStarGithub.setOnClickListener(v -> {
                SoundManager.playClick();
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/killindodo/DodoPress"));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not open browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnOpenTelegram != null) {
            btnOpenTelegram.setOnClickListener(v -> {
                SoundManager.playClick();
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/killindod0"));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not open Telegram: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnCopyTelegram != null) {
            btnCopyTelegram.setOnClickListener(v -> {
                SoundManager.playClick();
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Telegram Handle", "@killindod0");
                if (cm != null) cm.setPrimaryClip(clip);
                Toast.makeText(this, "Telegram handle @killindod0 copied! Send your feature requests directly.", Toast.LENGTH_LONG).show();
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

        if (btnFeedbackRequest != null) {
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
        }

        // Universal Driver Hub Listeners
        if (btnDevBrowseCatalog != null) {
            btnDevBrowseCatalog.setOnClickListener(v -> {
                SoundManager.playClick();
                showPrinterCatalogDialog();
            });
        }

        if (btnDevScanUsb != null) {
            btnDevScanUsb.setOnClickListener(v -> {
                SoundManager.playClick();
                autoDetectUsbPrinter();
            });
        }

        // Sound Effects Toggle
        if (cbSoundEffects != null) {
            cbSoundEffects.setChecked(SoundManager.isSoundEnabled());
            cbSoundEffects.setOnCheckedChangeListener((btn, isChecked) -> {
                SoundManager.setSoundEnabled(this, isChecked);
                if (isChecked) SoundManager.playClick();
            });
        }

        // Secret Easter Egg: Tap avatar 7 times
        if (frameDevAvatar != null) {
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
        }

        // Universal Tactile Spring Micro-Interactions
        TouchFeedback.applyAll(
            btnPickFile, btnScanDocument, btnQrLabel, btnAiAssistant, btnPrintNow, dockBtnPrint, btnDiagnostics,
            btnTogglePreviewMode, btnQuickZoom, btnRotatePage, btnPrevPage, btnNextPage,
            btnPresetDraft, btnPresetNormal, btnPresetDeep, btnPresetUltra,
            btnCopiesMinus, btnCopiesPlus, btnQueueJob,
            btnThemeMenu, btnTogglePanel, btnTabRecents, btnTabTone, btnTabGeometry, btnTabMargins, btnTabPages, btnTabWatermark,
            btnWmConfidential, btnWmDraft, btnWmPaid, btnWmOriginal, btnWmClear,
            btnPaperA4, btnPaperLetter, btnPaperLegal, btnPaperA5,
            btnTile1Up, btnTile2Up, btnTile4Up,
            btnPlaceFull, btnPlaceTopHalf, btnPlaceBottomHalf, btnPlaceLeftHalf, btnPlaceRightHalf, btnPlaceQuadTl,
            btnMarginZero, btnMarginNarrow, btnMarginNormal, btnMarginWide,
            btnIdClearPhotos, btnIdQuickPrint, btnIdQuickOrient, btnIdQuickGuides, btnIdStageExpand, btnIdTogglePanel,
            btnIdTabPhotos, btnIdTabLayout, btnIdTabPaper, btnIdTabMargins, btnIdTabTone, btnIdTabWatermark, btnIdTabCopies,
            btnIdCount1, btnIdCount2, btnIdPlaceTop, btnIdPlaceCenter, btnIdPlaceFull, btnIdPlaceGrid,
            btnIdSpace5, btnIdSpace10, btnIdSpace20, btnIdSpace30, btnIdSpace40,
            btnIdScale85, btnIdScale100, btnIdScale115, btnIdScale130,
            btnIdPaperA4, btnIdPaperLetter, btnIdPaperLegal, btnIdPaperA5, btnIdOrientPortrait, btnIdOrientLandscape,
            btnIdMarginZero, btnIdMarginHwMin, btnIdMarginNarrow, btnIdMarginNormal, btnIdMarginWide,
            btnIdToneDraft, btnIdToneNormal, btnIdToneDeep, btnIdToneUltra,
            btnIdWmConfidential, btnIdWmCopy, btnIdWmOfficial, btnIdWmClear,
            btnIdCopiesMinus, btnIdCopiesPlus, btnIdCopies1, btnIdCopies2, btnIdCopies5, btnIdCopies10,
            btnIdScanFront, btnIdPickFront, btnIdScanBack, btnIdPickBack,
            btnId2ScanFront, btnId2PickFront, btnId2ScanBack, btnId2PickBack,
            btnIdPrintNow, btnIdSendStudio, btnStarGithub, btnOpenTelegram, btnCopyTelegram,
            btnDevBrowseCatalog, btnDevScanUsb
        );
    }

    private void toggleIdOptionsPanel() {
        SoundManager.playClick();
        if (scrollIdOptions == null || btnIdTogglePanel == null) return;
        isIdPanelCollapsed = !isIdPanelCollapsed;
        float density = getResources().getDisplayMetrics().density;
        if (isIdPanelCollapsed) {
            scrollIdOptions.setVisibility(View.GONE);
            btnIdTogglePanel.setText("▲ EXPAND");
            if (btnIdStageExpand != null) btnIdStageExpand.setText("⛶ Fit");
            if (layoutIdSlidingPanel != null) {
                LinearLayout.LayoutParams panelLp = (LinearLayout.LayoutParams) layoutIdSlidingPanel.getLayoutParams();
                panelLp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                panelLp.weight = 0f;
                layoutIdSlidingPanel.setLayoutParams(panelLp);
            }
            if (cardIdSheetPreview != null) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) cardIdSheetPreview.getLayoutParams();
                lp.height = 0;
                lp.weight = 1.0f;
                cardIdSheetPreview.setLayoutParams(lp);
            }
        } else {
            scrollIdOptions.setVisibility(View.VISIBLE);
            btnIdTogglePanel.setText("▼ RETRACT");
            if (btnIdStageExpand != null) btnIdStageExpand.setText("⛶ Max");
            if (cardIdSheetPreview != null) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) cardIdSheetPreview.getLayoutParams();
                lp.height = (int) (235 * density);
                lp.weight = 0f;
                cardIdSheetPreview.setLayoutParams(lp);
            }
            if (layoutIdSlidingPanel != null) {
                LinearLayout.LayoutParams panelLp = (LinearLayout.LayoutParams) layoutIdSlidingPanel.getLayoutParams();
                panelLp.height = 0;
                panelLp.weight = 1.0f;
                layoutIdSlidingPanel.setLayoutParams(panelLp);
            }
        }
    }

    private void showIdControlSection(IdControlSection section) {
        SoundManager.playClick();
        currentIdControlSection = section;

        if (isIdPanelCollapsed) {
            toggleIdOptionsPanel();
        }

        if (cardIdPhotos != null) cardIdPhotos.setVisibility(section == IdControlSection.PHOTOS ? View.VISIBLE : View.GONE);
        if (cardIdLayout != null) cardIdLayout.setVisibility(section == IdControlSection.LAYOUT ? View.VISIBLE : View.GONE);
        if (cardIdPaper != null) cardIdPaper.setVisibility(section == IdControlSection.PAPER ? View.VISIBLE : View.GONE);
        if (cardIdMargins != null) cardIdMargins.setVisibility(section == IdControlSection.MARGINS ? View.VISIBLE : View.GONE);
        if (cardIdTone != null) cardIdTone.setVisibility(section == IdControlSection.TONE ? View.VISIBLE : View.GONE);
        if (cardIdWatermark != null) cardIdWatermark.setVisibility(section == IdControlSection.WATERMARK ? View.VISIBLE : View.GONE);
        if (cardIdCopies != null) cardIdCopies.setVisibility(section == IdControlSection.COPIES ? View.VISIBLE : View.GONE);

        if (scrollIdOptions != null) {
            scrollIdOptions.scrollTo(0, 0);
        }

        if (tvIdPanelHandle != null) {
            String title;
            switch (section) {
                case PHOTOS: title = "CARD PHOTOS & SCANS"; break;
                case LAYOUT: title = "LAYOUT, SPACING & SCALE"; break;
                case PAPER: title = "PAPER SIZE & ORIENTATION"; break;
                case MARGINS: title = "PRINT MARGINS (0-24mm)"; break;
                case TONE: title = "TONER DENSITY & HALFTONE"; break;
                case WATERMARK: title = "WATERMARK STAMP"; break;
                case COPIES: title = "COPIES COUNT"; break;
                default: title = "ID SETTINGS"; break;
            }
            tvIdPanelHandle.setText("── 🪪 " + title + " ──");
        }

        updateIdTabHighlight(section);
    }

    private void updateIdTabHighlight(IdControlSection section) {
        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);
        Button[] tabs = {btnIdTabPhotos, btnIdTabLayout, btnIdTabPaper, btnIdTabMargins, btnIdTabTone, btnIdTabWatermark, btnIdTabCopies};
        IdControlSection[] sections = {
            IdControlSection.PHOTOS,
            IdControlSection.LAYOUT,
            IdControlSection.PAPER,
            IdControlSection.MARGINS,
            IdControlSection.TONE,
            IdControlSection.WATERMARK,
            IdControlSection.COPIES
        };

        Button activeTab = null;
        for (int i = 0; i < tabs.length; i++) {
            Button b = tabs[i];
            if (b == null) continue;
            boolean isActive = (sections[i] == section);
            b.setBackground(ThemeManager.createChipButtonDrawable(p, isActive, this));
            b.setTextColor(isActive ? p.primaryButtonTextColor : p.chipTextColor);
            b.setTypeface(null, isActive ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            if (isActive) activeTab = b;
        }

        if (activeTab != null && scrollIdCategoryTabs != null) {
            final Button target = activeTab;
            scrollIdCategoryTabs.post(() -> {
                int scrollX = (target.getLeft() - (scrollIdCategoryTabs.getWidth() / 2)) + (target.getWidth() / 2);
                scrollIdCategoryTabs.smoothScrollTo(Math.max(0, scrollX), 0);
            });
        }
    }

    private void updateIdControlsHighlight() {
        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);

        if (btnIdCount1 != null) {
            boolean active = (idConfig.idCount == 1);
            btnIdCount1.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnIdCount1.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnIdCount2 != null) {
            boolean active = (idConfig.idCount == 2);
            btnIdCount2.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnIdCount2.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }

        if (btnIdPlaceTop != null) {
            boolean active = (idConfig.placement == IdCardEngine.IdPlacement.TOP_HALF);
            btnIdPlaceTop.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnIdPlaceTop.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnIdPlaceCenter != null) {
            boolean active = (idConfig.placement == IdCardEngine.IdPlacement.CENTERED);
            btnIdPlaceCenter.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnIdPlaceCenter.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnIdPlaceFull != null) {
            boolean active = (idConfig.placement == IdCardEngine.IdPlacement.TOP_AND_BOTTOM);
            btnIdPlaceFull.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnIdPlaceFull.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnIdPlaceGrid != null) {
            boolean active = (idConfig.placement == IdCardEngine.IdPlacement.FOUR_UP);
            btnIdPlaceGrid.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnIdPlaceGrid.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }

        Button[] spaceChips = {btnIdSpace5, btnIdSpace10, btnIdSpace20, btnIdSpace30, btnIdSpace40};
        int[] spaceVals = {5, 10, 20, 30, 40};
        for (int i = 0; i < spaceChips.length; i++) {
            if (spaceChips[i] != null) {
                boolean active = (idConfig.spacingMm == spaceVals[i]);
                spaceChips[i].setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
                spaceChips[i].setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
            }
        }

        if (btnIdScale85 != null) {
            boolean active = Math.abs(idConfig.scale - 0.85f) < 0.02f;
            btnIdScale85.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnIdScale85.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnIdScale100 != null) {
            boolean active = Math.abs(idConfig.scale - 1.0f) < 0.02f;
            btnIdScale100.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnIdScale100.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnIdScale115 != null) {
            boolean active = Math.abs(idConfig.scale - 1.15f) < 0.02f;
            btnIdScale115.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnIdScale115.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnIdScale130 != null) {
            boolean active = Math.abs(idConfig.scale - 1.30f) < 0.02f;
            btnIdScale130.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnIdScale130.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }

        if (btnIdPaperA4 != null) {
            boolean active = (idConfig.paperSize == SheetGeometry.PaperSize.A4);
            btnIdPaperA4.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnIdPaperA4.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnIdPaperLetter != null) {
            boolean active = (idConfig.paperSize == SheetGeometry.PaperSize.LETTER);
            btnIdPaperLetter.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnIdPaperLetter.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnIdPaperLegal != null) {
            boolean active = (idConfig.paperSize == SheetGeometry.PaperSize.LEGAL);
            btnIdPaperLegal.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnIdPaperLegal.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnIdPaperA5 != null) {
            boolean active = (idConfig.paperSize == SheetGeometry.PaperSize.A5);
            btnIdPaperA5.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnIdPaperA5.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }

        if (btnIdOrientPortrait != null) {
            boolean active = (idConfig.orientation == SheetGeometry.Orientation.PORTRAIT);
            btnIdOrientPortrait.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnIdOrientPortrait.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnIdOrientLandscape != null) {
            boolean active = (idConfig.orientation == SheetGeometry.Orientation.LANDSCAPE);
            btnIdOrientLandscape.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnIdOrientLandscape.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }

        Button[] marginChips = {btnIdMarginZero, btnIdMarginHwMin, btnIdMarginNarrow, btnIdMarginNormal, btnIdMarginWide};
        float[] marginVals = {0f, SheetGeometry.HARDWARE_MIN_MARGIN_MM, 8f, 12f, 20f};
        for (int i = 0; i < marginChips.length; i++) {
            if (marginChips[i] != null) {
                boolean active = Math.abs(idConfig.marginMm - marginVals[i]) < 0.2f;
                marginChips[i].setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
                marginChips[i].setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
            }
        }

        Button[] toneChips = {btnIdToneDraft, btnIdToneNormal, btnIdToneDeep, btnIdToneUltra};
        int[] toneVals = {ToneEngine.PRESET_DRAFT, ToneEngine.PRESET_NORMAL, ToneEngine.PRESET_DEEP, ToneEngine.PRESET_ULTRA};
        for (int i = 0; i < toneChips.length; i++) {
            if (toneChips[i] != null) {
                boolean active = (idConfig.darkness == toneVals[i]);
                toneChips[i].setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
                toneChips[i].setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
            }
        }

        Button[] copyChips = {btnIdCopies1, btnIdCopies2, btnIdCopies5, btnIdCopies10};
        int[] copyVals = {1, 2, 5, 10};
        for (int i = 0; i < copyChips.length; i++) {
            if (copyChips[i] != null) {
                boolean active = (idConfig.copies == copyVals[i]);
                copyChips[i].setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
                copyChips[i].setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
            }
        }
    }

    private void updateIdCount(int count) {
        SoundManager.playClick();
        idConfig.idCount = count;
        if (layoutId2Container != null) {
            layoutId2Container.setVisibility(count == 2 ? View.VISIBLE : View.GONE);
        }
        updateIdControlsHighlight();
        scheduleIdPreviewRender();
    }

    private void updateIdSpacing(int spacingMm) {
        idConfig.spacingMm = spacingMm;
        if (tvIdSpacingVal != null) {
            tvIdSpacingVal.setText(spacingMm + " mm");
        }
        if (seekIdSpacing != null && seekIdSpacing.getProgress() != spacingMm) {
            seekIdSpacing.setProgress(spacingMm);
        }
        updateIdControlsHighlight();
        scheduleIdPreviewRender();
    }

    private void updateIdScale(float scale) {
        idConfig.scale = scale;
        int pct = Math.round(scale * 100);
        if (tvIdScaleVal != null) {
            tvIdScaleVal.setText(pct + "%" + (pct == 100 ? " (Std 85.6×54mm)" : ""));
        }
        int progress = Math.max(0, Math.min(80, pct - 70));
        if (seekIdScale != null && seekIdScale.getProgress() != progress) {
            seekIdScale.setProgress(progress);
        }
        updateIdControlsHighlight();
        scheduleIdPreviewRender();
    }

    private void updateIdPlacement(IdCardEngine.IdPlacement placement) {
        SoundManager.playClick();
        idConfig.placement = placement;
        updateIdControlsHighlight();
        scheduleIdPreviewRender();
    }

    private void updateIdPaperSize(SheetGeometry.PaperSize size) {
        SoundManager.playClick();
        idConfig.paperSize = size;
        updateIdControlsHighlight();
        scheduleIdPreviewRender();
    }

    private void updateIdOrientation(SheetGeometry.Orientation orient) {
        SoundManager.playClick();
        idConfig.orientation = orient;
        if (btnIdQuickOrient != null) {
            btnIdQuickOrient.setText(orient == SheetGeometry.Orientation.PORTRAIT ? "PORTRAIT" : "LANDSCAPE");
        }
        updateIdControlsHighlight();
        scheduleIdPreviewRender();
    }

    private void toggleIdOrientation() {
        SheetGeometry.Orientation newOrient = (idConfig.orientation == SheetGeometry.Orientation.PORTRAIT)
            ? SheetGeometry.Orientation.LANDSCAPE
            : SheetGeometry.Orientation.PORTRAIT;
        updateIdOrientation(newOrient);
    }

    private void toggleIdCutGuides() {
        SoundManager.playClick();
        idConfig.showCutGuides = !idConfig.showCutGuides;
        if (btnIdQuickGuides != null) {
            btnIdQuickGuides.setText(idConfig.showCutGuides ? "✂ Guides: ON" : "✂ Guides: OFF");
        }
        if (btnIdToggleCutGuides != null) {
            btnIdToggleCutGuides.setText(idConfig.showCutGuides ? "✂ Cut Guides: ON" : "✂ Cut Guides: OFF");
        }
        scheduleIdPreviewRender();
    }

    private void updateIdMargin(float marginMm) {
        idConfig.marginMm = marginMm;
        if (tvIdMarginVal != null) {
            tvIdMarginVal.setText(String.format(Locale.US, "%.1f mm", marginMm) + (marginMm == 0 ? " (Edge)" : (marginMm <= 4.5f ? " (Drum Clip)" : "")));
        }
        if (layoutIdDrumWarning != null) {
            layoutIdDrumWarning.setVisibility(marginMm < SheetGeometry.HARDWARE_MIN_MARGIN_MM ? View.VISIBLE : View.GONE);
        }
        if (seekIdMargin != null && Math.round(marginMm) != seekIdMargin.getProgress()) {
            seekIdMargin.setProgress(Math.round(marginMm));
        }
        updateIdControlsHighlight();
        scheduleIdPreviewRender();
    }

    private void updateIdDarkness(int darkness) {
        idConfig.darkness = darkness;
        if (tvIdToneVal != null) {
            String label = (darkness <= 25) ? "Draft 25%" : (darkness <= 50) ? "Normal 50%" : (darkness <= 75) ? "Deep 75%" : "Ultra 100%";
            tvIdToneVal.setText(darkness + "% (" + label + ")");
        }
        if (seekIdDarkness != null && seekIdDarkness.getProgress() != darkness) {
            seekIdDarkness.setProgress(darkness);
        }
        updateIdControlsHighlight();
        scheduleIdPreviewRender();
    }

    private void updateIdWatermark(String text) {
        SoundManager.playClick();
        idConfig.watermarkText = text;
        if (etIdWatermark != null && !etIdWatermark.getText().toString().equals(text)) {
            etIdWatermark.setText(text);
        }
        scheduleIdPreviewRender();
    }

    private void updateIdCopies(int copies) {
        SoundManager.playClick();
        idConfig.copies = Math.max(1, Math.min(99, copies));
        if (tvIdCopiesCount != null) {
            tvIdCopiesCount.setText(String.valueOf(idConfig.copies));
        }
        updateIdControlsHighlight();
        updateIdCardSummary();
    }

    private void clearIdPhotos() {
        SoundManager.playClick();
        if (idFrontBitmap != null && !idFrontBitmap.isRecycled()) { idFrontBitmap.recycle(); idFrontBitmap = null; }
        if (idBackBitmap != null && !idBackBitmap.isRecycled()) { idBackBitmap.recycle(); idBackBitmap = null; }
        if (id2FrontBitmap != null && !id2FrontBitmap.isRecycled()) { id2FrontBitmap.recycle(); id2FrontBitmap = null; }
        if (id2BackBitmap != null && !id2BackBitmap.isRecycled()) { id2BackBitmap.recycle(); id2BackBitmap = null; }
        if (ivIdFrontPreview != null) ivIdFrontPreview.setImageDrawable(null);
        if (ivIdBackPreview != null) ivIdBackPreview.setImageDrawable(null);
        if (ivId2FrontPreview != null) ivId2FrontPreview.setImageDrawable(null);
        if (ivId2BackPreview != null) ivId2BackPreview.setImageDrawable(null);
        scheduleIdPreviewRender();
        Toast.makeText(this, "ID Card photos cleared.", Toast.LENGTH_SHORT).show();
    }

    private void updateIdCardSummary() {
        if (activePageIndex != 1) return;
        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);
        if (tvPageSummary != null) {
            tvPageSummary.setTextColor(p.brandPrimaryColor);
            String summary = "🪪 ID Sheet • " + idConfig.paperSize.name() + " " + idConfig.orientation.title
                + " • " + idConfig.idCount + " ID" + (idConfig.idCount > 1 ? "s" : "")
                + " • " + idConfig.copies + (idConfig.copies > 1 ? " copies" : " copy");
            tvPageSummary.setText(summary);
        }
        if (dockTvStatus != null) {
            UsbDevice printer = usbPrinterConnection.findPrinter();
            if (printer != null) {
                dockTvStatus.setText(usbPrinterConnection.getPrinterDisplayName(printer) + " (Ready)");
            } else {
                PrinterModel active = (driverManager != null) ? driverManager.getActivePrinter() : null;
                dockTvStatus.setText(active != null ? active.getDisplayName() : "Universal Printer");
            }
        }
        if (dockBtnPrint != null) {
            dockBtnPrint.setText("⚡ PRINT ID SHEET");
        }
    }

    private final Runnable idPreviewRunnable = this::renderIdCardPreviewSync;

    private void scheduleIdPreviewRender() {
        mainHandler.removeCallbacks(idPreviewRunnable);
        mainHandler.postDelayed(idPreviewRunnable, 60);
    }

    private void renderIdCardPreview() {
        scheduleIdPreviewRender();
    }

    private void renderIdCardPreviewSync() {
        if (ivIdSheetPreview == null) return;
        new Thread(() -> {
            try {
                float widthMm = idConfig.paperSize.getWidthMm(idConfig.orientation);
                float heightMm = idConfig.paperSize.getHeightMm(idConfig.orientation);
                float aspectRatio = widthMm / Math.max(1f, heightMm);

                int previewW = 600;
                int previewH = Math.round(previewW / aspectRatio);

                Bitmap previewBm = IdCardEngine.composeIdCardSheet(
                    idFrontBitmap, idBackBitmap,
                    id2FrontBitmap, id2BackBitmap,
                    idConfig, previewW, previewH,
                    widthMm, heightMm
                );

                runOnUiThread(() -> {
                    ivIdSheetPreview.setImageBitmap(previewBm);
                    if (tvIdPreviewInfo != null) {
                        String info = idConfig.paperSize.name() + " " + idConfig.orientation.title
                            + " • " + idConfig.idCount + " ID" + (idConfig.idCount > 1 ? "s" : "")
                            + " • " + idConfig.spacingMm + "mm Gap • "
                            + Math.round(idConfig.scale * 100) + "% Scale • "
                            + String.format(Locale.US, "%.1fmm Margin", idConfig.marginMm);
                        tvIdPreviewInfo.setText(info);
                    }
                    updateIdCardSummary();
                });
            } catch (Exception e) {
                DiagnosticLogManager.log("ID_CARD", "Preview error: " + e.getMessage());
            }
        }).start();
    }

    private void pickImage(int requestCode) {
        SoundManager.playClick();
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, requestCode);
    }

    private void printIdCardDirect() {
        SoundManager.playClick();
        if (idFrontBitmap == null && idBackBitmap == null && id2FrontBitmap == null && id2BackBitmap == null) {
            Toast.makeText(this, "Please scan or select at least one ID card photo first.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            float widthMm = idConfig.paperSize.getWidthMm(idConfig.orientation);
            float heightMm = idConfig.paperSize.getHeightMm(idConfig.orientation);
            int sheetW = Math.round((widthMm / 25.4f) * 300f);
            int sheetH = Math.round((heightMm / 25.4f) * 300f);

            Bitmap composedSheet = IdCardEngine.composeIdCardSheet(
                idFrontBitmap, idBackBitmap,
                id2FrontBitmap, id2BackBitmap,
                idConfig, sheetW, sheetH,
                widthMm, heightMm
            );

            File idCardPdf = new File(getCacheDir(), "id_card_" + System.currentTimeMillis() + ".pdf");
            PdfDocument document = new PdfDocument();
            int pagePtW = Math.round(widthMm * 72f / 25.4f);
            int pagePtH = Math.round(heightMm * 72f / 25.4f);
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pagePtW, pagePtH, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Rect dest = new Rect(0, 0, pagePtW, pagePtH);
            canvas.drawBitmap(composedSheet, null, dest, null);
            document.finishPage(page);

            try (FileOutputStream fos = new FileOutputStream(idCardPdf)) {
                document.writeTo(fos);
            }
            document.close();
            composedSheet.recycle();

            // Sync main print state with ID configuration
            currentPaperSize = idConfig.paperSize;
            currentOrientation = idConfig.orientation;
            currentMarginMm = idConfig.marginMm;
            copies = idConfig.copies;
            if (seekDarkness != null) seekDarkness.setProgress(idConfig.darkness);

            showPage(0);
            loadPdfFromUri(Uri.fromFile(idCardPdf));

            UsbDevice printer = usbPrinterConnection.findPrinter();
            if (printer != null) {
                Toast.makeText(this, "ID Card ready! Starting direct print...", Toast.LENGTH_SHORT).show();
                mainHandler.postDelayed(this::startPrinting, 500);
            } else {
                Toast.makeText(this, "ID Card loaded! Connect USB printer to print.", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Failed to compose ID card: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendIdCardToStudio() {
        SoundManager.playClick();
        if (idFrontBitmap == null && idBackBitmap == null && id2FrontBitmap == null && id2BackBitmap == null) {
            Toast.makeText(this, "Please scan or select an ID card photo first.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            float widthMm = idConfig.paperSize.getWidthMm(idConfig.orientation);
            float heightMm = idConfig.paperSize.getHeightMm(idConfig.orientation);
            int sheetW = Math.round((widthMm / 25.4f) * 300f);
            int sheetH = Math.round((heightMm / 25.4f) * 300f);

            Bitmap composedSheet = IdCardEngine.composeIdCardSheet(
                idFrontBitmap, idBackBitmap,
                id2FrontBitmap, id2BackBitmap,
                idConfig, sheetW, sheetH,
                widthMm, heightMm
            );
            File idCardPdf = new File(getCacheDir(), "id_studio_" + System.currentTimeMillis() + ".pdf");
            PdfDocument document = new PdfDocument();
            int pagePtW = Math.round(widthMm * 72f / 25.4f);
            int pagePtH = Math.round(heightMm * 72f / 25.4f);
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pagePtW, pagePtH, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            page.getCanvas().drawBitmap(composedSheet, null, new Rect(0, 0, pagePtW, pagePtH), null);
            document.finishPage(page);
            try (FileOutputStream fos = new FileOutputStream(idCardPdf)) {
                document.writeTo(fos);
            }
            document.close();
            composedSheet.recycle();

            currentPaperSize = idConfig.paperSize;
            currentOrientation = idConfig.orientation;
            copies = idConfig.copies;

            showPage(0);
            loadPdfFromUri(Uri.fromFile(idCardPdf));
            Toast.makeText(this, "ID Sheet loaded into Print Studio for custom options!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send to studio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    if (job.darkness > 0) {
                        setDarkness(job.darkness);
                    }
                    if (job.copies > 0) {
                        copies = job.copies;
                        if (tvCopiesCount != null) tvCopiesCount.setText(String.valueOf(copies));
                    }
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
        updateMarginDisplay(currentMarginMm);
        updatePageSummary();
        renderPageThumb();
    }

    private void setOrientation(SheetGeometry.Orientation orientation) {
        if (orientation == null) return;
        this.currentOrientation = orientation;
        if (viewSheetGeometry != null) {
            viewSheetGeometry.setOrientation(orientation);
        }
        updateOrientationButtonsHighlight();
        updateGeometryBadge();
        updateMarginDisplay(currentMarginMm);
        updatePageSummary();
        renderPageThumb();
    }

    private void toggleOrientation() {
        setOrientation(currentOrientation == SheetGeometry.Orientation.PORTRAIT
                ? SheetGeometry.Orientation.LANDSCAPE
                : SheetGeometry.Orientation.PORTRAIT);
    }

    private void updateOrientationButtonsHighlight() {
        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);
        int activeBg = com.dodopress.R.drawable.btn_primary;
        int normalBg = com.dodopress.R.drawable.btn_chip;
        int activeText = Color.WHITE;
        int normalText = p.textMedColor;

        boolean isPortrait = (currentOrientation == SheetGeometry.Orientation.PORTRAIT);

        if (btnOrientPortrait != null) {
            btnOrientPortrait.setBackgroundResource(isPortrait ? activeBg : normalBg);
            btnOrientPortrait.setTextColor(isPortrait ? activeText : normalText);
        }
        if (btnOrientLandscape != null) {
            btnOrientLandscape.setBackgroundResource(!isPortrait ? activeBg : normalBg);
            btnOrientLandscape.setTextColor(!isPortrait ? activeText : normalText);
        }
        if (tvOrientLabel != null) {
            tvOrientLabel.setText(currentOrientation.title + (isPortrait ? " (Vertical)" : " (Horizontal)"));
        }
        if (btnQuickOrient != null) {
            btnQuickOrient.setText(isPortrait ? "PORTRAIT" : "LANDSCAPE");
        }
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
        tvGeometryBadge.setText(currentPaperSize.name() + " • " + currentOrientation.title + " • " + modeDesc);
    }

    private void updateTilingHighlight(SheetGeometry.TilingMode mode) {
        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);

        if (btnTile1Up != null) {
            boolean active = (mode == SheetGeometry.TilingMode.ONE_UP);
            btnTile1Up.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnTile1Up.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnTile2Up != null) {
            boolean active = (mode == SheetGeometry.TilingMode.TWO_UP);
            btnTile2Up.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnTile2Up.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnTile4Up != null) {
            boolean active = (mode == SheetGeometry.TilingMode.FOUR_UP);
            btnTile4Up.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnTile4Up.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
    }

    private void renderRecentsShelf() {
        List<RecentDocsManager.RecentItem> recents = RecentDocsManager.getRecents(this);
        int count = (recents != null) ? recents.size() : 0;
        if (btnTabRecents != null) {
            btnTabRecents.setText(count > 0 ? "📁 Recents (" + count + ")" : "📁 Recent Files");
        }

        if (cardRecents != null) {
            cardRecents.setVisibility(currentControlSection == ControlSection.RECENTS ? View.VISIBLE : View.GONE);
        }

        if (layoutRecentsContainer == null) return;
        layoutRecentsContainer.removeAllViews();

        if (recents == null || recents.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No recent documents yet. Opened PDF or image files will appear here.");
            emptyTv.setTextColor(Color.parseColor("#94A3B8"));
            emptyTv.setTextSize(11f);
            int p = (int) (12 * getResources().getDisplayMetrics().density);
            emptyTv.setPadding(p, p, p, p);
            layoutRecentsContainer.addView(emptyTv);
            return;
        }

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
                    showControlSection(ControlSection.TONE);
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
        float density = getResources().getDisplayMetrics().density;

        // 1. Root background
        if (rootMainContainer != null) {
            rootMainContainer.setBackgroundColor(p.bgColor);
        }

        // 2. System Bar & Edge-to-Edge Theme Integration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Window window = getWindow();
            if (window != null) {
                WindowInsetsController controller = window.getInsetsController();
                if (controller != null) {
                    int statusAppearance = p.isLightStatusBar ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0;
                    int navAppearance = p.isLightStatusBar ? WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS : 0;
                    controller.setSystemBarsAppearance(statusAppearance, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                    controller.setSystemBarsAppearance(navAppearance, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                }
                window.setStatusBarColor(p.bgColor);
                window.setNavigationBarColor(p.surfaceColor);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            if (window != null) {
                View decor = window.getDecorView();
                int flags = decor.getSystemUiVisibility();
                if (p.isLightStatusBar) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    }
                } else {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                    }
                }
                decor.setSystemUiVisibility(flags);
                window.setStatusBarColor(p.bgColor);
                window.setNavigationBarColor(p.surfaceColor);
            }
        }

        // 3. Dynamic Aurora Canvas for Aether Aurora
        if (viewAuroraBg != null) {
            viewAuroraBg.setActive(p.isDynamicAurora);
        }

        // 4. Motion & Diagnostics Views
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

        // 5. Document Stage Background
        if (layoutPreviewContainer != null) {
            layoutPreviewContainer.setBackgroundColor(p.previewStageBgColor);
        }

        // 6. Navigation Bar Container & Floating Action Dock
        View bottomBar = findViewById(com.dodopress.R.id.layout_bottom_bar_container);
        if (bottomBar != null) {
            bottomBar.setBackgroundColor(p.surfaceColor);
        }
        if (floatingActionDock != null) {
            GradientDrawable dockBg = new GradientDrawable();
            dockBg.setShape(GradientDrawable.RECTANGLE);
            dockBg.setColor(p.surfaceColor);
            dockBg.setCornerRadius(18 * density);
            dockBg.setStroke((int) (1 * density), p.cardBorderColor);
            floatingActionDock.setBackground(dockBg);
        }

        // 7. Cards across all tabs
        if (cardDocPreview != null) cardDocPreview.setBackgroundResource(p.cardDrawableRes);
        if (cardTonerDensity != null) cardTonerDensity.setBackgroundResource(p.cardDrawableRes);
        if (cardGeometry != null) cardGeometry.setBackgroundResource(p.cardDrawableRes);
        if (cardMargins != null) cardMargins.setBackgroundResource(p.cardDrawableRes);
        if (cardPageConfig != null) cardPageConfig.setBackgroundResource(p.cardDrawableRes);
        if (cardWatermark != null) cardWatermark.setBackgroundResource(p.cardDrawableRes);
        if (cardRecents != null) cardRecents.setBackgroundResource(p.cardDrawableRes);
        if (cardIdSheetPreview != null) cardIdSheetPreview.setBackgroundResource(p.cardDrawableRes);
        if (cardIdPhotos != null) cardIdPhotos.setBackgroundResource(p.cardDrawableRes);
        if (cardIdLayout != null) cardIdLayout.setBackgroundResource(p.cardDrawableRes);
        if (cardIdPaper != null) cardIdPaper.setBackgroundResource(p.cardDrawableRes);
        if (cardIdMargins != null) cardIdMargins.setBackgroundResource(p.cardDrawableRes);
        if (cardIdTone != null) cardIdTone.setBackgroundResource(p.cardDrawableRes);
        if (cardIdWatermark != null) cardIdWatermark.setBackgroundResource(p.cardDrawableRes);
        if (cardIdCopies != null) cardIdCopies.setBackgroundResource(p.cardDrawableRes);
        if (cardGithubStars != null) cardGithubStars.setBackgroundResource(p.cardDrawableRes);
        if (cardTelegramFeedback != null) cardTelegramFeedback.setBackgroundResource(p.cardDrawableRes);
        if (cardAiByok != null) cardAiByok.setBackgroundResource(p.cardDrawableRes);

        // 8. Primary Buttons
        if (btnPickFile != null) {
            btnPickFile.setBackground(ThemeManager.createPrimaryButtonDrawable(p, this));
            btnPickFile.setTextColor(p.primaryButtonTextColor);
        }
        if (btnPrintNow != null) {
            btnPrintNow.setBackground(ThemeManager.createPrimaryButtonDrawable(p, this));
            btnPrintNow.setTextColor(p.primaryButtonTextColor);
        }
        if (dockBtnPrint != null) {
            dockBtnPrint.setBackground(ThemeManager.createPrimaryButtonDrawable(p, this));
            dockBtnPrint.setTextColor(p.primaryButtonTextColor);
        }
        if (btnIdPrintNow != null) {
            btnIdPrintNow.setBackground(ThemeManager.createPrimaryButtonDrawable(p, this));
            btnIdPrintNow.setTextColor(p.primaryButtonTextColor);
        }
        if (btnIdQuickPrint != null) {
            btnIdQuickPrint.setBackground(ThemeManager.createPrimaryButtonDrawable(p, this));
            btnIdQuickPrint.setTextColor(p.primaryButtonTextColor);
        }
        if (btnIdScanFront != null) {
            btnIdScanFront.setBackground(ThemeManager.createPrimaryButtonDrawable(p, this));
            btnIdScanFront.setTextColor(p.primaryButtonTextColor);
        }
        if (btnIdScanBack != null) {
            btnIdScanBack.setBackground(ThemeManager.createPrimaryButtonDrawable(p, this));
            btnIdScanBack.setTextColor(p.primaryButtonTextColor);
        }
        if (btnId2ScanFront != null) {
            btnId2ScanFront.setBackground(ThemeManager.createPrimaryButtonDrawable(p, this));
            btnId2ScanFront.setTextColor(p.primaryButtonTextColor);
        }
        if (btnId2ScanBack != null) {
            btnId2ScanBack.setBackground(ThemeManager.createPrimaryButtonDrawable(p, this));
            btnId2ScanBack.setTextColor(p.primaryButtonTextColor);
        }
        if (btnStarGithub != null) {
            btnStarGithub.setBackground(ThemeManager.createPrimaryButtonDrawable(p, this));
            btnStarGithub.setTextColor(p.primaryButtonTextColor);
        }

        // 9. Secondary Buttons
        if (btnScanDocument != null) {
            btnScanDocument.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnScanDocument.setTextColor(p.secondaryButtonTextColor);
        }
        if (btnQrLabel != null) {
            btnQrLabel.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnQrLabel.setTextColor(p.secondaryButtonTextColor);
        }
        if (btnAiAssistant != null) {
            btnAiAssistant.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnAiAssistant.setTextColor(p.secondaryButtonTextColor);
        }
        if (btnCopiesMinus != null) {
            btnCopiesMinus.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnCopiesMinus.setTextColor(p.secondaryButtonTextColor);
        }
        if (btnCopiesPlus != null) {
            btnCopiesPlus.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnCopiesPlus.setTextColor(p.secondaryButtonTextColor);
        }
        if (btnIdCopiesMinus != null) {
            btnIdCopiesMinus.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnIdCopiesMinus.setTextColor(p.secondaryButtonTextColor);
        }
        if (btnIdCopiesPlus != null) {
            btnIdCopiesPlus.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnIdCopiesPlus.setTextColor(p.secondaryButtonTextColor);
        }
        if (btnIdPickFront != null) {
            btnIdPickFront.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnIdPickFront.setTextColor(p.secondaryButtonTextColor);
        }
        if (btnIdPickBack != null) {
            btnIdPickBack.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnIdPickBack.setTextColor(p.secondaryButtonTextColor);
        }
        if (btnId2PickFront != null) {
            btnId2PickFront.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnId2PickFront.setTextColor(p.secondaryButtonTextColor);
        }
        if (btnId2PickBack != null) {
            btnId2PickBack.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnId2PickBack.setTextColor(p.secondaryButtonTextColor);
        }
        if (btnIdClearPhotos != null) {
            btnIdClearPhotos.setBackground(ThemeManager.createChipButtonDrawable(p, false, this));
            btnIdClearPhotos.setTextColor(p.chipTextColor);
        }
        if (btnPrevPage != null) {
            btnPrevPage.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnPrevPage.setTextColor(p.secondaryButtonTextColor);
        }
        if (btnNextPage != null) {
            btnNextPage.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnNextPage.setTextColor(p.secondaryButtonTextColor);
        }
        if (btnIdSendStudio != null) {
            btnIdSendStudio.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnIdSendStudio.setTextColor(p.secondaryButtonTextColor);
        }
        if (btnOpenTelegram != null) {
            btnOpenTelegram.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnOpenTelegram.setTextColor(p.secondaryButtonTextColor);
        }
        if (btnCopyTelegram != null) {
            btnCopyTelegram.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnCopyTelegram.setTextColor(p.secondaryButtonTextColor);
        }
        if (btnFeedbackRequest != null) {
            btnFeedbackRequest.setBackground(ThemeManager.createSecondaryButtonDrawable(p, this));
            btnFeedbackRequest.setTextColor(p.secondaryButtonTextColor);
        }

        // 10. Chip & Toggle Buttons
        if (btnTogglePreviewMode != null) {
            btnTogglePreviewMode.setBackground(ThemeManager.createChipButtonDrawable(p, false, this));
            btnTogglePreviewMode.setTextColor(p.chipTextColor);
        }
        if (btnRotatePage != null) {
            btnRotatePage.setBackground(ThemeManager.createChipButtonDrawable(p, false, this));
            btnRotatePage.setTextColor(p.chipTextColor);
        }
        if (btnQuickZoom != null) {
            btnQuickZoom.setBackground(ThemeManager.createChipButtonDrawable(p, false, this));
            btnQuickZoom.setTextColor(p.brandPrimaryColor);
        }
        if (btnStageExpand != null) {
            btnStageExpand.setBackground(ThemeManager.createChipButtonDrawable(p, false, this));
            btnStageExpand.setTextColor(p.brandPrimaryColor);
        }
        if (btnTogglePanel != null) {
            btnTogglePanel.setBackground(ThemeManager.createChipButtonDrawable(p, false, this));
            btnTogglePanel.setTextColor(p.brandPrimaryColor);
        }
        if (btnIdQuickOrient != null) {
            btnIdQuickOrient.setBackground(ThemeManager.createChipButtonDrawable(p, false, this));
            btnIdQuickOrient.setTextColor(p.chipTextColor);
        }
        if (btnIdQuickGuides != null) {
            btnIdQuickGuides.setBackground(ThemeManager.createChipButtonDrawable(p, false, this));
            btnIdQuickGuides.setTextColor(p.brandPrimaryColor);
        }
        if (btnIdStageExpand != null) {
            btnIdStageExpand.setBackground(ThemeManager.createChipButtonDrawable(p, false, this));
            btnIdStageExpand.setTextColor(p.brandPrimaryColor);
        }
        if (btnIdTogglePanel != null) {
            btnIdTogglePanel.setBackground(ThemeManager.createChipButtonDrawable(p, false, this));
            btnIdTogglePanel.setTextColor(p.brandPrimaryColor);
        }
        if (btnThemeMenu != null) {
            btnThemeMenu.setBackground(ThemeManager.createChipButtonDrawable(p, false, this));
        }
        if (btnClearRecents != null) {
            btnClearRecents.setBackground(ThemeManager.createChipButtonDrawable(p, false, this));
            btnClearRecents.setTextColor(p.chipTextColor);
        }
        if (btnQueueJob != null) {
            btnQueueJob.setBackground(ThemeManager.createChipButtonDrawable(p, false, this));
            btnQueueJob.setTextColor(p.brandPrimaryColor);
        }

        // 11. Text Colors
        if (tvAppTitle != null) tvAppTitle.setTextColor(p.textHighColor);
        if (tvDensityTitle != null) tvDensityTitle.setTextColor(p.textHighColor);
        if (tvDensityDesc != null) tvDensityDesc.setTextColor(p.textMedColor);
        if (tvGeometryTitle != null) tvGeometryTitle.setTextColor(p.textHighColor);
        if (tvGeometryDesc != null) tvGeometryDesc.setTextColor(p.textMedColor);
        if (tvMarginTitle != null) tvMarginTitle.setTextColor(p.textHighColor);
        if (tvMarginDesc != null) tvMarginDesc.setTextColor(p.textMedColor);
        if (tvCopiesTitle != null) tvCopiesTitle.setTextColor(p.textHighColor);
        if (tvPagesTitle != null) tvPagesTitle.setTextColor(p.textHighColor);
        if (tvPanelHandle != null) tvPanelHandle.setTextColor(p.textMedColor);
        if (tvIdPreviewBadge != null) tvIdPreviewBadge.setTextColor(p.brandPrimaryColor);
        if (tvIdSpacingVal != null) tvIdSpacingVal.setTextColor(p.brandPrimaryColor);
        if (tvIdScaleVal != null) tvIdScaleVal.setTextColor(p.brandPrimaryColor);
        if (tvIdMarginVal != null) tvIdMarginVal.setTextColor(p.brandPrimaryColor);
        if (tvIdToneVal != null) tvIdToneVal.setTextColor(p.brandPrimaryColor);
        if (tvIdCopiesCount != null) tvIdCopiesCount.setTextColor(p.brandPrimaryColor);
        if (tvIdPreviewInfo != null) tvIdPreviewInfo.setTextColor(p.textMedColor);
        if (tvIdPanelHandle != null) tvIdPanelHandle.setTextColor(p.textMedColor);

        if (rbPagesAll != null) rbPagesAll.setTextColor(p.textHighColor);
        if (rbPagesCurrent != null) rbPagesCurrent.setTextColor(p.textHighColor);
        if (rbPagesOdd != null) rbPagesOdd.setTextColor(p.textHighColor);
        if (rbPagesEven != null) rbPagesEven.setTextColor(p.textHighColor);
        if (rbPagesCustom != null) rbPagesCustom.setTextColor(p.textHighColor);
        if (cbCollate != null) cbCollate.setTextColor(p.textMedColor);
        if (cbReverseOrder != null) cbReverseOrder.setTextColor(p.textHighColor);
        if (cbInvertDark != null) cbInvertDark.setTextColor(p.textHighColor);
        if (dockTvStatus != null) dockTvStatus.setTextColor(p.textHighColor);

        // Highlight active sub-controls
        updatePaperSizeHighlight(currentPaperSize);
        updateOrientationButtonsHighlight();
        updatePlacementPresetHighlight(currentPlacementRegion);
        updateTilingHighlight(currentTilingMode);
        if (seekDarkness != null) {
            updatePresetChipHighlight(seekDarkness.getProgress());
        }
        updateMarginButtonsHighlight(currentMarginMm);
        updateMarginMathematicalReadout(currentMarginMm);
        updateTabHighlight(currentControlSection);
        updatePageSummary();

        // Highlight ID Copier sub-controls
        updateIdTabHighlight(currentIdControlSection);
        updateIdControlsHighlight();
        updateIdCardSummary();

        showPage(activePageIndex);
        refreshPrinterStatus();
    }

    private void toggleOptionsPanel() {
        SoundManager.playClick();
        if (scrollOptions == null || btnTogglePanel == null) return;
        isPanelCollapsed = !isPanelCollapsed;
        float density = getResources().getDisplayMetrics().density;
        if (isPanelCollapsed) {
            scrollOptions.setVisibility(View.GONE);
            btnTogglePanel.setText("▲ EXPAND");
            if (btnStageExpand != null) {
                btnStageExpand.setText("⛶ Fit");
            }
            if (layoutSlidingPanel != null) {
                LinearLayout.LayoutParams panelLp = (LinearLayout.LayoutParams) layoutSlidingPanel.getLayoutParams();
                panelLp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                panelLp.weight = 0f;
                layoutSlidingPanel.setLayoutParams(panelLp);
            }
            if (cardDocPreview != null) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) cardDocPreview.getLayoutParams();
                lp.height = 0;
                lp.weight = 1.0f;
                cardDocPreview.setLayoutParams(lp);
            }
        } else {
            scrollOptions.setVisibility(View.VISIBLE);
            btnTogglePanel.setText("▼ RETRACT");
            if (btnStageExpand != null) {
                btnStageExpand.setText("⛶ Max");
            }
            if (cardDocPreview != null) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) cardDocPreview.getLayoutParams();
                lp.height = (int) (235 * density);
                lp.weight = 0f;
                cardDocPreview.setLayoutParams(lp);
            }
            if (layoutSlidingPanel != null) {
                LinearLayout.LayoutParams panelLp = (LinearLayout.LayoutParams) layoutSlidingPanel.getLayoutParams();
                panelLp.height = 0;
                panelLp.weight = 1.0f;
                layoutSlidingPanel.setLayoutParams(panelLp);
            }
        }
        if (ivPagePreview != null) {
            ivPagePreview.post(() -> ivPagePreview.resetZoom());
        }
    }

    private void showControlSection(ControlSection section) {
        SoundManager.playClick();
        currentControlSection = section;

        if (isPanelCollapsed) {
            toggleOptionsPanel();
        }

        // Strictly show ONLY the selected section card, and hide all others
        if (cardRecents != null) cardRecents.setVisibility(section == ControlSection.RECENTS ? View.VISIBLE : View.GONE);
        if (cardTonerDensity != null) cardTonerDensity.setVisibility(section == ControlSection.TONE ? View.VISIBLE : View.GONE);
        if (cardGeometry != null) cardGeometry.setVisibility(section == ControlSection.GEOMETRY ? View.VISIBLE : View.GONE);
        if (cardMargins != null) cardMargins.setVisibility(section == ControlSection.MARGINS ? View.VISIBLE : View.GONE);
        if (cardPageConfig != null) cardPageConfig.setVisibility(section == ControlSection.PAGES ? View.VISIBLE : View.GONE);
        if (cardWatermark != null) cardWatermark.setVisibility(section == ControlSection.WATERMARK ? View.VISIBLE : View.GONE);

        // Reset scroll position to top of container
        if (scrollOptions != null) {
            scrollOptions.scrollTo(0, 0);
        }

        // Update panel handle header text to reflect the active section
        if (tvPanelHandle != null) {
            String title;
            switch (section) {
                case RECENTS:
                    title = "RECENT DOCUMENTS";
                    break;
                case TONE:
                    title = "DENSITY & TONER";
                    break;
                case GEOMETRY:
                    title = "TILING & POSITION";
                    break;
                case MARGINS:
                    title = "MARGINS (0-24mm)";
                    break;
                case PAGES:
                    title = "RANGE & COPIES";
                    break;
                case WATERMARK:
                    title = "WATERMARK & STAMP";
                    break;
                default:
                    title = "PRINT CONTROLS";
                    break;
            }
            tvPanelHandle.setText("── ⚙️ " + title + " ──");
        }

        updateTabHighlight(section);
    }

    private void updateTabHighlight(ControlSection section) {
        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);
        Button[] tabs = {btnTabRecents, btnTabTone, btnTabGeometry, btnTabMargins, btnTabPages, btnTabWatermark};
        ControlSection[] sections = {
            ControlSection.RECENTS,
            ControlSection.TONE,
            ControlSection.GEOMETRY,
            ControlSection.MARGINS,
            ControlSection.PAGES,
            ControlSection.WATERMARK
        };

        Button activeTab = null;
        for (int i = 0; i < tabs.length; i++) {
            Button b = tabs[i];
            if (b == null) continue;
            boolean isAct = (sections[i] == section);
            if (isAct) activeTab = b;
            b.setTextColor(isAct ? p.primaryButtonTextColor : p.chipTextColor);
            b.setTypeface(null, isAct ? Typeface.BOLD : Typeface.NORMAL);
            b.setBackground(ThemeManager.createChipButtonDrawable(p, isAct, this));
        }

        if (scrollCategoryTabs != null && activeTab != null) {
            final View finalActive = activeTab;
            scrollCategoryTabs.post(() -> {
                int targetX = finalActive.getLeft() - (int) (20 * getResources().getDisplayMetrics().density);
                scrollCategoryTabs.smoothScrollTo(Math.max(0, targetX), 0);
            });
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
        float density = getResources().getDisplayMetrics().density;
        ThemeManager.ThemePalette currentPalette = ThemeManager.ThemePalette.forTheme(currentTheme);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(currentPalette.surfaceColor);
        int pad = (int) (18 * density);
        root.setPadding(pad, pad, pad, (int) (24 * density));

        // Header Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("🎨 HARDWARE THEME STUDIO");
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(currentPalette.textHighColor);
        root.addView(tvTitle);

        // Subtitle
        TextView tvSub = new TextView(this);
        tvSub.setText("Select from 5 purpose-built, distinct visual identities designed for offline printing.");
        tvSub.setTextSize(11);
        tvSub.setTextColor(currentPalette.textMedColor);
        tvSub.setPadding(0, (int) (4 * density), 0, (int) (14 * density));
        root.addView(tvSub);

        scroll.addView(root);
        final android.app.AlertDialog dialog = builder.setView(scroll).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        for (final ThemeManager.AppTheme t : ThemeManager.AppTheme.values()) {
            final boolean isActive = (t == currentTheme);
            final ThemeManager.ThemePalette tp = ThemeManager.ThemePalette.forTheme(t);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            int cardPad = (int) (12 * density);
            card.setPadding(cardPad, cardPad, cardPad, cardPad);

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setCornerRadius(tp.cardCornerRadiusDp * density);
            cardBg.setColor(tp.cardBgColor);
            if (isActive) {
                cardBg.setStroke((int) (2f * density), tp.brandPrimaryColor);
            } else {
                cardBg.setStroke((int) (1f * density), tp.cardBorderColor);
            }
            card.setBackground(cardBg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int) (12 * density);
            card.setLayoutParams(lp);

            // Row 1: Header (Title, Subtitle, and Active Badge)
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout titleCol = new LinearLayout(this);
            titleCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            titleCol.setLayoutParams(colLp);

            TextView tvItemTitle = new TextView(this);
            tvItemTitle.setText(t.title);
            tvItemTitle.setTextSize(14);
            tvItemTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvItemTitle.setTextColor(tp.textHighColor);
            titleCol.addView(tvItemTitle);

            TextView tvItemSub = new TextView(this);
            tvItemSub.setText(t.subtitle);
            tvItemSub.setTextSize(10);
            tvItemSub.setTextColor(tp.textMedColor);
            titleCol.addView(tvItemSub);
            headerRow.addView(titleCol);

            if (isActive) {
                TextView activeBadge = new TextView(this);
                activeBadge.setText("✓ ACTIVE");
                activeBadge.setTextSize(10);
                activeBadge.setTypeface(null, android.graphics.Typeface.BOLD);
                activeBadge.setTextColor(tp.primaryButtonTextColor);
                activeBadge.setPadding((int) (10 * density), (int) (4 * density), (int) (10 * density), (int) (4 * density));
                GradientDrawable badgeBg = new GradientDrawable();
                badgeBg.setCornerRadius(8 * density);
                badgeBg.setColor(tp.brandPrimaryColor);
                activeBadge.setBackground(badgeBg);
                headerRow.addView(activeBadge);
            } else if (t == ThemeManager.AppTheme.AETHER_AURORA) {
                TextView auroraTag = new TextView(this);
                auroraTag.setText("✨ AURORA");
                auroraTag.setTextSize(9);
                auroraTag.setTypeface(null, android.graphics.Typeface.BOLD);
                auroraTag.setTextColor(tp.brandPrimaryColor);
                auroraTag.setPadding((int) (8 * density), (int) (3 * density), (int) (8 * density), (int) (3 * density));
                GradientDrawable auroraBg = new GradientDrawable();
                auroraBg.setCornerRadius(6 * density);
                auroraBg.setColor(Color.argb(40, 154, 124, 255));
                auroraBg.setStroke((int) (1 * density), tp.brandPrimaryColor);
                auroraTag.setBackground(auroraBg);
                headerRow.addView(auroraTag);
            }
            card.addView(headerRow);

            // Row 2: Visual Palette Swatches & Mini Component Preview
            LinearLayout previewRow = new LinearLayout(this);
            previewRow.setOrientation(LinearLayout.HORIZONTAL);
            previewRow.setGravity(Gravity.CENTER_VERTICAL);
            previewRow.setPadding(0, (int) (10 * density), 0, 0);

            // 4 color chips showing Palette Tokens
            int swatchSize = (int) (16 * density);
            int[] swatches = {tp.bgColor, tp.surfaceColor, tp.brandPrimaryColor, tp.accentColor};
            for (int swColor : swatches) {
                View sw = new View(this);
                LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(swatchSize, swatchSize);
                swLp.rightMargin = (int) (6 * density);
                sw.setLayoutParams(swLp);
                GradientDrawable swBg = new GradientDrawable();
                swBg.setShape(GradientDrawable.OVAL);
                swBg.setColor(swColor);
                swBg.setStroke(1, Color.argb(60, 150, 150, 150));
                sw.setBackground(swBg);
                previewRow.addView(sw);
            }

            View spacer = new View(this);
            LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, 0, 1.0f);
            spacer.setLayoutParams(spacerLp);
            previewRow.addView(spacer);

            // Representative Component: Primary Button sample
            TextView btnSample = new TextView(this);
            btnSample.setText(isActive ? "Selected" : "Select Theme");
            btnSample.setTextSize(11);
            btnSample.setTypeface(null, android.graphics.Typeface.BOLD);
            btnSample.setTextColor(tp.primaryButtonTextColor);
            btnSample.setPadding((int) (14 * density), (int) (6 * density), (int) (14 * density), (int) (6 * density));
            btnSample.setBackground(ThemeManager.createPrimaryButtonDrawable(tp, this));
            previewRow.addView(btnSample);

            card.addView(previewRow);

            card.setOnClickListener(v -> {
                SoundManager.playClick();
                card.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                applyAppTheme(t);
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "🎨 Activated: " + t.title, Toast.LENGTH_SHORT).show();
            });

            root.addView(card);
        }

        dialog.show();
    }

    private void updatePaperSizeHighlight(SheetGeometry.PaperSize size) {
        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);
        if (btnPaperA4 != null) {
            boolean active = (size == SheetGeometry.PaperSize.A4);
            btnPaperA4.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnPaperA4.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnPaperLetter != null) {
            boolean active = (size == SheetGeometry.PaperSize.LETTER);
            btnPaperLetter.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnPaperLetter.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnPaperLegal != null) {
            boolean active = (size == SheetGeometry.PaperSize.LEGAL);
            btnPaperLegal.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnPaperLegal.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnPaperA5 != null) {
            boolean active = (size == SheetGeometry.PaperSize.A5);
            btnPaperA5.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnPaperA5.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
    }

    private void updatePlacementPresetHighlight(SheetGeometry.PlacementRegion region) {
        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);
        Button[] btns = {btnPlaceFull, btnPlaceTopHalf, btnPlaceBottomHalf, btnPlaceLeftHalf, btnPlaceRightHalf, btnPlaceQuadTl};
        SheetGeometry.PlacementRegion[] regions = {
            SheetGeometry.PlacementRegion.FULL_PAGE,
            SheetGeometry.PlacementRegion.TOP_HALF,
            SheetGeometry.PlacementRegion.BOTTOM_HALF,
            SheetGeometry.PlacementRegion.LEFT_HALF,
            SheetGeometry.PlacementRegion.RIGHT_HALF,
            SheetGeometry.PlacementRegion.QUAD_TOP_LEFT
        };

        for (int i = 0; i < btns.length; i++) {
            Button b = btns[i];
            if (b == null) continue;
            boolean active = (region == regions[i]);
            b.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            b.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
    }

    private void setMarginPreset(float mm) {
        SoundManager.playClick();
        if (seekMargin != null) {
            seekMargin.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            seekMargin.setProgress(Math.round(mm));
        }
        currentMarginMm = mm;
        updateMarginDisplay(mm);
        renderPageThumb();
    }

    private void updateMarginDisplay(float mm) {
        if (tvMarginLabel != null) {
            String desc;
            if (mm == 0f) desc = "0 mm (Edge-to-Edge / Borderless)";
            else if (Math.abs(mm - SheetGeometry.HARDWARE_MIN_MARGIN_MM) < 0.1f) desc = "4.5 mm (Laser Drum HW Limit)";
            else if (Math.abs(mm - 8.0f) < 0.1f) desc = "8 mm (Narrow)";
            else if (Math.abs(mm - 12.0f) < 0.1f) desc = "12 mm (★ Standard)";
            else if (Math.abs(mm - 20.0f) < 0.1f) desc = "20 mm (Wide / Binder)";
            else desc = String.format(Locale.US, "%.1f mm (Custom)", mm);
            tvMarginLabel.setText(desc);
        }
        updateMarginButtonsHighlight(mm);
        updateMarginMathematicalReadout(mm);
        updatePageSummary();
    }

    private void updateMarginMathematicalReadout(float mm) {
        SheetGeometry.PaperSize paper = (currentPaperSize != null) ? currentPaperSize : SheetGeometry.PaperSize.A4;
        SheetGeometry.Orientation orient = (currentOrientation != null) ? currentOrientation : SheetGeometry.Orientation.PORTRAIT;

        float sheetW_mm = paper.getWidthMm(orient);
        float sheetH_mm = paper.getHeightMm(orient);
        int sheetW_pt = paper.getWidthPts(orient);
        int sheetH_pt = paper.getHeightPts(orient);

        float printW_mm = Math.max(0f, sheetW_mm - (2f * mm));
        float printH_mm = Math.max(0f, sheetH_mm - (2f * mm));
        float printW_pt = (printW_mm / 25.4f) * 72.0f;
        float printH_pt = (printH_mm / 25.4f) * 72.0f;
        float areaPct = Math.max(0f, Math.min(100f, ((printW_mm * printH_mm) / (sheetW_mm * sheetH_mm)) * 100f));

        if (tvMarginCalcSheet != null) {
            tvMarginCalcSheet.setText(String.format(Locale.US, "%.1f × %.1f mm (%d × %d pt)", sheetW_mm, sheetH_mm, sheetW_pt, sheetH_pt));
        }
        if (tvMarginCalcPrintable != null) {
            tvMarginCalcPrintable.setText(String.format(Locale.US, "%.1f × %.1f mm (%.0f × %.0f pt • %.1f%%)", printW_mm, printH_mm, printW_pt, printH_pt, areaPct));
        }
        if (tvMarginCalcHardware != null) {
            tvMarginCalcHardware.setText("4.5 mm (12.8 pt) Drum Non-Printable Edge");
        }
        if (tvMarginHwStatus != null) {
            if (SheetGeometry.isHardwareClipped(mm)) {
                float clipMm = SheetGeometry.getHardwareCutoffDeltaMm(mm);
                tvMarginHwStatus.setText(String.format(Locale.US, "⚠️ HARDWARE CLIPPING RISK: Drum cannot print in 4.5mm gray area! Outer %.1f mm will be cut off.", clipMm));
                tvMarginHwStatus.setBackgroundColor(0xFF2E1616);
                tvMarginHwStatus.setTextColor(0xFFF87171);
            } else {
                tvMarginHwStatus.setText("✓ 100% IN PRINTABLE BOUNDARY: Margin clears laser drum 4.5 mm limit");
                tvMarginHwStatus.setBackgroundColor(0xFF162E20);
                tvMarginHwStatus.setTextColor(0xFF4ADE80);
            }
        }
    }

    private void updateMarginButtonsHighlight(float mm) {
        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);

        if (btnMarginZero != null) {
            boolean active = (mm == 0f);
            btnMarginZero.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnMarginZero.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnMarginHwMin != null) {
            boolean active = Math.abs(mm - SheetGeometry.HARDWARE_MIN_MARGIN_MM) < 0.1f;
            btnMarginHwMin.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnMarginHwMin.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnMarginNarrow != null) {
            boolean active = Math.abs(mm - 8.0f) < 0.1f;
            btnMarginNarrow.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnMarginNarrow.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnMarginNormal != null) {
            boolean active = Math.abs(mm - 12.0f) < 0.1f;
            btnMarginNormal.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnMarginNormal.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnMarginWide != null) {
            boolean active = Math.abs(mm - 20.0f) < 0.1f;
            btnMarginWide.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnMarginWide.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
    }

    private void setDarkness(int value) {
        seekDarkness.setProgress(value);
        updatePresetChipHighlight(value);
    }

    private void updatePresetChipHighlight(int progress) {
        ThemeManager.ThemePalette p = ThemeManager.ThemePalette.forTheme(currentTheme);
        if (btnPresetDraft != null) {
            boolean active = (progress == ToneEngine.PRESET_DRAFT);
            btnPresetDraft.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnPresetDraft.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnPresetNormal != null) {
            boolean active = (progress == ToneEngine.PRESET_NORMAL);
            btnPresetNormal.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnPresetNormal.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnPresetDeep != null) {
            boolean active = (progress == ToneEngine.PRESET_DEEP);
            btnPresetDeep.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnPresetDeep.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
        if (btnPresetUltra != null) {
            boolean active = (progress == ToneEngine.PRESET_ULTRA);
            btnPresetUltra.setBackground(ThemeManager.createChipButtonDrawable(p, active, this));
            btnPresetUltra.setTextColor(active ? p.primaryButtonTextColor : p.chipTextColor);
        }
    }

    private void scheduleDarknessPreview(int darkness) {
        if (cleanPreviewBitmap == null || cleanPreviewBitmap.isRecycled()) return;

        pendingDarkness.set(darkness);
        final int token = previewRenderToken.get();
        previewExecutor.execute(() -> {
            int d = pendingDarkness.getAndSet(-1);
            if (d >= 0 && token == previewRenderToken.get()) {
                Bitmap source;
                synchronized (pdfRendererLock) {
                    if (cleanPreviewBitmap == null || cleanPreviewBitmap.isRecycled()) return;
                    source = cleanPreviewBitmap.copy(Bitmap.Config.ARGB_8888, false);
                }
                if (source == null) return;
                Bitmap updated = ToneEngine.processPreviewBitmap(source, d);
                source.recycle();
                if (updated != null) {
                    SheetGeometry.PaperSize targetPaper = (currentPaperSize != null) ? currentPaperSize : SheetGeometry.PaperSize.A4;
                    float sheetW_mm = targetPaper.getWidthMm(currentOrientation);
                    float sheetH_mm = targetPaper.getHeightMm(currentOrientation);
                    Canvas canvasTone = new Canvas(updated);
                    drawSheetDecorations(canvasTone, updated.getWidth(), updated.getHeight(), sheetW_mm, sheetH_mm, currentMarginMm, targetPaper, currentOrientation, false);

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
            if (tvPrinterStatus != null) {
                tvPrinterStatus.setText(displayName);
                tvPrinterStatus.setTextColor(Color.parseColor("#34D399"));
            }
            if (tvConnectionSymbol != null) {
                tvConnectionSymbol.setText("🖨️");
            }
            if (viewBeacon != null) viewBeacon.setBeaconColor(Color.parseColor("#10B981"));
            if (layoutStatusCapsule != null) {
                layoutStatusCapsule.setBackgroundResource(com.dodopress.R.drawable.badge_status_online);
            }
            if (dockTvStatus != null) {
                dockTvStatus.setText(displayName + " (Online)");
            }
            if (dockBtnPrint != null) {
                dockBtnPrint.setBackgroundResource(com.dodopress.R.drawable.btn_primary);
                dockBtnPrint.setText("⚡ PRINT NOW");
                dockBtnPrint.setTextColor(Color.WHITE);
            }
            DiagnosticLogManager.log("USB", "Connected: " + displayName);

            // Prompt if offline queue has items waiting
            List<SpoolManager.SpoolJob> queued = SpoolManager.getQueuedJobs(this);
            if (!queued.isEmpty()) {
                Toast.makeText(this, displayName + " connected: " + queued.size() + " queued job(s) waiting in Queue tab!", Toast.LENGTH_LONG).show();
            }
        } else {
            currentPrinterCutawayState = PrinterCutawayView.PrinterState.OFFLINE;
            if (tvPrinterStatus != null) {
                tvPrinterStatus.setText("Connect USB");
                tvPrinterStatus.setTextColor(Color.parseColor("#FBBF24"));
            }
            if (tvConnectionSymbol != null) {
                tvConnectionSymbol.setText("🔌");
            }
            if (viewBeacon != null) viewBeacon.setBeaconColor(Color.parseColor("#F59E0B"));
            if (layoutStatusCapsule != null) {
                layoutStatusCapsule.setBackgroundResource(com.dodopress.R.drawable.badge_status_offline);
            }
            if (dockTvStatus != null) {
                PrinterModel active = (driverManager != null) ? driverManager.getActivePrinter() : null;
                dockTvStatus.setText((active != null ? active.getDisplayName() : "Universal Printer") + " (Connect OTG)");
            }
            if (dockBtnPrint != null) {
                dockBtnPrint.setBackgroundResource(com.dodopress.R.drawable.btn_offline_dock);
                dockBtnPrint.setText("🔌 PLUG USB TO PRINT");
                dockBtnPrint.setTextColor(Color.parseColor("#FEF3C7"));
            }
            DiagnosticLogManager.log("USB", "Waiting for USB OTG cable connection...");
        }
    }

    private void showOfflinePrinterDialog() {
        SoundManager.playAlert();
        PrinterModel active = (driverManager != null) ? driverManager.getActivePrinter() : null;
        String pName = (active != null) ? active.getDisplayName() : "Universal USB Printer";
        new AlertDialog.Builder(this)
            .setTitle("🔌 Connect USB OTG Cable")
            .setMessage(pName + " requires a physical USB OTG cable connection.\n\n1. Connect USB OTG adapter to your phone\n2. Plug in the printer USB cable\n3. Turn ON printer power\n4. Accept the Android USB permission prompt\n\nDodoPress will automatically detect the printer and illuminate green!")
            .setPositiveButton("Open Diagnostics ⚡", (dialog, which) -> {
                DiagnosticLogManager.showDiagnosticDialog(this, getCurrentPrinterCutawayState());
            })
            .setNegativeButton("OK", null)
            .show();
    }

    private void refreshActivePrinterDisplay() {
        if (driverManager == null) return;
        PrinterModel active = driverManager.getActivePrinter();
        if (active == null) return;

        if (tvXpTitle != null) {
            tvXpTitle.setText("Print - " + active.getDisplayName());
        }

        if (tvActivePrinterName != null) {
            tvActivePrinterName.setText(active.getDisplayName());
        }
        if (tvDriverProtocolBadge != null) {
            tvDriverProtocolBadge.setText(active.protocol.name());
        }
        if (tvActivePrinterSpecs != null) {
            tvActivePrinterSpecs.setText(active.hardwareSpecs);
        }
        if (tvActiveDriverStatus != null) {
            boolean installed = driverManager.isDriverInstalled(active);
            if (active.isBuiltIn) {
                tvActiveDriverStatus.setText("Status: Built-in Native Filter");
                tvActiveDriverStatus.setTextColor(0xFF38BDF8);
            } else if (installed) {
                tvActiveDriverStatus.setText("Status: Installed (Offline Cached)");
                tvActiveDriverStatus.setTextColor(0xFF4ADE80);
            } else {
                tvActiveDriverStatus.setText("Status: Download Required (" + active.downloadSizeKb + " KB)");
                tvActiveDriverStatus.setTextColor(0xFFFACC15);
            }
        }

        // Keep bottom dock informed if offline
        if (dockTvStatus != null && usbPrinterConnection != null && usbPrinterConnection.getDevice() == null) {
            dockTvStatus.setText(active.getDisplayName() + " (Connect OTG)");
        }
    }

    private void autoDetectUsbPrinter() {
        UsbDevice dev = usbPrinterConnection.findPrinter();
        if (dev != null) {
            PrinterModel matched = driverManager.matchUsbDevice(dev);
            if (matched != null) {
                driverManager.setActivePrinter(matched);
                refreshActivePrinterDisplay();
                SoundManager.playXpPrintChime();
                Toast.makeText(this, "Auto-detected & Selected: " + matched.getDisplayName(), Toast.LENGTH_LONG).show();
            } else {
                String devName = usbPrinterConnection.getPrinterDisplayName(dev);
                Toast.makeText(this, "Connected USB printer: " + devName + "\n(Generic Class 7 Protocol)", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "No USB printer detected.\nConnect your printer via OTG adapter.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPrinterCatalogDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        float density = getResources().getDisplayMetrics().density;
        ThemeManager.ThemePalette currentPalette = ThemeManager.ThemePalette.forTheme(currentTheme);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(currentPalette.surfaceColor);
        int pad = (int) (16 * density);
        root.setPadding(pad, pad, pad, (int) (24 * density));

        // Title row with icon
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setPadding(0, 0, 0, (int) (4 * density));

        ImageView iconView = new ImageView(this);
        iconView.setImageResource(com.dodopress.R.drawable.ic_btn_driver_chip);
        int iconSize = (int) (22 * density);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconLp.rightMargin = (int) (8 * density);
        iconView.setLayoutParams(iconLp);
        titleRow.addView(iconView);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("UNIVERSAL DRIVER HUB");
        tvTitle.setTextSize(15);
        tvTitle.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        tvTitle.setTextColor(currentPalette.textHighColor);
        titleRow.addView(tvTitle);
        root.addView(titleRow);

        TextView tvSub = new TextView(this);
        tvSub.setText("Select your printer model to install driver packs for 100% offline printing.");
        tvSub.setTextSize(11);
        tvSub.setTextColor(currentPalette.textMedColor);
        tvSub.setPadding(0, 0, 0, (int) (10 * density));
        root.addView(tvSub);

        // Search Input Box
        EditText etSearch = new EditText(this);
        etSearch.setHint("Filter model (e.g. 2900, 1020, L3150)...");
        etSearch.setHintTextColor(currentPalette.textMutedColor);
        etSearch.setTextColor(currentPalette.textHighColor);
        etSearch.setTextSize(12);
        etSearch.setBackgroundResource(com.dodopress.R.drawable.input_bg);
        int searchPad = (int) (10 * density);
        etSearch.setPadding(searchPad, searchPad, searchPad, searchPad);
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (int) (40 * density));
        searchLp.bottomMargin = (int) (10 * density);
        etSearch.setLayoutParams(searchLp);
        root.addView(etSearch);

        // Brand Filter Chips Strip
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);

        String[] brands = new String[]{"ALL", "HP", "CANON", "EPSON", "BROTHER", "SAMSUNG", "PANTUM"};
        final String[] selectedBrand = new String[]{"ALL"};
        final Button[] brandButtons = new Button[brands.length];

        LinearLayout printerListContainer = new LinearLayout(this);
        printerListContainer.setOrientation(LinearLayout.VERTICAL);

        AlertDialog dialog = builder.setView(scroll).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Runnable refreshList = () -> {
            printerListContainer.removeAllViews();
            String query = etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
            String bFilter = selectedBrand[0];
            PrinterModel activeModel = driverManager.getActivePrinter();

            List<PrinterModel> all = driverManager.getAllPrinters();
            int count = 0;
            for (final PrinterModel model : all) {
                if (!bFilter.equals("ALL") && !model.brand.equalsIgnoreCase(bFilter)) {
                    continue;
                }
                if (!query.isEmpty()) {
                    boolean match = model.getDisplayName().toLowerCase(Locale.ROOT).contains(query)
                        || model.hardwareSpecs.toLowerCase(Locale.ROOT).contains(query)
                        || model.protocol.name().toLowerCase(Locale.ROOT).contains(query);
                    if (!match) continue;
                }
                count++;

                final boolean isActive = (activeModel != null && activeModel.id.equals(model.id));
                final boolean isInstalled = driverManager.isDriverInstalled(model);

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                int cPad = (int) (10 * density);
                card.setPadding(cPad, cPad, cPad, cPad);

                GradientDrawable cBg = new GradientDrawable();
                cBg.setCornerRadius(8 * density);
                cBg.setColor(isActive ? currentPalette.cardBgColor : 0xFF111827);
                if (isActive) {
                    cBg.setStroke((int) (2 * density), currentPalette.brandPrimaryColor);
                } else {
                    cBg.setStroke((int) (1 * density), 0xFF1F2937);
                }
                card.setBackground(cBg);

                LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                cLp.bottomMargin = (int) (8 * density);
                card.setLayoutParams(cLp);

                // Row 1: Brand pill + Model Name + Active Badge
                LinearLayout row1 = new LinearLayout(this);
                row1.setOrientation(LinearLayout.HORIZONTAL);
                row1.setGravity(Gravity.CENTER_VERTICAL);

                TextView tvBrandPill = new TextView(this);
                tvBrandPill.setText(model.brand.toUpperCase(Locale.ROOT));
                tvBrandPill.setTextSize(9);
                tvBrandPill.setTypeface(Typeface.create("monospace", Typeface.BOLD));
                tvBrandPill.setTextColor(0xFF38BDF8);
                tvBrandPill.setBackgroundResource(com.dodopress.R.drawable.badge_section_tag);
                int bp = (int) (4 * density);
                tvBrandPill.setPadding(bp, (int) (1 * density), bp, (int) (1 * density));
                LinearLayout.LayoutParams bpLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                bpLp.rightMargin = (int) (6 * density);
                tvBrandPill.setLayoutParams(bpLp);
                row1.addView(tvBrandPill);

                TextView tvModelName = new TextView(this);
                tvModelName.setText(model.modelName);
                tvModelName.setTextSize(12.5f);
                tvModelName.setTypeface(null, Typeface.BOLD);
                tvModelName.setTextColor(currentPalette.textHighColor);
                LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                tvModelName.setLayoutParams(nameLp);
                row1.addView(tvModelName);

                if (isActive) {
                    TextView tvActive = new TextView(this);
                    tvActive.setText("● ACTIVE");
                    tvActive.setTextSize(9);
                    tvActive.setTypeface(Typeface.create("monospace", Typeface.BOLD));
                    tvActive.setTextColor(0xFF34D399);
                    row1.addView(tvActive);
                }
                card.addView(row1);

                // Row 2: Specs & Protocol
                TextView tvSpecs = new TextView(this);
                tvSpecs.setText(model.protocol.name() + " • " + model.hardwareSpecs);
                tvSpecs.setTextSize(10);
                tvSpecs.setTextColor(currentPalette.textMedColor);
                tvSpecs.setPadding(0, (int) (3 * density), 0, (int) (5 * density));
                card.addView(tvSpecs);

                // Row 3: Driver status action indicator
                TextView tvAction = new TextView(this);
                if (model.isBuiltIn) {
                    tvAction.setText("⚡ Built-in Driver (Ready)");
                    tvAction.setTextColor(0xFF38BDF8);
                } else if (isInstalled) {
                    tvAction.setText("✓ Driver Cached Offline (Ready)");
                    tvAction.setTextColor(0xFF4ADE80);
                } else {
                    tvAction.setText("⬇ Download Driver Pack (" + model.downloadSizeKb + " KB)");
                    tvAction.setTextColor(0xFFFACC15);
                }
                tvAction.setTextSize(10);
                tvAction.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
                card.addView(tvAction);

                // Card Click Listener
                card.setOnClickListener(v -> {
                    SoundManager.playClick();
                    if (model.isBuiltIn || driverManager.isDriverInstalled(model)) {
                        driverManager.setActivePrinter(model);
                        refreshActivePrinterDisplay();
                        Toast.makeText(this, "Selected: " + model.getDisplayName(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        downloadAndActivateDriver(model, dialog);
                    }
                });

                printerListContainer.addView(card);
            }

            if (count == 0) {
                TextView empty = new TextView(this);
                empty.setText("No printers matching \"" + query + "\"");
                empty.setTextSize(12);
                empty.setTextColor(currentPalette.textMutedColor);
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(0, (int) (20 * density), 0, (int) (20 * density));
                printerListContainer.addView(empty);
            }
        };

        // Populate Brand chips
        for (int i = 0; i < brands.length; i++) {
            final String brand = brands[i];
            Button chip = new Button(this);
            chip.setText(brand);
            chip.setTextSize(10);
            chip.setTypeface(Typeface.create("monospace", Typeface.BOLD));
            chip.setPadding((int) (8 * density), 0, (int) (8 * density), 0);
            chip.setMinWidth(0);
            chip.setMinHeight(0);

            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, (int) (26 * density));
            chipLp.rightMargin = (int) (4 * density);
            chip.setLayoutParams(chipLp);

            final int idx = i;
            brandButtons[idx] = chip;

            chip.setOnClickListener(v -> {
                SoundManager.playClick();
                selectedBrand[0] = brand;
                for (int j = 0; j < brands.length; j++) {
                    boolean sel = brands[j].equals(brand);
                    brandButtons[j].setBackgroundResource(sel ? com.dodopress.R.drawable.btn_theme_chip_active : com.dodopress.R.drawable.btn_chip);
                    brandButtons[j].setTextColor(sel ? Color.WHITE : currentPalette.textMedColor);
                }
                refreshList.run();
            });

            chipRow.addView(chip);
        }
        chipScroll.addView(chipRow);
        LinearLayout.LayoutParams csLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        csLp.bottomMargin = (int) (12 * density);
        chipScroll.setLayoutParams(csLp);
        root.addView(chipScroll);

        root.addView(printerListContainer);
        scroll.addView(root);

        brandButtons[0].setBackgroundResource(com.dodopress.R.drawable.btn_theme_chip_active);
        brandButtons[0].setTextColor(Color.WHITE);
        for (int j = 1; j < brands.length; j++) {
            brandButtons[j].setBackgroundResource(com.dodopress.R.drawable.btn_chip);
            brandButtons[j].setTextColor(currentPalette.textMedColor);
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshList.run();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        refreshList.run();
        dialog.show();
    }

    private void downloadAndActivateDriver(PrinterModel model, AlertDialog parentDialog) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("Downloading Driver Pack");
        progress.setMessage("Connecting to repository for " + model.getDisplayName() + "...");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setMax(100);
        progress.setCancelable(false);
        progress.show();

        driverManager.downloadDriver(model, new DriverManager.DownloadCallback() {
            @Override
            public void onProgress(int percent, long bytesDownloaded, long totalBytes) {
                progress.setProgress(percent);
                long dlKb = bytesDownloaded / 1024;
                long totKb = totalBytes > 0 ? totalBytes / 1024 : model.downloadSizeKb;
                progress.setMessage("Downloading " + model.getDisplayName() + ":\n" + dlKb + " KB / " + totKb + " KB (" + percent + "%)");
            }

            @Override
            public void onSuccess(File driverFile) {
                progress.dismiss();
                driverManager.setActivePrinter(model);
                refreshActivePrinterDisplay();
                SoundManager.playXpPrintChime();
                Toast.makeText(MainActivity.this, "Driver installed! Switched to " + model.getDisplayName(), Toast.LENGTH_LONG).show();
                if (parentDialog != null && parentDialog.isShowing()) {
                    parentDialog.dismiss();
                }
            }

            @Override
            public void onError(String message) {
                progress.dismiss();
                Toast.makeText(MainActivity.this, "Download error: " + message, Toast.LENGTH_LONG).show();
            }
        });
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

    private int pendingCameraRequestCode = REQUEST_CAMERA_SCAN;

    private void launchCameraScanner(boolean directPrint) {
        launchCameraScanner(directPrint, REQUEST_CAMERA_SCAN);
    }

    private void launchCameraScanner(boolean directPrint, int requestCode) {
        this.isDirectInstantPhotocopy = directPrint;
        this.pendingCameraRequestCode = requestCode;
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA_REQ);
            return;
        }

        try {
            tempScanFile = new File(getCacheDir(), "scan_capture_" + requestCode + ".jpg");
            if (tempScanFile.exists()) tempScanFile.delete();

            Uri photoUri = Uri.parse("content://" + DodoFileProvider.AUTHORITY + "/" + tempScanFile.getName());
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(takePictureIntent, requestCode);
        } catch (Exception e) {
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CAMERA_REQ) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCameraScanner(isDirectInstantPhotocopy, pendingCameraRequestCode);
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

    private void processScannedIdCard(int requestCode) {
        if (tempScanFile == null || !tempScanFile.exists() || tempScanFile.length() == 0) {
            Toast.makeText(this, "No photo captured.", Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap bm = decodeFileScaled(tempScanFile, 1200);
        if (bm == null) {
            Toast.makeText(this, "Failed to decode captured ID photo.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (requestCode == REQUEST_SCAN_ID_FRONT) {
            if (idFrontBitmap != null && !idFrontBitmap.isRecycled()) idFrontBitmap.recycle();
            idFrontBitmap = bm;
            if (ivIdFrontPreview != null) ivIdFrontPreview.setImageBitmap(idFrontBitmap);
            Toast.makeText(this, "ID #1 Front scanned!", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_SCAN_ID_BACK) {
            if (idBackBitmap != null && !idBackBitmap.isRecycled()) idBackBitmap.recycle();
            idBackBitmap = bm;
            if (ivIdBackPreview != null) ivIdBackPreview.setImageBitmap(idBackBitmap);
            Toast.makeText(this, "ID #1 Back scanned!", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_SCAN_ID2_FRONT) {
            if (id2FrontBitmap != null && !id2FrontBitmap.isRecycled()) id2FrontBitmap.recycle();
            id2FrontBitmap = bm;
            if (ivId2FrontPreview != null) ivId2FrontPreview.setImageBitmap(id2FrontBitmap);
            Toast.makeText(this, "ID #2 Front scanned!", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_SCAN_ID2_BACK) {
            if (id2BackBitmap != null && !id2BackBitmap.isRecycled()) id2BackBitmap.recycle();
            id2BackBitmap = bm;
            if (ivId2BackPreview != null) ivId2BackPreview.setImageBitmap(id2BackBitmap);
            Toast.makeText(this, "ID #2 Back scanned!", Toast.LENGTH_SHORT).show();
        }
        renderIdCardPreview();
    }

    private Bitmap decodeUriScaled(Uri uri, int maxDim) {
        try {
            BitmapFactory.Options boundsOpts = new BitmapFactory.Options();
            boundsOpts.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, boundsOpts);
            }
            int sampleSize = 1;
            while ((boundsOpts.outWidth / sampleSize / 2) >= maxDim || (boundsOpts.outHeight / sampleSize / 2) >= maxDim) {
                sampleSize *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                return BitmapFactory.decodeStream(in, null, opts);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap decodeFileScaled(File file, int maxDim) {
        try {
            BitmapFactory.Options boundsOpts = new BitmapFactory.Options();
            boundsOpts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), boundsOpts);

            int sampleSize = 1;
            while ((boundsOpts.outWidth / sampleSize / 2) >= maxDim || (boundsOpts.outHeight / sampleSize / 2) >= maxDim) {
                sampleSize *= 2;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_CAMERA_SCAN) {
            processScannedDocument();
            return;
        }

        if (requestCode == REQUEST_SCAN_ID_FRONT || requestCode == REQUEST_SCAN_ID_BACK ||
            requestCode == REQUEST_SCAN_ID2_FRONT || requestCode == REQUEST_SCAN_ID2_BACK) {
            processScannedIdCard(requestCode);
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
                if (idFrontBitmap != null && !idFrontBitmap.isRecycled()) idFrontBitmap.recycle();
                idFrontBitmap = decodeUriScaled(uri, 1200);
                if (ivIdFrontPreview != null) ivIdFrontPreview.setImageBitmap(idFrontBitmap);
                renderIdCardPreview();
                Toast.makeText(this, "ID #1 Front loaded!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_PICK_ID_BACK) {
            Uri uri = data.getData();
            if (uri != null) {
                if (idBackBitmap != null && !idBackBitmap.isRecycled()) idBackBitmap.recycle();
                idBackBitmap = decodeUriScaled(uri, 1200);
                if (ivIdBackPreview != null) ivIdBackPreview.setImageBitmap(idBackBitmap);
                renderIdCardPreview();
                Toast.makeText(this, "ID #1 Back loaded!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_PICK_ID2_FRONT) {
            Uri uri = data.getData();
            if (uri != null) {
                if (id2FrontBitmap != null && !id2FrontBitmap.isRecycled()) id2FrontBitmap.recycle();
                id2FrontBitmap = decodeUriScaled(uri, 1200);
                if (ivId2FrontPreview != null) ivId2FrontPreview.setImageBitmap(id2FrontBitmap);
                renderIdCardPreview();
                Toast.makeText(this, "ID #2 Front loaded!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_PICK_ID2_BACK) {
            Uri uri = data.getData();
            if (uri != null) {
                if (id2BackBitmap != null && !id2BackBitmap.isRecycled()) id2BackBitmap.recycle();
                id2BackBitmap = decodeUriScaled(uri, 1200);
                if (ivId2BackPreview != null) ivId2BackPreview.setImageBitmap(id2BackBitmap);
                renderIdCardPreview();
                Toast.makeText(this, "ID #2 Back loaded!", Toast.LENGTH_SHORT).show();
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
        if (totalPages <= 0) {
            renderEmptySheetPreview();
            return;
        }

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
        final float targetMarginMm = currentMarginMm;
        final SheetGeometry.PaperSize targetPaper = (currentPaperSize != null) ? currentPaperSize : SheetGeometry.PaperSize.A4;
        final SheetGeometry.Orientation targetOrientation = currentOrientation;
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
                    float sheetW_mm = targetPaper.getWidthMm(targetOrientation);
                    float sheetH_mm = targetPaper.getHeightMm(targetOrientation);

                    int maxDim = 960;
                    int sheetPreviewW, sheetPreviewH;
                    if (sheetW_mm <= sheetH_mm) {
                        sheetPreviewH = maxDim;
                        sheetPreviewW = Math.round(maxDim * (sheetW_mm / sheetH_mm));
                    } else {
                        sheetPreviewW = maxDim;
                        sheetPreviewH = Math.round(maxDim * (sheetH_mm / sheetW_mm));
                    }

                    float pxPerMmX = (float) sheetPreviewW / sheetW_mm;
                    float pxPerMmY = (float) sheetPreviewH / sheetH_mm;
                    int marginPxX = Math.round(targetMarginMm * pxPerMmX);
                    int marginPxY = Math.round(targetMarginMm * pxPerMmY);

                    baseBm = Bitmap.createBitmap(sheetPreviewW, sheetPreviewH, Bitmap.Config.ARGB_8888);
                    baseBm.eraseColor(0xFFFFFFFF); // Clean sheet of paper
                    Canvas canvas = new Canvas(baseBm);

                    if (targetTilingMode == null || targetTilingMode == SheetGeometry.TilingMode.ONE_UP) {
                        PdfRenderer.Page page = pdfRenderer.openPage(targetPageIndex);
                        int pw = page.getWidth();
                        int ph = page.getHeight();

                        Rect targetBox = SheetGeometry.getPlacementBoundingBox(targetRegion, sheetPreviewW, sheetPreviewH, marginPxX, marginPxY);

                        int fitDocW = (targetRotation == 90f || targetRotation == 270f) ? ph : pw;
                        int fitDocH = (targetRotation == 90f || targetRotation == 270f) ? pw : ph;

                        Rect destRect = SheetGeometry.calculateAspectFit(fitDocW, fitDocH, targetBox, 0);

                        if (destRect.width() > 0 && destRect.height() > 0) {
                            int renderW = (targetRotation == 90f || targetRotation == 270f) ? destRect.height() : destRect.width();
                            int renderH = (targetRotation == 90f || targetRotation == 270f) ? destRect.width() : destRect.height();

                            Bitmap docBm = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888);
                            docBm.eraseColor(0xFFFFFFFF);
                            page.render(docBm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                            Bitmap finalDoc = (targetRotation != 0f) ? ToneEngine.rotateBitmap(docBm, targetRotation) : docBm;

                            canvas.drawBitmap(finalDoc, destRect.left, destRect.top, null);

                            if (finalDoc != docBm) finalDoc.recycle();
                            docBm.recycle();
                        }
                        page.close();
                    } else {
                        // Tiled Multi-Page Preview (2-Up or 4-Up)
                        Rect[] tileBoxes = SheetGeometry.calculateNUpBoxes(targetTilingMode, sheetPreviewW, sheetPreviewH, marginPxX, marginPxY, sheetW_mm, sheetH_mm);
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
                                int fitDocW = (targetRotation == 90f || targetRotation == 270f) ? ph : pw;
                                int fitDocH = (targetRotation == 90f || targetRotation == 270f) ? pw : ph;

                                Rect destRect = SheetGeometry.calculateAspectFit(fitDocW, fitDocH, tileBox, 0);

                                if (destRect.width() > 0 && destRect.height() > 0) {
                                    int renderW = (targetRotation == 90f || targetRotation == 270f) ? destRect.height() : destRect.width();
                                    int renderH = (targetRotation == 90f || targetRotation == 270f) ? destRect.width() : destRect.height();

                                    Bitmap docBm = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888);
                                    docBm.eraseColor(0xFFFFFFFF);
                                    page.render(docBm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                                    Bitmap finalDoc = (targetRotation != 0f) ? ToneEngine.rotateBitmap(docBm, targetRotation) : docBm;

                                    canvas.drawBitmap(finalDoc, destRect.left, destRect.top, null);
                                    canvas.drawRect(destRect, borderPaint);

                                    if (finalDoc != docBm) finalDoc.recycle();
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

            // 3. Auto-blank page detection (runs on clean document without UI border interference)
            final boolean isBlank = ToneEngine.isPageBlank(baseBm);

            // Keep clean undecorated document bitmap for high-performance dynamic darkness updates
            final Bitmap cleanCopy = baseBm.copy(Bitmap.Config.ARGB_8888, false);

            // 4. Tone adjusted bitmap for real-time laser preview
            final Bitmap toneAdjusted = ToneEngine.processPreviewBitmap(cleanCopy, targetDarkness);
            final Bitmap finalRaw = baseBm;

            // 5. Draw UI sheet decorations (4.5mm hardware gray area, margin guides, sheet border)
            // as crisp post-filter vector overlays on both display views
            float sheetW_mm = targetPaper.getWidthMm(targetOrientation);
            float sheetH_mm = targetPaper.getHeightMm(targetOrientation);

            if (toneAdjusted != null && !toneAdjusted.isRecycled()) {
                Canvas canvasTone = new Canvas(toneAdjusted);
                drawSheetDecorations(canvasTone, toneAdjusted.getWidth(), toneAdjusted.getHeight(), sheetW_mm, sheetH_mm, targetMarginMm, targetPaper, targetOrientation, false);
            }
            if (finalRaw != null && !finalRaw.isRecycled() && finalRaw != toneAdjusted) {
                Canvas canvasRaw = new Canvas(finalRaw);
                drawSheetDecorations(canvasRaw, finalRaw.getWidth(), finalRaw.getHeight(), sheetW_mm, sheetH_mm, targetMarginMm, targetPaper, targetOrientation, false);
            }

            mainHandler.post(() -> {
                if (token != previewRenderToken.get()) {
                    if (cleanCopy != null && !cleanCopy.isRecycled()) cleanCopy.recycle();
                    finalRaw.recycle();
                    if (toneAdjusted != null && toneAdjusted != finalRaw && !toneAdjusted.isRecycled()) {
                        toneAdjusted.recycle();
                    }
                    return;
                }

                if (cleanPreviewBitmap != null && !cleanPreviewBitmap.isRecycled()) {
                    cleanPreviewBitmap.recycle();
                }
                cleanPreviewBitmap = cleanCopy;

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

    private void renderEmptySheetPreview() {
        final SheetGeometry.PaperSize targetPaper = (currentPaperSize != null) ? currentPaperSize : SheetGeometry.PaperSize.A4;
        final SheetGeometry.Orientation targetOrientation = currentOrientation;
        final float targetMarginMm = currentMarginMm;
        final int token = previewRenderToken.incrementAndGet();

        previewExecutor.execute(() -> {
            if (token != previewRenderToken.get()) return;

            float sheetW_mm = targetPaper.getWidthMm(targetOrientation);
            float sheetH_mm = targetPaper.getHeightMm(targetOrientation);

            int maxDim = 960;
            int sheetW, sheetH;
            if (sheetW_mm <= sheetH_mm) {
                sheetH = maxDim;
                sheetW = Math.round(maxDim * (sheetW_mm / sheetH_mm));
            } else {
                sheetW = maxDim;
                sheetH = Math.round(maxDim * (sheetH_mm / sheetW_mm));
            }

            Bitmap sheetBm = Bitmap.createBitmap(sheetW, sheetH, Bitmap.Config.ARGB_8888);
            sheetBm.eraseColor(0xFFFFFFFF);
            Canvas canvas = new Canvas(sheetBm);

            drawSheetDecorations(canvas, sheetW, sheetH, sheetW_mm, sheetH_mm, targetMarginMm, targetPaper, targetOrientation, true);

            mainHandler.post(() -> {
                if (token != previewRenderToken.get()) {
                    sheetBm.recycle();
                    return;
                }
                if (cleanPreviewBitmap != null && !cleanPreviewBitmap.isRecycled()) {
                    cleanPreviewBitmap.recycle();
                }
                cleanPreviewBitmap = null;
                if (rawPageBitmap != null && !rawPageBitmap.isRecycled()) {
                    rawPageBitmap.recycle();
                }
                if (toneAdjustedBitmap != null && toneAdjustedBitmap != rawPageBitmap && !toneAdjustedBitmap.isRecycled()) {
                    toneAdjustedBitmap.recycle();
                }
                rawPageBitmap = sheetBm;
                toneAdjustedBitmap = sheetBm;

                if (ivPagePreview != null) {
                    ivPagePreview.setImageBitmap(sheetBm);
                }
            });
        });
    }

    private void drawSheetDecorations(Canvas canvas, int sheetW, int sheetH,
                                     float sheetW_mm, float sheetH_mm, float marginMm,
                                     SheetGeometry.PaperSize paper, SheetGeometry.Orientation orientation,
                                     boolean isEmptyState) {
        float pxPerMmX = (float) sheetW / sheetW_mm;
        float pxPerMmY = (float) sheetH / sheetH_mm;

        // 1. Hardware Non-Printable Margin (4.5 mm)
        int hwPxX = Math.round(SheetGeometry.HARDWARE_MIN_MARGIN_MM * pxPerMmX);
        int hwPxY = Math.round(SheetGeometry.HARDWARE_MIN_MARGIN_MM * pxPerMmY);

        boolean isClipped = SheetGeometry.isHardwareClipped(marginMm);

        Paint grayPaint = new Paint();
        grayPaint.setStyle(Paint.Style.FILL);
        grayPaint.setColor(isClipped ? 0x2AEE3333 : 0x1A000000);

        // Draw the 4 outer edge strips representing the physical drum grip / non-printable edge
        canvas.drawRect(0, 0, sheetW, hwPxY, grayPaint); // Top
        canvas.drawRect(0, sheetH - hwPxY, sheetW, sheetH, grayPaint); // Bottom
        canvas.drawRect(0, hwPxY, hwPxX, sheetH - hwPxY, grayPaint); // Left
        canvas.drawRect(sheetW - hwPxX, hwPxY, sheetW, sheetH - hwPxY, grayPaint); // Right

        // Hairline boundary line for the 4.5mm hardware limit
        Paint hwLinePaint = new Paint();
        hwLinePaint.setStyle(Paint.Style.STROKE);
        hwLinePaint.setStrokeWidth(1.5f);
        hwLinePaint.setColor(isClipped ? 0x99EF4444 : 0x33000000);
        canvas.drawRect(hwPxX, hwPxY, sheetW - hwPxX, sheetH - hwPxY, hwLinePaint);

        // 2. User Active Margin Guideline
        int userPxX = Math.round(marginMm * pxPerMmX);
        int userPxY = Math.round(marginMm * pxPerMmY);

        if (marginMm > 0) {
            Paint userMarginPaint = new Paint();
            userMarginPaint.setStyle(Paint.Style.STROKE);
            userMarginPaint.setStrokeWidth(2.0f);
            userMarginPaint.setPathEffect(new DashPathEffect(new float[]{8f, 6f}, 0f));
            userMarginPaint.setColor(isClipped ? 0xCCEF4444 : 0x700284C7);
            canvas.drawRect(userPxX, userPxY, sheetW - userPxX, sheetH - userPxY, userMarginPaint);
        }

        // 3. Physical Paper Outline
        Paint paperBorderPaint = new Paint();
        paperBorderPaint.setStyle(Paint.Style.STROKE);
        paperBorderPaint.setStrokeWidth(2.0f);
        paperBorderPaint.setColor(0xFF94A3B8);
        canvas.drawRect(0, 0, sheetW, sheetH, paperBorderPaint);

        // 4. If Empty State, draw clean blueprint watermark in sheet center
        if (isEmptyState) {
            Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

            // Icon / Sheet Title
            textPaint.setColor(0xFF334155);
            textPaint.setTextSize(26f);
            String title = (orientation == SheetGeometry.Orientation.PORTRAIT ? "📄 " : "📑 ")
                         + paper.name() + " " + orientation.title.toUpperCase(Locale.US)
                         + " (" + String.format(Locale.US, "%.0f×%.0f mm", sheetW_mm, sheetH_mm) + ")";
            canvas.drawText(title, sheetW / 2.0f, sheetH / 2.0f - 30f, textPaint);

            // Hardware info
            textPaint.setTextSize(18f);
            textPaint.setColor(0xFF64748B);
            float printW = Math.max(0f, sheetW_mm - (2f * marginMm));
            float printH = Math.max(0f, sheetH_mm - (2f * marginMm));
            String specs = String.format(Locale.US, "Printable: %.1f × %.1f mm (4.5mm HW Non-Printable Edge)", printW, printH);
            canvas.drawText(specs, sheetW / 2.0f, sheetH / 2.0f + 10f, textPaint);

            // Subtitle hint
            textPaint.setTextSize(15f);
            textPaint.setColor(0xFF94A3B8);
            canvas.drawText("Ready for document placement", sheetW / 2.0f, sheetH / 2.0f + 45f, textPaint);
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

        sb.append(" • ").append(currentPaperSize.name()).append(" ").append(currentOrientation.title);
        if (currentTilingMode != SheetGeometry.TilingMode.ONE_UP) {
            sb.append(" (").append(currentTilingMode.title).append(")");
        } else if (currentPlacementRegion != SheetGeometry.PlacementRegion.FULL_PAGE) {
            sb.append(" (").append(currentPlacementRegion.title).append(")");
        }

        if (currentMarginMm == 0f) {
            sb.append(" • 0mm (Edge-to-Edge)");
        } else {
            sb.append(String.format(Locale.US, " • %.1fmm margin", currentMarginMm));
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
                if (cleanPreviewBitmap != null && !cleanPreviewBitmap.isRecycled()) cleanPreviewBitmap.recycle();
                if (rawPageBitmap != null && !rawPageBitmap.isRecycled()) rawPageBitmap.recycle();
                if (toneAdjustedBitmap != null && toneAdjustedBitmap != rawPageBitmap && !toneAdjustedBitmap.isRecycled()) {
                    toneAdjustedBitmap.recycle();
                }
            } catch (Exception ignored) {}
            pdfRenderer = null;
            pdfPfd = null;
            cleanPreviewBitmap = null;
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
            DiagnosticLogManager.log("USB", "Cannot print: USB printer not detected.");
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
        if (btnPrintNow != null) btnPrintNow.setEnabled(false);
        if (dockBtnPrint != null) dockBtnPrint.setEnabled(false);
        if (pbPrintProgress != null) {
            pbPrintProgress.setVisibility(View.VISIBLE);
            pbPrintProgress.setProgress(0);
        }
        if (tvPrintStatus != null) tvPrintStatus.setVisibility(View.VISIBLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (viewAnimatedLogo != null) {
            viewAnimatedLogo.setPrintingActive(true);
        }
        if (laserScanView != null) {
            laserScanView.setVisibility(View.VISIBLE);
            laserScanView.startScan();
        }

        printEngine.printPdf(currentPdfFile, darkness, dpi, copies, collate, pagesToPrint, currentPaperSize, currentOrientation, currentPlacementRegion, currentTilingMode, invertDark, watermark, rotation, currentMarginMm, new PrintEngine.PrintProgressListener() {
            @Override
            public void onProgress(int currentPage, int total, String statusMessage) {
                DiagnosticLogManager.log("PRINT", statusMessage);
                runOnUiThread(() -> {
                    if (tvPrintStatus != null) tvPrintStatus.setText(statusMessage);
                    if (dockTvStatus != null) dockTvStatus.setText(statusMessage);
                    if (pbPrintProgress != null && total > 0) {
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
                    if (btnPrintNow != null) btnPrintNow.setEnabled(true);
                    if (dockBtnPrint != null) dockBtnPrint.setEnabled(true);
                    if (pbPrintProgress != null) pbPrintProgress.setVisibility(View.GONE);
                    if (tvPrintStatus != null) tvPrintStatus.setText("Print job finished successfully!");
                    if (dockTvStatus != null) dockTvStatus.setText("Print Complete!");
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
                    if (btnPrintNow != null) btnPrintNow.setEnabled(true);
                    if (dockBtnPrint != null) dockBtnPrint.setEnabled(true);
                    if (pbPrintProgress != null) pbPrintProgress.setVisibility(View.GONE);
                    if (tvPrintStatus != null) tvPrintStatus.setText("Printing error: " + e.getMessage());
                    if (dockTvStatus != null) dockTvStatus.setText("Error: " + e.getMessage());
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
