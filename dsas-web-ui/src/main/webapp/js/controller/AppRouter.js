/*jslint browser : true*/
/*global define*/
define([
	'backbone',
	'utils/logger',
	'views/HomeView',
	'views/ShorelineView',
	'models/ShorelineViewModel',
	'jquery',
	'views/PdbManagementHomeView'
], function (Backbone, log, HomeView, ShorelineView, ShorelineViewModel, $, PdbManagementView) {
	"use strict";
	var applicationRouter = Backbone.Router.extend({
		viewModels: {},
		currentView: null,
		routes: {
			'': 'displayShorelineToolset', // Effectively make Shorelines the default
			'shorelines': 'displayShorelineToolset',
			'shorelines/:activeTab': 'displayShorelineToolset',
			'baseline': 'displayBaselineToolset',
			'baseline/:activeTab': 'displayBaselineToolset',
			'pdb': 'displayPDBView',
			'login' : 'displayLoginView'
		},
		initialize: function () {
			log.trace("Initializing router");

			this.viewModels.shorelineViewModel = new ShorelineViewModel();
			
			return this;
		},
		displayHomeView: function () {
			log.trace("Routing to home view");
			
			this.homeView = new HomeView({
				router: this,
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
		},
		displayPDBView: function () {
			log.trace("Routing to PDB Management view");
			var pdbMgmtView = new PdbManagementView().render();
			return this.pdbView;
		},
		displayLoginView : function () {
			// Not yet implemented
		}
	});

	return applicationRouter;
});