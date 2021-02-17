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
    private ByteBuffer _LatestImage;
    @GuardedBy("this")
    private InputImage _LatestImageMetaData;
    // To keep the images and metadata in process.
    @GuardedBy("this")
    private ByteBuffer _ProcessingImage;
    @GuardedBy("this")
    private InputImage _ProcessingMetaData;

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

    public synchronized void Process(ByteBuffer p_Data, InputImage p_FrameMetadata) {
        _LatestImage = p_Data;
        _LatestImageMetaData = p_FrameMetadata;

        if (_ProcessingImage == null && _ProcessingMetaData == null) {
            ProcessLatestImage();
        }
    }

    public void Stop() {
        _Detector.close();
    }

    private synchronized void ProcessLatestImage() {
        _ProcessingImage = _LatestImage;
        _ProcessingMetaData = _LatestImageMetaData;

        _LatestImage = null;
        _LatestImageMetaData = null;

        if (_ProcessingImage != null && _ProcessingMetaData != null)
        {
            ProcessImage(_ProcessingImage, _ProcessingMetaData);
        }
    }

    private void ProcessImage(ByteBuffer p_Data, final InputImage p_FrameMetadata) {
        InputImage image = InputImage.fromByteBuffer(p_Data, p_FrameMetadata.getWidth(), p_FrameMetadata.getHeight(), p_FrameMetadata.getRotationDegrees(), p_FrameMetadata.getFormat());
        DetectInVisionImage(image);
    }

    private void DetectInVisionImage(InputImage p_Image) {
        _Detector.process(p_Image)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> results) {
                        OnSuccess(results);
                        ProcessLatestImage();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        OnFailure(e);
                    }
                });
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
