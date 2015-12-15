/*global define*/
define([
	'backbone'
], function (Backbone) {
	"use strict";
	var columnMatchingModel = Backbone.Model.extend({
		defaults: {
			layerColumns: null,
			layerName: null,
			defaultColumns: null,
			columnKeys: null,
			mandatoryColumns: null
		}
	});
	return columnMatchingModel;
});