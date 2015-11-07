/*jslint browser: true */
/*global define*/
define([
	'backbone',
	'models/sessionModel',
	'underscore',
	'localstorage'
], function (Backbone, SessionModel, _) {
	"use strict";
	var coll = Backbone.Collection.extend({
		model: SessionModel,
		localStorage: new Backbone.LocalStorage("dsas")
	});
	return coll;
});