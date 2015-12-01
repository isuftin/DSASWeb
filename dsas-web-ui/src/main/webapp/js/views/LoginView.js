/*jslint browser: true */
/*global define*/
define([
	'jquery',
	'handlebars',
	'backbone',
	'views/BaseView',
	'utils/logger',
	'text!templates/login-view.html',
	'utils/AuthUtil',
	'module'
], function ($,
		Handlebars,
		Backbone,
		BaseView,
		log,
		template,
		AuthUtil,
		module) {
	"use strict";

	var view = BaseView.extend({
		events: {
			'keypress input': 'keypressHandler',
			'click #button-submit': 'submitButtonClickHandler'
		},
		authUtil: AuthUtil,
		el: '#page-content-container',
		template: Handlebars.compile(template),
		baseUrl: module.config().contextPath,
		render: function () {
			BaseView.prototype.render.apply(this, arguments);
			return this;
		},
		initialize: function (args) {
			args = args || {};
			log.debug("DSASweb login view initializing");
			BaseView.prototype.initialize.apply(this, arguments);
		},
		keypressHandler: function (e) {
			// If the user hits the enter key at any input, trigger a submit
			var ENTER_KEY_CODE = 13;
			if (e.keyCode === ENTER_KEY_CODE) {
				this.$('#button-submit').trigger('click');
			}
		},
		submitButtonClickHandler: function () {
			var args = {
				username: this.$('#username').val(),
				password: this.$('#password').val()
			};
			var tryLogin = this.authUtil.submitLogin(args);
			tryLogin
					.done($.proxy(function (authInfo) {
						this.authUtil.updateCookieWithToken(authInfo.tokenId);
						var redirectTo = Backbone.history.location.hash.slice(1);
						window.location.href = this.baseUrl + redirectTo;
					}, this))
					.fail(function (args) {
						log.error(args.statusText);
					});

		}
	});

	return view;
});