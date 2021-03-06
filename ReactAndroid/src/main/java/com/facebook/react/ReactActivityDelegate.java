// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.react;

import javax.annotation.Nullable;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.widget.Toast;

import com.facebook.common.logging.FLog;
import com.facebook.infer.annotation.Assertions;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.devsupport.DoubleTapReloadRecognizer;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.modules.core.PermissionListener;

/**
 * Delegate class for {@link ReactActivity} and {@link ReactFragmentActivity}. You can subclass this
 * to provide custom implementations for e.g. {@link #getReactNativeHost()}, if your Application
 * class doesn't implement {@link ReactApplication}.
 */
public class ReactActivityDelegate {
  private static final String REDBOX_PERMISSION_MESSAGE =
    "Overlay permissions needs to be granted in order for react native apps to run in dev mode";

  private final @Nullable Activity mActivity;
  private final @Nullable FragmentActivity mFragmentActivity;
  private final String mMainComponentName;

  private @Nullable ReactRootView mReactRootView;
  private @Nullable DoubleTapReloadRecognizer mDoubleTapReloadRecognizer;
  private @Nullable PermissionListener mPermissionListener;

  public ReactActivityDelegate(Activity activity, String mainComponentName) {
    mActivity = activity;
    mMainComponentName = mainComponentName;
    mFragmentActivity = null;
  }

  public ReactActivityDelegate(FragmentActivity fragmentActivity, String mainComponentName) {
    mFragmentActivity = fragmentActivity;
    mMainComponentName = mainComponentName;
    mActivity = null;
  }

  protected @Nullable Bundle getLaunchOptions() {
    return null;
  }

  protected ReactRootView createRootView() {
    return new ReactRootView(getContext());
  }

  /**
   * Get the {@link ReactNativeHost} used by this app. By default, assumes
   * {@link Activity#getApplication()} is an instance of {@link ReactApplication} and calls
   * {@link ReactApplication#getReactNativeHost()}. Override this method if your application class
   * does not implement {@code ReactApplication} or you simply have a different mechanism for
   * storing a {@code ReactNativeHost}, e.g. as a static field somewhere.
   */
  protected ReactNativeHost getReactNativeHost() {
    return ((ReactApplication) getPlainActivity().getApplication()).getReactNativeHost();
  }

  public ReactInstanceManager getReactInstanceManager() {
    return getReactNativeHost().getReactInstanceManager();
  }

  protected void onCreate(Bundle savedInstanceState) {
    if (getReactNativeHost().getUseDeveloperSupport() && Build.VERSION.SDK_INT >= 23) {
      // Get permission to show redbox in dev builds.
      if (!Settings.canDrawOverlays(getContext())) {
        Intent serviceIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        getContext().startActivity(serviceIntent);
        FLog.w(ReactConstants.TAG, REDBOX_PERMISSION_MESSAGE);
        Toast.makeText(getContext(), REDBOX_PERMISSION_MESSAGE, Toast.LENGTH_LONG).show();
      }
    }

    mReactRootView = createRootView();
    mReactRootView.startReactApplication(
      getReactNativeHost().getReactInstanceManager(),
      mMainComponentName,
      getLaunchOptions());
    getPlainActivity().setContentView(mReactRootView);
    mDoubleTapReloadRecognizer = new DoubleTapReloadRecognizer();
  }

  protected void onPause() {
    if (getReactNativeHost().hasInstance()) {
      getReactNativeHost().getReactInstanceManager().onHostPause(getPlainActivity());
    }
  }

  protected void onResume() {
    if (getReactNativeHost().hasInstance()) {
      getReactNativeHost().getReactInstanceManager().onHostResume(
        getPlainActivity(),
        (DefaultHardwareBackBtnHandler) getPlainActivity());
    }
  }

  protected void onDestroy() {
    if (mReactRootView != null) {
      mReactRootView.unmountReactApplication();
      mReactRootView = null;
    }
    if (getReactNativeHost().hasInstance()) {
      getReactNativeHost().getReactInstanceManager().onHostDestroy(getPlainActivity());
    }
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (getReactNativeHost().hasInstance()) {
      getReactNativeHost().getReactInstanceManager()
        .onActivityResult(requestCode, resultCode, data);
    }
  }

  public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (getReactNativeHost().hasInstance() && getReactNativeHost().getUseDeveloperSupport()) {
      if (keyCode == KeyEvent.KEYCODE_MENU) {
        getReactNativeHost().getReactInstanceManager().showDevOptionsDialog();
        return true;
      }
      boolean didDoubleTapR = Assertions.assertNotNull(mDoubleTapReloadRecognizer)
        .didDoubleTapR(keyCode, getPlainActivity().getCurrentFocus());
      if (didDoubleTapR) {
        getReactNativeHost().getReactInstanceManager().getDevSupportManager().handleReloadJS();
        return true;
      }
    }
    return false;
  }

  public boolean onBackPressed() {
    if (getReactNativeHost().hasInstance()) {
      getReactNativeHost().getReactInstanceManager().onBackPressed();
      return true;
    }
    return false;
  }

  public boolean onNewIntent(Intent intent) {
    if (getReactNativeHost().hasInstance()) {
      getReactNativeHost().getReactInstanceManager().onNewIntent(intent);
      return true;
    }
    return false;
  }

  @TargetApi(Build.VERSION_CODES.M)
  public void requestPermissions(
    String[] permissions,
    int requestCode,
    PermissionListener listener) {
    mPermissionListener = listener;
    getPlainActivity().requestPermissions(permissions, requestCode);
  }

  public void onRequestPermissionsResult(
    int requestCode,
    String[] permissions,
    int[] grantResults) {
    if (mPermissionListener != null &&
      mPermissionListener.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
      mPermissionListener = null;
    }
  }

  private Context getContext() {
    if (mActivity != null) {
      return mActivity;
    }
    return Assertions.assertNotNull(mFragmentActivity);
  }

  private Activity getPlainActivity() {
    return ((Activity) getContext());
  }
}
