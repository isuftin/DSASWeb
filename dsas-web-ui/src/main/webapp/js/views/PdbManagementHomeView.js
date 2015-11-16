/*jslint browser: true */
/*global define*/
define([
	'jquery',
	'handlebars',
	'views/BaseView',
	'views/PdbManagementMapView',
	'views/PdbManagementView',
	'utils/logger',
	'text!templates/pdb-management-home-view.html'
], function (
		$,
		Handlebars,
		BaseView,
		PdbManagementMapView,
		PdbManagementView,
		log,
		template) {
	"use strict";

	var view = BaseView.extend({
		template: Handlebars.compile(template),
		el: '#page-content-container',
		render: function () {
			BaseView.prototype.render.apply(this, arguments);

			this.mgmtView = new PdbManagementView().render();
			$(this.mgmtView.el).appendTo(this.$('#management-view'));

			this.mapView = new PdbManagementMapView().render();
			$(this.mapView.el).appendTo(this.$('#map-view'));
			this.mapView.renderMap();
			
			return this;
		},
		initialize: function () {
			log.debug("DSASweb Proxy Datum Bias management home view initializing");
			BaseView.prototype.initialize.apply(this, arguments);
		}
	});

	return view;
});