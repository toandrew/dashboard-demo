var fling = window.fling || {};

(function() {
  'use strict';

  DashBoard.NAMESPACE = 'urn:flint:tv.matchstick.demo.dashboard';

  function DashBoard() {
    var self = this;

    self.textContainerElement = document.getElementById('textContainer');
    self.textMessageElement = document.getElementById('textMessage');

    self.dashBoardManager = new ReceiverManagerWrapper('~dashboard');
    self.messageBus = self.dashBoardManager.createMessageBus(DashBoard.NAMESPACE);
    self.messageBus.on("message", function (senderId, message) {
        var data = JSON.parse(message);
        ("onMessage" in self) && self.onMessage(senderId, data);
    });

    self.messageBus.onsenderConnected = self.onSenderConnected.bind(this);
    self.messageBus.onsenderDisonnected = self.onSenderDisconnected.bind(this);
  }

  // Adds event listening functions to DashBoard.prototype.
  DashBoard.prototype = {

    /**
     * Message received event; determines event message and command, and
     * choose function to call based on them.
     * @param {event} event the event to be processed.
     */
    onMessage: function(senderId, message) {
        console.log('onMessage: ' + message + " senderId:" + senderId);

        if (message.command == 'show') {
            this.onShow(senderId, message);
        } else if (message.command == 'leave') {
            this.onLeave(senderId, message);
        } else if (message.command == 'join') {
            this.onJoin(senderId, message);
        } else {
            console.log('Invalid message command: ' + message.command);
        }
    },


    /**
     * Sender Connected event
     * @param {event} event the sender connected event.
     */
    onSenderConnected: function(event) {
        console.log('onSenderConnected. Total number of senders: ' + this.dashBoardManager.getSenderList().length);
    },

    /**
     * Sender disconnected event; if all senders are disconnected,
     * closes the application.
     * @param {event} event the sender disconnected event.
     */
    onSenderDisconnected: function(event) {
        console.log('onSenderDisconnected. Total number of senders: ' + this.dashBoardManager.getSenderList().length);
        if (this.dashBoardManager.getSenderList().length == 0) {
            window.close();
        }
    },

    onShow: function(senderId, message) {
        console.log('****onShow****');
        var text = this.textMessageElement.innerText + '\n'
        this.textMessageElement.innerText = text + message.user + ': ' + message.info;

        this.broadcast(senderId, 'say', message.user, message.info);
    },

    onLeave: function(senderId, message) {
        console.log('****OnLeave****');

        this.broadcast(senderId, 'leave', message.user, 'leave!');
    },

    onJoin: function(senderId, message) {
        console.log('****onJoin****');

        this.broadcast(senderId, 'join', message.user, 'join!');
    },

    /**
     * broadcast a message to all users on dashboard message bus.
     *
     * @param {string} senderId sender's Id
     * @param {string} user the user whos send the message.
     * @param {string} message the message to send.
     */
    broadcast: function(senderId, type, user, message) {
        this.messageBus.send('{type:' + type + ',user:' + user + ', message:' + message + '}', senderId);
    }

  };

  // Exposes public functions and APIs
  fling.DashBoard = DashBoard;
})();
