/*jslint browser : true*/
/*global define*/
define([
	'backbone',
	'underscore',
	'utils/logger',
	'views/HomeView',
	'views/ShorelineView',
	'collections/sessionCollection',
	'models/sessionModel'
], function (Backbone, _, log, HomeView, ShorelineView, SessionCollection, SessionModel) {
	"use strict";
	var applicationRouter = Backbone.Router.extend({
		routes: {
			'shorelines': 'displayShorelineToolset'
		},

		initialize: function () {
			log.trace("Initializing router");
			this.displayHomeView();
			this.toolsetView = null;
			this.sessionCollection = new SessionCollection();
			this.sessionCollection.fetch();
			if (this.sessionCollection.models.length === 0) {
				this.sessionCollection.create(new SessionModel());
			}
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