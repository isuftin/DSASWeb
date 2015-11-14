/*jslint browser: true */
define([
	'handlebars',
	'views/BaseView',
	'utils/logger',
	'utils/constants',
	'utils/AppEvents',
	'openlayers',
	'utils/SessionUtil',
	'utils/OwsUtil',
	'utils/ShorelineUtil',
	'utils/MapUtil',
	'collections/ShorelineServiceModelCollection',
	'underscore',
	'text!templates/map.html'
], function (
		Handlebars,
		BaseView,
		log,
		Constants,
		AppEvents,
		OpenLayers,
		SessionUtil,
		OwsUtil,
		ShorelineUtil,
		MapUtil,
		ShorelineServiceModelCollection,
		_,
		template) {
	"use strict";

	var view = BaseView.extend({
		owsUtil: OwsUtil,
		template: Handlebars.compile(template),
		// The name given to the Area of Interest selection layer
		LAYER_AOI_NAME: 'layer-aoi-box',
		// The name of the control used to create area of interest selections
		CONTROL_IDENTIFY_AOI_ID: 'shoreline-identify-aoi-control',
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
			options.appEvents = AppEvents;
			BaseView.prototype.initialize.apply(this, [options]);
			
			this.mapDivId = options.mapDivId || 'map';

			this.initialExtent = [-15843196.966553, 2251625.961233, -5501572.7891212, 7593656.9932838];

			this.map = new OpenLayers.Map({
				projection: Constants.strings.epsg900913,
				displayProjection: new OpenLayers.Projection(Constants.strings.epsg900913)
			});

			this.map.addLayers(MapUtil.getBaseLayers());

			this.aoiSelectionLayer = MapUtil.createAoiSelectionLayer();

			this.aoiSelectionControl = new OpenLayers.Control.DrawFeature(this.aoiSelectionLayer,
					OpenLayers.Handler.RegularPolygon, {
						title: this.CONTROL_IDENTIFY_AOI_ID,
						handlerOptions: {
							sides: 4,
							irregular: true
						}
					});

			this.map.addLayer(this.aoiSelectionLayer);

			this.map.addControls([
				new OpenLayers.Control.MousePosition(),
				new OpenLayers.Control.ScaleLine({geodesic: true}),
				new OpenLayers.Control.LayerSwitcher({roundedCorner: true}),
				this.aoiSelectionControl
			]);

			this.listenTo(this.appEvents, this.appEvents.shorelines.aoiSelectionToggled, this.toggleAOIControl);
			this.listenTo(this.appEvents, this.appEvents.shorelines.aoiSelected, this.processAoiSelection);

			if (this.session) { // This is not functional during jasmine testing
				this.listenTo(this.session.get(SessionUtil.getCurrentSessionKey()), "change:shorelines", this.shorelinesUpdatedHandler);
			}

			return this;
		},
		/**
		 * Enables or disables the Area of Interest selection control
		 * 
		 * @param {Boolean} toggleOn
		 * @returns {OpenLayers.Control.DrawFeature} The AOI selection control
		 */
		toggleAOIControl: function (toggleOn) {
			if (toggleOn && !this.aoiSelectionControl.active) {
				this.aoiSelectionLayer.removeAllFeatures();
				ShorelineUtil.removeShorelineLayer({
					map : this.map
				});
				this.aoiSelectionControl.activate();
			} else {
				this.aoiSelectionControl.deactivate();
			}
			return this.aoiSelectionControl;
		},
		processAoiSelection: function () {
			var aoiContainsShorelines = false;
			if (this.aoiSelectionLayer.features.length === 1) {

				var shorelinesBoundsArray = this.session
						.get(SessionUtil.getCurrentSessionKey())
						.get('shorelines')
						.bbox;

				// Check if the user has any shorelines in their session
				if (!_.isEmpty(shorelinesBoundsArray)) {
					var bounds = this.aoiSelectionLayer.features[0].geometry.bounds.clone()
							.transform(new OpenLayers.Projection("EPSG:900913"),
									new OpenLayers.Projection("EPSG:4326"));
					var shorelinesBounds = this.owsUtil
							.getBoundsFromArray({
								array: shorelinesBoundsArray
							});
					aoiContainsShorelines = shorelinesBounds.intersectsBounds(bounds);

					if (aoiContainsShorelines) {
						var shorelineSvcModelCollection = new ShorelineServiceModelCollection(null, {
							workspace: SessionUtil.getCurrentSessionKey(),
							bbox: bounds.toArray().toString()
						});
						this.appEvents.trigger(this.appEvents.map.aoiSelected, shorelineSvcModelCollection);

						// Once the collection of shorelines is retrieved from the
						// back end, use the data to create the SLD and display the
						// layers.
						this.listenToOnce(shorelineSvcModelCollection, 'sync', function (collection) {
							ShorelineUtil.displayShorelinesForBounds({
								shorelineCollection: collection,
								map: this.map
							});

							this.map.zoomToExtent(this.aoiSelectionLayer.features[0].geometry.getBounds(), false);

							// Change what the AOI selection box looks like in order
							// to mark where the user has selected as their AOI
							var aoi = this.aoiSelectionLayer.features[0];
							var newStyle = $.extend({}, this.aoiSelectionLayer.defaultStyle);
							newStyle.fillOpacity = 0.0;
							newStyle.strokeOpacity = 1;
							this.aoiSelectionLayer.style = newStyle;
							this.aoiSelectionLayer.drawFeature(aoi, newStyle);

							// Zoom to the extent of the AOI selection
						});

						shorelineSvcModelCollection.fetch();
					} else {
						this.appEvents.trigger(this.appEvents.map.aoiSelected, null);
					}
				}

			} else {
				this.appEvents.trigger(this.appEvents.map.aoiSelected, null);
			}
			return aoiContainsShorelines;
		},
		remove: function () {
			BaseView.prototype.remove.apply(this);
			return this;
		}
	});

	return view;
});