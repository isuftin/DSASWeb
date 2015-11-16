/*global define*/
define([
	'handlebars',
	'underscore',
	'module',
	'text!templates/shoreline_color_sld.xml',
	'openlayers',
	'jquery'
], function (Handlebars,
		_,
		module,
		template,
		OpenLayers,
		$) {
	"use strict";

	var me = {
		LAYER_AOI_NAME: 'layer-aoi-box',
		MANDATORY_COLUMNS: ['date', 'uncy'],
		DEFAULT_COLUMNS: [
			{attr: module.config().columnAttrNames.MHW, defaultValue: "0"},
			{attr: module.config().columnAttrNames.source, defaultValue: ''},
			{attr: module.config().columnAttrNames.name, defaultValue: ''},
			{attr: module.config().columnAttrNames.distance, defaultValue: ''},
			{attr: module.config().columnAttrNames.defaultDirection, defaultValue: ''},
			{attr: module.config().columnAttrNames.biasUncertainty, defaultValue: ''}
		],
		SHORELINE_SLD_TEMPLATE: Handlebars.compile(template),
		SHORELINE_STAGE_ENDPOINT: 'service/stage-shoreline',
		GEOSERVER_PROXY_ENDPOINT: module.config().geoserverProxyEndpoint,
		/**
		 * Try to intelligently map between a given set of columns and what the 
		 * known default columns are. 
		 * 
		 * @param {Object<String, Object>} args
		 *	@property {Object<String, String>} layerColumns The columns being attempted to map
		 * @returns {Object<String, String>} The mapped columns
		 */
		createLayerUnionAttributeMap: function (args) {
			var attributes = args.attributes;
			var layerColumns = args.layerColumns;

			if (!layerColumns) {
				layerColumns = {};
				attributes
						.map(function (property) {
							return property.name;
						})
						.each(function (property) {
							layerColumns[property] = '';
						});
			}

			_.chain(layerColumns)
					.keys()
					.each(function (columnName) {
						var eqColName = this.MANDATORY_COLUMNS.find(function (column) {
							return column.toLowerCase() === columnName.toLowerCase();
						});

						if (!eqColName && this.DEFAULT_COLUMNS) {
							var defaultingColumnMatch = this.DEFAULT_COLUMNS.find(function (column) {
								return column.attr.toLowerCase() === columnName.toLowerCase();
							});
							if (defaultingColumnMatch) {
								eqColName = defaultingColumnMatch.attr;
							}
						}

						if (eqColName) {
							layerColumns[columnName] = eqColName;
						}
					}, this);


			return layerColumns;
		},
		getShorelineHeaderColumnNames: function (token) {
			return $.ajax(this.SHORELINE_STAGE_ENDPOINT, {
				'data': {
					'action': 'read-dbf',
					'token': token
				}
			});
		},
		importShorelineFromToken: function (args) {
			var token = args.token;
			var workspace = args.workspace;
			var layerColumns = args.layerColumns;
			var context = args.context || this;

			return $.ajax(this.SHORELINE_STAGE_ENDPOINT, {
				type: 'POST',
				context: context,
				data: {
					action: 'import',
					token: token,
					workspace: workspace,
					columns: JSON.stringify(layerColumns)
				}
			});
		},
		displayShorelinesForBounds: function (args) {
			args = args || {};
			var collection = args.shorelineCollection;
			var workspace = collection.workspace;
			var name = workspace + "_shorelines";
			var bbox = collection.bbox;
			var map = args.map;
			var cqlFilter = 'BBOX(geom, ' + bbox + ')';
			// Get all the dates for the models in a unique, sorted array
			var dates = _.chain(collection.models)
					.map(function (m) {
						return m.get('date');
					})
					.unique()
					.sortBy(function (d) {
						return new Date(d);
					})
					.value();
			var sld = this.createSLDBody({
				dates: dates,
				workspace: workspace
			});

			var layer = new OpenLayers.Layer.WMS(
					workspace + "_shorelines",
					this.GEOSERVER_PROXY_ENDPOINT + workspace + '/wms', {
						layers: [workspace + ":" + name],
						transparent: true,
						sld_body: sld,
						format: "image/png",
						bbox: bbox,
						cql_filter: cqlFilter
					}, {
				zoomToWhenAdded: false,
				isBaseLayer: false,
				unsupportedBrowsers: [],
				tileOptions: {
					// http://www.faqs.org/rfcs/rfc2616.html
					// This will cause any request larger than this many characters to be a POST
					maxGetUrlLength: 2048
				},
				title: name,
				singleTile: true,
				ratio: 1,
				layerType: "shorelines",
				displayInLayerSwitcher: false
			});

			this.removeShorelineLayer({
				map: map
			});

			map.addLayer(layer);

			return layer;
		},
		/**
		 * Removes the current shoreline layer on the map if one exists
		 * 
		 * @param {Object} args
		 *	@property {Object<OpenLayers.Map>} map the OpenLayers map to run against
		 * @returns {Object<OpenLayers.Layer.Vecotr} the layer that was removed from the map.
		 *	null if a shoreline layer did not exist on the map.
		 */
		removeShorelineLayer: function (args) {
			var map = args.map;
			var previousShorelinesLayer = map.getLayersBy('layerType', 'shorelines');
			if (previousShorelinesLayer.length) {
				map.removeLayer(previousShorelinesLayer[0], false);
			}
			return previousShorelinesLayer.length ? previousShorelinesLayer[0] : null;
		},
		/**
		 * Given a date string (or, really, any string), provides a hex color code
		 * that is 'unique' to the string provided. 
		 * 
		 * http://stackoverflow.com/questions/3426404/create-a-hexadecimal-colour-based-on-a-string-with-javascript
		 * 
		 * @param {String} dateString any string
		 * @returns {String} 6-digit hex color code
		 */
		getColorForDateString: function (dateString) {
			// str to hash
			for (var i = 0, hash = 0; i < dateString.length; hash = dateString.charCodeAt(i++) + ((hash << 5) - hash))
				;
			// int/hash to hex
			for (var i = 0, color = "#"; i < 3; color += ("00" + ((hash >> i++ * 8) & 0xFF).toString(16)).slice( - 2))
				;

			return color;
		},
		/**
		 * Given an array of dates and a workspace name, generates an SLD to 
		 * pair dates with colors to be sent in as a request to a WMS GetMap call
		 * 
		 * @param {Object} args
		 *	@property {Array<String>} dates expected in the format of YYYY-MM-DD
		 *	@property {String} workspace the name of the workspace the shoreline lives in
		 * @returns {String} the SLD xml 
		 */
		createSLDBody: function (args) {
			var dates = args.dates;
			var colorDatePairings = _.map(dates, function (d) {
				return {
					color: this.getColorForDateString(d),
					date: d
				};
			}, this);
			var workspace = args.workspace;
			var sldBody = this.SHORELINE_SLD_TEMPLATE({
				prefix: workspace,
				dates: colorDatePairings
			});

			return sldBody;
		}
	};

	// TODO - Write tests for createLayerUnionAttributeMap
	return _.extend({}, me);
});