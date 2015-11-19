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
			var $target = $(e.currentTarget);
			var active = $target.prop('checked');
			var $table = $target.parentsUntil('table').parent();
			var selectedClass = 'table-shoreline-selected';
			var $selectedTableRows = $table.find('.' + selectedClass);
			var $selectedToggles = $selectedTableRows.find('td:nth-child(1) input');
			
			// Do not want an infinite event loop here
			this.undelegateEvents();
			if (active) {
				$target.attr('checked', true);
				
				$selectedToggles.each(function(i, t) {
					$(t).bootstrapToggle('on');
				});
			} else {
				$target.attr('checked', false);
				$selectedToggles.each(function(i, t) {
					$(t).bootstrapToggle('off');
				});
			}
			this.delegateEvents();
			
			// TODO- Handle the updated toggles 
		},
		tableRowCLickHandler: function (e) {
			var $target = $(e.currentTarget);
			var id = $target.prop('id');
			var selectedClass = 'table-shoreline-selected';
			var rangeSelectActivated = e.shiftKey;
			var multiSelectActivated = e.altKey || e.ctrlKey;
			var $table = $target.parentsUntil('table').parent();
			var $tableRows = $table.find('tbody tr');
			var $selectedTableRows = $table.find('.' + selectedClass);

			if (!$(e.target).is('label')) { // User clicked on toggle, don't process

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
						var otherSelectedRows = _.map($selectedTableRows, function (a) {
							var id = $(a).prop('id');
							return parseInt(id.substring(id.lastIndexOf('-') + 1), 10);
						});
						if (otherSelectedRows.length > 0) {
							// The clicked row sits before any other selected rows.
							// Highlight the range from the first to the last row.
							if (rowId <= otherSelectedRows[0]) {
								for (var rIdx = rowId; rIdx < otherSelectedRows[otherSelectedRows.length - 1]; rIdx++) {
									$table.find('#shoreline-table-row-' + rIdx).addClass(selectedClass);
								}
							} else {
								for (var rIdx = otherSelectedRows[0]; rIdx < rowId; rIdx++) {
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
			}
		}
	});

	return view;
});
