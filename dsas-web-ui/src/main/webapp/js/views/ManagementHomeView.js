/*jslint browser: true */
/*global define*/
define([
	'jquery',
	'handlebars',
	'views/BaseView',
	'views/ManagementMapView',
	'views/ManagementView',
	'utils/logger',
	'text!templates/management-home-view.html'
], function (
		$,
		Handlebars,
		BaseView,
		ManagementMapView,
		ManagementView,
		log,
		template) {
	"use strict";

	var view = BaseView.extend({
		template: Handlebars.compile(template),
		el: '#page-content-container',
		render: function () {
			BaseView.prototype.render.apply(this, arguments);

			this.mgmtView = new ManagementView().render();
			$(this.mgmtView.el).appendTo(this.$('#management-view'));

			this.mapView = new ManagementMapView().render();
			$(this.mapView.el).appendTo(this.$('#map-view'));
			this.mapView.renderMap();
			
			return this;
		},
		initialize: function () {
			log.debug("DSASweb management home view initializing");
			BaseView.prototype.initialize.apply(this, arguments);
		}
	});

	return view;
});