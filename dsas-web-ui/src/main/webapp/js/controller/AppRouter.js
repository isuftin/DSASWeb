/*jslint browser : true*/
/*global define*/
define([
	'backbone',
	'utils/logger',
	'views/HomeView',
	'views/ShorelineView',
	'underscore'
], function (Backbone, log, HomeView, ShorelineView, _) {
	"use strict";
	var applicationRouter = Backbone.Router.extend({
		routes: {
			'': 'displayShorelineToolset', // Effectively make Shorelines the default
			'shorelines': 'displayShorelineToolset',
			'shorelines/:activeTab': 'displayShorelineToolset'
		},
		initialize: function () {
			log.trace("Initializing router");
			
			this.appEvents = _.extend({}, Backbone.Events);
			
			this.displayHomeView();
			
			return this;
		},
		displayHomeView: function () {
			log.trace("Routing to home view");
			this.homeView = new HomeView({
				router : this,
				appEvents : this.appEvents
			});
			this.homeView.render();
			return this.homeView;
		},
		displayShorelineToolset: function (activeTab) {
			log.trace("Routing to Shorelines toolset");

			this.shorelineView = new ShorelineView({
				el: '#toolset-span',
				parent : this.homeView,
				router : this,
				appEvents : this.appEvents
			}).render({
				activeTab : activeTab
			});

			this.homeView.subViews.shorelineView = this.shorelineView;
			
			return this.shorelineView;
		}
	});

	return applicationRouter;
});