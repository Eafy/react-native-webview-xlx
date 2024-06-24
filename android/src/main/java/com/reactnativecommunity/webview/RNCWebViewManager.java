package com.reactnativecommunity.webview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import com.reactnativecommunity.webview.dft.RNCWebView;
import com.reactnativecommunity.webview.protocal.RNCXLXWebviewProtocol;
import com.reactnativecommunity.webview.x5.RNCX5WebView;
import android.webkit.WebSettings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.views.scroll.ScrollEventType;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.reactnativecommunity.webview.events.TopLoadingErrorEvent;
import com.reactnativecommunity.webview.events.TopHttpErrorEvent;
import com.reactnativecommunity.webview.events.TopLoadingFinishEvent;
import com.reactnativecommunity.webview.events.TopLoadingProgressEvent;
import com.reactnativecommunity.webview.events.TopLoadingStartEvent;
import com.reactnativecommunity.webview.events.TopMessageEvent;
import com.reactnativecommunity.webview.events.TopShouldStartLoadWithRequestEvent;
import com.reactnativecommunity.webview.events.TopRenderProcessGoneEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages instances of {@link android.webkit.WebView}
 * <p>
 * Can accept following commands:
 * - GO_BACK
 * - GO_FORWARD
 * - RELOAD
 * - LOAD_URL
 * <p>
 * {@link android.webkit.WebView} instances could emit following direct events:
 * - topLoadingFinish
 * - topLoadingStart
 * - topLoadingStart
 * - topLoadingProgress
 * - topShouldStartLoadWithRequest
 * <p>
 * Each event will carry the following properties:
 * - target - view's react tag
 * - url - url set for the webview
 * - loading - whether webview is in a loading state
 * - title - title of the current page
 * - canGoBack - boolean, whether there is anything on a history stack to go back
 * - canGoForward - boolean, whether it is possible to request GO_FORWARD command
 */
@ReactModule(name = RNCWebViewManager.REACT_CLASS)
public class RNCWebViewManager extends ViewGroupManager<ViewGroup> {
  private static final String TAG = "RNCWebViewManager";

  public static final int COMMAND_GO_BACK = 1;
  public static final int COMMAND_GO_FORWARD = 2;
  public static final int COMMAND_RELOAD = 3;
  public static final int COMMAND_STOP_LOADING = 4;
  public static final int COMMAND_POST_MESSAGE = 5;
  public static final int COMMAND_INJECT_JAVASCRIPT = 6;
  public static final int COMMAND_LOAD_URL = 7;
  public static final int COMMAND_FOCUS = 8;

  // android commands
  public static final int COMMAND_CLEAR_FORM_DATA = 1000;
  public static final int COMMAND_CLEAR_CACHE = 1001;
  public static final int COMMAND_CLEAR_HISTORY = 1002;

  protected static final String REACT_CLASS = "RNCWebView";
  protected static final String HTML_ENCODING = "UTF-8";
  protected static final String HTML_MIME_TYPE = "text/html";
  protected static final String HTTP_METHOD_POST = "POST";
  // Use `webView.loadUrl("about:blank")` to reliably reset the view
  // state and release page resources (including any running JavaScript).
  protected static final String BLANK_URL = "about:blank";
  public static final String DEFAULT_DOWNLOADING_MESSAGE = "Downloading";
  public static final String DEFAULT_LACK_PERMISSION_TO_DOWNLOAD_MESSAGE =
    "Cannot download files as permission was denied. Please provide permission to write to storage, in order to download files.";

//  protected RNCX5WebChromeClient mWebChromeClient = null;
  protected boolean mAllowsFullscreenVideo = false;
  protected boolean mAllowsProtectedMedia = false;
  protected @Nullable String mUserAgent = null;
  protected @Nullable String mUserAgentWithApplicationName = null;

  public RNCWebViewManager() {
  }

  public RNCWebViewManager(WebViewConfig webViewConfig) {
  }

  @Override
  public String getName() {
    return REACT_CLASS;
  }

  protected ViewGroup createRNCWebViewInstance(ThemedReactContext reactContext) {
    if (RNCWebViewModule.isUseX5()) {
      RNCX5WebView webView = new RNCX5WebView(reactContext);
      String userAgentString = webView.getSettings().getUserAgentString();
      Log.i("Webview", "Webview X5 User-Agent: " + userAgentString);
      return webView;
    } else {
      RNCWebView webView = new RNCWebView(reactContext);
      String userAgentString = webView.getSettings().getUserAgentString();
      Log.i("Webview", "Webview Default User-Agent: " + userAgentString);
      return webView;
    }
  }

  @Override
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  protected ViewGroup createViewInstance(ThemedReactContext reactContext) {
    ViewGroup viewGroup = createRNCWebViewInstance(reactContext);
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)viewGroup;
    protocol.setupWebChromeClient(reactContext, mAllowsFullscreenVideo, mAllowsProtectedMedia);
    protocol.addLifecycleEventListener(reactContext);

    setMixedContentMode(viewGroup, "never");

    // Fixes broken full-screen modals/galleries due to body height being 0.
    viewGroup.setLayoutParams(
      new LayoutParams(LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT));

    return viewGroup;
  }

  @ReactProp(name = "javaScriptEnabled")
  public void setJavaScriptEnabled(ViewGroup view, boolean enabled) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setJavaScriptEnabled(enabled);
  }

  @ReactProp(name = "setBuiltInZoomControls")
  public void setBuiltInZoomControls(ViewGroup view, boolean enabled) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setBuiltInZoomControls(enabled);
  }

  @ReactProp(name = "setDisplayZoomControls")
  public void setDisplayZoomControls(ViewGroup view, boolean enabled) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setDisplayZoomControls(enabled);
  }

  @ReactProp(name = "setSupportMultipleWindows")
  public void setSupportMultipleWindows(ViewGroup view, boolean enabled){
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setSupportMultipleWindows(enabled);
  }

  @ReactProp(name = "showsHorizontalScrollIndicator")
  public void setShowsHorizontalScrollIndicator(ViewGroup view, boolean enabled) {
    view.setHorizontalScrollBarEnabled(enabled);
  }

  @ReactProp(name = "showsVerticalScrollIndicator")
  public void setShowsVerticalScrollIndicator(ViewGroup view, boolean enabled) {
    view.setVerticalScrollBarEnabled(enabled);
  }

  @ReactProp(name = "downloadingMessage")
  public void setDownloadingMessage(ViewGroup view, String message) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.setDownloadingMessage(message);
  }

  @ReactProp(name = "lackPermissionToDownloadMessage")
  public void setLackPermissionToDownlaodMessage(ViewGroup view, String message) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.setLackPermissionToDownlaodMessage(message);
  }

  @ReactProp(name = "cacheEnabled")
  public void setCacheEnabled(ViewGroup view, boolean enabled) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setCacheMode(enabled ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_NO_CACHE);
  }

  @ReactProp(name = "cacheMode")
  public void setCacheMode(ViewGroup view, String cacheModeString) {
    Integer cacheMode;
    switch (cacheModeString) {
      case "LOAD_CACHE_ONLY":
        cacheMode = WebSettings.LOAD_CACHE_ONLY;
        break;
      case "LOAD_CACHE_ELSE_NETWORK":
        cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK;
        break;
      case "LOAD_NO_CACHE":
        cacheMode = WebSettings.LOAD_NO_CACHE;
        break;
      case "LOAD_DEFAULT":
      default:
        cacheMode = WebSettings.LOAD_DEFAULT;
        break;
    }
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setCacheMode(cacheMode);
  }

  @ReactProp(name = "androidHardwareAccelerationDisabled")
  public void setHardwareAccelerationDisabled(ViewGroup view, boolean disabled) {
    if (disabled) {
      view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
  }

  @ReactProp(name = "androidLayerType")
  public void setLayerType(ViewGroup view, String layerTypeString) {
    int layerType = View.LAYER_TYPE_NONE;
    switch (layerTypeString) {
        case "hardware":
          layerType = View.LAYER_TYPE_HARDWARE;
          break;
        case "software":
          layerType = View.LAYER_TYPE_SOFTWARE;
          break;
    }
    view.setLayerType(layerType, null);
  }


  @ReactProp(name = "overScrollMode")
  public void setOverScrollMode(ViewGroup view, String overScrollModeString) {
    Integer overScrollMode;
    switch (overScrollModeString) {
      case "never":
        overScrollMode = View.OVER_SCROLL_NEVER;
        break;
      case "content":
        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS;
        break;
      case "always":
      default:
        overScrollMode = View.OVER_SCROLL_ALWAYS;
        break;
    }
    view.setOverScrollMode(overScrollMode);
  }

  @ReactProp(name = "nestedScrollEnabled")
  public void setNestedScrollEnabled(ViewGroup view, boolean enabled) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.setNestedScrollEnabled(enabled);
  }

  @ReactProp(name = "thirdPartyCookiesEnabled")
  public void setThirdPartyCookiesEnabled(ViewGroup view, boolean enabled) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
      protocol.setAcceptThirdPartyCookies(enabled);
    }
  }

  @ReactProp(name = "textZoom")
  public void setTextZoom(ViewGroup view, int value) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setTextZoom(value);
  }

  @ReactProp(name = "scalesPageToFit")
  public void setScalesPageToFit(ViewGroup view, boolean enabled) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setLoadWithOverviewMode(enabled);
    protocol.getSettingsProtocol().setUseWideViewPort(enabled);
  }

  @ReactProp(name = "domStorageEnabled")
  public void setDomStorageEnabled(ViewGroup view, boolean enabled) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setDomStorageEnabled(enabled);
  }

  @ReactProp(name = "userAgent")
  public void setUserAgent(ViewGroup view, @Nullable String userAgent) {
    if (userAgent != null) {
      mUserAgent = userAgent;
    } else {
      mUserAgent = null;
    }
    this.setUserAgentString(view);
  }

  @ReactProp(name = "applicationNameForUserAgent")
  public void setApplicationNameForUserAgent(ViewGroup view, @Nullable String applicationName) {
    if(applicationName != null) {
      if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        String defaultUserAgent = WebSettings.getDefaultUserAgent(view.getContext());
        mUserAgentWithApplicationName = defaultUserAgent + " " + applicationName;
      }
    } else {
      mUserAgentWithApplicationName = null;
    }
    this.setUserAgentString(view);
  }

  protected void setUserAgentString(ViewGroup view) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    if(mUserAgent != null) {
      protocol.getSettingsProtocol().setUserAgentString(mUserAgent);
    } else if(mUserAgentWithApplicationName != null) {
      protocol.getSettingsProtocol().setUserAgentString(mUserAgentWithApplicationName);
    } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      // handle unsets of `userAgent` prop as long as device is >= API 17
      protocol.getSettingsProtocol().setUserAgentString(WebSettings.getDefaultUserAgent(view.getContext()));
    }
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  @ReactProp(name = "mediaPlaybackRequiresUserAction")
  public void setMediaPlaybackRequiresUserAction(ViewGroup view, boolean requires) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setMediaPlaybackRequiresUserGesture(requires);
  }

  @ReactProp(name = "javaScriptCanOpenWindowsAutomatically")
  public void setJavaScriptCanOpenWindowsAutomatically(ViewGroup view, boolean enabled) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setJavaScriptCanOpenWindowsAutomatically(enabled);
  }

  @ReactProp(name = "allowFileAccessFromFileURLs")
  public void setAllowFileAccessFromFileURLs(ViewGroup view, boolean allow) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setAllowFileAccessFromFileURLs(allow);
  }

  @ReactProp(name = "allowUniversalAccessFromFileURLs")
  public void setAllowUniversalAccessFromFileURLs(ViewGroup view, boolean allow) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setAllowUniversalAccessFromFileURLs(allow);
  }

  @ReactProp(name = "saveFormDataDisabled")
  public void setSaveFormDataDisabled(ViewGroup view, boolean disable) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setSaveFormData(!disable);
  }

  @ReactProp(name = "injectedJavaScript")
  public void setInjectedJavaScript(ViewGroup view, @Nullable String injectedJavaScript) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.setInjectedJavaScript(injectedJavaScript);
  }

  @ReactProp(name = "injectedJavaScriptBeforeContentLoaded")
  public void setInjectedJavaScriptBeforeContentLoaded(ViewGroup view, @Nullable String injectedJavaScriptBeforeContentLoaded) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.setInjectedJavaScriptBeforeContentLoaded(injectedJavaScriptBeforeContentLoaded);
  }

  @ReactProp(name = "injectedJavaScriptForMainFrameOnly")
  public void setInjectedJavaScriptForMainFrameOnly(ViewGroup view, boolean enabled) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.setInjectedJavaScriptForMainFrameOnly(enabled);
  }

  @ReactProp(name = "injectedJavaScriptBeforeContentLoadedForMainFrameOnly")
  public void setInjectedJavaScriptBeforeContentLoadedForMainFrameOnly(ViewGroup view, boolean enabled) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.setInjectedJavaScriptBeforeContentLoadedForMainFrameOnly(enabled);
  }

  @ReactProp(name = "messagingEnabled")
  public void setMessagingEnabled(ViewGroup view, boolean enabled) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.setMessagingEnabled(enabled);
  }

  @ReactProp(name = "messagingModuleName")
  public void setMessagingModuleName(ViewGroup view, String moduleName) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.setMessagingModuleName(moduleName);
  }

  @ReactProp(name = "incognito")
  public void setIncognito(ViewGroup view, boolean enabled) {
    // Don't do anything when incognito is disabled
    if (!enabled) {
      return;
    }

    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.removeAllCookie();

    // Disable caching
    protocol.getSettingsProtocol().setCacheMode(WebSettings.LOAD_NO_CACHE);
    protocol.clearHistory();
    protocol.clearCache(true);

    // No form data or autofill enabled
    protocol.clearFormData();
    protocol.getSettingsProtocol().setSavePassword(false);
    protocol.getSettingsProtocol().setSaveFormData(false);
  }

  @ReactProp(name = "source")
  public void setSource(ViewGroup view, @Nullable ReadableMap source) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    if (source != null) {
      if (source.hasKey("html")) {
        String html = source.getString("html");
        String baseUrl = source.hasKey("baseUrl") ? source.getString("baseUrl") : "";
        protocol.loadDataWithBaseURL(baseUrl, html, HTML_MIME_TYPE, HTML_ENCODING, null);
        return;
      }
      if (source.hasKey("uri")) {
        String url = source.getString("uri");
        String previousUrl = protocol.getUrl();
        if (previousUrl != null && previousUrl.equals(url)) {
          return;
        }
        if (source.hasKey("method")) {
          String method = source.getString("method");
          if (method.equalsIgnoreCase(HTTP_METHOD_POST)) {
            byte[] postData = null;
            if (source.hasKey("body")) {
              String body = source.getString("body");
              try {
                postData = body.getBytes("UTF-8");
              } catch (UnsupportedEncodingException e) {
                postData = body.getBytes();
              }
            }
            if (postData == null) {
              postData = new byte[0];
            }
            protocol.postUrl(url, postData);
            return;
          }
        }
        HashMap<String, String> headerMap = new HashMap<>();
        if (source.hasKey("headers")) {
          ReadableMap headers = source.getMap("headers");
          ReadableMapKeySetIterator iter = headers.keySetIterator();
          while (iter.hasNextKey()) {
            String key = iter.nextKey();
            if ("user-agent".equals(key.toLowerCase(Locale.ENGLISH))) {
              if (protocol.getWebSettings() != null) {
                protocol.getSettingsProtocol().setUserAgentString(headers.getString(key));
              }
            } else {
              headerMap.put(key, headers.getString(key));
            }
          }
        }
        protocol.loadUrl(url, headerMap);
        return;
      }
    }
    protocol.loadUrl(BLANK_URL);
  }

  @ReactProp(name = "basicAuthCredential")
  public void setBasicAuthCredential(ViewGroup view, @Nullable ReadableMap credential) {
    @Nullable BasicAuthCredential basicAuthCredential = null;
    if (credential != null) {
      if (credential.hasKey("username") && credential.hasKey("password")) {
        String username = credential.getString("username");
        String password = credential.getString("password");
        basicAuthCredential = new BasicAuthCredential(username, password);
      }
    }
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.setBasicAuthCredential(basicAuthCredential);
  }

  @ReactProp(name = "onContentSizeChange")
  public void setOnContentSizeChange(ViewGroup view, boolean sendContentSizeChangeEvents) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.setSendContentSizeChangeEvents(sendContentSizeChangeEvents);
  }

  @ReactProp(name = "mixedContentMode")
  public void setMixedContentMode(ViewGroup view, @Nullable String mixedContentMode) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      if (mixedContentMode == null || "never".equals(mixedContentMode)) {
        protocol.getSettingsProtocol().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
      } else if ("always".equals(mixedContentMode)) {
        protocol.getSettingsProtocol().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
      } else if ("compatibility".equals(mixedContentMode)) {
        protocol.getSettingsProtocol().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
      }
    }
  }

  @ReactProp(name = "urlPrefixesForDefaultIntent")
  public void setUrlPrefixesForDefaultIntent(
    ViewGroup view,
    @Nullable ReadableArray urlPrefixesForDefaultIntent) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.setUrlPrefixesForDefaultIntent(urlPrefixesForDefaultIntent);
  }

  @ReactProp(name = "allowsFullscreenVideo")
  public void setAllowsFullscreenVideo(
    ViewGroup view,
    @Nullable Boolean allowsFullscreenVideo) {
    mAllowsFullscreenVideo = allowsFullscreenVideo != null && allowsFullscreenVideo;
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.setupWebChromeClient((ReactContext)view.getContext(), mAllowsFullscreenVideo, mAllowsProtectedMedia);
  }

  @ReactProp(name = "allowFileAccess")
  public void setAllowFileAccess(
    ViewGroup view,
    @Nullable Boolean allowFileAccess) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setAllowFileAccess(allowFileAccess != null && allowFileAccess);
  }

  @ReactProp(name = "geolocationEnabled")
  public void setGeolocationEnabled(
    ViewGroup view,
    @Nullable Boolean isGeolocationEnabled) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setGeolocationEnabled(isGeolocationEnabled != null && isGeolocationEnabled);
  }

  @ReactProp(name = "onScroll")
  public void setOnScroll(ViewGroup view, boolean hasScrollEvent) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.setHasScrollEvent(hasScrollEvent);
  }

  @SuppressLint("WrongConstant")
  @ReactProp(name = "forceDarkOn")
  public void setForceDarkOn(ViewGroup view, boolean enabled) {
    // Only Android 10+ support dark mode
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
      // Switch WebView dark mode
      if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
        RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
        int forceDarkMode = enabled ? WebSettingsCompat.FORCE_DARK_ON : WebSettingsCompat.FORCE_DARK_OFF;
        protocol.getSettingsProtocol().setForceDark(forceDarkMode);
      }

      // Set how WebView content should be darkened.
      // PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING:  checks for the "color-scheme" <meta> tag.
      // If present, it uses media queries. If absent, it applies user-agent (automatic)
      // More information about Force Dark Strategy can be found here:
      // https://developer.android.com/reference/androidx/webkit/WebSettingsCompat#setForceDarkStrategy(android.webkit.WebSettings)
      if (enabled && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
        RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
        protocol.setForceDarkStrategy(WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING);
      }
    }
  }

  @ReactProp(name = "minimumFontSize")
  public void setMinimumFontSize(ViewGroup view, int fontSize) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.getSettingsProtocol().setMinimumFontSize(fontSize);
  }

  @ReactProp(name = "allowsProtectedMedia")
  public void setAllowsProtectedMedia(ViewGroup view, boolean enabled) {
    // This variable is used to keep consistency
    // in case a new WebChromeClient is created
    // (eg. when mAllowsFullScreenVideo changes)
    mAllowsProtectedMedia = enabled;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
      protocol.setAllowsProtectedMedia(enabled);
    }
  }

  @Override
  protected void addEventEmitters(ThemedReactContext reactContext, ViewGroup view) {
    // Do not register default touch emitter and let WebView implementation handle touches
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)view;
    protocol.initWebViewClient();
  }

  @Override
  public Map getExportedCustomDirectEventTypeConstants() {
    Map export = super.getExportedCustomDirectEventTypeConstants();
    if (export == null) {
      export = MapBuilder.newHashMap();
    }
    // Default events but adding them here explicitly for clarity
    export.put(TopLoadingStartEvent.EVENT_NAME, MapBuilder.of("registrationName", "onLoadingStart"));
    export.put(TopLoadingFinishEvent.EVENT_NAME, MapBuilder.of("registrationName", "onLoadingFinish"));
    export.put(TopLoadingErrorEvent.EVENT_NAME, MapBuilder.of("registrationName", "onLoadingError"));
    export.put(TopMessageEvent.EVENT_NAME, MapBuilder.of("registrationName", "onMessage"));
    // !Default events but adding them here explicitly for clarity

    export.put(TopLoadingProgressEvent.EVENT_NAME, MapBuilder.of("registrationName", "onLoadingProgress"));
    export.put(TopShouldStartLoadWithRequestEvent.EVENT_NAME, MapBuilder.of("registrationName", "onShouldStartLoadWithRequest"));
    export.put(ScrollEventType.getJSEventName(ScrollEventType.SCROLL), MapBuilder.of("registrationName", "onScroll"));
    export.put(TopHttpErrorEvent.EVENT_NAME, MapBuilder.of("registrationName", "onHttpError"));
    export.put(TopRenderProcessGoneEvent.EVENT_NAME, MapBuilder.of("registrationName", "onRenderProcessGone"));
    return export;
  }

  @Override
  public @Nullable
  Map<String, Integer> getCommandsMap() {
    return MapBuilder.<String, Integer>builder()
      .put("goBack", COMMAND_GO_BACK)
      .put("goForward", COMMAND_GO_FORWARD)
      .put("reload", COMMAND_RELOAD)
      .put("stopLoading", COMMAND_STOP_LOADING)
      .put("postMessage", COMMAND_POST_MESSAGE)
      .put("injectJavaScript", COMMAND_INJECT_JAVASCRIPT)
      .put("loadUrl", COMMAND_LOAD_URL)
      .put("requestFocus", COMMAND_FOCUS)
      .put("clearFormData", COMMAND_CLEAR_FORM_DATA)
      .put("clearCache", COMMAND_CLEAR_CACHE)
      .put("clearHistory", COMMAND_CLEAR_HISTORY)
      .build();
  }

  @Override
  public void receiveCommand(@NonNull ViewGroup rootT, String commandId, @Nullable ReadableArray args) {
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)rootT;
    switch (commandId) {
      case "goBack":
        protocol.goBack();
        break;
      case "goForward":
        protocol.goForward();
        break;
      case "reload":
        protocol.reload();
        break;
      case "stopLoading":
        protocol.stopLoading();
        break;
      case "postMessage":
        try {
          JSONObject eventInitDict = new JSONObject();
          eventInitDict.put("data", args.getString(0));
          protocol.evaluateJavascriptWithFallback("(function () {" +
            "var event;" +
            "var data = " + eventInitDict.toString() + ";" +
            "try {" +
            "event = new MessageEvent('message', data);" +
            "} catch (e) {" +
            "event = document.createEvent('MessageEvent');" +
            "event.initMessageEvent('message', true, true, data.data, data.origin, data.lastEventId, data.source);" +
            "}" +
            "document.dispatchEvent(event);" +
            "})();");
        } catch (JSONException e) {
          throw new RuntimeException(e);
        }
        break;
      case "injectJavaScript":
        protocol.evaluateJavascriptWithFallback(args.getString(0));
        break;
      case "loadUrl":
        if (args == null) {
          throw new RuntimeException("Arguments for loading an url are null!");
        }
        protocol.setWaitingForCommandLoadUrl(false);
        protocol.loadUrl(args.getString(0));
        break;
      case "requestFocus":
        protocol.requestViewFocus();
        break;
      case "clearFormData":
        protocol.clearFormData();
        break;
      case "clearCache":
        boolean includeDiskFiles = args != null && args.getBoolean(0);
        protocol.clearCache(includeDiskFiles);
        break;
      case "clearHistory":
        protocol.clearHistory();
        break;
    }
    super.receiveCommand(rootT, commandId, args);
  }

  @Override
  public void onDropViewInstance(ViewGroup webView) {
    super.onDropViewInstance(webView);
    RNCXLXWebviewProtocol protocol = (RNCXLXWebviewProtocol)webView;
    protocol.removeLifecycleEventListener();
    protocol.cleanupCallbacksAndDestroy();
  }

  public static RNCWebViewModule getModule(ReactContext reactContext) {
    return reactContext.getNativeModule(RNCWebViewModule.class);
  }
}

