package com.sample.compressor;

public class CompressorConstant {

    public static final String EXTRA_FILE_LINK = "fileLink";
    /** For uploads, returns upload status string. */
    public static final String EXTRA_STATUS = "status";
    /** Indicates an upload completed successfully. */
    public static final String STATUS_COMPLETE = "complete";
    /** Indicates an upload failed. */
    public static final String STATUS_FAILED = "failed";

    /** Action for upload broadcast intent filter. */
    public static final String BROADCAST_COMPRESS_UPLOAD = "com.sample.compressor.android.BROADCAST_UPLOAD";

    /** Action for upload broadcast intent filter. */
    public static final String BROADCAST_STOPPED = "com.android.ServiceStopped";

}
