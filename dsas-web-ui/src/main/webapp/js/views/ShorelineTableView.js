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
				var $tableRows = $table.find('tbody tr');

				if ($target.hasClass(selectedClass)) {
					$target.removeClass(selectedClass);
				} else {
					// The user has deselected this row and because no other rows were
					// selected, deselect all other rows as well. Also perform the
					// same action if, for some reason, the user has held down
					// the range selection and multiple selection keys at the same time
					if ((!rangeSelectActivated && !multiSelectActivated) ||
							(rangeSelectActivated && multiSelectActivated)) {
						$tableRows.removeClass(selectedClass);
					}
					
					if (rangeSelectActivated) {
						// Find the row that was clicked
						var rowId = parseInt(id.substring(id.lastIndexOf('-') + 1), 10);
						// Find all other selected rows
						var otherSelectedRows = _.map($table.find('.' + selectedClass), function (a) {
							var id = $(a).prop('id');
							return parseInt(id.substring(id.lastIndexOf('-') + 1), 10);
						});
						if (otherSelectedRows.length > 0) {
							// The clicked row sits before any other selected rows.
							// Highlight the range from the first to the last row.
							if (rowId <= otherSelectedRows[0]) {
								for (var rIdx = rowId;rIdx < otherSelectedRows[otherSelectedRows.length - 1];rIdx++) {
									$table.find('#shoreline-table-row-' + rIdx).addClass(selectedClass);
								}
							} else {
								for (var rIdx = otherSelectedRows[0];rIdx < rowId;rIdx++) {
									$table.find('#shoreline-table-row-' + rIdx).addClass(selectedClass);
								}
							}
						}
					} else if (multiSelectActivated) {
						$target.addClass(selectedClass);
					} else {
						$tableRows.removeClass(selectedClass);
						$target.addClass(selectedClass);
					}
				}
			} else {
				// TODO - handle toggle
			}
		}
	});

	return view;
});
