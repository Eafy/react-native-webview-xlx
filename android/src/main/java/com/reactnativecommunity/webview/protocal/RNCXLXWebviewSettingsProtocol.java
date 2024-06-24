package com.reactnativecommunity.webview.protocal;

public interface RNCXLXWebviewSettingsProtocol {

  void setJavaScriptEnabled(boolean enabled);

  void setBuiltInZoomControls(boolean en);

  void setDisplayZoomControls(boolean en);

  void setSupportMultipleWindows(boolean en);

  void setCacheMode(int mode);

  void setTextZoom(int val);

  void setLoadWithOverviewMode(boolean enabled);

  void setUseWideViewPort(boolean enabled);

  void setDomStorageEnabled(boolean enabled);

  void setUserAgentString(String str);

  void setMediaPlaybackRequiresUserGesture(boolean enable);

  void setJavaScriptCanOpenWindowsAutomatically(boolean enable);

  void setAllowFileAccessFromFileURLs(boolean enable);

  void setAllowUniversalAccessFromFileURLs(boolean enable);

  void setSaveFormData(boolean enable);

  void setSavePassword(boolean enable);

  void setMixedContentMode(int val);

  void setAllowFileAccess(boolean enable);

  void setGeolocationEnabled(boolean enable);

  void setForceDark(int val);

  void setMinimumFontSize(int val);
}
