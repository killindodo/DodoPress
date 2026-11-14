#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <unistd.h>
#include <pthread.h>
#include <jni.h>
#include "jbig.h"
#include "zjs.h"

#define BE32(x) ((((uint32_t)(x) & 0xFF000000) >> 24) | \
                 (((uint32_t)(x) & 0x00FF0000) >> 8)  | \
                 (((uint32_t)(x) & 0x0000FF00) << 8)  | \
                 (((uint32_t)(x) & 0x000000FF) << 24))

#define BE16(x) ((((uint16_t)(x) & 0xFF00) >> 8) | \
                 (((uint16_t)(x) & 0x00FF) << 8))

static const unsigned char pantum_toner_table[72] = {
    0x00, 0x00, 0x00, 0x51, 0x00, 0x73, 0x00, 0x6e,
    0x00, 0x96, 0x00, 0x7a, 0x00, 0x64, 0x00, 0x96,
    0x00, 0xc8, 0x00, 0x64, 0x00, 0x51, 0x00, 0x64,
    0x00, 0x64, 0x00, 0x64, 0x00, 0x64, 0x00, 0x64,
    0x00, 0x64, 0x00, 0x64, 0x00, 0x64, 0x00, 0x64,
    0x00, 0x64, 0x00, 0x64, 0x00, 0x64, 0x00, 0x64,
    0x00, 0x78, 0x00, 0x87, 0x00, 0x96, 0x00, 0xb4,
    0x00, 0xc8, 0x00, 0x78, 0x00, 0x96, 0x00, 0xbe,
    0x01, 0x5e, 0x01, 0x90, 0x00, 0x64, 0x00, 0x64
};

typedef struct _BIE_CHAIN {
    unsigned char *data;
    size_t len;
    struct _BIE_CHAIN *next;
} BIE_CHAIN;

typedef struct {
    char MediaClass[64];
    char MediaColor[64];
    char MediaType[64];
    char OutputType[64];
    uint32_t AdvanceDistance;
    uint32_t AdvanceMedia;
    uint32_t Collate;
    uint32_t CutMedia;
    uint32_t Duplex;
    uint32_t HWResolution[2];
    uint32_t ImagingBoundingBox[4];
    uint32_t InsertSheet;
    uint32_t Jog;
    uint32_t LeadingEdge;
    uint32_t Margins[2];
    uint32_t ManualFeed;
    uint32_t MediaPosition;
    uint32_t MediaWeight;
    uint32_t MirrorPrint;
    uint32_t NegativePrint;
    uint32_t NumCopies;
    uint32_t Orientation;
    uint32_t OutputFaceUp;
    uint32_t PageSize[2];
    uint32_t Separations;
    uint32_t TraySwitch;
    uint32_t Tumble;
    uint32_t cupsWidth;
    uint32_t cupsHeight;
    uint32_t cupsMediaType;
    uint32_t cupsBitsPerColor;
    uint32_t cupsBitsPerPixel;
    uint32_t cupsBytesPerLine;
    uint32_t cupsColorOrder;
    uint32_t cupsColorSpace;
    uint32_t cupsCompression;
    uint32_t cupsRowCount;
    uint32_t cupsRowFeed;
    uint32_t cupsRowStep;
    uint32_t cupsNumColors;
    float cupsBorderlessScalingFactor;
    float cupsPageSize[2];
    float cupsImagingBBox[4];
    uint32_t cupsInteger[16];
    float cupsReal[16];
    char cupsString[16][64];
    char cupsMarkerType[64];
    char cupsRenderingIntent[64];
    char cupsPageSizeName[64];
} cups_page_header2_t;

typedef struct {
    pthread_t thread;
    int in_fd;
    int out_fd;
    int err_fd;
    volatile int canceled;
    volatile int done;
    int exit_code;
} FilterJob;

static ssize_t write_all(int fd, const void *buf, size_t count, volatile int *canceled) {
    size_t written = 0;
    while (written < count) {
        if (canceled && *canceled) return -1;
        ssize_t n = write(fd, (const char *)buf + written, count - written);
        if (n <= 0) return -1;
        written += n;
    }
    return (ssize_t)written;
}

static ssize_t read_all(int fd, void *buf, size_t count, volatile int *canceled) {
    size_t total = 0;
    while (total < count) {
        if (canceled && *canceled) return -1;
        ssize_t n = read(fd, (char *)buf + total, count - total);
        if (n <= 0) return (total == 0) ? n : (ssize_t)total;
        total += n;
    }
    return (ssize_t)total;
}

static void write_chunk_header(int fd, uint32_t type, uint32_t items, uint32_t payload_size, uint16_t reserved, volatile int *canceled) {
    ZJ_HEADER h;
    h.size = BE32(sizeof(ZJ_HEADER) + payload_size);
    h.type = BE32(type);
    h.items = BE32(items);
    h.reserved = BE16(reserved);
    h.signature = 0x5a5a;
    write_all(fd, &h, sizeof(h), canceled);
}

static void write_item_uint32(int fd, uint16_t item, uint32_t value, volatile int *canceled) {
    ZJ_ITEM_UINT32 it;
    it.header.size = BE32(sizeof(ZJ_ITEM_UINT32));
    it.header.item = BE16(item);
    it.header.type = ZJIT_UINT32;
    it.header.param = 0;
    it.value = BE32(value);
    write_all(fd, &it, sizeof(it), canceled);
}

static void jbig_out_callback(unsigned char *start, size_t len, void *cbarg) {
    BIE_CHAIN **root = (BIE_CHAIN **)cbarg;
    BIE_CHAIN *current;

    BIE_CHAIN *node = (BIE_CHAIN *)malloc(sizeof(BIE_CHAIN));
    if (!node) return;
    node->data = (unsigned char *)malloc(len);
    if (!node->data) { free(node); return; }
    memcpy(node->data, start, len);
    node->len = len;
    node->next = NULL;

    if (*root == NULL) {
        *root = node;
    } else {
        current = *root;
        while (current->next) current = current->next;
        current->next = node;
    }
}

static void free_chain(BIE_CHAIN *chain) {
    while (chain) {
        BIE_CHAIN *next = chain->next;
        if (chain->data) free(chain->data);
        free(chain);
        chain = next;
    }
}

static void pantum_start_doc(int fd, int total_pages, volatile int *canceled) {
    // 1. UEL
    write_all(fd, "\033%-12345X", 9, canceled);
    // 2. Magic JZJZ
    write_all(fd, "JZJZ", 4, canceled);
    // 3. Toner Param Record (88 bytes total: 16 header + 72 data)
    write_chunk_header(fd, 0x11, 0, 72, 0, canceled);
    write_all(fd, pantum_toner_table, sizeof(pantum_toner_table), canceled);
    // 4. START_DOC (52 bytes: 16 header + 36 items)
    write_chunk_header(fd, ZJT_START_DOC, 3, 3 * sizeof(ZJ_ITEM_UINT32), 0x0024, canceled);
    write_item_uint32(fd, 0, (uint32_t)total_pages, canceled); // ZJI_PAGECOUNT
    write_item_uint32(fd, 1, 0, canceled);                     // ZJI_DMCOLLATE
    write_item_uint32(fd, 2, 1, canceled);                     // ZJI_DMDUPLEX
}

static void pantum_start_page(int fd, int paper_code, int copies, int dpi, int width, int height, volatile int *canceled) {
    int num_items = 14;
    write_chunk_header(fd, ZJT_START_PAGE, num_items, num_items * sizeof(ZJ_ITEM_UINT32), 0x00a8, canceled);
    write_item_uint32(fd, 0x0003, (uint32_t)paper_code, canceled); // PAPER_SIZE_PARAM
    write_item_uint32(fd, 0x0004, (uint32_t)copies, canceled);     // COPIES_PARAM
    write_item_uint32(fd, 0x0005, 7, canceled);                    // INPUT_TRAY_PARAM (7=Auto)
    write_item_uint32(fd, 0x0006, 1, canceled);                    // MEDIA_TYPE_PARAM (1=Plain)
    write_item_uint32(fd, 0x0007, 1, canceled);                    // CHANNEL_NUM_PARAM (1=Mono)
    write_item_uint32(fd, 0x0008, (uint32_t)dpi, canceled);        // H_RESOLUTION_PARAM
    write_item_uint32(fd, 0x0009, (uint32_t)dpi, canceled);        // V_RESOLUTION_PARAM
    write_item_uint32(fd, 0x0016, 0, canceled);                    // RET_PARAM
    write_item_uint32(fd, 0x000c, (uint32_t)width, canceled);      // IMG_WIDTH_PARAM
    write_item_uint32(fd, 0x000d, (uint32_t)height, canceled);     // IMG_HEIGHT_PARAM
    write_item_uint32(fd, 0x0010, 1, canceled);                    // VIDEO_DEPTH_PARAM (1 bit)
    write_item_uint32(fd, 0x0011, (uint32_t)width, canceled);      // VIDEO_WIDTH_PARAM
    write_item_uint32(fd, 0x0012, (uint32_t)height, canceled);     // VIDEO_HEIGHT_PARAM
    write_item_uint32(fd, 0xff00, 3, canceled);                    // DENSITY_PARAM (3=Normal)
}

static void pantum_write_page_jbig(int fd, unsigned char *bitmap, int width, int height, volatile int *canceled) {
    struct jbg_enc_state se;
    BIE_CHAIN *chain = NULL;
    unsigned char *bitmaps[1];
    bitmaps[0] = bitmap;

    jbg_enc_init(&se, width, height, 1, bitmaps, jbig_out_callback, &chain);
    jbg_enc_options(&se, JBG_ILEAVE | JBG_SMID,
                    JBG_DELAY_AT | JBG_LRLTWO | JBG_TPDON | JBG_DPON,
                    256, /* L0: 256 lines per stripe */
                    0, 0);
    jbg_enc_out(&se);
    jbg_enc_free(&se);

    if (!chain) return;

    // First node contains BIH (20 bytes)
    BIE_CHAIN *curr = chain;
    if (curr && curr->len >= 20) {
        write_chunk_header(fd, ZJT_JBIG_BIH, 0, 20, 0, canceled);
        write_all(fd, curr->data, 20, canceled);

        size_t rem = curr->len - 20;
        unsigned char *p = curr->data + 20;
        while (rem > 0) {
            size_t chunk = (rem > 65536) ? 65536 : rem;
            write_chunk_header(fd, ZJT_JBIG_BID, 0, (uint32_t)chunk, 0, canceled);
            write_all(fd, p, chunk, canceled);
            p += chunk;
            rem -= chunk;
        }
        curr = curr->next;
    }

    // Subsequent nodes contain BID
    while (curr) {
        size_t rem = curr->len;
        unsigned char *p = curr->data;
        while (rem > 0) {
            size_t chunk = (rem > 65536) ? 65536 : rem;
            write_chunk_header(fd, ZJT_JBIG_BID, 0, (uint32_t)chunk, 0, canceled);
            write_all(fd, p, chunk, canceled);
            p += chunk;
            rem -= chunk;
        }
        curr = curr->next;
    }

    free_chain(chain);

    // End JBIG
    write_chunk_header(fd, ZJT_END_JBIG, 0, 0, 0, canceled);
}

static void pantum_end_page(int fd, volatile int *canceled) {
    write_chunk_header(fd, ZJT_END_PAGE, 0, 0, 0, canceled);
}

static void pantum_end_doc(int fd, volatile int *canceled) {
    write_chunk_header(fd, ZJT_END_DOC, 0, 0, 0, canceled);
    write_all(fd, "\033%-12345X@PJL\n@PJL EOJ\n\033%-12345X", 31, canceled);
}

int pantum_filter_core(int in_fd, int out_fd, int err_fd, volatile int *canceled) {
    // 1. Read sync word (4 bytes)
    uint32_t sync = 0;
    ssize_t n = read_all(in_fd, &sync, 4, canceled);
    if (n < 4) {
        dprintf(err_fd, "pantum: failed to read raster magic\n");
        return 1;
    }

    // RaS3 (0x52615333), RaS2 (0x52615332) -> Big-endian stream
    // 3SaR (0x33536152), 2SaR (0x32536152) -> Little-endian stream
    int stream_is_be = 1;
    if (memcmp(&sync, "RaS3", 4) == 0 || memcmp(&sync, "RaS2", 4) == 0) {
        stream_is_be = 1;
    } else if (memcmp(&sync, "3SaR", 4) == 0 || memcmp(&sync, "2SaR", 4) == 0) {
        stream_is_be = 0;
    }

    int page_count = 0;
    int doc_started = 0;

    cups_page_header2_t hdr;
    while (1) {
        if (canceled && *canceled) break;
        n = read_all(in_fd, &hdr, sizeof(hdr), canceled);
        if (n < (ssize_t)sizeof(hdr)) {
            // End of input stream
            break;
        }

        uint32_t width = stream_is_be ? BE32(hdr.cupsWidth) : hdr.cupsWidth;
        uint32_t height = stream_is_be ? BE32(hdr.cupsHeight) : hdr.cupsHeight;
        uint32_t bytes_per_line = stream_is_be ? BE32(hdr.cupsBytesPerLine) : hdr.cupsBytesPerLine;
        uint32_t paper_code = stream_is_be ? BE32(hdr.cupsInteger[0]) : hdr.cupsInteger[0];
        uint32_t dpi = stream_is_be ? BE32(hdr.cupsInteger[1]) : hdr.cupsInteger[1];
        uint32_t copies = stream_is_be ? BE32(hdr.NumCopies) : hdr.NumCopies;

        if (paper_code == 0) paper_code = 9; // Default A4
        if (dpi == 0) dpi = 600;
        if (copies == 0) copies = 1;
        if (bytes_per_line == 0) bytes_per_line = (width + 7) / 8;

        if (!doc_started) {
            pantum_start_doc(out_fd, 0, canceled);
            doc_started = 1;
        }

        page_count++;
        dprintf(err_fd, "pantum: processing page %d (%ux%u @ %udpi, paper %u)\n",
                page_count, width, height, dpi, paper_code);

        size_t bitmap_size = (size_t)bytes_per_line * height;
        unsigned char *bitmap = (unsigned char *)malloc(bitmap_size);
        if (!bitmap) {
            dprintf(err_fd, "pantum: out of memory allocating bitmap (%zu bytes)\n", bitmap_size);
            return 2;
        }

        ssize_t read_bytes = read_all(in_fd, bitmap, bitmap_size, canceled);
        if (read_bytes < (ssize_t)bitmap_size) {
            dprintf(err_fd, "pantum: short read of raster data (%zd < %zu)\n", read_bytes, bitmap_size);
            free(bitmap);
            break;
        }

        pantum_start_page(out_fd, paper_code, copies, dpi, width, height, canceled);
        pantum_write_page_jbig(out_fd, bitmap, width, height, canceled);
        pantum_end_page(out_fd, canceled);

        free(bitmap);
    }

    if (doc_started) {
        pantum_end_doc(out_fd, canceled);
        dprintf(err_fd, "pantum: completed %d page(s)\n", page_count);
    }

    return 0;
}

int pantum_filter(int in_fd, int out_fd, int err_fd) {
    return pantum_filter_core(in_fd, out_fd, err_fd, NULL);
}

// Standalone entry point
int main(int argc, char **argv) {
    (void)argc;
    (void)argv;
    return pantum_filter(STDIN_FILENO, STDOUT_FILENO, STDERR_FILENO);
}

// JNI Implementation for com.dynamixsoftware.drv.DrvRuntime.libpantum
static void *filter_thread_proc(void *arg) {
    FilterJob *job = (FilterJob *)arg;
    job->exit_code = pantum_filter_core(job->in_fd, job->out_fd, job->err_fd, &job->canceled);
    close(job->in_fd);
    close(job->out_fd);
    close(job->err_fd);
    job->done = 1;
    return NULL;
}

JNIEXPORT jlong JNICALL Java_com_dynamixsoftware_drv_DrvRuntime_00024libpantum_procExec(
    JNIEnv *env, jobject thiz, jobjectArray args, jobjectArray envp, jstring workDir, jintArray pipes) {
    (void)thiz;
    (void)args;
    (void)envp;
    (void)workDir;

    int p_in[2];  // Java writes to p_in[1], filter reads from p_in[0]
    int p_out[2]; // filter writes to p_out[1], Java reads from p_out[0]
    int p_err[2]; // filter writes to p_err[1], Java reads from p_err[0]

    if (pipe(p_in) != 0 || pipe(p_out) != 0 || pipe(p_err) != 0) {
        return 0;
    }

    jint pipe_fds[3];
    pipe_fds[0] = p_in[1];  // Java stdin fd
    pipe_fds[1] = p_out[0]; // Java stdout fd
    pipe_fds[2] = p_err[0]; // Java stderr fd
    (*env)->SetIntArrayRegion(env, pipes, 0, 3, pipe_fds);

    FilterJob *job = (FilterJob *)malloc(sizeof(FilterJob));
    if (!job) return 0;
    memset(job, 0, sizeof(FilterJob));
    job->in_fd = p_in[0];
    job->out_fd = p_out[1];
    job->err_fd = p_err[1];

    if (pthread_create(&job->thread, NULL, filter_thread_proc, job) != 0) {
        close(p_in[0]); close(p_in[1]);
        close(p_out[0]); close(p_out[1]);
        close(p_err[0]); close(p_err[1]);
        free(job);
        return 0;
    }

    return (jlong)(intptr_t)job;
}

JNIEXPORT jint JNICALL Java_com_dynamixsoftware_drv_DrvRuntime_00024libpantum_procWait(
    JNIEnv *env, jobject thiz, jlong handle) {
    (void)env;
    (void)thiz;
    if (handle == 0) return -1;
    FilterJob *job = (FilterJob *)(intptr_t)handle;
    pthread_join(job->thread, NULL);
    int code = job->exit_code;
    free(job);
    return code;
}

JNIEXPORT void JNICALL Java_com_dynamixsoftware_drv_DrvRuntime_00024libpantum_procDestroy(
    JNIEnv *env, jobject thiz, jlong handle) {
    (void)env;
    (void)thiz;
    if (handle == 0) return;
    FilterJob *job = (FilterJob *)(intptr_t)handle;
    job->canceled = 1;
    // Closing in_fd interrupts any blocking read in the worker
    close(job->in_fd);
    pthread_join(job->thread, NULL);
    free(job);
}
