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
		appEvents : {
			map : {
				aoiSelected : 'map_aoi_selected'
			},
			shorelines : {
				aoiSelectionToggled : 'shorelines_aoi_selection_toggled',
				aoiSelected : 'shorelines_aoi_selected'
			}
		},
		currentView : null,
		routes: {
			'': 'displayShorelineToolset', // Effectively make Shorelines the default
			'shorelines': 'displayShorelineToolset',
			'shorelines/:activeTab': 'displayShorelineToolset',
			'baseline': 'displayBaselineToolset',
			'baseline/:activeTab': 'displayBaselineToolset'
		},
		initialize: function () {
			log.trace("Initializing router");
			
			 _.extend(this.appEvents, Backbone.Events);
			
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

			if (this.currentView !== null) {
				this.currentView.remove();
			}

			this.shorelineView = new ShorelineView({
				parent : this.homeView,
				router : this,
				appEvents : this.appEvents
			}).render({
				activeTab : activeTab
			});

			$(this.shorelineView.el).appendTo($('#toolset-span'));

			this.currentView = this.shorelineView;

			return this.shorelineView;
		},
		displayBaselineToolset : function (activeTab) {
			if (this.currentView !== null) {
				this.currentView.remove();
			}
		}
	});

	return applicationRouter;
});