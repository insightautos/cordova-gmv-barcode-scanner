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
package com.mobisys.cordova.plugins.mlkit.barcode.scanner.ui.camera;

import android.Manifest;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.common.images.Size;

import java.io.IOException;

import androidx.annotation.RequiresPermission;

public class CameraSourcePreview extends ViewGroup {
    private static final String TAG = "CameraSourcePreview";
    private final Context _Context;
    private final SurfaceView _SurfaceView;
    private final View _ViewFinderView;
    private final Button _TorchButton;
    public double ViewFinderWidth;
    public double ViewFinderHeight;
    private boolean _StartRequested;
    private boolean _SurfaceAvailable;
    private CameraSource _CameraSource;
    private boolean _FlashState = false;
    private int _previewWidth = 320;
    private int _previewHeight = 240;
    private int _layoutWidth = 0;
    private int _layoutHeight = 0;
    private GraphicOverlay _Overlay;

    public CameraSourcePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        _Context = context;
        _StartRequested = false;
        _SurfaceAvailable = false;

        _SurfaceView = new SurfaceView(context);
        _SurfaceView.getHolder().addCallback(new SurfaceCallback());
        addView(_SurfaceView);

        _ViewFinderView = new View(_Context);
        _ViewFinderView.setBackgroundResource(getResources().getIdentifier("rounded_rectangle", "drawable", _Context.getPackageName()));
        _ViewFinderView.layout(0, 0, 500, 500);
        addView(_ViewFinderView);

        _TorchButton = new Button(_Context);
        _TorchButton.setBackgroundResource(getResources().getIdentifier("torch_inactive", "drawable", _Context.getPackageName()));
        _TorchButton.layout(0, 0, dpToPx(45), dpToPx(45));
        _TorchButton.setMaxWidth(50);
        _TorchButton.setRotation(90);

        _TorchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    _CameraSource.setFlashMode(!_FlashState ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
                    _FlashState = !_FlashState;
                    _TorchButton.setBackgroundResource(getResources().getIdentifier(_FlashState ? "torch_active" : "torch_inactive", "drawable", _Context.getPackageName()));
                } catch (Exception e) {

                }
            }
        });


        addView(_TorchButton);
    }

    public int dpToPx(int dp) {
        float density = _Context.getResources()
                .getDisplayMetrics()
                .density;
        return Math.round((float) dp * density);
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public void start(CameraSource cameraSource) throws IOException, SecurityException {
        if (cameraSource == null) {
            stop();
        }

        _CameraSource = cameraSource;
        _CameraSource.ViewFinderHeight = ViewFinderHeight;
        _CameraSource.ViewFinderWidth = ViewFinderWidth;

        if (_CameraSource != null) {
            _StartRequested = true;
            startIfReady();
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public void start(CameraSource cameraSource, GraphicOverlay overlay) throws IOException, SecurityException {
        _Overlay = overlay;
        start(cameraSource);
    }

    public void stop() {
        if (_CameraSource != null) {
            _CameraSource.stop();
        }
    }

    public void release() {
        if (_CameraSource != null) {
            _CameraSource.release();
            _CameraSource = null;
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private void startIfReady() throws IOException, SecurityException {
        if (_StartRequested && _SurfaceAvailable) {
            _CameraSource.start(_SurfaceView.getHolder());
            if (_Overlay != null) {
                Size size = _CameraSource.getPreviewSize();
                SurfaceLayout();
                int min = Math.min(size.getWidth(), size.getHeight());
                int max = Math.max(size.getWidth(), size.getHeight());
                if (isPortraitMode()) {
                    // Swap width and height sizes when in portrait, since it will be rotated by
                    // 90 degrees
                    _Overlay.setCameraInfo(min, max, _CameraSource.getCameraFacing());
                } else {
                    _Overlay.setCameraInfo(max, min, _CameraSource.getCameraFacing());
                }
                _Overlay.clear();
            }
            _StartRequested = false;
        }
    }

    private void SurfaceLayout() {
        if (_CameraSource != null) {
            Size size = _CameraSource.getPreviewSize();
            if (size != null) {
                _previewWidth = size.getWidth();
                _previewHeight = size.getHeight();
            }
        }

        // Swap width and height sizes when in portrait, since it will be rotated 90 degrees
        if (isPortraitMode()) {
            int tmp = _previewWidth;
            //noinspection SuspiciousNameCombination
            _previewWidth = _previewHeight;
            _previewHeight = tmp;
        }

        // Computes height and width for potentially doing fit width.
        int childWidth;
        int childHeight;
        int childXOffset = 0;
        int childYOffset = 0;
        float widthRatio = (float) _layoutWidth / (float) _previewWidth;
        float heightRatio = (float) _layoutHeight / (float) _previewHeight;

        // To fill the view with the camera preview, while also preserving the correct aspect ratio,
        // it is usually necessary to slightly oversize the child and to crop off portions along one
        // of the dimensions.  We scale up based on the dimension requiring the most correction, and
        // compute a crop offset for the other dimension.
        if (widthRatio > heightRatio) {
            childWidth = _layoutWidth;
            childHeight = (int) ((float) _previewHeight * widthRatio);
        } else {
            childWidth = (int) ((float) _previewWidth * heightRatio);
            childHeight = _layoutHeight;
        }

        childYOffset = (_layoutHeight - childHeight) / 2;
        childXOffset = (_layoutWidth - childWidth) / 2;

        for (int i = 0; i < getChildCount(); ++i) {
            // One dimension will be cropped.  We shift child over or up by this offset and adjust
            // the size to maintain the proper aspect ratio.
            getChildAt(i).layout(-1 * childXOffset, -1 * childYOffset, childWidth - childXOffset, childHeight - childYOffset);
        }

        _SurfaceView.layout(childXOffset, childYOffset, childWidth + childXOffset, childHeight + childYOffset);

        int actualWidth = (int) (_layoutWidth * ViewFinderWidth);
        int actualHeight = (int) (_layoutHeight * ViewFinderHeight);

        _ViewFinderView.layout(_layoutWidth / 2 - actualWidth / 2, _layoutHeight / 2 - actualHeight / 2, _layoutWidth / 2 + actualWidth / 2, _layoutHeight / 2 + actualHeight / 2);

        int buttonSize = dpToPx(45);
        int torchLeft = _layoutWidth / 2 + actualWidth / 2 + (_layoutWidth - (_layoutWidth / 2 + actualWidth / 2)) / 2 - buttonSize / 2;
        int torchTop = _layoutHeight - (_layoutWidth - torchLeft);

        //mTorchButton.layout(torchLeft, torchTop, torchLeft + buttonSize, torchTop + buttonSize);
        _TorchButton.layout(torchLeft - buttonSize / 2, torchTop - buttonSize / 2, torchLeft + buttonSize / 2, torchTop + buttonSize / 2);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        _layoutWidth = right - left;
        _layoutHeight = bottom - top;

        SurfaceLayout();

        try {
            startIfReady();
        } catch (SecurityException se) {
            Log.e(TAG, "Do not have permission to start the camera", se);
        } catch (IOException e) {
            Log.e(TAG, "Could not start camera source.", e);
        }
    }

    private boolean isPortraitMode() {
        int orientation = _Context.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false;
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        }

        Log.d(TAG, "isPortraitMode returning false by default");
        return false;
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder surface) {
            _SurfaceAvailable = true;

            try {
                startIfReady();
            } catch (SecurityException se) {
                Log.e(TAG, "Do not have permission to start the camera", se);
            } catch (IOException e) {
                Log.e(TAG, "Could not start camera source.", e);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surface) {
            _SurfaceAvailable = false;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }
    }
}
