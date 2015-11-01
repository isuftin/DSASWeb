/*global define*/
define([
	'underscore',
	'module'
], function (_, module) {
	"use strict";

	var me = {
		MANDATORY_COLUMNS: ['date', 'uncy'],
		DEFAULT_COLUMNS: [
			{attr: module.config().columnAttrNames.MHW, defaultValue: "0"},
			{attr: module.config().columnAttrNames.source, defaultValue: ''},
			{attr: module.config().columnAttrNames.name, defaultValue: ''},
			{attr: module.config().columnAttrNames.distance, defaultValue: ''},
			{attr: module.config().columnAttrNames.defaultDirection, defaultValue: ''},
			{attr: module.config().columnAttrNames.biasUncertainty, defaultValue: ''}
		],
		SHORELINE_STAGE_ENDPOINT: 'service/stage-shoreline',
		/**
		 * Try to intelligently map between a given set of columns and what the 
		 * known default columns are. 
		 * 
		 * @param {Object<String, Object>} args
		 *	@property {Object<String, String>} layerColumns The columns being attempted to map
		 * @returns {Object<String, String>} The mapped columns
		 */
		createLayerUnionAttributeMap: function (args) {
			var attributes = args.attributes,
					layerColumns = args.layerColumns;

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
			var token = args.token,
				workspace = args.workspace,
				layerColumns = args.layerColumns,
				context = args.context || this;
			
			return $.ajax(this.SHORELINE_STAGE_ENDPOINT, {
				type: 'POST',
				context : context,
				data: {
					action: 'import',
					token: token,
					workspace: workspace,
					columns: JSON.stringify(layerColumns)
				}
			});
		}
	};

	// TODO - Write tests for createLayerUnionAttributeMap
	return _.extend({}, me);
});