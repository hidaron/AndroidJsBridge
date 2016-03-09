;(function() {
	if (window.AndroidWebViewBridge) { return; }
	var startupRCVDMessageQueue = [];
	var messageHandlers = {};
	var responseCallbacks = {};
	var uniqueId = 1;

	function init(defaultHandler) {
		if (AndroidWebViewBridge._defaultHandler) { throw new Error('AndroidWebViewBridge.init called twice'); }
		AndroidWebViewBridge._defaultHandler = defaultHandler;
		var receivedMessages = startupRCVDMessageQueue;
		startupRCVDMessageQueue = null;
		for (var i=0; i<receivedMessages.length; i++) {
			dispatchMessageFromAndroid(receivedMessages[i]);
		}
	}

	function _handleMessageFromAndroid(messageJSON) {
		if (startupRCVDMessageQueue) {
			startupRCVDMessageQueue.push(messageJSON);
		} 
		else {
			dispatchMessageFromAndroid(messageJSON);
		}
	}

	function sendDataToAndroid(data, responseCallback) {
		callAndroidHandler(null, data, responseCallback);
	}
  
    function callAndroidHandler(handlerName, data, responseCallback) {
        var message = ( handlerName!=null && handlerName!=undefined ) ? { handlerName:handlerName, data:data } : { data:data};
        if (responseCallback) {
            var callbackId = 'cb_'+(uniqueId++)+'_JS_'+new Date().getTime();
            responseCallbacks[callbackId] = responseCallback;
            message['callbackId'] = callbackId;
		}
		sendMessageToAndroid(message);
    }

	function registerHandlerForAndroid(handlerName, handler) {
		messageHandlers[handlerName] = handler;
	}

	function dispatchMessageFromAndroid(messageJSON) {
		setTimeout(function timeoutDispatchMessageFromAndroid() {
			var message = JSON.parse(messageJSON);
			var messageHandler;
			var responseCallback;
			if (message.responseId) {
				responseCallback = responseCallbacks[message.responseId];
				if (!responseCallback) { return; }
				responseCallback(message.responseData);
				delete responseCallbacks[message.responseId];
			} 
			else {
				if (message.callbackId) {
					var callbackResponseId = message.callbackId;
					responseCallback = function(responseData) {	
						sendDataToAndroid({ responseId:callbackResponseId, responseData:responseData });
					};
				}
				var handler = AndroidWebViewBridge._defaultHandler;
				if (message.handlerName) {
					handler = messageHandlers[message.handlerName];
				}
				try {
					handler(message.data, responseCallback);
				} 
				catch(exception) {
					if (typeof console != 'undefined') {
						console.log("AndroidWebViewBridge: WARNING: javascript handler threw.", message, exception);
					}
				}
			}
		});
	}

	function sendMessageToAndroid(message){
		prompt(JSON.stringify(message));
	}

	window.AndroidWebViewBridge = {
		init: init,
		sendDataToAndroid: sendDataToAndroid,
		registerHandlerForAndroid: registerHandlerForAndroid,
		callAndroidHandler: callAndroidHandler,
		sendMessageToAndroid: sendMessageToAndroid,
		_handleMessageFromAndroid: _handleMessageFromAndroid
	};

	var readyEvent = document.createEvent('Events');
	readyEvent.initEvent('AndroidWebViewBridgeReady');
	document.dispatchEvent(readyEvent);
})();
