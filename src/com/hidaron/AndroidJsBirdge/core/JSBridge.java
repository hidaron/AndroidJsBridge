package com.hidaron.AndroidJsBirdge.core;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JsPromptResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class JSBridge {
	private static final String TAG = JSBridge.class.getSimpleName();
	private static final String BRIDGE_FILE_NAME = "AndroidWebViewBridge.js";
	private static final String BRIDGE_DEFAULT_MODULE = "AndroidWebViewBridge";
	private static final String BRIDGE_JS_HANDLE_MESSAGE = "_handleMessageFromAndroid";
	private static int UNIQUE_CALLBACK_ID;
	private Map<String, JSCallback> mJSCallbacks = new HashMap<String, JSCallback>();
	private Map<String, NativeHandler> mNativeHandlers = new HashMap<String, NativeHandler>();
	private List<Message> mStartupMessageQueue = new ArrayList<Message>();
	private JSWebView mWebView;
	private JSCallback mDefaultCallback;
	private NativeHandler mDefaultHandler;

	public interface JSCallback {

		public void onJSCallback(String callbackId, String callback);
	}

	public interface NativeHandler {

		public void onNativeHandle(String handle,
								   final HandleResult handleResult);
	}

	public interface HandleResult {

		public void result(String result);
	}

	public static class Message {

		public String data;
		public String handler;
		public String callbackId;
		public String responseId;

		public Message(String data) {
			this.data = data;
		}

		public Message(String handlerName, String data) {
			this.handler = handlerName;
			this.data = data;
		}
	}

	public JSBridge(JSWebView webview) {
		mWebView = webview;
		initBridge();
	}

	public JSBridge(JSWebView webview, JSCallback defaultCallback,
			NativeHandler defaultHandler) {
		this(webview);
		mDefaultCallback = defaultCallback;
		mDefaultHandler = defaultHandler;
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void initBridge() {
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageFinished(WebView view, String url) {
				Log.d(TAG, "onPageFinished => url :" + url);
				String js = getJsBridge();
				if (!TextUtils.isEmpty(js)) {
					mWebView.loadUrl("javascript:" + js);
				} else {
					Log.e(TAG, "load JSBridge failed!");
				}
				if (null != mStartupMessageQueue) {
					int messageQueueLength = mStartupMessageQueue.size();
					for (int i = 0; i < messageQueueLength; i++) {
						Message message = mStartupMessageQueue.get(i);
						dispatchNativeMessage(message);
					}
					mStartupMessageQueue.clear();
					mStartupMessageQueue = null;
				}
			}

		});
		mWebView.setWebChromeClient(new WebChromeClient() {

			@Override
			public boolean onJsPrompt(WebView view, String url, String message,
					String defaultValue, JsPromptResult result) {
				Log.i(TAG, "onJsPrompt => url :" + url + " message :" + message
						+ " defaultValue :" + defaultValue);
				result.cancel();
				try {
					JSONObject msgJson = new JSONObject(message);
					if (null != msgJson) {
						try {
							JSONObject data = msgJson.getJSONObject("data");
							if (null != data) {
								boolean hasResponseData = data
										.has("responseData");
								if (hasResponseData) {
									dispatchJSCallback(message);
								} else {
									dispatchJSMessage(message);
								}
								return true;
							}
						} catch (JSONException ignore) {
						}
						try {
							String data = msgJson.getString("data");
							if (null != data) {
								dispatchJSMessage(message);
							}
						} catch (JSONException ignore) {
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				return true;
			}

		});
	}

	private void dispatchJSMessage(String message) {
		try {
			final JSONObject msgObj = new JSONObject(message);
			final String data = msgObj.getString("data");
			String responseId = null;
			String handlerName = null;
			boolean hasCallbackId = msgObj.has("callbackId");
			if (hasCallbackId) {
				responseId = msgObj.getString("callbackId");
			}
			boolean hasHandlerName = msgObj.has("handlerName");
			if (hasHandlerName) {
				handlerName = msgObj.getString("handlerName");
			}

			final String handleResponseId = responseId;
			final HandleResult handleResult = new HandleResult() {

				@Override
				public void result(String result) {
					if (!TextUtils.isEmpty(handleResponseId)) {
						Message responseMsg = new Message(result);
						responseMsg.responseId = handleResponseId;
						send(responseMsg);
					}
				}
			};

			if (!TextUtils.isEmpty(handlerName)) {
				if (mNativeHandlers.containsKey(handlerName)) {
					final NativeHandler handler = mNativeHandlers
							.get(handlerName);
					if (null != handler) {
						handler.onNativeHandle(data, handleResult);
					}
				} else {
					if (null != mDefaultHandler) {
						mDefaultHandler.onNativeHandle(data, handleResult);
					}
				}
			} else {
				if (null != mDefaultHandler) {
					mDefaultHandler.onNativeHandle(data, handleResult);
				}
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void dispatchJSCallback(String message) {
		synchronized (mJSCallbacks) {
			try {
				JSONObject msgJson = new JSONObject(message);
				JSONObject data = msgJson.getJSONObject("data");
				String callbackId = data.getString("responseId");
				JSONObject callbackData = data.getJSONObject("responseData");
				String callbackDataStr = "";
				if (null != callbackData) {
					callbackDataStr = callbackData.toString();
				}
				if (!TextUtils.isEmpty(callbackId)) {
					if (mJSCallbacks.containsKey(callbackId)) {
						JSCallback callback = mJSCallbacks.remove(callbackId);
						if (null != callback) {
							callback.onJSCallback(callbackId, callbackDataStr);
						} else {
							if (null != mDefaultCallback) {
								mDefaultCallback.onJSCallback(callbackId,
										callbackDataStr);
							}
						}
					} else {
						handleJSCallback(callbackId, callbackDataStr);
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	public void handleJSCallback(String messageId, String message) {
		// do stuff
	}

	public void sendJSMessage(Message message) {
		synchronized (mJSCallbacks) {
			message.callbackId = genCallbackId();
			mJSCallbacks.put(message.callbackId, null);
			send(message);
		}
	}

	public void sendJSMessage(Message message, JSCallback callback) {
		synchronized (mJSCallbacks) {
			message.callbackId = genCallbackId();
			mJSCallbacks.put(message.callbackId, callback);
			send(message);
		}
	}

	public void sendJSMessage(Message message, String messageId) {
		synchronized (mJSCallbacks) {
			if (!mJSCallbacks.containsKey(messageId)) {
				message.callbackId = messageId;
				send(message);
			} else {
				Log.d(TAG, "send twice, message has been exits : " + messageId);
			}
		}
	}

	private void send(Message message) {
		if (null != message && !TextUtils.isEmpty(message.data)) {
			addToMessageQueue(message);
		}
	}

	public void removeCallback(String messageId) {
		synchronized (mJSCallbacks) {
			if (mJSCallbacks.containsKey(messageId)) {
				mJSCallbacks.remove(messageId);
			}
		}
	}

	public void registerHandler(String handlerName, NativeHandler handler) {
		if (!mNativeHandlers.containsKey(handlerName)) {
			mNativeHandlers.put(handlerName, handler);
		} else {
			Log.e(TAG, "register twice, handler has been exits : "
					+ handlerName);
		}
	}

	public void removeHandler(String handlerName) {
		if (mNativeHandlers.containsKey(handlerName)) {
			mNativeHandlers.remove(handlerName);
		}
	}

	private String genCallbackId() {
		return "ANDROID_CALLBACK_" + (++UNIQUE_CALLBACK_ID);
	}

	private void addToMessageQueue(Message message) {
		if (null != mStartupMessageQueue) {
			mStartupMessageQueue.add(message);
			return;
		}
		dispatchNativeMessage(message);
	}

	private void dispatchNativeMessage(Message message) {
		if (null != message) {
			JSONObject jsMsg = new JSONObject();
			try {
				if (!TextUtils.isEmpty(message.responseId)) {
					jsMsg.put("responseId", message.responseId);
					jsMsg.put("responseData", message.data);
				} else {
					jsMsg.put("data", message.data);
				}
				if (!TextUtils.isEmpty(message.callbackId)) {
					jsMsg.put("callbackId", message.callbackId);
				}

				if (!TextUtils.isEmpty(message.handler)) {
					jsMsg.put("handlerName", message.handler);
				}
				final String jsStr = jsMsg.toString();
				mWebView.post(new Runnable() {

					@Override
					public void run() {
						mWebView.loadUrl("javascript: " + BRIDGE_DEFAULT_MODULE
								+ "." + BRIDGE_JS_HANDLE_MESSAGE + "('" + jsStr
								+ "');");
					}
				});
			} catch (JSONException e) {
				e.printStackTrace();
			}

		}
	}

	private String getJsBridge() {
		StringBuilder js = new StringBuilder();
		InputStream is = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		BufferedReader br = null;
		try {
			is = mWebView.getContext().getAssets().open(BRIDGE_FILE_NAME);
			String read = null;
			br = new BufferedReader(new InputStreamReader(is));
			while (null != (read = br.readLine())) {
				js.append(read);
			}
			return js.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != is) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (null != br) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (null != bos) {
				try {
					bos.flush();
					bos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

}
