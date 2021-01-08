/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mobisys.cordova.plugins.mlkit.barcode.scanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.mobisys.cordova.plugins.mlkit.barcode.scanner.ui.camera.CameraSource;
import com.mobisys.cordova.plugins.mlkit.barcode.scanner.ui.camera.CameraSourcePreview;
import com.mobisys.cordova.plugins.mlkit.barcode.scanner.ui.camera.GraphicOverlay;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public final class BarcodeCaptureActivity extends AppCompatActivity
        implements BarcodeScanningProcessor.BarcodeUpdateListener {
    public static final String BarcodeFormat = "MLKitBarcodeFormat";
    public static final String BarcodeType = "MLKitBarcodeType";
    public static final String BarcodeValue = "MLKitBarcodeValue";
    // ----------------------------------------------------------------------------
    // |  Private Properties
    // ----------------------------------------------------------------------------
    private static final String TAG = "Barcode-reader";
    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    // ----------------------------------------------------------------------------
    // |  Public Properties
    // ----------------------------------------------------------------------------
    public Integer DetectionTypes;
    public double ViewFinderWidth = .5;
    public double ViewFinderHeight = .7;
    private CameraSource _CameraSource;
    private CameraSourcePreview _Preview;
    private GraphicOverlay<BarcodeGraphic> _GraphicOverlay;
    private ScaleGestureDetector _ScaleGestureDetector;
    private GestureDetector _GestureDetector;

    // ----------------------------------------------------------------------------
    // |  Public Functions
    // ----------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Hide the status bar and action bar.
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getActionBar() != null) {
            getActionBar().hide();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(getResources().getIdentifier("barcode_capture", "layout", getPackageName()));

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // read parameters from the intent used to launch the activity.
        DetectionTypes = getIntent().getIntExtra("DetectionTypes", 1234);
        ViewFinderWidth = getIntent().getDoubleExtra("ViewFinderWidth", .5);
        ViewFinderHeight = getIntent().getDoubleExtra("ViewFinderHeight", .7);

        _Preview = (CameraSourcePreview) findViewById(getResources().getIdentifier("preview", "id", getPackageName()));
        _Preview.ViewFinderWidth = ViewFinderWidth;
        _Preview.ViewFinderHeight = ViewFinderHeight;
        _GraphicOverlay = (GraphicOverlay<BarcodeGraphic>) findViewById(getResources().getIdentifier("graphicOverlay", "id", getPackageName()));

        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(true, false);
        } else {
            requestCameraPermission();
        }

        _GestureDetector = new GestureDetector(this, new CaptureGestureListener());
        _ScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
    }


    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean b = _ScaleGestureDetector.onTouchEvent(e);
        boolean c = _GestureDetector.onTouchEvent(e);

        return b || c || super.onTouchEvent(e);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            DetectionTypes = getIntent().getIntExtra("DetectionTypes", 0);
            ViewFinderWidth = getIntent().getDoubleExtra("ViewFinderWidth", .5);
            ViewFinderHeight = getIntent().getDoubleExtra("ViewFinderHeight", .7);

            createCameraSource(true, false);
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length + " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Camera permission required")
                .setMessage(getResources().getIdentifier("no_camera_permission", "string", getPackageName()))
                .setPositiveButton(getResources().getIdentifier("ok", "string", getPackageName()), listener).show();
    }

    @Override
    public void onBarcodeDetected(Barcode barcode) {
        // do something with barcode data returned

        Intent data = new Intent();
        data.putExtra(BarcodeFormat, barcode.getFormat());
        data.putExtra(BarcodeType, barcode.getValueType());
        data.putExtra(BarcodeValue, barcode.getRawValue());
        setResult(CommonStatusCodes.SUCCESS, data);
        finish();
    }

    // ----------------------------------------------------------------------------
    // |  Protected Functions
    // ----------------------------------------------------------------------------
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (_Preview != null) {
            _Preview.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        if (_Preview != null) {
            _Preview.release();
        }
    }

    // ----------------------------------------------------------------------------
    // |  Private Functions
    // ----------------------------------------------------------------------------
    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        int detectionType = 0;

        if (DetectionTypes == 0 || DetectionTypes == 1234) {
            detectionType = (Barcode.FORMAT_CODE_39 | Barcode.FORMAT_DATA_MATRIX);
        } else {
            detectionType = DetectionTypes;
        }

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(detectionType).build();

        BarcodeScanner barcodeDetector = BarcodeScanning.getClient(options);
        BarcodeScanningProcessor scanningProcessor = new BarcodeScanningProcessor(barcodeDetector, this);

        CameraSource.Builder builder = new CameraSource.Builder(getApplicationContext(), scanningProcessor)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1600, 1024)
                .setRequestedFps(30.0f);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = builder.setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
        }

        _CameraSource = builder.setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null).build();
    }

    private void startCameraSource() throws SecurityException {
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (_CameraSource != null) {
            try {
                _Preview.start(_CameraSource, _GraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                _CameraSource.release();
                _CameraSource = null;
            }
        }
    }

    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM);
            }
        };

        findViewById(getResources().getIdentifier("topLayout", "id", getPackageName())).setOnClickListener(listener);
        Snackbar
                .make(_GraphicOverlay, getResources().getIdentifier("permission_camera_rationale", "string", getPackageName()),
                        Snackbar.LENGTH_INDEFINITE)
                .setAction(getResources().getIdentifier("ok", "string", getPackageName()), listener).show();
    }

    // ----------------------------------------------------------------------------
    // |  Helper classes
    // ----------------------------------------------------------------------------
    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return super.onSingleTapConfirmed(e);
        }
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            _CameraSource.doZoom(detector.getScaleFactor());
        }
    }
}
