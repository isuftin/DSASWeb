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
		events: {
			'change [data-toggle="table"] input': 'toggleChangeHandler',
			'click [data-toggle="table"] tbody tr': 'tableRowCLickHandler'
		},
		render: function () {
			BaseView.prototype.render.apply(this, arguments);
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
		updateColorColumn: function () {
			this.$('#shoreline-table tbody tr > td:nth-child(4)').each(function (i, td) {
				var $td = $(td);
				$td.css('background-color', $td.attr('data-color'));
			});
		},
		createTableSorter: function () {
			return this.$('[data-toggle="table"]').tablesorter({
				dateFormat: 'yyyy-mm-dd',
				headers: {
					0: {
						sorter: 'vis'
					},
					3: {
						sorter: false
					}

				}
			}).bind('sortStart', function () {
				$(this).trigger("updateCache");
			});
		},
		toggleChangeHandler: function (e) {
			var $e = $(e.currentTarget);
			var active = $e.prop('checked');
			if (active) {
				$e.attr('checked', true);
			} else {
				$e.attr('checked', false);
			}
		},
		tableRowCLickHandler: function (e) {
			if (!$(e.target).is('label')) { // User clicked on toggle, don't process
				var $target = $(e.currentTarget);
				var id = $target.prop('id');
				var selectedClass = 'table-shoreline-selected';
				var rangeSelectActivated = e.shiftKey;
				var multiSelectActivated = e.altKey || e.ctrlKey;
				var $table = $target.parentsUntil('table').parent();

				if ($target.hasClass(selectedClass)) {
					$target.removeClass(selectedClass);
					// The user has deselected this row and because no other rows were
					// selected, deselect all other rows as well
					if (!rangeSelectActivated && !multiSelectActivated) {
						$table.find('tbody tr').removeClass(selectedClass);
					}
				} else {
					$target.addClass(selectedClass);
				}
			} else {
				
			}
		}
	});

	return view;
});
