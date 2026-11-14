package com.dynamixsoftware.drv;

public abstract class DrvRuntime {
    public static final class libhplip extends DrvRuntime {
        static {
            System.loadLibrary("hplip");
        }

        @Override
        public native void procDestroy(long handle);

        @Override
        public native long procExec(String[] args, String[] env, String workDir, int[] pipes);

        @Override
        public native int procWait(long handle);
    }

    public static final class libpantum extends DrvRuntime {
        private static volatile boolean isLoaded = false;

        static {
            try {
                System.loadLibrary("pantum");
                isLoaded = true;
            } catch (Throwable ignored) {
            }
        }

        public static void ensureLoaded(String explicitPath) {
            if (!isLoaded && explicitPath != null) {
                try {
                    System.load(explicitPath);
                    isLoaded = true;
                } catch (Throwable ignored) {
                }
            }
        }

        @Override
        public native void procDestroy(long handle);

        @Override
        public native long procExec(String[] args, String[] env, String workDir, int[] pipes);

        @Override
        public native int procWait(long handle);
    }

    public abstract void procDestroy(long handle);
    public abstract long procExec(String[] args, String[] env, String workDir, int[] pipes);
    public abstract int procWait(long handle);
}
