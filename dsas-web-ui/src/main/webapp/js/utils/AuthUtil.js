/*global define*/
define([
	'underscore',
	'jquery',
	'module'
], function (
		_,
		$,
		module) {
	"use strict";
	var me = {
		TOKEN_COOKIE_LABEL: module.config().authTokenLabel,
		SECURITY_SERVICE_PATH: module.config().contextPath + module.config().SECURITY_SERVICE_PATH,
		checkAuthStatus: function () {
			return $.ajax({
				url: this.SECURITY_SERVICE_PATH + '/check',
				type: 'GET'
			});
		},
		submitLogin: function (args) {
			args = args || {};
			var username = args.username;
			var password = args.password;

			return $.ajax({
				url: this.SECURITY_SERVICE_PATH + '/auth/authenticate',
				type: 'POST',
				dataType: 'json',
				data: {
					username: username,
					password: password
				}
			});
		},
		updateCookieWithToken: function (token) {
			$.removeCookie(this.TOKEN_COOKIE_LABEL, {path: "/"});
			$.cookie(this.TOKEN_COOKIE_LABEL, token, {expires: 1, path: "/"});
		}
	};

	return _.extend({}, me);
});