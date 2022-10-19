package com.nabla.sdk.core.ui.helpers.mediapicker.mimetypedetector;

/**
 * Thrown by MimeTypeDetector when it cannot read bytes.
 *
 * @author Adam Hooper &lt;adam@adamhooper.com&gt;
 */
class GetBytesException extends Exception {
    private static final long serialVersionUID = 1L;

    public GetBytesException(Throwable cause) {
        super(cause);
    }
}
