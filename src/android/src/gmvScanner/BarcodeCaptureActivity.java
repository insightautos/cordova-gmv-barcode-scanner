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
package com.dealrinc.gmvScanner;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.android.material.snackbar.Snackbar;

import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.view.WindowManager;
import android.view.Display;
import android.graphics.Point;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.dealrinc.gmvScanner.ui.camera.CameraSource;
import com.dealrinc.gmvScanner.ui.camera.CameraSourcePreview;

import com.dealrinc.gmvScanner.ui.camera.GraphicOverlay;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Activity for the multi-tracker app.  This app detects barcodes and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and ID of each barcode.
 */
public final class BarcodeCaptureActivity extends AppCompatActivity {
    private static final String TAG = "Barcode-reader";

    // intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_BARCODE_CAPTURE = 9001;

    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    // constants used to pass extra data in the intent
    public Integer DetectionTypes;
    public double ViewFinderWidth = .5;
    public double ViewFinderHeight = .7;
    public float ViewFinderZoom = 1.0f;

    public static final String BarcodeObject = "Barcode";

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    // helper objects for detecting taps and pinches.
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    private volatile static String TextValue;
    private volatile static String BarcodeValue;
    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Hide the status bar and action bar.
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(getResources().getIdentifier("barcode_capture", "layout", getPackageName()));

        mPreview = (CameraSourcePreview) findViewById(getResources().getIdentifier("preview", "id", getPackageName()));

        // read parameters from the intent used to launch the activity.
        DetectionTypes = getIntent().getIntExtra("DetectionTypes", 1234);
        ViewFinderWidth = getIntent().getDoubleExtra("ViewFinderWidth", .5);
        ViewFinderHeight = getIntent().getDoubleExtra("ViewFinderHeight", .7);
        ViewFinderZoom = getIntent().getFloatExtra("ViewFinderZoom", 1.0f);

        mPreview.ViewFinderWidth = ViewFinderWidth;
        mPreview.ViewFinderHeight = ViewFinderHeight;

        Log.d(TAG, "Cordova Input Parameters. DetectionTypes: " + DetectionTypes + " ViewFinderWidth: " + ViewFinderWidth + " ViewFinderHeight: " + ViewFinderHeight + " ViewFinderZoom: "  + ViewFinderZoom);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(true, false);
        } else {
            requestCameraPermission();
        }

        gestureDetector = new GestureDetector(this, new CaptureGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        findViewById(getResources().getIdentifier("topLayout", "id", getPackageName())).setOnClickListener(listener);
        Snackbar.make(mGraphicOverlay, getResources().getIdentifier("permission_camera_rationale", "string", getPackageName()),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getResources().getIdentifier("ok", "string", getPackageName()), listener)
                .show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean b = scaleGestureDetector.onTouchEvent(e);

        boolean c = gestureDetector.onTouchEvent(e);

        return b || c || super.onTouchEvent(e);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();

        int detectionType = 0;

        if(DetectionTypes == 0) {
            detectionType = (com.google.mlkit.vision.barcode.Barcode.FORMAT_CODE_39
                    |com.google.mlkit.vision.barcode.Barcode.FORMAT_DATA_MATRIX
                    |com.google.mlkit.vision.barcode.Barcode.FORMAT_QR_CODE);
        } else if(DetectionTypes == 1234) {

        } else {
            detectionType = DetectionTypes;
        }

        BarcodeScannerOptions barcodeScannerOptions = new BarcodeScannerOptions.Builder().setBarcodeFormats(detectionType).build();
        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        CameraSource.Builder builder = new CameraSource.Builder(getApplicationContext(), this, mPreview)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(4000, 3000)
                //.setRequestedPreviewSize(1500, 1000)
                .setRequestedFps(30.0f);

        // make sure that auto focus is an available option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = builder.setFocusMode(
                    autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null);
        }

        mCameraSource = builder
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .build();

        mCameraSource.ViewFinderWidth = ViewFinderWidth;
        mCameraSource.ViewFinderHeight = ViewFinderHeight;
        mCameraSource.ViewFinderZoom = ViewFinderZoom;
        mPreview.mCameraSource = mCameraSource;
        Log.i(TAG, "Before camera layout");
        mPreview.invalidate();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraSource.release();
        mCameraSource = null;
        barcodeScanner.close();
        barcodeScanner = null;
        textRecognizer.close();
        textRecognizer = null;
        if (mPreview != null) {
            mPreview.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
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

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Camera permission required")
                .setMessage(getResources().getIdentifier("no_camera_permission", "string", getPackageName()))
                .setPositiveButton(getResources().getIdentifier("ok", "string", getPackageName()), listener)
                .show();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    /**
     * onTap returns the tapped barcode result to the calling Activity.
     *
     * @param rawX - the raw position of the tap
     * @param rawY - the raw position of the tap.
     * @return true if the activity is ending.
     */
    private boolean onTap(float rawX, float rawY) {
        // Find tap point in preview frame coordinates.
        return false;
        /*
        int[] location = new int[2];
        mGraphicOverlay.getLocationOnScreen(location);
        float x = (rawX - location[0]) / mGraphicOverlay.getWidthScaleFactor();
        float y = (rawY - location[1]) / mGraphicOverlay.getHeightScaleFactor();

        // Find the barcode whose center is closest to the tapped point.
        Barcode best = null;
        float bestDistance = Float.MAX_VALUE;
        for (BarcodeGraphic graphic : mGraphicOverlay.getGraphics()) {
            Barcode barcode = graphic.getBarcode();
            if (barcode.getBoundingBox().contains((int) x, (int) y)) {
                // Exact hit, no need to keep looking.
                best = barcode;
                break;
            }
            float dx = x - barcode.getBoundingBox().centerX();
            float dy = y - barcode.getBoundingBox().centerY();
            float distance = (dx * dx) + (dy * dy);  // actually squared distance
            if (distance < bestDistance) {
                best = barcode;
                bestDistance = distance;
            }
        }

        if (best != null) {
            Intent data = new Intent();
            data.putExtra(BarcodeObject, best);
            setResult(CommonStatusCodes.SUCCESS, data);
            finish();
            return true;
        }
        return false;*/
    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
        }
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p/>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mCameraSource.doZoom(detector.getScaleFactor());
        }
    }

    private static int transliterate(char c) {
        return "0123456789.ABCDEFGH..JKLMN.P.R..STUVWXYZ".indexOf(c) % 10;
    }

    private static char getCheckDigit(String vin) {
        String map = "0123456789X";
        String weights = "8765432X098765432";
        int sum = 0;
        for (int i = 0; i < 17; ++i) {
            sum += transliterate(vin.charAt(i)) * map.indexOf(weights.charAt(i));
        }
        return map.charAt(sum % 11);
    }

    private static boolean validateVin(String vin) {
        if(vin.length()!=17) return false;
        return getCheckDigit(vin) == vin.charAt(8);
    }

    BarcodeScanner barcodeScanner;
    TextRecognizer textRecognizer;

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    String lastCreatedImage = null;

    public static String getRealPathFromUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void saveToInternalStorage(Bitmap bitmap) {
        if (lastCreatedImage != null) {
            String path = getRealPathFromUri(getApplicationContext(), Uri.parse(lastCreatedImage));
            Log.v(TAG, "File: " + path);
            File fdelete = new File(path);
            if (fdelete.exists()) {
                if (fdelete.delete()) {
                    Log.v(TAG, "file Deleted: " + lastCreatedImage);
                } else {
                    Log.v(TAG, "file not Deleted: " + lastCreatedImage);
                }
            }
        }

        lastCreatedImage = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Barcode Camera Frame", "Sample Description");

        Log.v(TAG, "file created " + lastCreatedImage);
    }

    boolean detectionActive = false;

    int processBarcodeFrames = 0;
    int lastTextCheck = 0;

    public void detectBarcodes(Bitmap bitmap, long frameStartMs) {
        //Log.d(TAG, "detectionCalled");

        if (detectionActive) {
            //Log.v(TAG, "Skipping Frame");
            return;
        }
        detectionActive = true;
        if (DetectionTypes == 0) {
            // Resizing for VIN detection because we don't need the max resolution.
            bitmap = getResizedBitmap(bitmap, 1300);
        }

        /*
         For Debug Purposes.
         Must add `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>` to AndroidManifest.xml, and then provide permissions to the app from the android app settings.
        */
        //saveToInternalStorage(bitmap);

        InputImage inputImage = InputImage.fromBitmap(bitmap, processBarcodeFrames % 2 == 0 ? 0 : 180);

        final long detectorStartMs = SystemClock.elapsedRealtime();
        Task<List<com.google.mlkit.vision.barcode.Barcode>> task = barcodeScanner.process(inputImage);
        Bitmap finalBitmap = bitmap;
        task.addOnSuccessListener((List<com.google.mlkit.vision.barcode.Barcode> barcodes) -> {
            if (barcodes.isEmpty()) {
                //Log.v(TAG, "No barcode has been detected");
            }
            for (int i = 0; i < barcodes.size(); ++i) {
                com.google.mlkit.vision.barcode.Barcode barcode = barcodes.get(i);

                String val = barcode.getRawValue();
                Log.v(TAG, "Scanned Barcode: " +val);

                if (DetectionTypes == 0) {
                    if(val.length() < 17) {
                        continue;
                    }
                    val = val.replaceAll("[ioqIOQ]", "");

                    val = val.substring(0, Math.min(val.length(), 17));

                    if(validateVin(val)) {
                        Log.v(TAG, "Detected VIN: " +val);
                        Intent data = new Intent();
                        data.putExtra("barcodeType", String.valueOf(barcode.getValueType()));
                        data.putExtra("barcodeValue", val);
                        setResult(CommonStatusCodes.SUCCESS, data);
                        finish();
                        return;
                    }
                } else {
                    Log.v(TAG, "Detected Barcode: " +val);
                    Intent data = new Intent();
                    data.putExtra("barcodeType", String.valueOf(barcode.getValueType()));
                    data.putExtra("barcodeValue", val);
                    setResult(CommonStatusCodes.SUCCESS, data);
                    finish();
                    return;
                }
            }

            processBarcodeFrames ++;

            if (DetectionTypes == 0) {
                InputImage textInputImage;
                // Resizing for OCR detection because we don't need the max resolution.
                Bitmap resizedBitmap = getResizedBitmap(finalBitmap, 1000);
                if (lastTextCheck == 0) {
                    lastTextCheck = 1;
                    textInputImage = InputImage.fromBitmap(resizedBitmap, 180);
                } else {
                    textInputImage = InputImage.fromBitmap(resizedBitmap, 0);
                    lastTextCheck = 0;
                }
                //processBarcodeFrames = 0;
                Task<Text> textTask = textRecognizer.process(textInputImage);
                textTask.addOnSuccessListener((Text text) -> {
                    if (!text.getText().equals("")) {
                        Log.v(TAG, "Detected Text: " + text.getText());

                        List<Text.TextBlock> textBlocks = text.getTextBlocks();
                        for (int i = 0; i < textBlocks.size(); ++i) {
                            Text.TextBlock textBlock = textBlocks.get(i);

                            String val = textBlock.getText();
                            // Match vins that have the correct pattern, must be: 1. 17 digits long, 2. have 0-9 or X in the 9th digit, 3. characters 1-8 and 10-12 must be an allowed character, 4. last 4 digits must be numbers
                            Pattern p = Pattern.compile("([ABCDEFGHJKLMNPRSTUVWXYZ0-9]{8}[0-9X]{1}[ABCDEFGHJKLMNPRSTUVWXYZ0-9]{3}[0-9]{5})");
                            Matcher m = p.matcher(val);
                            while (m.find()) {
                                if(validateVin(m.group(1))) {
                                    Log.v(TAG, "Detected VIN from Text: " + m.group(1));
                                    Intent data = new Intent();
                                    data.putExtra("barcodeType", "OCR");
                                    data.putExtra("barcodeValue", m.group(1));
                                    setResult(CommonStatusCodes.SUCCESS, data);
                                    finish();
                                    return;
                                }
                            }
                        }
                    }
                    long endMs = SystemClock.elapsedRealtime();
                    long currentFrameLatencyMs = endMs - frameStartMs;
                    long currentDetectorLatencyMs = endMs - detectorStartMs;
                    Log.d(TAG, "Frame latency w/ OCR:" + currentFrameLatencyMs);
                    Log.d(TAG, "Detector latency w/ OCR:" + currentDetectorLatencyMs);
                    detectionActive = false;
                    //Log.v(TAG, "Frame Processed");
                }).addOnFailureListener((Exception e) -> {
                    Log.e(TAG, "Barcode detection failed " + e);
                    detectionActive = false;
                });
            } else {
                long endMs = SystemClock.elapsedRealtime();
                long currentFrameLatencyMs = endMs - frameStartMs;
                long currentDetectorLatencyMs = endMs - detectorStartMs;
                Log.d(TAG, "Frame latency:" + currentFrameLatencyMs);
                Log.d(TAG, "Detector latency:" + currentDetectorLatencyMs);
                detectionActive = false;
            }
        }).addOnFailureListener((Exception e) -> {
            Log.e(TAG, "Barcode detection failed " + e);
            detectionActive = false;
        });
    }
}
