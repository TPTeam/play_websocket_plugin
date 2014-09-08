var WS = WS || {};

(function (func) {
    if (typeof require === 'function' && typeof define === 'function') {
        // define module, put dependencies here
        define([ '' ], func);
    } else {
        // go ahead with global scope pollution :-)
        WS = func();
    }
})(function () {
    "use strict";

    var WS_Manager = (function () {
        var isFree = true, queue = [];

        function onSignal() {
            var obj;
            //console.log("onSignal for ");
            isFree = true;
            if (queue.length > 0) {
                obj = queue.pop();
                isFree = false;
                obj.shadowInit();
            }
        }

        return {

            initWs: function (wsObj) {

                wsObj.on("open", onSignal);
                wsObj.on("error", onSignal);

                //console.log("WS_Manager: isFree = "+isFree);
                if (isFree) {
                    isFree = false;
                    wsObj.shadowInit();
                } else {
                    queue.push(wsObj);
                }
            }
        }
    })();

    var WS = function (_webSocketId, _placeholderCssSel, verbose) {

        this.listeners = {};

        this.id = _webSocketId;
        this.placeholderCssSel = _placeholderCssSel;

        this.address = "";

        this.wsSocket = null;
        this.hangOutCheck = null;
        this.pong = JSON.stringify({
            "pong": true
        });

        this.verbose = (verbose !== undefined) ? verbose : false;

        this.TIMEOUT = 2000;
        this.PONG_TIMEOUT = 500;

    };

    WS.prototype.off = function (type, listener) {

        if (this.listeners[type] !== undefined) {

            var index = this.listeners[type].indexOf(listener);

            if (index !== -1) {

                this.listeners[type].splice(index, 1);

            }

        }

    };

    WS.prototype.on = function (type, listener) {

        if (this.listeners[type] === undefined) {

            this.listeners[type] = [];

        }

        if (this.listeners[type].indexOf(listener) === -1) {

            this.listeners[type].push(listener);
        }

    };

    WS.prototype.emit = function (event) {

        for (var listener in this.listeners[event.type]) {

            this.listeners[event.type][listener](event, this.id);

        }

    };

    WS.prototype.isReady = function () {
        return (this.wsSocket && this.wsSocket.readyState === 1);
    };

    WS.prototype.close = function (msg) {
        if (this.verbose) {
            console.log("WebSocket: " + this.id + " closing by me");
        }
        this.wsSocket.close();
    };

    WS.prototype.init = function() {
        WS_Manager.initWs(this);
    };

    WS.prototype.shadowInit = function () {

        var that = this;

        if (this.verbose)
            console.log("WebSocket: " + that.id + " connecting to " + this.address);

        try {
            this.wsSocket = window['MozWebSocket'] ? new MozWebSocket(this.address) : new WebSocket(this.address);
        } catch (e) {
            console.error("WebSocket: " + that.id + " cannot connect to WebSocket at " + this.address);
        }

        if (this.wsSocket !== undefined) {

            this.wsSocket.onopen = function () {

                if (that.verbose)
                    console.log("WebSocket: " + that.id + " is opened ", that.wsSocket);

                that.emit({
                    type   : 'open',
                    content: that
                });

            };
            this.wsSocket.onerror = function (event) {
                if (that.verbose)
                    console.error("WebSocket: " + that.id + " has had an error. ", that.wsSocket);

                that.emit({
                    type   : 'error',
                    content: that
                });
            };
            this.wsSocket.onmessage = function (event) {

                var data = JSON.parse(event.data);

                if (that.verbose)
                    console.log("WebSocket: " + that.id + " has received ", event);

                if ((data === undefined) || (data.error)) {
                    if (that.verbose) {
                        console.error("WebSocket: " + that.id + " data error -> ", event.data);
                    }
                    that.emit({
                        type   : 'error',
                        content: event
                    });
                    that.close();

                } else {

                    var msg = jQuery.parseJSON(event.data);

                    if (msg.ping != undefined) {
                        if ($(that.placeholderCssSel).length > 0) {
                            clearTimeout(that.hangOutCheck);
                            that.hangOutCheck = setTimeout(function () {
                                that.init();
                            }, that.TIMEOUT);
                            setTimeout(function () {
                                that.wsSocket.send(that.pong);
                            }, that.PONG_TIMEOUT);
                        } else {
                            that.wsSocket.close();
                        }
                    } else {
                        that.emit({
                            type   : 'message',
                            content: msg
                        });
                    }
                }
            };
            this.wsSocket.onclose = function () {
                if (that.verbose)
                    console.log("WebSocket: " + that.id + " closed by server. Placeholder: " + that.placeholderCssSel);

                that.emit({
                    type   : 'close',
                    content: that
                });

                if ($(that.placeholderCssSel).length > 0) {
                    // reactivate connection
                    setTimeout(function () {
                        that.init();
                    }, that.TIMEOUT);
                }
            };
        }
    };

    WS.prototype.send = function (msg) {
        if (this.isReady()) {
            if (this.verbose)
                console.log("WebSocket: " + this.id + " is sending ", msg);

            this.wsSocket.send(JSON.stringify(msg));

        }
        else {

            if (this.verbose)
                console.warn("WebSocket: " + this.id + " is not ready, maybe missing 'init'? ", this);

        }
    };

    return WS;
})