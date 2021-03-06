package me.xiaopan.sketchsample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Environment;
import android.text.format.Formatter;

import com.tencent.bugly.crashreport.CrashReport;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import me.xiaopan.sketch.SketchMonitor;
import me.xiaopan.sketch.drawable.RefDrawable;
import me.xiaopan.sketch.feature.large.Tile;
import me.xiaopan.sketch.process.ImageProcessor;
import me.xiaopan.sketch.request.DisplayRequest;
import me.xiaopan.sketch.request.DownloadRequest;
import me.xiaopan.sketch.request.LoadRequest;
import me.xiaopan.sketch.request.UriScheme;
import me.xiaopan.sketch.util.SketchUtils;
import me.xiaopan.sketch.util.UnableCreateDirException;
import me.xiaopan.sketch.util.UnableCreateFileException;

class SampleSketchMonitor extends SketchMonitor {

    private static final int INSTALL_FAILED_RETRY_TIME_INTERVAL = 30 * 60 * 1000;

    private Context context;
    private long lastUploadInstallFailedTime;
    private long lastUploadDecodeNormalImageFailedTime;
    private long lastUploadDecodeGifImageFailedTime;
    private long lastUploadProcessImageFailedTime;
    private boolean uploadDecodeGifImageFailed;

    public SampleSketchMonitor(Context context) {
        super(context);
        this.context = context.getApplicationContext();
        logName = "SampleSketchMonitor";
    }

    @Override
    public void onDecodeGifImageError(Throwable throwable, LoadRequest request, int outWidth, int outHeight, String outMimeType) {
        super.onDecodeGifImageError(throwable, request, outWidth, outHeight, outMimeType);

        boolean notFoundSoFile = throwable instanceof UnsatisfiedLinkError || throwable instanceof ExceptionInInitializerError;
        if (notFoundSoFile) {
            // 如果是找不到so文件异常，那么每次运行只上报一次
            if (uploadDecodeGifImageFailed) {
                return;
            }
            uploadDecodeGifImageFailed = true;
        } else {
            // 其它异常每半小时上报一次
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUploadDecodeGifImageFailedTime < INSTALL_FAILED_RETRY_TIME_INTERVAL) {
                return;
            }
            lastUploadDecodeGifImageFailedTime = currentTime;
        }

        StringBuilder builder = new StringBuilder();

        builder.append("Sketch")
                .append(" - ").append("DecodeGifImageFailed")
                .append(" - ").append(throwable.getClass().getSimpleName())
                .append(" - ").append(decodeUri(context, request.getUri()));

        builder.append("\n").append("exceptionMessage: ").append(throwable.getMessage());

        if (notFoundSoFile) {
            if (Build.VERSION.SDK_INT >= 21) {
                builder.append("\n").append("abiInfo: ").append(Arrays.toString(Build.SUPPORTED_ABIS));
            } else {
                builder.append("\n").append("abiInfo: ").append("abi1=").append(Build.CPU_ABI).append(", abi2=").append(Build.CPU_ABI2);
            }
        }

        if (throwable instanceof OutOfMemoryError) {
            long maxMemory = Runtime.getRuntime().maxMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long totalMemory = Runtime.getRuntime().totalMemory();
            String maxMemoryFormatted = Formatter.formatFileSize(this.context, maxMemory);
            String freeMemoryFormatted = Formatter.formatFileSize(this.context, freeMemory);
            String totalMemoryFormatted = Formatter.formatFileSize(this.context, totalMemory);
            builder.append("\n")
                    .append("memoryInfo: ")
                    .append("maxMemory=").append(maxMemoryFormatted)
                    .append(", freeMemory=").append(freeMemoryFormatted)
                    .append(", totalMemory=").append(totalMemoryFormatted);
        }

        builder.append("\n")
                .append("imageInfo: ")
                .append("outWidth=").append(outWidth)
                .append(", outHeight=").append(outHeight)
                .append(", outMimeType=").append(outMimeType);

        CrashReport.postCatchedException(new Exception(builder.toString(), throwable));
    }

    @Override
    public void onDecodeNormalImageError(Throwable throwable, LoadRequest request, int outWidth, int outHeight, String outMimeType) {
        super.onDecodeNormalImageError(throwable, request, outWidth, outHeight, outMimeType);

        // 每半小时上报一次
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUploadDecodeNormalImageFailedTime < INSTALL_FAILED_RETRY_TIME_INTERVAL) {
            return;
        }
        lastUploadDecodeNormalImageFailedTime = currentTime;

        StringBuilder builder = new StringBuilder();

        builder.append("Sketch")
                .append(" - ").append("DecodeNormalImageFailed")
                .append(" - ").append(throwable.getClass().getSimpleName())
                .append(" - ").append(decodeUri(context, request.getUri()));

        builder.append("\n").append("exceptionMessage: ").append(throwable.getMessage());

        if (throwable instanceof OutOfMemoryError) {
            long maxMemory = Runtime.getRuntime().maxMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long totalMemory = Runtime.getRuntime().totalMemory();
            String maxMemoryFormatted = Formatter.formatFileSize(this.context, maxMemory);
            String freeMemoryFormatted = Formatter.formatFileSize(this.context, freeMemory);
            String totalMemoryFormatted = Formatter.formatFileSize(this.context, totalMemory);
            builder.append("\n").append("memoryInfo: ")
                    .append("maxMemory=").append(maxMemoryFormatted)
                    .append(", freeMemory=").append(freeMemoryFormatted)
                    .append(", totalMemory=").append(totalMemoryFormatted);
        }

        builder.append("\n").append("imageInfo: ")
                .append("outWidth=").append(outWidth)
                .append(", outHeight=").append(outHeight)
                .append(", outMimeType=").append(outMimeType);

        CrashReport.postCatchedException(new Exception(builder.toString(), throwable));
    }

    @Override
    public void onInstallDiskCacheError(Exception e, File cacheDir) {
        super.onInstallDiskCacheError(e, cacheDir);

        // 每半小时上传一次
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUploadInstallFailedTime < INSTALL_FAILED_RETRY_TIME_INTERVAL) {
            return;
        }
        lastUploadInstallFailedTime = currentTime;

        StringBuilder builder = new StringBuilder();

        builder.append("Sketch")
                .append(" - ").append("InstallDiskCacheFailed");
        if (e instanceof UnableCreateDirException) {
            builder.append(" - ").append("UnableCreateDirException");
        } else if (e instanceof UnableCreateFileException) {
            builder.append(" - ").append("UnableCreateFileException");
        } else {
            builder.append(" - ").append(e.getClass().getSimpleName());
        }
        builder.append(" - ").append(cacheDir.getPath());

        builder.append("\n").append("exceptionMessage: ").append(e.getMessage());

        String sdcardState = Environment.getExternalStorageState();
        builder.append("\n").append("sdcardState: ").append(sdcardState);

        if (Environment.MEDIA_MOUNTED.equals(sdcardState)) {
            File sdcardDir = Environment.getExternalStorageDirectory();
            long totalBytes = SketchUtils.getTotalBytes(sdcardDir);
            long availableBytes = SketchUtils.getAvailableBytes(sdcardDir);
            builder.append("\n")
                    .append("sdcardSize: ")
                    .append(Formatter.formatFileSize(context, availableBytes))
                    .append("/")
                    .append(Formatter.formatFileSize(context, totalBytes));
        }

        CrashReport.postCatchedException(new Exception(builder.toString(), e));
    }

    @Override
    public void onProcessImageError(Throwable e, String imageUri, ImageProcessor processor) {
        super.onProcessImageError(e, imageUri, processor);

        // 每半小时上报一次
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUploadProcessImageFailedTime < INSTALL_FAILED_RETRY_TIME_INTERVAL) {
            return;
        }
        lastUploadProcessImageFailedTime = currentTime;

        String outOfMemoryInfo = e instanceof OutOfMemoryError ? String.format("\nmemoryState: %s", getSystemState()) : "";
        String log = String.format("Sketch - %s - %s" +
                        "\nexceptionMessage: %s" +
                        "%s",
                processor.getKey(),
                decodeUri(context, imageUri),
                e.getMessage(),
                outOfMemoryInfo);

        CrashReport.postCatchedException(new Exception(log, e));
    }

    @Override
    public void onDownloadError(final DownloadRequest request, Throwable throwable) {
        super.onDownloadError(request, throwable);


    }

    @Override
    public void onTileSortError(IllegalArgumentException e, List<Tile> tileList, boolean useLegacyMergeSort) {
        super.onTileSortError(e, tileList, useLegacyMergeSort);

        String log = String.format("Sketch - TileSortError - %s " +
                        "\ntiles: %s",
                useLegacyMergeSort ? "useLegacyMergeSort. " : "",
                SketchUtils.tileListToString(tileList));

        CrashReport.postCatchedException(new Exception(log, e));
    }

    @Override
    public void onBitmapRecycledOnDisplay(DisplayRequest request, RefDrawable refDrawable) {
        super.onBitmapRecycledOnDisplay(request, refDrawable);

        String log = String.format("Sketch - BitmapRecycledOnDisplay - %s " +
                        "\ndrawable: %s",
                decodeUri(context, request.getUri()),
                refDrawable.getInfo());

        CrashReport.postCatchedException(new Exception(log));
    }

    @Override
    public void onInBitmapExceptionForRegionDecoder(String imageUri, int imageWidth, int imageHeight, Rect srcRect, int inSampleSize, Bitmap inBitmap) {
        super.onInBitmapExceptionForRegionDecoder(imageUri, imageWidth, imageHeight, srcRect, inSampleSize, inBitmap);

        String log = String.format("Sketch - InBitmapExceptionForRegionDecoder - %s" +
                        "\nimageSize：%dx%d" +
                        "\nsrcRect：%s" +
                        "\ninSampleSize：%d" +
                        "\ninBitmap：%dx%d, %d, %s" +
                        "\nsystemState：%s",
                decodeUri(context, imageUri),
                imageWidth, imageHeight,
                srcRect.toString(),
                inSampleSize,
                inBitmap.getWidth(), inBitmap.getHeight(),
                SketchUtils.getByteCount(inBitmap),
                inBitmap.getConfig(),
                getSystemState());

        CrashReport.postCatchedException(new Exception(log));
    }

    @Override
    public void onInBitmapException(String imageUri, int imageWidth, int imageHeight, int inSampleSize, Bitmap inBitmap) {
        super.onInBitmapException(imageUri, imageWidth, imageHeight, inSampleSize, inBitmap);

        String log = String.format("Sketch - InBitmapException - %s" +
                        "\nimageSize：%dx%d" +
                        "\ninSampleSize：%d" +
                        "\ninBitmap：%dx%d, %d, %s" +
                        "\nsystemState：%s",
                decodeUri(context, imageUri),
                imageWidth, imageHeight,
                inSampleSize,
                inBitmap.getWidth(), inBitmap.getHeight(),
                SketchUtils.getByteCount(inBitmap),
                inBitmap.getConfig(),
                getSystemState());

        CrashReport.postCatchedException(new Exception(log));
    }

    private String decodeUri(Context context, String imageUri) {
        UriScheme scheme = UriScheme.valueOfUri(imageUri);
        if (scheme != null && scheme == UriScheme.DRAWABLE) {
            try {
                int resId = Integer.parseInt(UriScheme.DRAWABLE.crop(imageUri));
                return context.getResources().getResourceName(resId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return imageUri;
    }

    private String getSystemInfo() {
        return String.format(Locale.getDefault(), "%s, %d", Build.MODEL, Build.VERSION.SDK_INT);
    }

    private String getMemoryInfo() {
        String freeMemory = Formatter.formatFileSize(context, Runtime.getRuntime().freeMemory());
        String maxMemory = Formatter.formatFileSize(context, Runtime.getRuntime().maxMemory());
        return String.format("%s/%s", freeMemory, maxMemory);
    }

    private String getSystemState() {
        return String.format(Locale.getDefault(), "%s, %s", getSystemInfo(), getMemoryInfo());
    }
}
