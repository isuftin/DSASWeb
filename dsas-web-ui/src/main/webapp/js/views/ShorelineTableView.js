/*jslint browser: true */
/*global define*/
define([
	'jquery',
	'handlebars',
	'views/BaseView',
	'backbone',
	'underscore',
	'text!templates/shoreline-table.html'
], function (
		$,
		Handlebars,
		BaseView,
		Backbone,
		_,
		template) {
	"use strict";

	var view = BaseView.extend({
		template: Handlebars.compile(template),
		render: function () {
			BaseView.prototype.render.apply(this, arguments);
			$(window).on('resize', $.proxy(function () {
				$(this.el).find('table').bootstrapTable('resetWidth');
			}, this));
			return this;
		},
		initialize: function (options) {
			this.context = options;
			Backbone.View.prototype.initialize.apply(this, arguments);
		},
		remove: function () {
			BaseView.prototype.remove.apply(this);
			return this;
		},
		updateColorColumn : function () {
			this.$('#shoreline-table tbody tr > td:nth-child(4)').each(function (i, td) {
				var $td = $(td);
				$td.css('background-color', $td.attr('data-color'));
			});
		}
	});

	return view;
});
