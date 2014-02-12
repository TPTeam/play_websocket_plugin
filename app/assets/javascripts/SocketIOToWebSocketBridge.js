var SITWS = SITWS || {};

(function(func) {
	if (typeof require === 'function' && typeof define === 'function') {
		// define module, put dependencies here
		define([ 'socketio', 'websocket_plugin' ], func);
	} else {
		// go ahead with global scope pollution :-)
		SITWS = func(io, WS);
	}
})(function(socketio, ws) {
	
	/**
	 * @preserve Copyright 2013 mrDOob.
	 *           https://github.com/mrdoob/eventtarget.js/ THankS mr DOob!
	 */
	var EventTarget = function() {

		/*
		 * event = { type: 'eventName', content : data }
		 */

		var listeners = {};

		/**
		 * {function(string, function)}
		 */
		this.addEventListener = this.on = function(type, listener) {

			if (listeners[type] === undefined) {

				listeners[type] = [];

			}

			if (listeners[type].indexOf(listener) === -1) {

				listeners[type].push(listener);
			}

		};

		this.dispatchEvent = this.emit = function(event) {

			for ( var listener in listeners[event.type]) {

				listeners[event.type][listener](event);

			}

		};

		this.removeEventListener = this.off = function(type, listener) {

			var index = listeners[type].indexOf(listener);

			if (index !== -1) {

				listeners[type].splice(index, 1);

			}

		};

	};

	var SITWS = function(_webSocketId, _placeholderCssSel) {
		
		EventTarget.call(this);

		var that = this;
		
		this.socketio = null
			
		this.id = _webSocketId
		this.placeholderCssSel = _placeholderCssSel

		this.address = ""
		this.socketio_address = ""
			
			
		this.hangOutCheck = null
		this.pong = JSON.stringify({
			"pong" : true
		})

		this.verbose = false;
		
	}
	
	SITWS.prototype = {

			isReady : function() {
				return (this.wsSocket && this.wsSocket.readyState === 1);
			},

			close : function(msg) {
				console.log("WebSocket: " + this.id + " closing by me");
				this.wsSocket.close();
			},


			init : function() {
				var that = this;

				console.log("SITWebSocket: " + that.id + " connecting to " + this.address+ " socketio "+ this.socketio_address);
				
				that.socketio = socketio.connect(that.socketio_address, {
					'force new connection' : true
				})
				that.socketio.on('message', function(data) {
		            try {
		            	var toSend = 	{
	            							'socketio' : JSON.parse(data)
            							}
		            	var toSendString = JSON.stringify(toSend)
		            	// here is your handler on messages from server
		            	that.wsSocket.send(toSendString)
		            } catch(err) {
		            	console.warn("error ",err)
		                //console.warn("received invalid json")
		            }
		        })

				try {
					this.wsSocket = window['MozWebSocket'] ? new MozWebSocket(this.address) : new WebSocket(this.address);
					var that = this;
					this.wsSocket.onopen = function() {
						console.log("WebSocket: " + that.id + " is opened ", that.wsSocket);
						that.emit({
							type : 'open',
							content : that
						});

					};
					this.wsSocket.onerror = function(event) {
						console.error("WebSocket: " + that.id + " has had an error. ", that.wsSocket);
					};
					this.wsSocket.onmessage = function(event) {

						if (that.verbose) {
							console.log("WebSocket: " + that.id + " has received ", event);
						}

						var data = JSON.parse(event.data);

						if ((data === undefined) || (data.error)) {
							console.error("WebSocket: " + that.id + " data error -> ", event.data);
							that.emit({
								type : 'error',
								content : event
							});
							that.close();

						} else {

							var msg = jQuery.parseJSON(event.data);

							if (msg.ping != undefined) {

								if ($(that.placeholderCssSel).length > 0) {
									try {
										clearTimeout(that.hangOutCheck);
									} catch (err) {
										console.error("ERROR -> ", err);
									}
									that.hangOutCheck = setTimeout(function() {
										that.init();
										// that.close()
									}, 1000)
									this.send(that.pong);
								} else {

									this.close();
								}

							} else {
								that.emit({
									type : 'message',
									content : msg
								});
							}
						}
					};
					this.wsSocket.onclose = function() {
						console.log("WebSocket: " + that.id + " closed by server. Placeholder: " + that.placeholderCssSel);
						that.emit({
							type : 'close',
							content : that
						});
						if ($(that.placeholderCssSel).length > 0) {
							// reactivate connection
							setTimeout(function() {
								that.init();
							}, 500);
						}
					};
				} catch (err) {
					console.error("WebSocket: " + that.id + " cannot connect to WebSocket at " + this.address);
					console.error("WebSocket: " + that.id + " error -> ", err);
				}

			},

			send : function(msg) {
				if (this.wsSocket !== null && this.wsSocket !== undefined) {
					try {
						if (this.verbose) {
							console.log("WebSocket: " + this.id + " is sending ", msg);
						}
						this.wsSocket.send(JSON.stringify(msg));
					} catch (e) {
						console.warn("WebSocket: " + this.id + " reported error ", e);
					}
				} else {
					console.error("WebSocket: this.wsSocket is not ready, maybe missing 'init'? ", this);
				}
			}
		}
	
	return SITWS;
})