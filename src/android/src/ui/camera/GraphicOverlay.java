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

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.gms.vision.CameraSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * A view which renders a series of custom graphics to be overlayed on top of an associated preview
 * (i.e., the camera preview).  The creator can add graphics objects, update the objects, and remove
 * them, triggering the appropriate drawing and invalidation within the view.<p>
 * <p>
 * Supports scaling and mirroring of the graphics relative the camera's preview properties.  The
 * idea is that detection items are expressed in terms of a preview size, but need to be scaled up
 * to the full view size, and also mirrored in the case of the front-facing camera.<p>
 * <p>
 * Associated {@link Graphic} items should use the following methods to convert to view coordinates
 * for the graphics that are drawn:
 * <ol>
 * <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of the
 * supplied value from the preview scale to the view scale.</li>
 * <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the coordinate
 * from the preview's coordinate system to the view coordinate system.</li>
 * </ol>
 */
public class GraphicOverlay<T extends GraphicOverlay.Graphic> extends View {
    private final Object _Lock = new Object();
    private int _PreviewWidth;
    private float _WidthScaleFactor = 1.0f;
    private int _PreviewHeight;
    private float _HeightScaleFactor = 1.0f;
    private int _Facing = CameraSource.CAMERA_FACING_BACK;
    private Set<T> _Graphics = new HashSet<>();

    /**
     * Base class for a custom graphics object to be rendered within the graphic overlay.  Subclass
     * this and implement the {@link Graphic#draw(Canvas)} method to define the
     * graphics element.  Add instances to the overlay using {@link GraphicOverlay#add(Graphic)}.
     */
    public static abstract class Graphic {
        private GraphicOverlay _Overlay;

        public Graphic(GraphicOverlay overlay) {
            _Overlay = overlay;
        }

        /**
         * Draw the graphic on the supplied canvas.  Drawing should use the following methods to
         * convert to view coordinates for the graphics that are drawn:
         * <ol>
         * <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of
         * the supplied value from the preview scale to the view scale.</li>
         * <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the
         * coordinate from the preview's coordinate system to the view coordinate system.</li>
         * </ol>
         *
         * @param canvas drawing canvas
         */
        public abstract void draw(Canvas canvas);

        /**
         * Adjusts a horizontal value of the supplied value from the preview scale to the view
         * scale.
         */
        public float scaleX(float horizontal) {
            return horizontal * _Overlay._WidthScaleFactor;
        }

        /**
         * Adjusts a vertical value of the supplied value from the preview scale to the view scale.
         */
        public float scaleY(float vertical) {
            return vertical * _Overlay._HeightScaleFactor;
        }

        /**
         * Adjusts the x coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        public float translateX(float x) {
            if (_Overlay._Facing == CameraSource.CAMERA_FACING_FRONT) {
                return _Overlay.getWidth() - scaleX(x);
            } else {
                return scaleX(x);
            }
        }

        /**
         * Adjusts the y coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        public float translateY(float y) {
            return scaleY(y);
        }

        public void postInvalidate() {
            _Overlay.postInvalidate();
        }
    }

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Removes all graphics from the overlay.
     */
    public void clear() {
        synchronized (_Lock) {
            _Graphics.clear();
        }
        postInvalidate();
    }

    /**
     * Adds a graphic to the overlay.
     */
    public void add(T graphic) {
        synchronized (_Lock) {
            _Graphics.add(graphic);
        }
        postInvalidate();
    }

    /**
     * Removes a graphic from the overlay.
     */
    public void remove(T graphic) {
        synchronized (_Lock) {
            _Graphics.remove(graphic);
        }
        postInvalidate();
    }

    /**
     * Returns a copy (as a list) of the set of all active graphics.
     *
     * @return list of all active graphics.
     */
    public List<T> getGraphics() {
        synchronized (_Lock) {
            return new Vector(_Graphics);
        }
    }

    /**
     * Returns the horizontal scale factor.
     */
    public float getWidthScaleFactor() {
        return _WidthScaleFactor;
    }

    /**
     * Returns the vertical scale factor.
     */
    public float getHeightScaleFactor() {
        return _HeightScaleFactor;
    }

    /**
     * Sets the camera attributes for size and facing direction, which informs how to transform
     * image coordinates later.
     */
    public void setCameraInfo(int previewWidth, int previewHeight, int facing) {
        synchronized (_Lock) {
            _PreviewWidth = previewWidth;
            _PreviewHeight = previewHeight;
            _Facing = facing;
        }
        postInvalidate();
    }

    /**
     * Draws the overlay with its associated graphic objects.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (_Lock) {
            if ((_PreviewWidth != 0) && (_PreviewHeight != 0)) {
                _WidthScaleFactor = (float) canvas.getWidth() / (float) _PreviewWidth;
                _HeightScaleFactor = (float) canvas.getHeight() / (float) _PreviewHeight;
            }

            for (Graphic graphic : _Graphics) {
                graphic.draw(canvas);
            }
        }
    }
}