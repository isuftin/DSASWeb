// Describes the response from the server when calling the getShorelinesWithBbox service
define([
	'backbone',
	'utils/ShorelineUtil'
], function (
		Backbone,
		ShorelineUtil) {
	"use strict";
	var model = Backbone.Model.extend({
		defaults: {
			id: '',
			date: '',
			mhw: false,
			workspace: '',
			source: '',
			name: '',
			color: '#FFFFFF'
		},
		getColorForDateString: ShorelineUtil.getColorForDateString,
		constructor: function (model) {
			Backbone.Model.apply(this, arguments);
			this.set("color", this.getColorForDateString(model.date));
		}
	});
	return model;
});