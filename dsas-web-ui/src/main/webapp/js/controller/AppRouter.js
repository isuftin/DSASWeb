/*jslint browser : true*/
/*global define*/
define([
	'backbone',
	'utils/logger',
	'views/ManagementHomeView'
], function (Backbone, log, ManagementView) {
	"use strict";
	var applicationRouter = Backbone.Router.extend({
		routes: {
			'': 'displayManagementView',
			'login' : 'displayLoginView'
		},
		initialize: function () {
			log.trace("Initializing router");
			return this;
		},
		displayManagementView: function () {
			log.trace("Routing to Management view");
			this.managementView = new ManagementView().render();
			return this.managementView;
		},
		displayLoginView : function () {
			// Not yet implemented
		}
	});

	return applicationRouter;
});