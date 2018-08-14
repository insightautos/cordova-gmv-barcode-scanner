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

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

/**
 * Main activity demonstrating how to pass extra parameters to an activity that
 * reads barcodes.
 */
public class SecondaryActivity extends Activity implements View.OnClickListener {

    // use a compound button so either checkbox or switch widgets work.
    private CompoundButton autoFocus;
    private CompoundButton useFlash;
    private TextView statusMessage;
    private TextView barcodeValue;
    public static final String BarcodeObject = "Barcode";

    private static final int RC_BARCODE_CAPTURE = 9001;
    private static final String TAG = "BarcodeMain";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResources().getIdentifier("activity_gmv_barcode_scanner", "layout", getPackageName()));

        findViewById(getResources().getIdentifier("read_barcode", "id", getPackageName())).setOnClickListener(this);

        Intent intent = new Intent(this, BarcodeCaptureActivity.class);

        intent.putExtra("DetectionTypes", getIntent().getIntExtra("DetectionTypes", 1234));
        intent.putExtra("ViewFinderWidth", getIntent().getDoubleExtra("DetectionTypes", .5));
        intent.putExtra("ViewFinderHeight", getIntent().getDoubleExtra("DetectionTypes", .7));

        startActivityForResult(intent, RC_BARCODE_CAPTURE);

    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == getResources().getIdentifier("read_barcode", "id", getPackageName())) {
            // launch barcode activity.
            Intent intent = new Intent(this, BarcodeCaptureActivity.class);

            startActivityForResult(intent, RC_BARCODE_CAPTURE);
        }

    }

    /**
     * Called when an activity you launched exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional
     * data from it.  The <var>resultCode</var> will be
     * {@link #RESULT_CANCELED} if the activity explicitly returned that,
     * didn't return any result, or crashed during its operation.
     * <p/>
     * <p>You will receive this call immediately before onResume() when your
     * activity is re-starting.
     * <p/>
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     * @see #startActivityForResult
     * @see #createPendingResult
     * @see #setResult(int)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Activity exited");
        if (requestCode == RC_BARCODE_CAPTURE) {
            Intent d = new Intent();
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    d.putExtra(BarcodeObject, barcode);
                    setResult(CommonStatusCodes.SUCCESS, data);
                } else {
                    d.putExtra("err", "USER_CANCELLED");
                    setResult(CommonStatusCodes.ERROR, d);
                }
            } else {
                d.putExtra("err", "There was an error with the barcode reader.");
                setResult(CommonStatusCodes.ERROR, d);
            }
            finish();
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
