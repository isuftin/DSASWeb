/*jslint browser: true */
/*global define*/
define([
	'backbone',
	'models/ShorelineServiceModel',
	'underscore',
	'localstorage'
], function (Backbone, ShorelineServiceModel, _) {
	"use strict";
	var coll = Backbone.Collection.extend({
		model: ShorelineServiceModel,
		workspace : '',
		bbox: ['-180.0000', '-90.0000', '180.0000', '90.0000'],
		initialize: function (incomingModels, options) {
			this.workspace = options.workspace;
			this.bbox = options.bbox || this.bbox;
			if (incomingModels) {
				this.models = incomingModels;
			}
		},
		url : function () {
			var url = 'service/shoreline?action=getShorelinesWithBbox&workspace=' + this.workspace;
			url += "&bbox=" + this.bbox;
			return url;
		},
		parse : function (response) {
			return JSON.parse(response.shorelines);
		}
	});
	return coll;
});