/*jslint browser : true*/
/*global define*/
define([
	'backbone',
	'utils/logger',
	'views/HomeView',
	'views/ShorelineView'
], function (Backbone, log, HomeView, ShorelineView) {
	"use strict";
	var applicationRouter = Backbone.Router.extend({
		routes: {
			'': 'displayShorelineToolset', // Effectively make Shorelines the default
			'shorelines': 'displayShorelineToolset',
			'shorelines/:activeTab': 'displayShorelineToolset'
		},
		initialize: function () {
			log.trace("Initializing router");
			this.displayHomeView();
		},
		displayHomeView: function () {
			log.trace("Routing to home view");
			this.homeView = new HomeView({
				router : this
			});
			this.homeView.render();
			return this.homeView;
		},
		displayShorelineToolset: function (activeTab) {
			log.trace("Routing to Shorelines toolset");

			var shorelineView = new ShorelineView({
				el: '#toolset-span',
				parent : this.homeView,
				router : this
			}).render({
				activeTab : activeTab
			});

			this.homeView.subViews.shorelineView = shorelineView;
			
			return shorelineView;
		}
	});

	return applicationRouter;
});