/*jslint browser: true */
define([
	'handlebars',
	'views/BaseView',
	'utils/logger',
	'utils/constants',
	'openlayers',
	'text!templates/map.html'
], function (Handlebars, BaseView, log, Constants, OpenLayers, template) {
	"use strict";

	var view = BaseView.extend({
		template: Handlebars.compile(template),
		/*
		 * Renders the object's template using it's context into the view's element.
		 * @returns {extended BaseView}
		 */
		render: function () {
			BaseView.prototype.render.apply(this, arguments);
			
			this.map.render(this.mapDivId);
			
			this.map.zoomToExtent(this.initialExtent, true);
			
			return this;
		},
		/*
		 * @constructs
		 * @param {Object} options
		 *		@prop collection {ModelCollection instance}
		 *      @prop el {Jquery element} - render view in $el.
		 */
		initialize: function (options) {
			log.debug("DSASweb Map view initializing");
			options = options || {};
			this.mapDivId = options.mapDivId || 'map';
			
			this.initialExtent = [-15843196.966553,2251625.961233,-5501572.7891212,7593656.9932838];
			
			this.map = new OpenLayers.Map({
				projection: Constants.strings.epsg900913,
				displayProjection: new OpenLayers.Projection(Constants.strings.epsg900913)
			});

			var arcGisOnlineUrlPrepend = "http://services.arcgisonline.com/ArcGIS/rest/services/";
			var arcGisOnlineUrlPostpend = "/MapServer/tile/${z}/${y}/${x}";

			this.map.addLayers([
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
				}),
				new OpenLayers.Layer.Markers('geocoding-marker-layer', {
					displayInLayerSwitcher: false
				})
			]);

			this.map.addLayer(new OpenLayers.Layer.Markers('geocoding-marker-layer', {
				displayInLayerSwitcher: false
			}));

			this.map.addControls([
				new OpenLayers.Control.MousePosition(),
				new OpenLayers.Control.ScaleLine({geodesic: true}),
				new OpenLayers.Control.LayerSwitcher({roundedCorner: true})
			]);

			BaseView.prototype.initialize.apply(this);
		},
		remove: function () {
			BaseView.prototype.remove.apply(this);
			return this;
		}
	});

	return view;
});