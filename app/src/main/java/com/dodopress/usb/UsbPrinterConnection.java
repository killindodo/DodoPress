package com.dodopress.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

public class UsbPrinterConnection {
    public static final String ACTION_USB_PERMISSION = "com.dodopress.USB_PERMISSION";
    public static final int USB_CLASS_PRINTER = 7;
    public static final int HP_VENDOR_ID = 1008; // 0x03F0
    public static final int HP_M1005_PRODUCT_ID = 15127; // 0x3B17

    private final Context context;
    private final UsbManager usbManager;
    private UsbDevice device;
    private UsbInterface printerInterface;
    private UsbEndpoint endpointOut;
    private UsbEndpoint endpointIn;
    private UsbDeviceConnection connection;

    public UsbPrinterConnection(Context context) {
        this.context = context.getApplicationContext();
        this.usbManager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
    }

    public UsbDevice findPrinter() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for (UsbDevice dev : deviceList.values()) {
            if (isPrinterDevice(dev)) {
                this.device = dev;
                return dev;
            }
        }
        return null;
    }

    public boolean isPrinterDevice(UsbDevice dev) {
        if (dev.getVendorId() == HP_VENDOR_ID) {
            return true;
        }
        for (int i = 0; i < dev.getInterfaceCount(); i++) {
            UsbInterface iface = dev.getInterface(i);
            if (iface.getInterfaceClass() == USB_CLASS_PRINTER) {
                return true;
            }
        }
        return false;
    }

    public String getPrinterDisplayName(UsbDevice dev) {
        if (dev == null) return "No Printer Connected";

        if (dev.getVendorId() == HP_VENDOR_ID && dev.getProductId() == HP_M1005_PRODUCT_ID) {
            return "HP LaserJet M1005 MFP";
        }
        String name = dev.getProductName();
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        if (dev.getVendorId() == HP_VENDOR_ID) {
            return "HP LaserJet USB Printer";
        }
        return "USB Printer (Class 7)";
    }

    public boolean hasPermission(UsbDevice dev) {
        return dev != null && usbManager.hasPermission(dev);
    }

    public void requestPermission(UsbDevice dev, Runnable onGranted, Runnable onDenied) {
        this.device = dev;
        PermissionReceiver receiver = new PermissionReceiver(this, onGranted, onDenied);

        Intent intent = new Intent(ACTION_USB_PERMISSION);
        intent.setPackage(context.getPackageName());

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 31) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, flags);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter);
        }

        usbManager.requestPermission(dev, pi);
    }

    public void open() throws IOException {
        if (device == null) {
            findPrinter();
        }
        if (device == null) {
            throw new IOException("No USB printer found. Please connect via USB OTG cable.");
        }
        if (!usbManager.hasPermission(device)) {
            throw new IOException("USB permission not granted. Please approve USB access.");
        }

        connection = usbManager.openDevice(device);
        if (connection == null) {
            throw new IOException("Unable to open USB connection to printer.");
        }

        endpointOut = null;
        endpointIn = null;
        printerInterface = null;

        UsbInterface selectedInterface = null;
        UsbEndpoint selectedOut = null;
        UsbEndpoint selectedIn = null;

        // Pass 1: Prioritize standard USB_CLASS_PRINTER (Class 7) interface
        // Critical for MFPs (like HP LaserJet M1005 MFP) where Interface 0 is Scanner and Interface 1 is Printer!
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface iface = device.getInterface(i);
            Log.d("DodoPress-USB", "Inspect interface " + i + ": class=" + iface.getInterfaceClass()
                + ", subclass=" + iface.getInterfaceSubclass() + ", protocol=" + iface.getInterfaceProtocol()
                + ", endpoints=" + iface.getEndpointCount());

            if (iface.getInterfaceClass() == USB_CLASS_PRINTER) {
                UsbEndpoint out = null;
                UsbEndpoint in = null;
                for (int e = 0; e < iface.getEndpointCount(); e++) {
                    UsbEndpoint ep = iface.getEndpoint(e);
                    if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.getDirection() == UsbConstants.USB_DIR_OUT && out == null) {
                            out = ep;
                        } else if (ep.getDirection() == UsbConstants.USB_DIR_IN && in == null) {
                            in = ep;
                        }
                    }
                }
                if (out != null) {
                    selectedInterface = iface;
                    selectedOut = out;
                    selectedIn = in;
                    Log.i("DodoPress-USB", "Successfully selected PRINTER class interface " + i + " (OUT endpoint: " + out.getEndpointNumber() + ")");
                    break;
                }
            }
        }

        // Pass 2: Fallback to any interface with bulk OUT endpoint if no class 7 interface found
        if (selectedInterface == null) {
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                UsbInterface iface = device.getInterface(i);
                UsbEndpoint out = null;
                UsbEndpoint in = null;
                for (int e = 0; e < iface.getEndpointCount(); e++) {
                    UsbEndpoint ep = iface.getEndpoint(e);
                    if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.getDirection() == UsbConstants.USB_DIR_OUT && out == null) {
                            out = ep;
                        } else if (ep.getDirection() == UsbConstants.USB_DIR_IN && in == null) {
                            in = ep;
                        }
                    }
                }
                if (out != null) {
                    selectedInterface = iface;
                    selectedOut = out;
                    selectedIn = in;
                    Log.w("DodoPress-USB", "Fallback: Selected non-printer interface " + i + " (OUT endpoint: " + out.getEndpointNumber() + ")");
                    break;
                }
            }
        }

        if (selectedInterface == null || selectedOut == null) {
            connection.close();
            connection = null;
            throw new IOException("Could not find printer bulk OUT endpoint.");
        }

        printerInterface = selectedInterface;
        endpointOut = selectedOut;
        endpointIn = selectedIn;

        if (!connection.claimInterface(printerInterface, true)) {
            connection.close();
            connection = null;
            throw new IOException("Could not claim USB printer interface.");
        }

        if (printerInterface.getAlternateSetting() > 0) {
            connection.setInterface(printerInterface);
        }
    }

    public void writeBulk(byte[] data, int offset, int length) throws IOException {
        if (connection == null || endpointOut == null) {
            throw new IOException("USB connection is not open.");
        }
        int sent = 0;
        while (sent < length) {
            int toSend = Math.min(length - sent, 16384);
            int transferred = connection.bulkTransfer(endpointOut, data, offset + sent, toSend, 15000);
            if (transferred < 0) {
                throw new IOException("USB bulk transfer failed (code " + transferred + ").");
            }
            sent += transferred;
        }
    }

    public OutputStream getOutputStream() {
        return new BulkOutputStream(this);
    }

    public void close() {
        try {
            if (connection != null) {
                if (printerInterface != null) {
                    connection.releaseInterface(printerInterface);
                }
                connection.close();
            }
        } catch (Exception ignored) {
        } finally {
            connection = null;
            printerInterface = null;
            endpointOut = null;
            endpointIn = null;
        }
    }

    public UsbDevice getDevice() {
        return device;
    }

    public static class PermissionReceiver extends BroadcastReceiver {
        private final UsbPrinterConnection parent;
        private final Runnable onGranted;
        private final Runnable onDenied;

        public PermissionReceiver(UsbPrinterConnection parent, Runnable onGranted, Runnable onDenied) {
            this.parent = parent;
            this.onGranted = onGranted;
            this.onDenied = onDenied;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                try {
                    context.unregisterReceiver(this);
                } catch (Exception ignored) {}
                UsbDevice d = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (d != null) {
                    parent.device = d;
                }
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                if (granted) {
                    if (onGranted != null) onGranted.run();
                } else {
                    if (onDenied != null) onDenied.run();
                }
            }
        }
    }

    public static class BulkOutputStream extends OutputStream {
        private final UsbPrinterConnection conn;
        private final byte[] single = new byte[1];

        public BulkOutputStream(UsbPrinterConnection conn) {
            this.conn = conn;
        }

        @Override
        public void write(int b) throws IOException {
            single[0] = (byte) b;
            write(single, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            int written = 0;
            while (written < len) {
                int chunk = Math.min(len - written, 16384);
                conn.writeBulk(b, off + written, chunk);
                written += chunk;
            }
        }
    }
}
