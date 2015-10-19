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
	var applicationRouter = Backbone.Router.extend({
		routes: {
			'shorelines': 'displayShorelineToolset'
		},

		initialize: function () {
			log.trace("Initializing router");
			this.displayHomeView();
			this.toolsetView = null;
		},
		
		displayHomeView : function () {
			log.trace("Routing to home view");
			var homeView = new HomeView();
			homeView.render();
			return homeView;
		},
		
		displayShorelineToolset : function () {
			log.trace("Routing to home view with shorelines set");
			
			var shorelineView = new ShorelineView({
				el : '#toolset-span'
			}).render();
			
			this.toolsetView = shorelineView;
			
			return shorelineView;
		}
	});

	return applicationRouter;
});