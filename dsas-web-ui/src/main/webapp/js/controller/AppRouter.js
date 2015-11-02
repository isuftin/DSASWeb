/*jslint browser : true*/
/*global define*/
define([
	'backbone',
	'utils/logger',
	'views/HomeView',
	'views/ShorelineView',
	'collections/SessionCollection',
	'models/sessionModel',
	'utils/SessionUtil',
	'models/ShorelineViewModel',
	'jquery',
	'underscore'
], function (Backbone, log, HomeView, ShorelineView, SessionCollection, SessionModel, SessionUtil, ShorelineViewModel, $, _) {
	"use strict";
	var applicationRouter = Backbone.Router.extend({
		appEvents: {
			map: {
				aoiSelected: 'map_aoi_selected'
			},
			shorelines: {
				aoiSelectionToggled: 'shorelines_aoi_selection_toggled',
				aoiSelected: 'shorelines_aoi_selected',
				columnsMatched: 'shorelines_columns_matched',
				layerImportSuccess: 'shorelines_layer_import_success',
				layerImportFail: 'shorelines_layer_import_fail'
			}
		},
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

			_.extend(this.appEvents, Backbone.Events);

			this.viewModels.shorelineViewModel = new ShorelineViewModel();
			
			// Create a new session collection, check if it exists in localstorage. 
			// If so, I'm done. Otherwise, call out to the server to create a workspace
			// for this session and then create the session based on the workspace
			// name. 
			this.session = new SessionCollection();
			this.session.fetch();
			if (this.session.models.length === 0) {
				SessionUtil
						.prepareSession()
						.done($.proxy(function (response) {
							var workspace = response.workspace;
							this.session.create(new SessionModel({
								id: workspace
							}));
							SessionUtil.updateSessionUsingWMSGetCapabilitiesResponse({
								session: this.session.get(workspace),
								context: this
							});
						}, this))
						.error(function (response) {
							// TODO - What happens if I can't create a session on
							// the server? If I can't do that, I have to bail out 
							// of the application because the user can't upload
							// any files or really do much of anything. Send the
							// user to a 500 Error page?
							log.error("Could not create a session on the workspace.");
						});
			} else {
				SessionUtil.updateSessionUsingWMSGetCapabilitiesResponse({
					session: this.session.get(SessionUtil.getCurrentSessionKey()),
					context: this
				});
			}

			this.displayHomeView();

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

			$(this.shorelineView.el).appendTo($('#toolset-span'));

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