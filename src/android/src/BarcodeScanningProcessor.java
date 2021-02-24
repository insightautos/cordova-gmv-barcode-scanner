package com.mobisys.cordova.plugins.mlkit.barcode.scanner;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.common.InputImage;

import java.nio.ByteBuffer;
import java.util.List;

import androidx.annotation.GuardedBy;
import androidx.annotation.UiThread;

public class BarcodeScanningProcessor {
    private static final String TAG = "Barcode-Processor";

    private final BarcodeScanner _Detector;
    private final BarcodeUpdateListener _BarcodeUpdateListener;
    // To keep the latest images and its metadata.
    @GuardedBy("this")
    private InputImage _LatestImage;
    // To keep the images and metadata in process.
    @GuardedBy("this")
    private InputImage _ProcessingImage;

    public BarcodeScanningProcessor(BarcodeScanner p_BarcodeDetector, Context p_Context)
    {
        _Detector = p_BarcodeDetector;

        if (p_Context instanceof BarcodeUpdateListener)
        {
            _BarcodeUpdateListener = (BarcodeUpdateListener) p_Context;
        }
        else
        {
            throw new RuntimeException("Hosting activity must implement BarcodeUpdateListener");
        }
    }

    public synchronized void Process(InputImage p_Image) {
        _LatestImage = p_Image;

        if (_ProcessingImage == null) {
            ProcessLatestImage();
        }
    }

    public void Stop() {
        _Detector.close();
    }

    private synchronized void ProcessLatestImage() {
        _ProcessingImage = _LatestImage;

        _LatestImage = null;

        if (_ProcessingImage != null)
        {
            ProcessImage(_ProcessingImage);
        }
    }

    private void ProcessImage(final InputImage p_Image) {
        DetectInVisionImage(p_Image);
    }

    private void DetectInVisionImage(InputImage p_Image) {
        _Detector.process(p_Image)
                .addOnSuccessListener(results -> {
                    OnSuccess(results);
                    ProcessLatestImage();
                })
                .addOnFailureListener(e -> OnFailure(e));
    }

    private void OnSuccess(List<Barcode> p_Barcodes) {
        for (Barcode barcode : p_Barcodes) {
            _BarcodeUpdateListener.onBarcodeDetected(barcode);
        }
    }

    private void OnFailure(Exception e) {
        Log.e(TAG, "Barcode detection failed " + e);
    }

    public interface BarcodeUpdateListener {
        @UiThread
        void onBarcodeDetected(Barcode p_Barcode);
    }
}
