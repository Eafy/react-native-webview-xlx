package com.reactnativecommunity.webview.x5;

import static com.reactnativecommunity.webview.RNCWebViewManager.DEFAULT_DOWNLOADING_MESSAGE;
import static com.reactnativecommunity.webview.RNCWebViewManager.DEFAULT_LACK_PERMISSION_TO_DOWNLOAD_MESSAGE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebSettingsCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.CatalystInstance;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.common.build.ReactBuildConfig;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.ContentSizeChangeEvent;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.facebook.react.views.scroll.OnScrollDispatchHelper;
import com.facebook.react.views.scroll.ScrollEvent;
import com.facebook.react.views.scroll.ScrollEventType;
import com.reactnativecommunity.webview.BasicAuthCredential;
import com.reactnativecommunity.webview.RNCWebViewManager;
import com.reactnativecommunity.webview.RNCWebViewModule;
import com.reactnativecommunity.webview.RNCXLXWebviewProtocol;
import com.reactnativecommunity.webview.URLUtil;
import com.reactnativecommunity.webview.events.TopMessageEvent;
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.DownloadListener;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Subclass of {@link WebView} that implements {@link LifecycleEventListener} interface in order
 * to call {@link WebView#destroy} on activity destroy event and also to clear the client
 */
public class RNCX5WebView extends WebView implements LifecycleEventListener,
  RNCXLXWebviewProtocol<WebSettings> {
  private static final String TAG = "RNCWebViewManager";
  protected static final String JAVASCRIPT_INTERFACE = "ReactNativeWebView";

  protected @Nullable
  String injectedJS;
  protected @Nullable
  String injectedJSBeforeContentLoaded;

  /**
   * android.webkit.WebChromeClient fundamentally does not support JS injection into frames other
   * than the main frame, so these two properties are mostly here just for parity with iOS & macOS.
   */
  protected boolean injectedJavaScriptForMainFrameOnly = true;
  protected boolean injectedJavaScriptBeforeContentLoadedForMainFrameOnly = true;

  protected boolean messagingEnabled = false;
  protected @Nullable
  String messagingModuleName;
  protected @Nullable
  RNCX5WebViewClient mRNCWebViewClient;
  protected @Nullable
  CatalystInstance mCatalystInstance;
  protected boolean sendContentSizeChangeEvents = false;
  private OnScrollDispatchHelper mOnScrollDispatchHelper;
  protected boolean hasScrollEvent = false;
  protected boolean nestedScrollEnabled = false;
  public ProgressChangedFilter progressChangedFilter;

  private ThemedReactContext mReactContext = null;

  /**
   * WebView must be created with an context of the current activity
   * <p>
   * Activity Context is required for creation of dialogs internally by WebView
   * Reactive Native needed for access to ReactNative internal system functionality
   */
  public RNCX5WebView(ThemedReactContext reactContext) {
    super(reactContext);
    mReactContext = reactContext;
    this.createCatalystInstance();
    progressChangedFilter = new ProgressChangedFilter();
    getX5WebViewExtension().setVerticalTrackDrawable(null);   //X5
  }

  public void setIgnoreErrFailedForThisURL(String url) {
    mRNCWebViewClient.setIgnoreErrFailedForThisURL(url);
  }

  public void setBasicAuthCredential(BasicAuthCredential credential) {
    mRNCWebViewClient.setBasicAuthCredential(credential);
  }

  public void setSendContentSizeChangeEvents(boolean sendContentSizeChangeEvents) {
    this.sendContentSizeChangeEvents = sendContentSizeChangeEvents;
  }

  public void setHasScrollEvent(boolean hasScrollEvent) {
    this.hasScrollEvent = hasScrollEvent;
  }

  public void setNestedScrollEnabled(boolean nestedScrollEnabled) {
    this.nestedScrollEnabled = nestedScrollEnabled;
  }

  @Override
  public void onHostResume() {
    // do nothing
  }

  @Override
  public void onHostPause() {
    // do nothing
  }

  @Override
  public void onHostDestroy() {
    cleanupCallbacksAndDestroy();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (this.nestedScrollEnabled) {
      requestDisallowInterceptTouchEvent(true);
    }
    return super.onTouchEvent(event);
  }

  @Override
  protected void onSizeChanged(int w, int h, int ow, int oh) {
    super.onSizeChanged(w, h, ow, oh);

    if (sendContentSizeChangeEvents) {
      dispatchEvent(
        this,
        new ContentSizeChangeEvent(
          this.getId(),
          w,
          h
        )
      );
    }
  }

  @Override
  public void setWebViewClient(WebViewClient client) {
    super.setWebViewClient(client);
    if (client instanceof RNCX5WebViewClient) {
      mRNCWebViewClient = (RNCX5WebViewClient) client;
      mRNCWebViewClient.setProgressChangedFilter(progressChangedFilter);
    }
  }

  WebChromeClient mWebChromeClient;
  @Override
  public void setWebChromeClient(WebChromeClient client) {
    this.mWebChromeClient = client;
    super.setWebChromeClient(client);
    if (client instanceof RNCX5WebChromeClient) {
      ((RNCX5WebChromeClient) client).setProgressChangedFilter(progressChangedFilter);
    }
  }

  public @Nullable
  RNCX5WebViewClient getRNCWebViewClient() {
    return mRNCWebViewClient;
  }

  public void setInjectedJavaScript(@Nullable String js) {
    injectedJS = js;
  }

  public void setInjectedJavaScriptBeforeContentLoaded(@Nullable String js) {
    injectedJSBeforeContentLoaded = js;
  }

  public void setInjectedJavaScriptForMainFrameOnly(boolean enabled) {
    injectedJavaScriptForMainFrameOnly = enabled;
  }

  public void setInjectedJavaScriptBeforeContentLoadedForMainFrameOnly(boolean enabled) {
    injectedJavaScriptBeforeContentLoadedForMainFrameOnly = enabled;
  }

  protected RNCWebViewBridge createRNCWebViewBridge(RNCX5WebView webView) {
    return new RNCWebViewBridge(webView);
  }

  protected void createCatalystInstance() {
    ReactContext reactContext = (ReactContext) this.getContext();

    if (reactContext != null) {
      mCatalystInstance = reactContext.getCatalystInstance();
    }
  }

  @SuppressLint("AddJavascriptInterface")
  public void setMessagingEnabled(boolean enabled) {
    if (messagingEnabled == enabled) {
      return;
    }

    messagingEnabled = enabled;

    if (enabled) {
      addJavascriptInterface(createRNCWebViewBridge(this), JAVASCRIPT_INTERFACE);
    } else {
      removeJavascriptInterface(JAVASCRIPT_INTERFACE);
    }
  }

  public void setMessagingModuleName(String moduleName) {
    messagingModuleName = moduleName;
  }

  public void evaluateJavascriptWithFallback(String script) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      evaluateJavascript(script, null);
      return;
    }

    try {
      loadUrl("javascript:" + URLEncoder.encode(script, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      // UTF-8 should always be supported
      throw new RuntimeException(e);
    }
  }

  public void callInjectedJavaScript() {
    if (getSettings().getJavaScriptEnabled() &&
      injectedJS != null &&
      !TextUtils.isEmpty(injectedJS)) {
      evaluateJavascriptWithFallback("(function() {\n" + injectedJS + ";\n})();");
    }
  }

  public void callInjectedJavaScriptBeforeContentLoaded() {
    if (getSettings().getJavaScriptEnabled() &&
      injectedJSBeforeContentLoaded != null &&
      !TextUtils.isEmpty(injectedJSBeforeContentLoaded)) {
      evaluateJavascriptWithFallback("(function() {\n" + injectedJSBeforeContentLoaded + ";\n})();");
    }
  }

  public void onMessage(String message) {
    ReactContext reactContext = (ReactContext) this.getContext();
    RNCX5WebView mContext = this;

    if (mRNCWebViewClient != null) {
      WebView webView = this;
      webView.post(new Runnable() {
        @Override
        public void run() {
          if (mRNCWebViewClient == null) {
            return;
          }
          WritableMap data = mRNCWebViewClient.createWebViewEvent(webView, webView.getUrl());
          data.putString("data", message);

          if (mCatalystInstance != null) {
            mContext.sendDirectMessage("onMessage", data);
          } else {
            dispatchEvent(webView, new TopMessageEvent(webView.getId(), data));
          }
        }
      });
    } else {
      WritableMap eventData = Arguments.createMap();
      eventData.putString("data", message);

      if (mCatalystInstance != null) {
        this.sendDirectMessage("onMessage", eventData);
      } else {
        dispatchEvent(this, new TopMessageEvent(this.getId(), eventData));
      }
    }
  }

  protected void sendDirectMessage(final String method, WritableMap data) {
    WritableNativeMap event = new WritableNativeMap();
    event.putMap("nativeEvent", data);

    WritableNativeArray params = new WritableNativeArray();
    params.pushMap(event);

    mCatalystInstance.callFunction(messagingModuleName, method, params);
  }

  protected void onScrollChanged(int x, int y, int oldX, int oldY) {
    super.onScrollChanged(x, y, oldX, oldY);

    if (!hasScrollEvent) {
      return;
    }

    if (mOnScrollDispatchHelper == null) {
      mOnScrollDispatchHelper = new OnScrollDispatchHelper();
    }

    if (mOnScrollDispatchHelper.onScrollChanged(x, y)) {
      ScrollEvent event = ScrollEvent.obtain(
        this.getId(),
        ScrollEventType.SCROLL,
        x,
        y,
        mOnScrollDispatchHelper.getXFlingVelocity(),
        mOnScrollDispatchHelper.getYFlingVelocity(),
        this.computeHorizontalScrollRange(),
        this.computeVerticalScrollRange(),
        this.getWidth(),
        this.getHeight());

      dispatchEvent(this, event);
    }
  }

  protected void dispatchEvent(WebView webView, Event event) {
    ReactContext reactContext = (ReactContext) webView.getContext();
    EventDispatcher eventDispatcher =
      reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher();
    eventDispatcher.dispatchEvent(event);
  }

  public void cleanupCallbacksAndDestroy() {
    setWebViewClient(null);
    destroy();
  }

  @Override
  public void destroy() {
    if (mWebChromeClient != null) {
      mWebChromeClient.onHideCustomView();
    }
    super.destroy();
  }

  protected class RNCWebViewBridge {
    RNCX5WebView mContext;

    RNCWebViewBridge(RNCX5WebView c) {
      mContext = c;
    }

    /**
     * This method is called whenever JavaScript running within the web view calls:
     * - window[JAVASCRIPT_INTERFACE].postMessage
     */
    @JavascriptInterface
    public void postMessage(String message) {
      mContext.onMessage(message);
    }
  }

  public static class ProgressChangedFilter {
    private boolean waitingForCommandLoadUrl = false;

    public void setWaitingForCommandLoadUrl(boolean isWaiting) {
      waitingForCommandLoadUrl = isWaiting;
    }

    public boolean isWaitingForCommandLoadUrl() {
      return waitingForCommandLoadUrl;
    }
  }

  /*************************  RNCXLXWebviewProtocol  *********************************/

  @Override
  public void initWebViewClient() {
    setWebViewClient(new RNCX5WebViewClient());
  }

  @Override
  public void setupWebChromeClient(ReactContext reactContext, boolean mAllowsFullscreenVideo, boolean mAllowsProtectedMedia) {
    Activity activity = reactContext.getCurrentActivity();

    if (mAllowsFullscreenVideo && activity != null) {
      int initialRequestedOrientation = activity.getRequestedOrientation();

      mWebChromeClient = new RNCX5WebChromeClient(reactContext, this) {
        @Override
        public Bitmap getDefaultVideoPoster() {
          return Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        }

        @Override
        public void onShowCustomView(View view, IX5WebChromeClient.CustomViewCallback callback) {
          if (mVideoView != null) {
            callback.onCustomViewHidden();
            return;
          }

          mVideoView = view;
          mCustomViewCallback = callback;

          activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mVideoView.setSystemUiVisibility(FULLSCREEN_SYSTEM_UI_VISIBILITY);
            activity.getWindow().setFlags(
              WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
              WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            );
          }

          mVideoView.setBackgroundColor(Color.BLACK);

          // Since RN's Modals interfere with the View hierarchy
          // we will decide which View to hide if the hierarchy
          // does not match (i.e., the WebView is within a Modal)
          // NOTE: We could use `mWebView.getRootView()` instead of `getRootView()`
          // but that breaks the Modal's styles and layout, so we need this to render
          // in the main View hierarchy regardless
          ViewGroup rootView = getRootView();
          rootView.addView(mVideoView, FULLSCREEN_LAYOUT_PARAMS);

          // Different root views, we are in a Modal
          if (rootView.getRootView() != mWebView.getRootView()) {
            mWebView.getRootView().setVisibility(View.GONE);
          } else {
            // Same view hierarchy (no Modal), just hide the WebView then
            mWebView.setVisibility(View.GONE);
          }

          mReactContext.addLifecycleEventListener(this);
        }

        @Override
        public void onHideCustomView() {
          if (mVideoView == null) {
            return;
          }

          // Same logic as above
          ViewGroup rootView = getRootView();

          if (rootView.getRootView() != mWebView.getRootView()) {
            mWebView.getRootView().setVisibility(View.VISIBLE);
          } else {
            // Same view hierarchy (no Modal)
            mWebView.setVisibility(View.VISIBLE);
          }

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
          }

          rootView.removeView(mVideoView);
          mCustomViewCallback.onCustomViewHidden();

          mVideoView = null;
          mCustomViewCallback = null;

          activity.setRequestedOrientation(initialRequestedOrientation);

          mReactContext.removeLifecycleEventListener(this);
        }
      };
    } else {
      if (mWebChromeClient != null) {
        mWebChromeClient.onHideCustomView();
      }

      mWebChromeClient = new RNCX5WebChromeClient(reactContext, this) {
        @Override
        public Bitmap getDefaultVideoPoster() {
          return Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        }
      };
    }
    ((RNCX5WebChromeClient)mWebChromeClient).setAllowsProtectedMedia(mAllowsProtectedMedia);
    this.setWebChromeClient(mWebChromeClient);

    if (ReactBuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      WebView.setWebContentsDebuggingEnabled(true);
    }

    setDownloadListener(new DownloadListener() {
      public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        setIgnoreErrFailedForThisURL(url);

        RNCWebViewModule module = RNCWebViewManager.getModule(reactContext);

        DownloadManager.Request request;
        try {
          request = new DownloadManager.Request(Uri.parse(url));
        } catch (IllegalArgumentException e) {
          Log.w(TAG, "Unsupported URI, aborting download", e);
          return;
        }

        String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
        String downloadMessage = "Downloading " + fileName;

        //Attempt to add cookie, if it exists
        URL urlObj = null;
        try {
          urlObj = new URL(url);
          String baseUrl = urlObj.getProtocol() + "://" + urlObj.getHost();
          String cookie = CookieManager.getInstance().getCookie(baseUrl);
          request.addRequestHeader("Cookie", cookie);
        } catch (MalformedURLException e) {
          Log.w(TAG, "Error getting cookie for DownloadManager", e);
        }

        //Finish setting up request
        request.addRequestHeader("User-Agent", userAgent);
        request.setTitle(fileName);
        request.setDescription(downloadMessage);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

        module.setDownloadRequest(request);

        if (module.grantFileDownloaderPermissions(getDownloadingMessage(), getLackPermissionToDownloadMessage())) {
          module.downloadFile(getDownloadingMessage());
        }
      }
    });
  }

  protected @Nullable String mDownloadingMessage = null;
  protected @Nullable String mLackPermissionToDownloadMessage = null;
  private String getDownloadingMessage() {
    return  mDownloadingMessage == null ? DEFAULT_DOWNLOADING_MESSAGE : mDownloadingMessage;
  }

  private String getLackPermissionToDownloadMessage() {
    return  mDownloadingMessage == null ? DEFAULT_LACK_PERMISSION_TO_DOWNLOAD_MESSAGE : mLackPermissionToDownloadMessage;
  }

  @Override
  public WebSettings getSettings() {
    return super.getSettings();
  }

  public void setDownloadingMessage(String msg) {
    mDownloadingMessage = msg;
  }

  public void setLackPermissionToDownlaodMessage(String msg) {
    mLackPermissionToDownloadMessage = msg;
  }
  public void setAcceptThirdPartyCookies(boolean en) {
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, en);
  }

  public void removeAllCookie() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      CookieManager.getInstance().removeAllCookies(null);
    } else {
      CookieManager.getInstance().removeAllCookie();
    }
  }

  public void setUrlPrefixesForDefaultIntent(@Nullable ReadableArray urlPrefixesForDefaultIntent) {
    RNCX5WebViewClient client = getRNCWebViewClient();
    if (client != null && urlPrefixesForDefaultIntent != null) {
      client.setUrlPrefixesForDefaultIntent(urlPrefixesForDefaultIntent);
    }
  }

  public void setForceDarkStrategy(@WebSettingsCompat.ForceDarkStrategy int forceDarkBehavior) {
  }

  public void setAllowsProtectedMedia(boolean en) {
    WebChromeClient client = getWebChromeClient();
    if (client != null && client instanceof RNCX5WebChromeClient) {
      ((RNCX5WebChromeClient) client).setAllowsProtectedMedia(en);
    }
  }
}