# AndroidJsBridge
javascript birdge of android

## How to use

### on android side

1.send empty message to javascript
````
JSBridge jsBridge = new JSBridge(mWebView);
JSBridge.Message message = new JSBridge.Message("alertReceivedParmas",
		"hello javascript!");
jsBridge.sendJSMessage(message);
````
2.send message to javascript with javascript callback
````
JSBridge jsBridge = new JSBridge(mWebView);
JSBridge.Message message = new JSBridge.Message("alertReceivedParmas",
		"hello javascript!");
jsBridge.sendJSMessage(message, new JSBridge.JSCallback() {

	@Override
	public void onJSCallback(String callbackId, String callback) {
	  Log.d(TAG, "JSCallback :" + callback);
	}
});
````
3.send empty message with default javascript callback
````
JSBridge jsBridge = new JSBridge(mWebView, new JSBridge.JSCallback() {

	@Override
	public void onJSCallback(String callbackId, String callback) {
		Log.d(TAG, "JSCallback :" + callback);
	}
}, null);
JSBridge.Message message = new JSBridge.Message("alertReceivedParmas",
		"hello javascript!");
jsBridge.sendJSMessage(message);
JSBridge.Message message1 = new JSBridge.Message("alertReceivedParmas",
	"hello world!");
jsBridge.sendJSMessage(message1);
````
4.send empty message with message id
````
JSBridge jsBridge = new JSBridge(mWebView) {

	@Override
	public void handleJSCallback(String messageId, String message) {
		super.handleJSCallback(messageId, message);
		if (MESSAGE_ID.equals(messageId)) {
			Log.d(TAG, "message :" + message);
		}
	}
};
JSBridge.Message message = new JSBridge.Message("alertReceivedParmas",
		"hello javascript!");
jsBridge.sendJSMessage(message, MESSAGE_ID);
````
5.register handler to handle javascript message
````
JSBridge jsBridge = new JSBridge(mWebView);
jsBridge.registerHandler("printReceivedParmas",
		new JSBridge.NativeHandler() {

			@Override
			public void onNativeHandle(final String handle,
					final HandleResult handleResult) {
				mWebView.postDelayed(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(MainActivity.this, handle,
								Toast.LENGTH_LONG).show();
						handleResult.result("hello world");
					}
				}, 1 * 1000);
			}
		});
````
6.register default handler
````
JSBridge jsBridge = new JSBridge(mWebView, null,
		new JSBridge.NativeHandler() {

			@Override
			public void onNativeHandle(String handle,
					HandleResult handleResult) {
				handleResult.result("default handle :" + handle);
			}
		});
````
