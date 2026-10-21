package com.dodopress.driver;

import java.io.Serializable;

/**
 * Hardware printer specification model representing supported brands and models in DodoPress.
 */
public class PrinterModel implements Serializable {
    public final String id;
    public final String brand;
    public final String modelName;
    public final PrinterProtocol protocol;
    public final int vendorId;
    public final int[] productIds;
    public final String driverPackFileName;
    public final String downloadUrl;
    public final int downloadSizeKb;
    public final String hardwareSpecs;
    public final boolean isBuiltIn;

    public PrinterModel(
            String id,
            String brand,
            String modelName,
            PrinterProtocol protocol,
            int vendorId,
            int[] productIds,
            String driverPackFileName,
            String downloadUrl,
            int downloadSizeKb,
            String hardwareSpecs,
            boolean isBuiltIn
    ) {
        this.id = id;
        this.brand = brand;
        this.modelName = modelName;
        this.protocol = protocol;
        this.vendorId = vendorId;
        this.productIds = productIds;
        this.driverPackFileName = driverPackFileName;
        this.downloadUrl = downloadUrl;
        this.downloadSizeKb = downloadSizeKb;
        this.hardwareSpecs = hardwareSpecs;
        this.isBuiltIn = isBuiltIn;
    }

    public boolean matchesUsb(int vid, int pid) {
        if (this.vendorId != vid) return false;
        if (this.productIds == null || this.productIds.length == 0) return true;
        for (int p : this.productIds) {
            if (p == pid) return true;
        }
        return false;
    }

    public String getDisplayName() {
        return brand + " " + modelName;
    }
}
