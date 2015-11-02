/*global define*/
define([
	'jquery',
	'utils/logger',
	'openlayers',
	'underscore'
], function ($, log, OpenLayers, _) {
	"use strict";

	var utils = {
		geoserverProxyEndpoint: 'geoserver/',
		/**
		 * Creates an OpenLayers.Bounds object from a bounding box array
		 * 
		 * @param {Object} args
		 *	@property {array<Number>} array the array to create bounds from
		 *	@property {String} epsgFromCode if a transformation is required,
		 *	the projection the incoming bound are in
		 *	@property {String} epsgToCode if a transformation is required,
		 *	the projection the returned bounds should be in. If this parameter
		 *	is not included or is "EPSG:4326", not transformation will be performed
		 *	@property {boolean} flipAxis when creating bounds, flip lat/lon
		 * @returns {OpenLayers.Bounds} the bounds created from the array
		 */
		getBoundsFromArray: function (args) {
			args = args || {};
			var array = args.array,
					defaultEPSGCode = "EPSG:4326",
					epsgFromCode = args.epsgFromCode || defaultEPSGCode,
					epsgToCode = args.epsgToCode || defaultEPSGCode,
					flipAxis = args.flipAxis || false,
					bounds = OpenLayers.Bounds.fromArray(array, flipAxis);

			if (epsgToCode !== defaultEPSGCode) {
				bounds.transform(
						new OpenLayers.Projection(epsgFromCode),
						new OpenLayers.Projection(epsgToCode)
						);
			}
			
			return bounds;
		},
		/**
		 * Extends an OpenLayers.Bounds with another
		 * 
		 * @param {OpenLayers.Bounds} bounds1 the bounds to extend
		 * @param {OpenLayers.Bounds} bounds2 the bounds to extend with
		 * @returns {OpenLayers.Bounds} The extended bounds object
		 */
		extendBounds : function (bounds1, bounds2) {
			var clonedBounds1 = bounds1.clone();
			var clonedBounds2 = bounds2.clone();
			clonedBounds1.extend(clonedBounds2);
			return clonedBounds1;
		},
		getWMSCapabilities: function (args) {
			args = args || {};
			var namespace = args.namespace || 'ows';
			var url = this.geoserverProxyEndpoint + namespace + '/wms';
			var deferred = $.Deferred();
			$.ajax(url, {
				data: {
					service: 'wms',
					version: '1.3.0',
					request: 'GetCapabilities',
					cb: new Date().getTime() // Cachebreaking
				},
				context: {
					deferred: deferred,
					scope: args.context || this
				},
				success: function (data) {
					var getCapsResponse = new OpenLayers.Format.WMSCapabilities.v1_3_0().read(data);
					this.deferred.resolveWith(this.scope, [getCapsResponse]);
				},
				error: function () {
					this.deferred.rejectWith(this.scope, arguments);
				}
			});
			return deferred;
		}
	};

	return _.extend({}, utils);
});