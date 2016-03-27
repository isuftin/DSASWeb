/*global Handlebars */
/*global CONFIG */
/*global Shorelines */
/*global OpenLayers */
/*global ProxyDatumBias */
/*global Baseline */
/*global Transects */

//TODO- 
//	- When execution plan layers become a real thing, make sure that they are 
//		deleted when a user deletes their session. Also add it to the sweeper process.

"use strict";
var TransectVerification = {
	stage: 'transect_verification',
	suffixes: ['_baseline'], // There are no layers specific to this stage
	DEFAULT_SPACING: 50,
	WPS_REQUEST_TEMPLATE: null,
	WPS_REQUEST: null,
	UTM_LAYER: null,
	controlIdentifiers: {
		'utmSelector': "#ctrl-transect-verification-utm",
		'submitButton': "#ctrl-transect-verification-submit"
	},
	// Define the object being passed into the Handlebars template to create a 
	// complete WPS request
	WPS_REQUEST_OBJECT: {
		processName: "gs:CreateExecutionPlan",
		shorelineInfo: null,
		bounds: null,
		baseline: null,
		baselineNS: null,
		calculationProjection: null,
		spacing: Transects.DEFAULT_SPACING,
		smoothing: null,
		length: null,
		workspace: null,
		store: 'ch-input',
		planLayer: null,
		isReady: false
	},
	description: {
		'stage': '<p>Transect Verification Description</p>',
		'view-tab': 'Transect Verification View Tab Text.'
	},
	appInit: function () {
		$.get('templates/wps-request-create-execution-plan.mustache').done(function (data) {
			TransectVerification.WPS_REQUEST_TEMPLATE = Handlebars.compile(data);
		});

		$(this.controlIdentifiers.submitButton).on('click', $.proxy(this.processCalculation, this));

		this.UTM_LAYER = new OpenLayers.Layer.ArcGIS93Rest("UTM Zones",
				"http://www.usda.gov/giseas1/rest/services/NRCS/Designated_UTM_Zone/MapServer/export",
				{
					layers: "show:0",
					transparent: true,
					srs: 'EPSG:4326',
					format: 'png32'
				},
				{
					buffer: 2,
					visibility: true,
					projection: 'EPSG:3857',
					isBaseLayer: false,
					transitionEffect: 'resize'
				});

	},
	enterStage: function () {
		CONFIG.map.getMap().addLayer(this.UTM_LAYER);

		var wpsRequestObject = this.createWPSRequestObject();

		if (wpsRequestObject.isReady) { // If this stage is ready, enable controls
			this.WPS_REQUEST = this.getWPSRequest({wpsRequestObject: wpsRequestObject});
			$(this.controlIdentifiers.utmSelector).prop('disabled', false);
			$(this.controlIdentifiers.submitButton).prop('disabled', false);
		} else {
			$(this.controlIdentifiers.utmSelector).prop('disabled', true);
			$(this.controlIdentifiers.submitButton).prop('disabled', true);
		}
	},
	leaveStage: function () {
		// TODO- Remove execution plan layer
		CONFIG.map.getMap().removeLayer(this.UTM_LAYER);
	},
	createWPSRequestObject: function () {
		var wpsRequestObject = $.extend(true, {}, TransectVerification.WPS_REQUEST_OBJECT);
		var length = $('#create-transects-input-length').val();
		var smoothing = $('#create-transects-input-smoothing').val();
		var shorelines = CONFIG.tempSession.getStage(Shorelines.stage).layers;
		var baseline = Baseline.getActive();
		var shorelineInfo = [];
		var calculationProjection = $(this.controlIdentifiers.utmSelector).val(); // TODO- Where do we get this from? Baseline centroid? Currently a select list
		var spacing = $('#create-transects-input-spacing').val();
		var workspace = CONFIG.tempSession.getCurrentSessionKey();
		var planLayer = "execution_plan_layer_" + new Date().getTime(); // TODO- Is this valid?

		// Do initial check to make sure I have all the values needed
		if (shorelines.length !== 0 && // Layers were selected
				Shorelines.aoiBoundsSelected !== null && // Area of Interest was defined
				!baseline.isBlank()) {// Baseline was chosen

			var baselineNS = baseline.split(':')[0] + '="gov.usgs.cida.ch.' + baseline.split(':')[0] + '"';
			var bounds = Shorelines.aoiBoundsSelected.clone().transform(new OpenLayers.Projection(CONFIG.strings.epsg900913), new OpenLayers.Projection(CONFIG.strings.epsg4326)).toArray(false);

			if (spacing.isBlank() || isNaN(parseFloat(spacing))) {
				spacing = Transects.DEFAULT_SPACING;
			}

			if (length.isBlank() || isNaN(parseFloat(length))) {
				length = null;
			}

			// Create the shoreline object needed to create the WPS request
			shorelines.forEach(function (shoreline) {
				var shorelineWorkspace = shoreline.split(':')[0];
				this.push({
					name: shoreline,
					excludedShorelines: CONFIG.tempSession.getDisabledShorelines(shorelineWorkspace),
					workspace: shorelineWorkspace
				});
			}, shorelineInfo);

			if (smoothing.isBlank() || isNaN(smoothing)) {
				smoothing = Transects.DEFAULT_SMOOTHING;
			} else {
				smoothing = smoothing <= 0.0 ? 0.0 : smoothing;
			}

			wpsRequestObject.shorelineInfo = shorelineInfo;
			wpsRequestObject.bounds = bounds;
			wpsRequestObject.baseline = baseline;
			wpsRequestObject.baselineNS = baselineNS;
			wpsRequestObject.calculationProjection = calculationProjection;
			wpsRequestObject.spacing = spacing;
			wpsRequestObject.smoothing = smoothing;
			wpsRequestObject.length = length;
			wpsRequestObject.workspace = workspace;
			wpsRequestObject.planLayer = planLayer;
			wpsRequestObject.isReady = true;
		}

		return wpsRequestObject;
	},
	getWPSRequest: function (args) {
		args = args || {};
		if (!args.wpsRequestObject) {
			return null;
		}
		return  TransectVerification.WPS_REQUEST_TEMPLATE(args.wpsRequestObject);
	},
	submitWPSRequest: function (args) {
		args = args || {};
		if (!args.wpsRequestObject) {
			throw "TransectVerification.submitWPSRequest() was called without a wps request object";
		}
		return CONFIG.ows.executeWPSProcess({
			processIdentifier: args.wpsRequestObject.processName,
			request: TransectVerification.WPS_REQUEST,
			context: args.context || this
		});
	},
	processCalculation: function () {
		$.when(this.submitWPSRequest({
			wpsRequestObject: this.WPS_REQUEST,
			context: this
		})).done(function (data, textStatus, jqXHR) {
			// TODO- What do we do here?
		}).fail(function (jqXHR, textStatus, errorThrown) {
			// TODO- What do we do here?
		});

	}
};