define([
	'underscore',
	'backbone'
], function (
		_,
		Backbone) {
	"use strict";
	var events = _.extend({
		staging: {
			columnsMatched: 'shorelines_columns_matched'
		}
	}, Backbone.Events);

	return events;
});