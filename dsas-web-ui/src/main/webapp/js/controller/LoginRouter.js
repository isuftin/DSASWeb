/*jslint browser : true*/
/*global define*/
define([
	'backbone',
	'utils/logger',
	'views/LoginView'
], function (Backbone, log, LoginView) {
	"use strict";
	var applicationRouter = Backbone.Router.extend({
		routes: {
			'': 'displayLoginView',
			':route(/:subroute)': 'displayLoginView'
		},
		initialize: function () {
			log.trace("Initializing login router");
			return this;
		},
		displayLoginView: function () {
			log.trace("Routing to Management view");
			this.loginView = new LoginView({
				'context': {'redirectedFrom': "/" + Array.prototype.join.call(arguments, "/")}
			}).render();
			return this.loginView;
		}
	});

	return applicationRouter;
});