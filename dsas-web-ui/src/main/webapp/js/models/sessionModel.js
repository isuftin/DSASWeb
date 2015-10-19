define([
	'backbone'
], function (Backbone) {
	"use strict";
	var session = Backbone.Model.extend({
		defaults: {
			isPerm: false,
			shorelines: {
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
				layers: [],
				viewing: ''
			},
			transects: {
				layers: [],
				viewing: ''
			},
			bias: {
				layers: [],
				viewing: ''
			},
			intersections: {
				layers: [],
				viewing: ''
			},
			calculation: {
				layers: [],
				viewing: ''
			},
			results: {
				layers: [],
				viewing: ''
			}
		}
	});
	return session;
});