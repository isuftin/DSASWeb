/*global define*/
define([
	'openlayers',
	'underscore',
	'jquery'
], function (
		OpenLayers,
		_,
		$
		) {
	"use strict";

	var me = {
		createAoiSelectionLayer: function () {
			var aoiSelectionLayer = new OpenLayers.Layer.Vector(this.LAYER_AOI_NAME, {
				displayInLayerSwitcher: false
			});
			aoiSelectionLayer.defaultStyle = $.extend({}, aoiSelectionLayer.styleMap.styles['default'].defaultStyle);
			aoiSelectionLayer.events.register('beforefeatureadded', null, function (e) {
				e.object.removeAllFeatures();
			});
			aoiSelectionLayer.events.register('featureadded', null, function () {
				this.style = $.extend({}, this.defaultStyle);
				this.drawFeature(this.features[0], this.style);
			});
			return aoiSelectionLayer;
		},
		getBaseLayers: function () {
			var arcGisOnlineUrlPrepend = "http://services.arcgisonline.com/ArcGIS/rest/services/";
			var arcGisOnlineUrlPostpend = "/MapServer/tile/${z}/${y}/${x}";
			return [
				new OpenLayers.Layer.XYZ("World Imagery",
						arcGisOnlineUrlPrepend + "World_Imagery" + arcGisOnlineUrlPostpend,
						{
							sphericalMercator: true,
							isBaseLayer: true,
							numZoomLevels: 20,
							wrapDateLine: true
						}
				),
				new OpenLayers.Layer.XYZ("Street",
						arcGisOnlineUrlPrepend + "World_Street_Map" + arcGisOnlineUrlPostpend,
						{
							sphericalMercator: true,
							isBaseLayer: true,
							numZoomLevels: 20,
							wrapDateLine: true
						}
				),
				new OpenLayers.Layer.XYZ("Topo",
						arcGisOnlineUrlPrepend + "World_Topo_Map" + arcGisOnlineUrlPostpend,
						{
							sphericalMercator: true,
							isBaseLayer: true,
							numZoomLevels: 20,
							wrapDateLine: true
						}
				),
				new OpenLayers.Layer.XYZ("Terrain",
						arcGisOnlineUrlPrepend + "World_Terrain_Base" + arcGisOnlineUrlPostpend,
						{
							sphericalMercator: true,
							isBaseLayer: true,
							numZoomLevels: 14,
							wrapDateLine: true
						}
				),
				new OpenLayers.Layer.XYZ("Shaded Relief",
						arcGisOnlineUrlPrepend + "World_Shaded_Relief" + arcGisOnlineUrlPostpend,
						{
							sphericalMercator: true,
							isBaseLayer: true,
							numZoomLevels: 14,
							wrapDateLine: true
						}
				),
				new OpenLayers.Layer.XYZ("Physical",
						arcGisOnlineUrlPrepend + "World_Physical_Map" + arcGisOnlineUrlPostpend,
						{
							sphericalMercator: true,
							isBaseLayer: true,
							numZoomLevels: 9,
							wrapDateLine: true
						}
				),
				new OpenLayers.Layer.XYZ("Ocean",
						arcGisOnlineUrlPrepend + "Ocean_Basemap" + arcGisOnlineUrlPostpend,
						{
							sphericalMercator: true,
							isBaseLayer: true,
							numZoomLevels: 17,
							wrapDateLine: true
						}
				),
				new OpenLayers.Layer.XYZ("Boundaries",
						arcGisOnlineUrlPrepend + "Reference/World_Boundaries_and_Places_Alternate" + arcGisOnlineUrlPostpend,
						{
							layers: '0',
							numZoomLevels: 13,
							transparent: true,
							displayInLayerSwitcher: true
						},
				{
					visibility: false,
					isBaseLayer: false
				}),
				new OpenLayers.Layer.XYZ("World Reference",
						arcGisOnlineUrlPrepend + "Reference/World_Reference_Overlay" + arcGisOnlineUrlPostpend,
						{
							layers: '0',
							numZoomLevels: 14,
							transparent: true,
							displayInLayerSwitcher: true
						},
				{
					visibility: false,
					isBaseLayer: false
				})
			];
		}
	};

	// TODO - Write tests for createLayerUnionAttributeMap
	return _.extend({}, me);
});