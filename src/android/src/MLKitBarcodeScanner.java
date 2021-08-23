package com.mobisys.cordova.plugins.mlkit.barcode.scanner;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.CommonStatusCodes;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;


/**
 * This class echoes a string called from JavaScript.
 */
public class MLKitBarcodeScanner extends CordovaPlugin {

    private static final int RC_BARCODE_CAPTURE = 9001;
    private CallbackContext _CallbackContext;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException
    {
        Context context = cordova.getActivity().getApplicationContext();
        _CallbackContext = callbackContext;

        if (action.equals("startScan")) {
            class OneShotTask implements Runnable {
                private final Context context;
                private final JSONArray args;

                private OneShotTask(Context ctx, JSONArray as) {
                    context = ctx;
                    args = as;
                }

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
        Intent intent = new Intent(context, CaptureActivity.class);
        intent.putExtra("DetectionTypes", args.optInt(0, 1234));
        intent.putExtra("DetectorSize", args.optDouble(1, 0.5));
        intent.putExtra("MirrorCamera", args.optBoolean(2, false));

        //intent.putExtra("ViewFinderWidth", args.optDouble(1, .5));
       // intent.putExtra("ViewFinderHeight", args.optDouble(2, .7));

        this.cordova.setActivityResultCallback(this);
        this.cordova.startActivityForResult(this, intent, RC_BARCODE_CAPTURE);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Integer barcodeFormat = data.getIntExtra(CaptureActivity.BarcodeFormat, 0);
                    Integer barcodeType = data.getIntExtra(CaptureActivity.BarcodeType, 0);
                    String barcodeValue = data.getStringExtra(CaptureActivity.BarcodeValue);
                    JSONArray result = new JSONArray();
                    result.put(barcodeValue);
                    result.put(barcodeFormat);
                    result.put(barcodeType);
                    _CallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));

                    Log.d("MLKitBarcodeScanner", "Barcode read: " + barcodeValue);
                }
            } else {
                String err = data.getStringExtra("err");
                JSONArray result = new JSONArray();
                result.put(err);
                result.put("");
                result.put("");
                _CallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, result));
            }
        }
    }

    @Override
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        _CallbackContext = callbackContext;
    }
}
