/*jslint browser: true */
/*global define*/
define([
	'openlayers',
	'handlebars',
	'views/BaseView',
	'utils/logger',
	'utils/Constants',
	'utils/MapUtil',
	'text!templates/pdb-management-map-view.html'
], function (
		OpenLayers,
		Handlebars,
		BaseView,
		log,
		Constants,
		MapUtil,
		template) {
	"use strict";

	var view = BaseView.extend({
		template: Handlebars.compile(template),
		render: function () {
			BaseView.prototype.render.apply(this, arguments);
			
			return this;
		},
		initialize: function (args) {
			args = args || {};
			
			this.initialExtent = [-15843196.966553, 2251625.961233, -5501572.7891212, 7593656.9932838];
			this.map = new OpenLayers.Map({
				projection: Constants.strings.epsg900913,
				displayProjection: new OpenLayers.Projection(Constants.strings.epsg900913)
			});
			this.map.addLayers(MapUtil.getBaseLayers());
			
			
			log.debug("DSASweb Proxy Datum Bias map view initializing");
			BaseView.prototype.initialize.apply(this, arguments);
			return this;
		},
		renderMap : function () {
			this.map.render('map');
			
			this.map.zoomToExtent(this.initialExtent, true);
		}
	});

	return view;
});