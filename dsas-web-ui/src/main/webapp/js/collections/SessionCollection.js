/*jslint browser: true */
/*global define*/
define([
	'backbone',
	'models/sessionModel',
	'localstorage'
], function (Backbone, SessionModel) {
	"use strict";
	var coll = Backbone.Collection.extend({
		model: SessionModel,
		localStorage: new Backbone.LocalStorage("dsas")
	});
	return coll;
});