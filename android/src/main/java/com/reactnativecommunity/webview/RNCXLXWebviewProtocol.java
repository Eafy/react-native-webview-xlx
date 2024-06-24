package com.reactnativecommunity.webview;

import android.view.ViewGroup;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebSettingsCompat;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.ThemedReactContext;

public interface RNCXLXWebviewProtocol<T1> {

  public void initWebViewClient();

  public void setupWebChromeClient(ReactContext reactContext, boolean mAllowsFullscreenVideo, boolean mAllowsProtectedMedia);

  public T1 getSettings();

  public void setDownloadingMessage(String msg);

  public void setLackPermissionToDownlaodMessage(String msg);

  public void setAcceptThirdPartyCookies(boolean en);

  public void removeAllCookie();

  public void setUrlPrefixesForDefaultIntent(@Nullable ReadableArray urlPrefixesForDefaultIntent);

  void setForceDarkStrategy(@WebSettingsCompat.ForceDarkStrategy int forceDarkBehavior);

  void setAllowsProtectedMedia(boolean en);
}
