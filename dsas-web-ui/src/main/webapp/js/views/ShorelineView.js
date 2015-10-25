/*jslint browser: true */
define([
	'handlebars',
	'views/BaseView',
	'utils/logger',
	'text!templates/shoreline-view.html'
], function (Handlebars, BaseView, log, template) {
	"use strict";
	var view = BaseView.extend({
		DEFAULT_TAB : "tabpanel-shorelines-view",
		template: Handlebars.compile(template),
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
			return this;
		},
		tabToggled: function (e) {
			var clickedTab = $(e.target).attr('data-target');
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
		},
		remove: function () {
			BaseView.prototype.remove.apply(this);
			return this;
		}
	});

	return view;
});