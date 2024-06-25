package com.reactnativecommunity.webview.protocal;

import androidx.annotation.Nullable;
import androidx.webkit.WebSettingsCompat;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.uimanager.ThemedReactContext;
import com.reactnativecommunity.webview.BasicAuthCredential;

import java.util.Map;

public interface RNCXLXWebviewProtocol {

  String checkVersion();

  void addLifecycleEventListener(ThemedReactContext reactContext);

  void removeLifecycleEventListener();

  void initWebViewClient();

  void setupWebChromeClient(ReactContext reactContext, boolean mAllowsFullscreenVideo, boolean mAllowsProtectedMedia);

  Object getWebSettings();

  RNCXLXWebviewSettingsProtocol getSettingsProtocol();

  void setDownloadingMessage(String msg);

  void setLackPermissionToDownlaodMessage(String msg);

  void setAcceptThirdPartyCookies(boolean en);

  void removeAllCookie();

  void setUrlPrefixesForDefaultIntent(@Nullable ReadableArray urlPrefixesForDefaultIntent);

  void setForceDarkStrategy(@WebSettingsCompat.ForceDarkStrategy int forceDarkBehavior);

  void setAllowsProtectedMedia(boolean en);

  String getUrl();

  void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl);

  void postUrl(String url, byte[] postData);

  void loadUrl(String url, Map<String, String> additionalHttpHeaders);

  void loadUrl(String url);

  void setBasicAuthCredential(BasicAuthCredential credential);

  void setSendContentSizeChangeEvents(boolean sendContentSizeChangeEvents);

  void setHasScrollEvent(boolean hasScrollEvent);

  void evaluateJavascriptWithFallback(String script);

  void setWaitingForCommandLoadUrl(boolean en);

  void goBack();

  void goForward();

  void reload();

  void stopLoading();

  void requestViewFocus();

  void clearFormData();

  void clearCache(boolean en);

  void clearHistory();

  void cleanupCallbacksAndDestroy();

  void setNestedScrollEnabled(boolean nestedScrollEnabled);

  void setInjectedJavaScript(@Nullable String js);

  void setInjectedJavaScriptBeforeContentLoaded(@Nullable String js);

  void setInjectedJavaScriptForMainFrameOnly(boolean enabled);

  void setInjectedJavaScriptBeforeContentLoadedForMainFrameOnly(boolean enabled);

  void setMessagingEnabled(boolean enabled);

  void setMessagingModuleName(String moduleName);
}