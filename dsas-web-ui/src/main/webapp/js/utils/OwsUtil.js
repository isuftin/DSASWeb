define([
	'jquery',
	'utils/logger',
	'openlayers'
], function ($, log, OpenLayers) {
	"use strict";
	var geoserverProxyEndpoint = 'geoserver/';

	var getWMSCapabilities = function (args) {
		args = args || {};
		var namespace = args.namespace || 'ows';
		var url = geoserverProxyEndpoint + namespace + '/wms';
		var deferred = $.Deferred();
		$.ajax(url, {
			data : {
				service : 'wms',
				version : '1.3.0',
				request : 'GetCapabilities',
				cb : new Date().getTime() // Cachebreaking
			},
			context : {
				deferred : deferred,
				scope : args.context || this
			},
			success : function (data) {
				var getCapsResponse = new OpenLayers.Format.WMSCapabilities.v1_3_0().read(data);
				this.deferred.resolveWith(this.scope, [getCapsResponse]);
			},
			error : function () {
				this.deferred.rejectWith(this.scope, arguments);
			}
		});
		return deferred;
	};

	return {
		getWMSCapabilities: getWMSCapabilities
	};
});