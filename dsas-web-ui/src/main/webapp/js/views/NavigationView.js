/*jslint browser: true */
define([
	'handlebars',
	'views/BaseView',
	'utils/logger',
	'text!templates/navigation-view.html'
], function (Handlebars, BaseView, log, template) {
	"use strict";

	var view = BaseView.extend({
		template: Handlebars.compile(template),
		events: {
			'click #tabs-navigation a': 'tabToggled'
		},
		/*
		 * Renders the object's template using it's context into the view's element.
		 * @returns {extended BaseView}
		 */
		render: function (options) {
			options = options || {};
			BaseView.prototype.render.apply(this, [options]);
			return this;
		},
		/*
		 * @constructs
		 * @param {Object} options
		 */
		initialize: function (options) {
			log.debug("DSASweb Navigation view initializing");
			BaseView.prototype.initialize.apply(this, [options]);
			return this;
		},
		
		tabToggled: function (e) {
			var $target = $(e.target);
			var clickedTab = $target.attr('data-target');
			this.router.navigate(clickedTab, {trigger: true});
			
			// Set the active tab 
			this.$('#tabs-navigation li').removeClass('active');
			$target.parent().addClass('active');
		}
	});

	return view;
});