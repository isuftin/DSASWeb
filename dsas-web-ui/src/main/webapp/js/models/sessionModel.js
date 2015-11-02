define([
	'backbone'
], function (Backbone) {
	"use strict";
	var session = Backbone.Model.extend({
		defaults: {
			shorelines: {
				bbox: [],
				layers: [],
				viewing: [],
				groupingColumn: 'date',
				view: {
					layer: {
						'dates-disabled': []
					}
				}
			},
			baseline: {
				bbox: [],
				layers: [],
				viewing: ''
			},
			transects: {
				bbox: [],
				layers: [],
				viewing: ''
			},
			bias: {
				bbox: [],
				layers: [],
				viewing: ''
			},
			intersections: {
				bbox: [],
				layers: [],
				viewing: ''
			},
			calculation: {
				bbox: [],
				layers: [],
				viewing: ''
			},
			results: {
				bbox: [],
				layers: [],
				viewing: ''
			}
		}
	});
	return session;
});