/*jslint browser : true*/
/*global define*/
define([
	'utils/AppEvents',
	'backbone',
	'utils/logger',
	'views/HomeView',
	'views/ShorelineView',
	'models/ShorelineViewModel',
	'jquery',
	'underscore'
], function (AppEvents, Backbone, log, HomeView, ShorelineView, ShorelineViewModel, $, _) {
	"use strict";
	var applicationRouter = Backbone.Router.extend({
		viewModels: {},
		currentView: null,
		routes: {
			'': 'displayShorelineToolset', // Effectively make Shorelines the default
			'shorelines': 'displayShorelineToolset',
			'shorelines/:activeTab': 'displayShorelineToolset',
			'baseline': 'displayBaselineToolset',
			'baseline/:activeTab': 'displayBaselineToolset'
		},
		initialize: function () {
			log.trace("Initializing router");

			_.extend(AppEvents, Backbone.Events);
			
			this.appEvents = AppEvents;
			
			this.viewModels.shorelineViewModel = new ShorelineViewModel();
			
			return this;
		},
		displayHomeView: function () {
			log.trace("Routing to home view");
			
			this.homeView = new HomeView({
				router: this,
				appEvents: this.appEvents,
				session : this.session
			});
			this.homeView.render();
			return this.homeView;
		},
		displayShorelineToolset: function (activeTab) {
			log.trace("Routing to Shorelines toolset");

			if (this.homeView === undefined) {
				this.displayHomeView();
			}
			
			if (this.currentView !== null) {
				this.currentView.remove();
			}

			this.shorelineView = new ShorelineView({
				parent: this.homeView,
				router: this,
				appEvents: this.appEvents,
				model: this.viewModels.shorelineViewModel,
				session : this.session
			}).render({
				activeTab: activeTab
			});

			$(this.shorelineView.el).appendTo(this.homeView.$('#toolset-span'));

			this.currentView = this.shorelineView;

			return this.shorelineView;
		},
		displayBaselineToolset: function (activeTab) {
			if (this.currentView !== null) {
				this.currentView.remove();
			}
		}
	});

	return applicationRouter;
});