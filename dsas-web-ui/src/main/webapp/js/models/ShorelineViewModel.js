/*global define*/
define([
	'backbone'
], function (Backbone) {
	"use strict";
	var shorelineModel = Backbone.Model.extend({
		defaults : {
			aoiToggledOn : false
		}
	});
	return shorelineModel;
});