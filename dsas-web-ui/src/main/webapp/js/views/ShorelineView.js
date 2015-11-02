/*jslint browser: true */
define([
	'handlebars',
	'views/BaseView',
	'views/ShorelineManagementView',
	'views/ShorelineViewerView',
	'utils/SessionUtil',
	'utils/logger',
	'text!templates/shoreline-view.html'
], function (Handlebars, BaseView, ShorelineManagementView, ShorelineViewerView, SessionUtil, log, template) {
	"use strict";
	var view = BaseView.extend({
		// Defines what the default tab in the shorelines toolbox is
		DEFAULT_TAB: "tabpanel-shorelines-view",
		template: Handlebars.compile(template),
		activeChildView: null,
		events: {
			'click #tabs-shorelines a': 'tabToggled'
		},
		/*
		 * Renders the object's template using it's context into the view's element.
		 * @returns {extended BaseView}
		 */
		render: function (options) {
			options = options || {};
			this.context.activeTab = options.activeTab || this.DEFAULT_TAB;
			BaseView.prototype.render.apply(this, [options]);

			var subViewParams = {
				parent: this,
				router: this.router,
				appEvents: this.appEvents,
				model: this.model,
				session: this.session
			};

			if (this.context.activeTab === this.DEFAULT_TAB) {
				this.activeChildView = new ShorelineViewerView(subViewParams);
			} else {
				this.activeChildView = new ShorelineManagementView(subViewParams);
			}

			this.activeChildView.render({
				el: this.$('#' + this.context.activeTab)
			});

			return this;
		},
		tabToggled: function (e) {
			var clickedTab = $(e.target).attr('data-target');
			this.activeChildView.remove();
			this.router.navigate('shorelines/' + clickedTab, {trigger: true});
		},
		/*
		 * @constructs
		 * @param {Object} options
		 */
		initialize: function (options) {
			Handlebars.registerHelper('ifIsActiveTab', function (a, b, output) {
				if (a === b) {
					return output;
				}
				return null;
			});
			log.debug("DSASweb Shoreline view initializing");
			BaseView.prototype.initialize.apply(this, [options]);

			this.listenTo(this.appEvents, this.appEvents.map.aoiSelected, this.processAoiSelection);
			this.listenTo(this.appEvents, this.appEvents.layerImportSuccess, this.shorelineImportCompleteHandler);

			return this;
		},
		processAoiSelection: function () {
			// TODO
		},
		shorelineImportCompleteHandler: function () {
			log.info("Shorelines imported");
			SessionUtil.updateSessionUsingWMSGetCapabilitiesResponse({
				session: this.session.get(SessionUtil.getCurrentSessionKey()),
				context: this
			}).done(function () {
				
			}).fail(function () {
				
			});
		}
	});

	return view;
});