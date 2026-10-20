package com.dodopress.cups;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CupsRasterWriter {
    public static final int CUPS_RASTER_SYNC_V3 = 0x52615333; // "RaS3"
    public static final int COLOR_SPACE_GRAY = 0;
    public static final int COLOR_SPACE_RGB = 1;

    public static void writeFileHeader(OutputStream out) throws IOException {
        writeInt(out, CUPS_RASTER_SYNC_V3);
    }

    public static void writePageHeader(OutputStream out, int width, int height, int dpi, int colorSpace, int pageWidthPts, int pageHeightPts) throws IOException {
        int bitsPerColor = 8;
        int numColors = (colorSpace == COLOR_SPACE_RGB) ? 3 : 1;
        int bitsPerPixel = bitsPerColor * numColors;
        int bytesPerLine = width * numColors;

        int paperCode = 9; // A4 default
        String pageSizeName = "A4";
        if (pageWidthPts > 600 && pageHeightPts > 950) {
            paperCode = 5; // Legal
            pageSizeName = "Legal";
        } else if (pageWidthPts > 600) {
            paperCode = 1; // Letter
            pageSizeName = "Letter";
        }

        writeString(out, ""); // MediaClass
        writeString(out, ""); // MediaColor
        writeString(out, "Plain"); // MediaType
        writeString(out, "0"); // OutputType
        writeInt(out, 0); // AdvanceDistance
        writeInt(out, 0); // AdvanceMedia
        writeInt(out, 0); // Collate
        writeInt(out, 0); // CutMedia
        writeInt(out, 0); // Duplex
        writeInt(out, dpi); // HWResolution[0]
        writeInt(out, dpi); // HWResolution[1]
        writeInt(out, 0); // ImagingBBox[0]
        writeInt(out, 0); // ImagingBBox[1]
        writeInt(out, pageWidthPts); // ImagingBBox[2]
        writeInt(out, pageHeightPts); // ImagingBBox[3]
        writeInt(out, 0); // InsertSheet
        writeInt(out, 0); // Jog
        writeInt(out, 0); // LeadingEdge
        writeInt(out, 0); // Margins[0]
        writeInt(out, 0); // Margins[1]
        writeInt(out, 0); // ManualFeed
        writeInt(out, 7); // MediaPosition (7 = Auto)
        writeInt(out, 0); // MediaWeight
        writeInt(out, 0); // MirrorPrint
        writeInt(out, 0); // NegativePrint
        writeInt(out, 1); // NumCopies
        writeInt(out, 0); // Orientation
        writeInt(out, 0); // OutputFaceUp
        writeInt(out, pageWidthPts); // PageSize[0]
        writeInt(out, pageHeightPts); // PageSize[1]
        writeInt(out, 0); // Separations
        writeInt(out, 0); // TraySwitch
        writeInt(out, 0); // Tumble
        writeInt(out, width); // cupsWidth
        writeInt(out, height); // cupsHeight
        writeInt(out, 0); // cupsMediaType
        writeInt(out, bitsPerColor); // cupsBitsPerColor
        writeInt(out, bitsPerPixel); // cupsBitsPerPixel
        writeInt(out, bytesPerLine); // cupsBytesPerLine
        writeInt(out, 0); // cupsColorOrder (0 = chunky)
        writeInt(out, colorSpace); // cupsColorSpace (1 = RGB, 0 = Gray)
        writeInt(out, 0); // cupsCompression
        writeInt(out, 1); // cupsRowCount
        writeInt(out, 0); // cupsRowFeed
        writeInt(out, 2); // cupsRowStep
        writeInt(out, numColors); // cupsNumColors
        writeFloat(out, 1.0f); // cupsBorderlessScalingFactor
        writeFloat(out, (float) pageWidthPts); // cupsPageSize[0]
        writeFloat(out, (float) pageHeightPts); // cupsPageSize[1]
        writeFloat(out, 0f); // cupsImagingBBox[0]
        writeFloat(out, 0f); // cupsImagingBBox[1]
        writeFloat(out, (float) pageWidthPts); // cupsImagingBBox[2]
        writeFloat(out, (float) pageHeightPts); // cupsImagingBBox[3]

        // 16 integer options (cupsInteger0..15)
        writeInt(out, paperCode); // cupsInteger0: 9=A4, 1=Letter, 5=Legal
        writeInt(out, dpi);       // cupsInteger1: 600 or 300 DPI
        for (int i = 2; i < 16; i++) {
            writeInt(out, 0);
        }
        // 16 real options (cupsReal0..15)
        for (int i = 0; i < 16; i++) {
            writeFloat(out, 0f);
        }
        // 16 string options (cupsString0..15) (64 bytes each)
        for (int i = 0; i < 16; i++) {
            writeString(out, "");
        }
        writeString(out, ""); // cupsMarkerType
        writeString(out, ""); // cupsRenderingIntent
        writeString(out, pageSizeName); // cupsPageSizeName
    }

    public static void writeInt(OutputStream out, int value) throws IOException {
        out.write((value >>> 24) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    public static void writeFloat(OutputStream out, float value) throws IOException {
        writeInt(out, Float.floatToIntBits(value));
    }

    public static void writeString(OutputStream out, String s) throws IOException {
        byte[] bytes = new byte[64];
        if (s != null) {
            byte[] strBytes = s.getBytes(StandardCharsets.US_ASCII);
            int len = Math.min(strBytes.length, 63);
            System.arraycopy(strBytes, 0, bytes, 0, len);
        }
        out.write(bytes);
    }
}
