/*global define*/
define([
	'handlebars',
	'underscore',
	'module',
	'jquery'
], function (Handlebars,
		_,
		module,
		$) {
	"use strict";

	var me = {
		MANDATORY_COLUMNS:module.config().pdbRequiredColumns,
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
		importFromToken: function (args) {
			var token = args.token;
			var workspace = args.workspace;
			var layerColumns = args.layerColumns;
			var context = args.context || this;
			var location = args.location;
			
			return $.ajax('..' + location + '/workspace/' + workspace, {
				type: 'POST',
				context: context,
				data: {
					columns: JSON.stringify(layerColumns)
				}
			});
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

	return _.extend({}, me);
});