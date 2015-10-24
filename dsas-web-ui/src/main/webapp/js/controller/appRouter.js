/*jslint browser : true*/
/*global define*/
define([
	'backbone',
	'underscore',
	'utils/logger',
	'views/HomeView',
	'views/ShorelineView'
], function (Backbone, _, log, HomeView, ShorelineView) {
	"use strict";
	var test = this;
	var applicationRouter = Backbone.Router.extend({
		routes: {
			'shorelines': 'displayShorelineToolset'
		},
		initialize: function () {
			log.trace("Initializing router");
			this.toolsetView = null;
			this.displayHomeView();
		},
		displayHomeView: function () {
			log.trace("Routing to home view");
			this.homeView = new HomeView();
			this.homeView.render();
			return this.homeView;
		},
		displayShorelineToolset: function () {
			log.trace("Routing to home view with shorelines set");

			var shorelineView = new ShorelineView({
				el: '#toolset-span',
				parent : this.homeView
			}).render();

			this.homeView.subViews['shorelineView'] = shorelineView;
			this.toolsetView = shorelineView;

			return shorelineView;
		}
	});

	return applicationRouter;
});