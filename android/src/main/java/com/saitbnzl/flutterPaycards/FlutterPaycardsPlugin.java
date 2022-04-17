package com.saitbnzl.flutterPaycards;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import com.saitbnzl.flutterPaycards.sdk.Card;
import com.saitbnzl.flutterPaycards.sdk.ScanCardIntent;
import com.saitbnzl.flutterPaycards.sdk.ui.ScanCardActivity;

import java.util.HashMap;
import java.util.Map;

/** FlutterPaycardsPlugin */
public class FlutterPaycardsPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler, PluginRegistry.ActivityResultListener {
  private Registrar mRegistrar;
  private Result mResult;
  private Activity activity;
  private Context context;

  private MethodChannel channel;

  static final int REQUEST_CODE_SCAN_CARD = 1;

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_paycards");
    FlutterPaycardsPlugin instance = new FlutterPaycardsPlugin(registrar);
    registrar.addActivityResultListener(instance);
    channel.setMethodCallHandler(instance);
  }

  private FlutterPaycardsPlugin(PluginRegistry.Registrar registrar) {
    mRegistrar = registrar;
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (mResult != null) {
      result.error("ALREADY_ACTIVE", "Scan card is already active", null);
      return;
    }
    mResult = result;
    Activity activity = null;
    if (mRegistrar != null) {
      activity = mRegistrar.activity();
    }
    if (activity == null) {
      activity = this.activity;
    }
    

    if (call.method.equals("startRecognizer")) {
      Intent scanIntent = new Intent(activity, ScanCardActivity.class);
      activity.startActivityForResult(scanIntent, REQUEST_CODE_SCAN_CARD);
    } else {
      result.notImplemented();
    }
  }
  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    if(mResult==null){
      return  false;
    }
    if (requestCode == REQUEST_CODE_SCAN_CARD) {
      if (resultCode == Activity.RESULT_OK) {
        Card card = data.getParcelableExtra(ScanCardIntent.RESULT_PAYCARDS_CARD);
        if(card!=null){
          String cardData = "Card number: " + card.getCardNumberRedacted() + "\n"
                  + "Card holder: " + card.getCardHolderName() + "\n"
                  + "Card expiration date: " + card.getExpirationDate();
          Log.i("flutter_paycards", "Card info: " + cardData);
          Map<String, Object> response = new HashMap<>();
          if(card!=null){
            response.put("cardHolderName", card.getCardHolderName());
            response.put("cardNumber", card.getCardNumber());
            if(card.getExpirationDate()!=null){
              response.put("expiryMonth", card.getExpirationDate().substring(0,2));
              response.put("expiryYear", card.getExpirationDate().substring(3,5));
            }
            mResult.success(response);
          }else{
            mResult.error("NOT_RECOGNIZED",null,null);
          }
        }
      } else if (resultCode == Activity.RESULT_CANCELED) {
        mResult.error("CANCELED",null,null);
        Log.i("flutter_paycards", "Scan canceled");
      } else {
        mResult.error("SCAN_FAILED",null,null);
        Log.i("flutter_paycards", "Scan failed");
      }
      mResult=null;
      return true;
    }
    return false;
  }

  public FlutterPaycardsPlugin() {
    // All Android plugin classes must support a no-args
    // constructor. A no-arg constructor is provided by
    // default without declaring one, but we include it here for
    // clarity.
    //
    // At this point your plugin is instantiated, but it
    // isn't attached to any Flutter experience. You should not
    // attempt to do any work here that is related to obtaining
    // resources or manipulating Flutter.
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    channel = new MethodChannel(binding.getBinaryMessenger(), "flutter_paycards");
    channel.setMethodCallHandler(this);
    context = binding.getApplicationContext();
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    this.activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    activity = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivity() {
    activity = null;
  }
}
