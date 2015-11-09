/*jslint browser: true */
/*global define*/
define([
	'handlebars',
	'views/BaseView',
	'utils/logger',
	'text!templates/shoreline-viewer-view.html'
], function (
		Handlebars, 
		BaseView, 
		log, 
		template) {
	"use strict";
	var view = BaseView.extend({
		events: {
			'click #button-shorelines-aoi-toggle': 'handleAoiSelectionClick',
			'click #button-shorelines-aoi-done': 'aoiSelected'
		},
		template: Handlebars.compile(template),
		initialize: function (options) {
			BaseView.prototype.initialize.apply(this, [options]);
			return this;
		},
		render: function (options) {
			options = options || {};

			BaseView.prototype.render.apply(this);

			$(this.el).appendTo(options.el);

			if (this.model.get('aoiToggledOn') === true) {
				// Clicking the button is not enough here. Sometimes the DOM is
				// not yet appended even though appendTo is supposed to be synchronous.
				this.$('#button-shorelines-aoi-toggle')
						.addClass('active')
						.prop('aria-pressed', true);
				this.toggleAoiSelection(true);
			}

			return this;
		},
		handleAoiSelectionClick: function (e) {
			this.toggleAoiSelection(!$(e.target).hasClass('active'));
		},
		toggleAoiSelection: function (activated) {
			log.debug("AOI Selection Toggled " + (activated ? "on" : "off"));
			this.appEvents.trigger(this.appEvents.shorelines.aoiSelectionToggled, activated);
			this.$('#description-aoi').toggleClass('hidden');
			this.model.set('aoiToggledOn', activated);
		},
		aoiSelected: function () {
			log.debug("AOI Selected");
			this.appEvents.trigger(this.appEvents.shorelines.aoiSelected);
		},
		remove: function () {
			this.appEvents.trigger(this.appEvents.shorelines.aoiSelectionToggled, false);
			BaseView.prototype.remove.apply(this);
			return this;
		}
	});

	return view;
});