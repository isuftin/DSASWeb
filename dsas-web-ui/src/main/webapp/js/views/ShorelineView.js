/*jslint browser: true */
define([
	'handlebars',
	'views/BaseView',
	'utils/logger',
	'text!templates/shoreline-view.html'
], function (Handlebars, BaseView, log, template) {
	"use strict";
	var view = BaseView.extend({
		// Defines what the default tab in the shorelines toolbox is
		DEFAULT_TAB : "tabpanel-shorelines-view",
		template: Handlebars.compile(template),
		events: {
			'click #tabs-shorelines a': 'tabToggled',
			'click #shorelines-aoi-select-toggle' : 'toggleAoiSelection',
			'click #shorelines-aoi-select-button-done' : 'aoiSelected'
		},
		/*
		 * Renders the object's template using it's context into the view's element.
		 * @returns {extended BaseView}
		 */
		render: function (options) {
			options = options || {};
			this.context.activeTab = options.activeTab || this.DEFAULT_TAB;
			BaseView.prototype.render.apply(this, [options]);
			return this;
		},
		tabToggled: function (e) {
			var clickedTab = $(e.target).attr('data-target');
			this.router.navigate('shorelines/' + clickedTab, {trigger: true});
		},
		toggleAoiSelection : function (e) {
			var activated = !$(e.target).hasClass('active');
			log.debug("AOI Selection Toggled " + (activated ? "on" : "off"));
			this.appEvents.trigger(this.appEvents.shorelines.aoiSelectionToggled, activated);
			
			this.$('#description-aoi').toggleClass('hidden');
		},
		aoiSelected : function () {
			log.debug("AOI Selected");
			this.appEvents.trigger(this.appEvents.shorelines.aoiSelected);
		},
		processAoiSelection : function (bounds) {
			if (bounds) {
				// TODO
			}
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
			
			
			return this;
		},
		remove: function () {
			BaseView.prototype.remove.apply(this);
			return this;
		}
	});

	return view;
});