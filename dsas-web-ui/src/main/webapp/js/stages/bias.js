/* global LOG */
/* global CONFIG */
var ProxyDatumBias = {
	overrideWorkspace: CONFIG.name.proxydatumbias,
	overrideStore: "Uploads",
	stage: 'bias',
	suffixes: ['_bias'],
	mandatoryColumns: ['the_geom', 'segment_id', 'bias', 'avg_slope', 'uncyb'],
	columnMatchingTemplate: undefined,
	$removeButton: $('#bias-remove-btn'),
	$biasSelectList : $('#bias-list'),
	description: {
		'stage': '<p>Need description of PDB step here.',
		'view-tab': 'Select a published collection of bias lines/points to add to the workspace.',
		'manage-tab': ' Upload a zipped shapefile to add it to the workspace.',
		'upload-button': 'Upload a zipped shapefile which includes proxy datum bias features.'
	},
	appInit: function () {
		ProxyDatumBias.initializeUploader();
		
		ProxyDatumBias.$removeButton.on('click', ProxyDatumBias.removeResource);

		$.get('templates/column-matching-modal.mustache').done(function (data) {
			ProxyDatumBias.columnMatchingTemplate = Handlebars.compile(data);
		});

		CONFIG.ows.getWMSCapabilities({
			namespace: ProxyDatumBias.overrideWorkspace,
			callbacks: {
				success: [
					function () {
						LOG.trace('OnReady.js:: WMS Capabilities retrieved for ' 
							+ ProxyDatumBias.overrideWorkspace 
							+ ' workspace');
					}
				],
				error: [
					function () {
						CONFIG.ui.createModalWindow({
							headerHtml: 'Unable to interrogate OWS server',
							bodyHtml: 'The application could not interrogate the OWS server to get ' 
								+ ProxyDatumBias.overrideWorkspace 
								+ ' layers.'
						});
					}
				]
			}
		});
	},
	enterStage: function () {
		LOG.debug('bias.js::enterStage');
		CONFIG.ui.switchTab({
			caller: ProxyDatumBias,
			tab: 'view'
		});
	},
	leaveStage: function () {
		LOG.debug('bias.js::leaveStage');
		ProxyDatumBias.closeProxyDatumBiasIdWindows();
	},
	/**
	 * Calls DescribeFeatureType against OWS service and tries to add the layer(s) to the map 
	 */
	addProxyDatumBias: function (layers) {
		LOG.info('bias.js::addProxyDatumBias');

		LOG.debug('bias.js::addProxyDatumBias: Adding ' + layers.length + ' bias layers to map');
		layers.each(function (layer) {
			var layerTitle = layer.title;
			var layerPrefix = layer.prefix;
			var layerName = layer.name;

			var addToMap = function (data, textStatus, jqXHR) {
				LOG.trace('bias.js::addProxyDatumBias: Attempting to add bias layer ' + layerTitle + ' to the map.');
				CONFIG.ows.getDescribeFeatureType({
					layerNS: layerPrefix,
					layerName: layerName,
					callbacks: [
						function (describeFeaturetypeRespone) {
							LOG.trace('bias.js::addProxyDatumBias: Parsing layer attributes to check that they contain the attributes needed.');
							var attributes = describeFeaturetypeRespone.featureTypes[0].properties;
							if (attributes.length < ProxyDatumBias.mandatoryColumns.length) {
								LOG.warn('bias.js::addProxyDatumBias: There are not enough attributes in the selected shapefile to constitute a valid bias layer. Will be deleted. Needed: ' + ProxyDatumBias.mandatoryColumns.length + ', Found in upload: ' + attributes.length);
								ProxyDatumBias.removeResource();
								CONFIG.ui.showAlert({
									message: 'Not enough attributes in upload - Check Logs',
									caller: ProxyDatumBias,
									displayTime: 7000,
									style: {
										classes: ['alert-error']
									}
								});
							}

							var layerColumns = Util.createLayerUnionAttributeMap({
								caller: ProxyDatumBias,
								attributes: attributes
							});
							var foundAll = true;
							ProxyDatumBias.mandatoryColumns.each(function (mc) {
								if (layerColumns.values().indexOf(mc) === -1) {
									foundAll = false;
								}
							});

							if (layerPrefix !== CONFIG.name.published && !foundAll) {
								CONFIG.ui.buildColumnMatchingModalWindow({
									layerName: layerName,
									columns: layerColumns,
									caller: ProxyDatumBias,
									template: ProxyDatumBias.columnMatchingTemplate,
									continueCallback: function () {
										ProxyDatumBias.addLayerToMap({
											layer: layer,
											describeFeaturetypeRespone: describeFeaturetypeRespone
										});
									}
								});
							} else {
								ProxyDatumBias.addLayerToMap({
									layer: layer,
									describeFeaturetypeRespone: describeFeaturetypeRespone
								});
							}
						}
					]
				});
			};

			CONFIG.ows.getUTMZoneCount({
				layerPrefix: layer.prefix,
				layerName: layer.name,
				callbacks: {
					success: [
						function (data, textStatus, jqXHR) {
							LOG.trace('bias.js::addProxyDatumBias: UTM Zone Count Returned. ' + data + ' UTM zones found');
							if (data > 1) {
								CONFIG.ui.showAlert({
									message: 'ProxyDatumBias spans ' + data + ' UTM zones',
									caller: ProxyDatumBias,
									displayTime: 5000
								});
							}
							addToMap(data, textStatus, jqXHR);
						}
					],
					error: [
						function (data, textStatus, jqXHR) {
							LOG.warn('bias.js::addProxyDatumBias: Could not retrieve UTM count for this resource. It is unknown whether or not this bias resource crosses more than 1 UTM zone. This could cause problems later.');
							addToMap(data, textStatus, jqXHR);
						}
					]
				}
			});
		});
	},
	/**
	 * Uses a OWS DescribeFeatureType response to add a layer to a map
	 */
	addLayerToMap: function (args) {
		LOG.info('bias.js::addLayerToMap');
		var layer = args.layer;
		LOG.debug('bias.js::addLayerToMap: Adding bias layer ' + layer.title + 'to map');
		var properties = CONFIG.ows.getLayerPropertiesFromWFSDescribeFeatureType({
			describeFeatureType: args.describeFeaturetypeRespone,
			includeGeom: false
		})[layer.name];

		CONFIG.ows.getFilteredFeature({
			layerPrefix: layer.prefix,
			layerName: layer.name,
			propertyArray: properties,
			scope: this,
			callbacks: {
				success: [
					function (features) {
						LOG.info('bias.js::addLayerToMap: WFS GetFileterdFeature returned successfully');
						if (CONFIG.map.getMap().getLayersByName(layer.title).length === 0) {
							LOG.info('bias.js::addLayerToMap: Layer does not yet exist on the map. Loading layer: ' + layer.title);

							var stage = CONFIG.tempSession.getStage(ProxyDatumBias.stage);

							var wmsLayer = new OpenLayers.Layer.WMS(
								layer.title,
								'geoserver/' + layer.prefix + '/wms',
								{
									layers: [layer.name],
									transparent: true,
									sld_body: ProxyDatumBias.createSLDBody({
										layerName: layer.prefix + ':' + layer.name
									}),
									format: "image/png"
								},
							{
								prefix: layer.prefix,
								zoomToWhenAdded: true, // Include this layer when performing an aggregated zoom
								isBaseLayer: false,
								unsupportedBrowsers: [],
								describedFeatures: features,
								tileOptions: {
									// http://www.faqs.org/rfcs/rfc2616.html
									// This will cause any request larger than this many characters to be a POST
									maxGetUrlLength: 2048
								},
								singleTile: true,
								ratio: 1,
								displayInLayerSwitcher: false
							});

							CONFIG.map.getMap().addLayer(wmsLayer);
							wmsLayer.redraw(true);
						}
					}
				],
				error: [
					function () {
						LOG.warn('bias.js::addLayerToMap: Failed to retrieve a successful WFS GetFileterdFeature response');
					}
				]
			}
		});
	},
	clear: function () {
		$("#bias-list").val('');
		ProxyDatumBias.listboxChanged();
	},
	listboxChanged: function () {
		LOG.info('bias.js::listboxChanged: Proxy datum bias listbox has changed');
		$("#bias-list option:not(:selected)").each(function (index, option) {
			var layers = CONFIG.map.getMap().getLayersBy('name', option.text);
			if (layers.length) {
				$(layers).each(function (i, layer) {
					CONFIG.map.getMap().removeLayer(layer);
				});
			}
		});

		var layerInfos = [];
		var $selectedBiases = $("#bias-list option:selected");
		var stage = CONFIG.tempSession.getStage(ProxyDatumBias.stage);
		stage.viewing = [];
		if ($selectedBiases.val()) {
			$selectedBiases.each(function (index, option) {
				LOG.debug('bias.js::biasSelected: A bias (' + option.text + ') was selected from the select list');
				var layerFullName = option.value;
				var layerNamespace = layerFullName.split(':')[0];
				var layerTitle = layerFullName.split(':')[1];
				var layer = CONFIG.ows.getLayerByName({
					layerNS: layerNamespace,
					layerName: layerTitle
				});
				layerInfos.push(layer);
				stage.viewing.push(layerFullName);
				if (layerFullName.has(CONFIG.name.proxydatumbias)) {
					ProxyDatumBias.enableRemoveButton();
				}
			});
			ProxyDatumBias.addProxyDatumBias(layerInfos);
		} else {
			ProxyDatumBias.disableRemoveButton();
		}
		CONFIG.tempSession.persistSession();
	},
	populateFeaturesList: function () {
		CONFIG.ui.populateFeaturesList({
			caller: ProxyDatumBias
		});
	},
	initializeUploader: function (args) {
		CONFIG.ui.initializeUploader($.extend({
			caller: ProxyDatumBias
		}, args));
	},
	closeProxyDatumBiasIdWindows: function () {
		$('#FramedCloud_close').trigger('click');
	},
	disableRemoveButton: function () {
		ProxyDatumBias.$removeButton.attr('disabled', 'disabled');
	},
	enableRemoveButton: function () {
		ProxyDatumBias.$removeButton.removeAttr('disabled');
	},
	removeResource: function (args) {
		args = args || {};
		var layer = args.layer || $('#bias-list option:selected')[0].text;
		var store = args.store || 'ch-input';
		var callbacks = args.callbacks || [
			function (data, textStatus, jqXHR) {
				CONFIG.ui.showAlert({
					message: 'ProxyDatumBias removed',
					caller: ProxyDatumBias,
					displayTime: 4000,
					style: {
						classes: ['alert-success']
					}
				});

				CONFIG.ows.getWMSCapabilities({
					namespace: CONFIG.name.proxydatumbias,
					callbacks: {
						success: [
							function () {
								$('#bias-list').val('');
								$('#bias-list').trigger('change');
								CONFIG.ui.switchTab({
									caller: ProxyDatumBias,
									tab: 'view'
								});
								ProxyDatumBias.populateFeaturesList();
							}
						]
					}
				});
			}
		];
		try {
			CONFIG.tempSession.removeResource({
				session: CONFIG.name.proxydatumbias,
				store: store,
				layer: layer,
				callbacks: callbacks
			});
		} catch (ex) {
			CONFIG.ui.showAlert({
				message: 'Unable to remove resource - ' + ex,
				caller: ProxyDatumBias,
				displayTime: 4000,
				style: {
					classes: ['alert-error']
				}
			});
		}
	},
	createSLDBody: function (args) {
		var sldBody = '';
		var fillColor = '#2E2EFE';

		sldBody += '<?xml version="1.0" encoding="ISO-8859-1"?>' +
			'<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">' +
			'<NamedLayer>' +
			'<Name>' + args.layerName + '</Name>' +
			'<UserStyle>' +
			'<FeatureTypeStyle>' +
			'<Rule>' +
			'<PointSymbolizer>' +
			'<Graphic>' +
			'<Mark>' +
			'<WellKnownName>triangle</WellKnownName>' +
			'<Fill>' +
			'<CssParameter name="fill">' + fillColor + '</CssParameter>' +
			'</Fill>' +
			'<Stroke>' +
			'<CssParameter name="stroke">#000000</CssParameter>' +
			'<CssParameter name="stroke-width">2</CssParameter>' +
			'</Stroke>' +
			'</Mark>' +
			'<Size>8</Size>' +
			'</Graphic>' +
			'</PointSymbolizer>' +
			'</Rule>' +
			'</FeatureTypeStyle>' +
			'</UserStyle>' +
			'</NamedLayer>' +
			'</StyledLayerDescriptor>';
		return sldBody;
	},
	getBiasRef : function () {
		var biasRef = '',
			selectedBias = ProxyDatumBias.$biasSelectList.find(':selected')[0],
			defaultBias = ProxyDatumBias.$biasSelectList.find('option.session-layer')[0];
		
		if (selectedBias) {
			biasRef = selectedBias.value;
		} else if (defaultBias) {
			biasRef = defaultBias.value;
		}
		
		return biasRef;
	},
	getActive: function () {
		return $('#bias-list').children(':selected').map(function (i, v) {
			return v.value;
		}).toArray();
	}
};
