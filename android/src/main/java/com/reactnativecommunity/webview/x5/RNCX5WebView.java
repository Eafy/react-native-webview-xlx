package com.reactnativecommunity.webview.x5;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.webkit.JavascriptInterface;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.CatalystInstance;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.ContentSizeChangeEvent;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.facebook.react.views.scroll.OnScrollDispatchHelper;
import com.facebook.react.views.scroll.ScrollEvent;
import com.facebook.react.views.scroll.ScrollEventType;
import com.reactnativecommunity.webview.BasicAuthCredential;
import com.reactnativecommunity.webview.events.TopMessageEvent;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Subclass of {@link WebView} that implements {@link LifecycleEventListener} interface in order
 * to call {@link WebView#destroy} on activity destroy event and also to clear the client
 */
public class RNCX5WebView extends WebView implements LifecycleEventListener {
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

  /**
   * WebView must be created with an context of the current activity
   * <p>
   * Activity Context is required for creation of dialogs internally by WebView
   * Reactive Native needed for access to ReactNative internal system functionality
   */
  public RNCX5WebView(ThemedReactContext reactContext) {
    super(reactContext);
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
}