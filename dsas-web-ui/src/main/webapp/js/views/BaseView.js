/*jslint browser: true */
/*global define*/
define([
	'backbone',
	'underscore'
], function (Backbone, _) {
	"use strict";

	var view = Backbone.View.extend({
		/*
		 * Renders the object's template using it's context into the view's element.
		 * @returns {extended Backbone.View}
		 */
		render: function () {
			var html = this.template(this.context);
			this.$el.html(html);

			return this;
		},
		/*
		 * This would either be overriden when extending or when instantiating a view the user would
		 * pass in a template property.
		 * @returns {String}
		 */
		template: function () {
			return 'No template specified';
		},
		/*
		 * @constructs
		 * @param {Object} options
		 *		@prop router {Backbone.Router instance} - defaults to null
		 *		@prop template {Function} optional - Returns html to be rendered. Will override the template property.
		 *		@prop context {Object} to be used when rendering templateName - defaults to {}
		 *      @prop el {String} - render view in $el.
		 * @returns
		 */
		initialize: function (options) {
			options = options || {};

			this.context = {};

			_.each([
				'context',
				'session',
				'model',
				'appEvents',
				'template',
				'router',
				'parent',
				'template'
			], function (item) {
				if (_.has(options, item)) {
					this[item] = options[item];
				}
			}, this);

			Backbone.View.prototype.initialize.apply(this, arguments);
		}
	});

	return view;
});