define([
	'underscore',
	'backbone'
], function (
		_,
		Backbone) {
	"use strict";
	var events = _.extend({
		map: {
			// The object passed with this event will be the ShorelineServiceModelCollection.
			// The fecth call on this object will be performed after this event has fired.
			aoiSelected: 'map_aoi_selected'
		},
		shorelines: {
			aoiSelectionToggled: 'shorelines_aoi_selection_toggled',
			aoiSelected: 'shorelines_aoi_selected',
			columnsMatched: 'shorelines_columns_matched',
			layerImportSuccess: 'shorelines_layer_import_success',
			layerImportFail: 'shorelines_layer_import_fail'
		},
		session: {
			wmsGetCapsCompleted: 'session_wms_getcaps_completed',
			wmsGetCapsFailed: 'session_wms_getcaps_failed'
		}
	}, Backbone.Events);

	return events;
});