/*jslint browser : true*/
/*global define*/
define([
	'backbone',
	'utils/logger',
	'views/PdbManagementHomeView',
	'jquery'
], function (Backbone, log, PdbManagementView, $) {
	"use strict";
	var applicationRouter = Backbone.Router.extend({
		routes: {
			'': 'displayPDBView',
			'login' : 'displayLoginView'
		},
		initialize: function () {
			log.trace("Initializing router");
			return this;
		},
		displayPDBView: function () {
			log.trace("Routing to PDB Management view");
			var pdbMgmtView = new PdbManagementView().render();
			return this.pdbView;
		},
		displayLoginView : function () {
			// Not yet implemented
		}
	});

	return applicationRouter;
});