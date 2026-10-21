package com.dodopress.driver;

/**
 * Supported hardware printer protocol standards in DodoPress Universal Driver Hub.
 */
public enum PrinterProtocol {
    HPLIP_ZJS("HP ZjStream / foo2zjs", "HP Host-Based Laser (M1005, 1020, P1108)", true),
    CANON_CAPT("Canon CAPT Architecture", "Canon Advanced Printing (LBP 2900B, 3000)", false),
    EPSON_ESC_PR("Epson ESC/P-R Raster", "Epson EcoTank & Inkjet (L3150, L3250)", true),
    PCL_6("PCL 6 / PCL-XL Enhanced", "Universal Office Standard (HP, Brother, Ricoh)", true),
    PCL_3_GUI("PCL 3 GUI Inkjet", "HP DeskJet, Ink Tank, OfficeJet Series", true),
    SAMSUNG_SPL("Samsung Smart Print Language", "Samsung & Pantum Compact Laser Series", false),
    GENERIC_PWG("PWG / Apple Raster / Mopria", "Standard Driverless USB/Network Printers", true);

    public final String title;
    public final String description;
    public final boolean isUniversalKernel;

    PrinterProtocol(String title, String description, boolean isUniversalKernel) {
        this.title = title;
        this.description = description;
        this.isUniversalKernel = isUniversalKernel;
    }
}
