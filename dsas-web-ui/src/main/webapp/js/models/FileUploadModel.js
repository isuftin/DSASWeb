/*global define*/
define([
	'backbone',
	'module'
], function (
		Backbone,
		module) {
	"use strict";
	var model = Backbone.Model.extend({
		defaults: {
			// This is created once the file is uploaded
			token: '',
			// Shoreline, Baseline, PDB, etc
			fileType: '',
			// What is the maximum file size allowed? This should also be checked
			// on the back end
			maxFileSize: Number.MAX_VALUE,
			// An array of allowed file types. Usually this will be just zips
			allowedFileTypes: [".zip"],
			// The request object that will be used. Useful for tracking
			xhr: new XMLHttpRequest(),
			// When the request object comes back, what will the callback scope be?
			callbackScope: this,
			// Where should the upload go to? Typically this would be the staging endpoint
			uploadEndpoint: module.config().SHAPEFILE_SERVICE_PATH,
			// If there is a column mapping stage to this upload, these columns will be mandatory
			mandatoryColumns : []
		}
	});

	return model;
});