/*jslint browser: true */
/*global define*/
define([
	'handlebars',
	'views/BaseView',
	'backbone',
	'underscore',
	'text!templates/shoreline-table.html'
], function (
		Handlebars,
		BaseView,
		Backbone,
		_,
		template) {
	"use strict";

	var view = Backbone.View.extend({
		template: Handlebars.compile(template),
		render: function () {
			BaseView.prototype.render.apply(this, arguments);
			return this;
		},
		initialize: function (options) {
			this.context = options;
			Backbone.View.prototype.initialize.apply(this, arguments);
		}
	});

	return view;
});
