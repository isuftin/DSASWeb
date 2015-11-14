/*jslint browser: true */
/*global define*/
define([
	'utils/AppEvents',
	'handlebars',
	'views/BaseView',
	'views/ShorelineTableView',
	'utils/logger',
	'text!templates/shoreline-viewer-view.html',
	'jquery'
], function (
		AppEvents,
		Handlebars,
		BaseView,
		ShorelineTableView,
		log,
		template,
		$
		) {
	"use strict";
	var view = BaseView.extend({
		events: {
			'click #button-shorelines-aoi-toggle': 'handleAoiSelectionClick',
			'click #button-shorelines-aoi-done': 'aoiSelected'
		},
		template: Handlebars.compile(template),
		initialize: function (options) {
			options = options || {};
			options.appEvents = AppEvents;
			BaseView.prototype.initialize.apply(this, [options]);
			this.listenTo(this.appEvents, this.appEvents.map.aoiSelected, function (e) {
				this.listenToOnce(e, 'sync', this.displayShorelinesData);
				this.toggleAoiSelection();
			});
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
		toggleAoiSelection: function () {
			var activate = this.$('#description-aoi').hasClass('hidden');
			log.debug("AOI Selection Toggled " + (activate ? "on" : "off"));
			this.appEvents.trigger(this.appEvents.shorelines.aoiSelectionToggled, activate);
			this.$('#description-aoi').toggleClass('hidden');
			if (!activate) {
				this.$('#button-shorelines-aoi-toggle').removeClass('active');
				this.$('#button-shorelines-aoi-toggle').prop("aria-pressed", false);
			} else {
				this.removeShorelinesData();
			}
			this.model.set('aoiToggledOn', activate);
		},
		displayShorelinesData: function (shorelineCollection) {
			var models = shorelineCollection.models;
			this.shorelineTableView = new ShorelineTableView({
				models: models
			});
			this.shorelineTableView.render();
			$(this.shorelineTableView.el).appendTo(this.$('#shoreline-table'));
			this.$('#shoreline-table').removeClass('hidden');
			this.$('[data-toggle="table"]').bootstrapTable();
		},
		removeShorelinesData: function () {
			if (this.shorelineTableView) {
				this.shorelineTableView.remove();
			}
		},
		aoiSelected: function () {
			log.debug("AOI Selected");
			this.removeShorelinesData();
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