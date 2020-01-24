package com.dealrinc.gmvScanner;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;


/**
 * This class echoes a string called from JavaScript.
 */
public class CDVAndroidScanner extends CordovaPlugin {

    private CallbackContext mCallbackContext;

    private static final int RC_BARCODE_CAPTURE = 9001;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		Context context = cordova.getActivity().getApplicationContext();
        mCallbackContext = callbackContext;
		if (action.equals("startScan")) {

            class OneShotTask implements Runnable {
                private Context context;
                private JSONArray args;
                private OneShotTask(Context ctx, JSONArray as) { context = ctx; args = as; }
                public void run() {
                    openNewActivity(context, args);
                }
            }
            Thread t = new Thread(new OneShotTask(context, args));
            t.start();
            return true;
        }
        return false;
    }

    private void openNewActivity(Context context, JSONArray args) {
		Intent intent = new Intent(context, BarcodeCaptureActivity.class);
        intent.putExtra("DetectionTypes", args.optInt(0, 1234));
        intent.putExtra("ViewFinderWidth", args.optDouble(1, .5));
        intent.putExtra("ViewFinderHeight", args.optDouble(2, .7));

        this.cordova.setActivityResultCallback(this);
        this.cordova.startActivityForResult(this, intent, RC_BARCODE_CAPTURE);
	}


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    JSONArray result = new JSONArray();
                    result.put(barcode.rawValue);
                    result.put("");
                    result.put("");
                    mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));

                    Log.d("CDVAndroidScanner", "Barcode read: " + barcode.displayValue);
                }
            } else {
                String err = data.getStringExtra("err");
                JSONArray result = new JSONArray();
                result.put(err);
                result.put("");
                result.put("");
                mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, result));
            }
        }
    }

    @Override
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        mCallbackContext = callbackContext;
    }
}
