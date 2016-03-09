package com.hidaron.AndroidJsBirdge.core;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class JSWebView extends WebView {
	private static final String TAG = JSWebView.class.getSimpleName();

	public JSWebView(Context context) {
		super(context, null);
	}

	public JSWebView(Context context, AttributeSet attrs) {
		super(context, attrs, 0);
	}

	public JSWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		setWebChromeClient(new WebChromeClient());
		setWebViewClient(new WebViewClient());
		removeJsInterface();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void removeJsInterface() {
		if (isHoneycomb()) {
			removeJavascriptInterface("searchBoxJavaBridge_");
			removeJavascriptInterface("accessibility");
			removeJavascriptInterface("accessibilityTraversal");
		}
	}

	private boolean isHoneycomb() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	private boolean isJellyBeanMR1() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
	}

	@Override
	public void addJavascriptInterface(Object obj, String interfaceName) {
		if (isJellyBeanMR1()) {
			super.addJavascriptInterface(obj, interfaceName);
		}
	}
}
