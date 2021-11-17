package com.mobisys.cordova.plugins.mlkit.barcode.scanner;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.google.android.gms.common.api.CommonStatusCodes;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * This class echoes a string called from JavaScript.
 */
public class MLKitBarcodeScanner extends CordovaPlugin {

  private static final int RC_BARCODE_CAPTURE = 9001;
  private CallbackContext _CallbackContext;
  private Boolean _BeepOnSuccess;
  private Boolean _VibrateOnSuccess;
  private MediaPlayer _MediaPlayer;
  private Vibrator _Vibrator;
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    _Vibrator = (Vibrator) cordova.getContext().getSystemService(Context.VIBRATOR_SERVICE);
    _MediaPlayer = new MediaPlayer();

    try {
      AssetFileDescriptor descriptor = cordova.getContext().getAssets().openFd("beep.ogg");
      _MediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
      descriptor.close();
      _MediaPlayer.prepare();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
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
          try {
            openNewActivity(context, args);
          } catch (JSONException e) {
            _CallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.toString()));
          }
        }
      }
      Thread t = new Thread(new OneShotTask(context, args));
      t.start();
      return true;
    }
    return false;
  }

  private void openNewActivity(Context context, JSONArray args) throws JSONException {
    JSONObject config = args.getJSONObject(0);
    Intent intent = new Intent(context, CaptureActivity.class);
    intent.putExtra("BarcodeFormats", config.optInt("barcodeFormats", 1234));
    intent.putExtra("DetectorSize", config.optDouble("detectorSize", 0.5));
    intent.putExtra("RotateCamera", config.optBoolean("rotateCamera", false));

    _BeepOnSuccess = config.optBoolean("beepOnSuccess", false);
    _VibrateOnSuccess = config.optBoolean("vibrateOnSuccess", false);

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

          if (_BeepOnSuccess) {
            _MediaPlayer.start();
          }

          if (_VibrateOnSuccess) {
            Integer duration = 200;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              _Vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
              //deprecated in API 26
              _Vibrator.vibrate(duration);
            }
          }

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
