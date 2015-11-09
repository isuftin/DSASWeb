// Describes the response from the server when calling the getShorelinesWithBbox service
define([
	'backbone'
], function (Backbone) {
	"use strict";
	var model = Backbone.Model.extend({
		defaults: {
			id: '',
			date: '',
			mhw: false,
			workspace: '',
			source: '',
			name: ''
		}
	});
	return model;
});