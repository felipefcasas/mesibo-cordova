var exec = require('cordova/exec');

module.exports = {
	setAccessToken: function(params, success, error) {
		exec(success, error, 'MesiboCordova', 'setAccessToken', [params]);
	},

	stopMesibo: function(success, error) {
		exec(success, error, 'MesiboCordova', 'stopMesibo', []);
	},

	readProfileMessages: function(params, success, error) {
		exec(success, error, 'MesiboCordova', 'readProfileMessages', [params]);
	},

	deleteRoomMessages: function(params, success, error) {
		exec(success, error, 'MesiboCordova', 'deleteRoomMessages', [params]);
	},

	readRoomMessages: function(params, success, error) {
		exec(success, error, 'MesiboCordova', 'readRoomMessages', [params]);
	},

	stopReadDbSession: function(success, error) {
		exec(success, error, 'MesiboCordova', 'stopReadDbSession', []);
	},

	sendMessage: function(params, success, error) {
		exec(success, error, 'MesiboCordova', 'sendMessage', [params]);
	},

	saveCustomMessage: function(params, success, error) {
		exec(success, error, 'MesiboCordova', 'saveCustomMessage', [params]);
	},

	sendActivity: function(params, success, error) {
		exec(success, error, 'MesiboCordova', 'sendActivity', [params]);
	},

	sendFile: function(params, success, error) {
		exec(success, error, 'MesiboCordova', 'sendFile', [params]);
	},

	getProfiles: function(success, error) {
		exec(success, error, 'MesiboCordova', 'getProfiles', []);
	},

	getProfile: function(params, success, error) {
		exec(success, error, 'MesiboCordova', 'getProfile', [params]);
	},

	call: function(params, success, error) {
		exec(success, error, 'MesiboCordova', 'call', [params]);
	},

	onMessage: function(success, error) {
		exec(success, error, 'MesiboCordova', 'onMessage', []);
	},

	onMessageStatus: function(success, error) {
		exec(success, error, 'MesiboCordova', 'onMessageStatus', []);
	},

	onActivity: function(success, error) {
		exec(success, error, 'MesiboCordova', 'onActivity', []);
	},

	onLocation: function(success, error) {
		exec(success, error, 'MesiboCordova', 'onLocation', []);
	},

	onFile: function(success, error) {
		exec(success, error, 'MesiboCordova', 'onFile', []);
	},

	onUserProfile: function(success, error) {
		exec(success, error, 'MesiboCordova', 'onUserProfile', []);
	}
};