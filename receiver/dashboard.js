var fling = window.fling || {};

(function () {
    'use strict';

    DashBoard.NAMESPACE = 'urn:flint:tv.matchstick.demo.dashboard';

    function DashBoard() {
        var self = this;


        self.users = {};

        self.textMessageElement = document.getElementById('text_message');
//
//
//      var i = 0;
//      window.setInterval(function () {
//          self.textMessageElement.innerHTML = self.textMessageElement.value + '\n' +i ;
//          self.textMessageElement.scrollTop = self.textMessageElement.scrollHeight;
//          i++;
//      }, 200);


        self.usersElement = document.getElementById('users');
        self.systemInfoElement = document.getElementById('system_info');

        console.log("init:" + self.textMessageElement.innerHTML);

        self.dashBoardManager = new ReceiverManagerWrapper('~dashboard');
        self.messageBus = self.dashBoardManager.createMessageBus(DashBoard.NAMESPACE);
        self.messageBus.on("message", function (senderId, message) {
            var data = JSON.parse(message);
            ("onMessage" in self) && self.onMessage(senderId, data);
        });

        self.messageBus.onsenderConnected = self.onSenderConnected.bind(this);
        self.messageBus.onsenderDisconnected = self.onSenderDisconnected.bind(this);

        self.dashBoardManager.open();


    }

    // Adds event listening functions to DashBoard.prototype.
    DashBoard.prototype = {

        /**
         * Message received event; determines event message and command, and
         * choose function to call based on them.
         * @param {event} event the event to be processed.
         */
        onMessage: function (senderId, message) {
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
        onSenderConnected: function (event) {
            console.log('onSenderConnected. Total number of senders: ' + this.dashBoardManager.getSenderList());
        },

        /**
         * Sender disconnected event; if all senders are disconnected,
         * closes the application.
         * @param {event} event the sender disconnected event.
         */
        onSenderDisconnected: function (event) {
            console.log('onSenderDisconnected. Total number of senders: ' + this.dashBoardManager.getSenderList().length);
            if (this.dashBoardManager.getSenderList().length == 0) {
                window.close();
            }
        },

        onShow: function (senderId, message) {
            console.log('****onShow****');

            this.users[senderId] = message.user;

            this.showMessage('say', message.user, message.info);

            this.broadcast('say', message.user, message.info);
        },

        onLeave: function (senderId, message) {
            console.log('****OnLeave****');

            delete this.users[senderId];

            this.showMessage('leave', message.user, 'left!');

            this.broadcast('leave', message.user, 'left!');
        },

        onJoin: function (senderId, message) {
            console.log('****onJoin****');

            this.users[senderId] = message.user;

            this.showMessage('join', message.user, 'joined!');

            this.broadcast('join', message.user, 'joined!');
        },

        /**
         * broadcast a message to all users on dashboard message bus.
         *
         * @param {string} senderId sender's Id
         * @param {string} user the user whos send the message.
         * @param {string} message the message to send.
         */
        broadcast: function (type, user, message) {
            this.messageBus.send('{type:' + type + ',user:' + user + ', message:' + message + '}', '*:*');
        },

        getInfoWithColor: function (info, color) {
            console.log('<font color="' + color + '">' + info + '</font>');
            return '<font color="' + color + '">' + info + '</font>';
        },

        updateUsers: function (activeUser) {
            var senderIds = this.dashBoardManager.getSenderList();
            var userNames = null;
            for (var senderId in senderIds) {
                var user = this.users[senderId];
                if (user == activeUser) {
                    user = this.getInfoWithColor(user, 'red');
                }
                if (userNames == null) {
                    userNames = user;
                } else {
                    userNames = user + ',' + userNames;
                }
            }

            this.usersElement.innerHTML = userNames;
        },

        showMessage: function (type, user, info) {
            var myDate = new Date();


            if (type == 'say') {
                this.textMessageElement.innerHTML = '\n' + this.getInfoWithColor(info, 'blue') + '\n';
                this.systemInfoElement.innerHTML = this.getInfoWithColor(user, 'red') + ' ' + this.getInfoWithColor('is talking', 'blue') + ' on ' + myDate.toLocaleString();
                this.textMessageElement.scrollTop = this.textMessageElement.scrollHeight;

            } else if (type == 'join' || type == 'leave') {
                this.textMessageElement.innerHTML = '\n' + this.getInfoWithColor(user, 'red') + ' ' + this.getInfoWithColor(info, 'blue') + '\n';
                this.systemInfoElement.innerHTML = this.getInfoWithColor(user, 'red') + ' ' + this.getInfoWithColor(info, 'blue') + ' on ' + myDate.toLocaleString();
                this.textMessageElement.scrollTop = this.textMessageElement.scrollHeight;

            }

            this.updateUsers(user);
        }
    };

    // Exposes public functions and APIs
    fling.DashBoard = DashBoard;
})();
