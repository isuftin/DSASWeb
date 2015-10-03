/* global LOG, CONFIG, OpenLayers, Handlebars */
var Shorelines = {
	stage: 'shorelines',
	suffixes: ['_shorelines'],
	mandatoryColumns: ['date', 'uncy'],
	defaultingColumns: [
		{attr: CONFIG.strings.columnAttrNames.MHW, defaultValue: "0"},
		{attr: CONFIG.strings.columnAttrNames.source, defaultValue: ''},
		{attr: CONFIG.strings.columnAttrNames.name, defaultValue: ''},
		{attr: CONFIG.strings.columnAttrNames.distance, defaultValue: ''},
		{attr: CONFIG.strings.columnAttrNames.defaultDirection, defaultValue: ''},
		{attr: CONFIG.strings.columnAttrNames.biasUncertainty, defaultValue: ''}
	],
	groupingColumn: 'date',
	dateFormat: '{yyyy}-{MM}-{dd}',
	selectedFeatureClass: 'selected-feature-row',
	columnMatchingTemplate: undefined,
	featureTableRowTemplate: undefined,
	CONTROL_IDENTIFY_ID: 'shoreline-identify-control',
	CONTROL_IDENTIFY_AOI_ID: 'shoreline-identify-aoi-control',
	LAYER_AOI_NAME: 'layer-aoi-box',
	// Defines which columns from the shorelines table are shown in the click-to-id box
	clickToIdColumnNames: ['date', 'source', 'mhw', 'uncy'],
	$buttonSelectAOI: $('#shorelines-aoi-select-toggle'),
	$buttonSelectAOIDone: $('#shorelines-aoi-select-done'),
	$controlSelectSortingColumn: $('#ctrl-shorelines-sort-select'),
	$containerSelectSorting: $('#shorelines-feature-table-button-sort'),
	$descriptionAOI: $('#description-aoi'),
	$shorelineFeatureTableContainer: $('#shorelines-feature-table-container'),
	aoiBoundsSelected: null,
	shorelinesServiceEndpoint: 'service/shoreline?',
	uploadRequest: {
		'endpoint': 'service/stage-shoreline',
		'paramsInBody': false,
		'forceMultipart': false,
		params: {
			'response.encoding': 'json',
			'filename.param': 'qqfile',
			'action': 'stage',
			'workspace': 'added during app init'
		}
	},
	uploadExtraParams: {
		'action': 'stage'
	},
	description: {
		'stage': '<p>Shorelines are geospatial polylines which represent the location of the shoreline and various points in time</p> <p>Add shorelines to your workspace with the selection box above or upload your own zipped shapefile containing shoreline polylines within the Manage tab.</p><p>Use the map to investigate the selected shorelines, clicking to enable/disable for DSASweb processing.</p><hr />View and select existing published shorelines, or upload your own. Shorelines represent snap-shots of the coastline at various points in time.',
		'view-tab': 'Select a published collection of shorelines to add to the workspace.',
		'manage-tab': ' Upload a zipped shapefile to add it to the workspace.',
		'upload-button': 'Upload a zipped shapefile which includes shoreline polyline features.'
	},
	appInit: function () {
		"use strict";
		this.initSession();
		this.uploadRequest.params.workspace = CONFIG.tempSession.session.id;
		this.initializeUploader();
		this.bindSelectAOIButton();
		this.bindSelectAOIDoneButton();
		$.get('templates/column-matching-modal.mustache').done(function (data) {
			Shorelines.columnMatchingTemplate = Handlebars.compile(data);
			Handlebars.registerHelper('printDefaultValue', function () {
				if (this.defaultValue) {
					return new Handlebars.SafeString(' Default: "' + this.defaultValue + '"');
				} else {
					return '';
				}
			});
		});
		$.get('templates/shoreline-feature-table-row.mustache').done(function (data) {
			Shorelines.featureTableRowTemplate = Handlebars.compile(data);
			Handlebars.registerHelper('isChecked', function () {
				if (this.checked) {
					return new Handlebars.SafeString('checked="checked"');
				} else {
					return '';
				}
			});
		});
		Shorelines.enterStage();
	},
	initSession: function () {
		"use strict";
		var sessionStage = CONFIG.tempSession.getStage(this.stage);
		// Add a disabled dates key
		if (!sessionStage.datesDisabled) {
			sessionStage.datesDisabled = [];
		}

		// Add a disabled shoreline id for published and this workspace;
		var sKey = CONFIG.tempSession.getCurrentSessionKey();
		if (!sessionStage.slDisabled) {
			sessionStage.slDisabled = Object.extended({
				published: []
			});
		}
		if (!sessionStage.slDisabled[sKey]) {
			sessionStage.slDisabled[sKey] = [];
		}

		// Cleaning up legacy
		// Remove individual shorelines layers from session
		Object.keys(sessionStage).forEach(function (k) {
			if (k.indexOf(':') !== -1) {
				delete sessionStage[k];
			}
		});
		if (sessionStage.dateFormat) {
			delete sessionStage.dateFormat;
		}

		CONFIG.tempSession.setStage({
			stage: this.stage,
			obj: sessionStage
		});
		CONFIG.tempSession.persistSession();
		Shorelines.getAvailableAuxillaryColumns();
	},
	enterStage: function () {
		"use strict";
		LOG.debug('Shorelines.js::enterStage');
		Shorelines.activateShorelineIdControl();
		CONFIG.ui.switchTab({
			caller: Shorelines,
			tab: 'view'
		});
	},
	leaveStage: function () {
		"use strict";
		LOG.debug('Shorelines.js::leaveStage');
		Shorelines.deactivateShorelineIdControl();
		Shorelines.closeShorelineIdWindows();
		Shorelines.toggleBindSelectAOIButton(false);
	},
	getAvailableAuxillaryColumns: function () {
		"use strict";
		LOG.debug('Shorelines.js::updateSessionWithAuxillaryColumns');
		$.get(this.shorelinesServiceEndpoint, {
			'action': 'getAuxillaryNames',
			'workspace': CONFIG.tempSession.getCurrentSessionKey()
		}, function (obj, status) {
			Shorelines.hideSortingSelectionContainer();
			if (status === 'success' && obj.success === 'true') {
				var names = JSON.parse(obj.names),
					$option = $('<option />');
				if (names.length) {
					Shorelines.$controlSelectSortingColumn.empty();
					Shorelines.$controlSelectSortingColumn.append($option.clone());
					names.forEach(function (name) {
						var option = $option.clone().attr('value', name).html(name);
						Shorelines.$controlSelectSortingColumn.append(option);
					});
					Shorelines.bindSortingSelectionControl();
					Shorelines.updateSortingSelectionControl(obj.name);
					Shorelines.showSortingSelectionContainer();
				}
			}
		});
	},
	showSortingSelectionContainer: function () {
		"use strict";
		Shorelines.$containerSelectSorting.removeClass('hidden');
	},
	hideSortingSelectionContainer: function () {
		"use strict";
		Shorelines.$containerSelectSorting.addClass('hidden');
	},
	bindSortingSelectionControl: function () {
		"use strict";
		Shorelines.$controlSelectSortingColumn.off('change', Shorelines.sortingSelectionUpdated);
		Shorelines.$controlSelectSortingColumn.on('change', Shorelines.sortingSelectionUpdated);
	},
	updateSortingSelectionControl: function (name) {
		"use strict";
		Shorelines.$controlSelectSortingColumn.val(name);
	},
	sortingSelectionUpdated: function (e) {
		"use strict";
		var value = e.target.value,
			deferred = $.Deferred();
		deferred
			.then(function () {
				Shorelines.updateSortingColumnOnTable();
			}, function () {
				CONFIG.ui.showAlert({
					message: 'Could not update sorting column. ' +
						'If error continues to happen, contact support',
					caller: Shorelines,
					displayTime: 5000
				});
			});
		Shorelines.updateSortingColumnOnServer({
			name: value,
			deferred: deferred
		});
	},
	/**
	 * Calls DescribeFeatureType against OWS service and tries to add the layer(s) to the map 
	 * @param {type} layers
	 * @returns {undefined}
	 */
	addShorelines: function (layers) {
		"use strict";
		LOG.info('Shorelines.js::addShorelines');
		LOG.debug('Shorelines.js::addShorelines: Adding ' + layers.length + ' shoreline layers to map');
		layers.each(function (layer) {
			var layerTitle = layer.title;
			var layerPrefix = layer.prefix;
			var layerName = layer.name;
			var addToMap = function () {
				LOG.trace('Shorelines.js::addShorelines: Attempting to add shoreline layer ' + layerTitle + ' to the map.');
				CONFIG.ows.getDescribeFeatureType({
					layerNS: layerPrefix,
					layerName: layerName,
					callbacks: [
						function (describeFeaturetypeRespone) {
							LOG.trace('Shorelines.js::addShorelines: Parsing layer attributes to check that they contain the attributes needed.');
							var attributes = describeFeaturetypeRespone.featureTypes[0].properties;
							if (attributes.length < Shorelines.mandatoryColumns.length) {
								LOG.warn('Shorelines.js::addShorelines: There are not enough attributes in the selected shapefile to constitute a valid shoreline. Will be deleted. Needed: ' + Shorelines.mandatoryColumns.length + ', Found in upload: ' + attributes.length);
								Shorelines.removeResource();
								CONFIG.ui.showAlert({
									message: 'Not enough attributes in upload - Check Logs',
									caller: Shorelines,
									displayTime: 7000,
									style: {
										classes: ['alert-error']
									}
								});
							}

							var layerColumns = Util.createLayerUnionAttributeMap({
								caller: Shorelines,
								attributes: attributes
							});
							var foundAll = true;
							Shorelines.mandatoryColumns.each(function (mc) {
								if (layerColumns.values().indexOf(mc) === -1) {
									foundAll = false;
								}
							});
							Shorelines.defaultingColumns.each(function (col) {
								if (layerColumns.values().indexOf(col.attr) === -1) {
									foundAll = false;
								}
							});
							if (layerPrefix !== CONFIG.name.published && !foundAll) {
								CONFIG.ui.buildColumnMatchingModalWindow({
									layerName: layerName,
									columns: layerColumns,
									caller: Shorelines,
									template: Shorelines.columnMatchingTemplate,
									continueCallback: function () {
										Shorelines.addLayerToMap({
											layer: layer,
											describeFeaturetypeRespone: describeFeaturetypeRespone
										});
									}
								});
							} else {
								Shorelines.addLayerToMap({
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
							LOG.trace('Shorelines.js::addShorelines: UTM Zone Count Returned. ' + data + ' UTM zones found');
							if (data > 1) {
								CONFIG.ui.showAlert({
									message: 'Shoreline spans ' + data + ' UTM zones',
									caller: Shorelines,
									displayTime: 5000
								});
							}
							addToMap(data, textStatus, jqXHR);
						}
					],
					error: [
						function (data, textStatus, jqXHR) {
							LOG.warn('Shorelines.js::addShorelines: Could not retrieve UTM count for this resource. It is unknown whether or not this shoreline resource crosses more than 1 UTM zone. This could cause problems later.');
							addToMap(data, textStatus, jqXHR);
						}
					]
				}
			});
		});
	},
	addLayerToMap: function (args) {
		"use strict";
		var name = args.name,
			prefix = args.prefix,
			title = args.title,
			bounds = args.bounds,
			cqlFilter = 'BBOX(geom, ' + bounds + ')',
			source = args.source,
			shorelines = args.shorelines,
			dates = shorelines.map(function (s) {
				return s.date;
			}).union(),
			colorDatePairings = Util.createColorGroups(dates),
			sldBody = Shorelines.createSLDBody({
				colorDatePairings: colorDatePairings,
				groupColumn: Shorelines.groupingColumn,
				layer: {
					prefix: prefix,
					name: name
				}
			}),
			wmsLayer = new OpenLayers.Layer.WMS(
				name,
				CONFIG.ows.geoserverProxyEndpoint + prefix + '/wms', {
					layers: [name],
					transparent: true,
					sld_body: sldBody,
					format: "image/png",
					bbox: bounds,
					cql_filter: cqlFilter
				}, {
				prefix: prefix,
				zoomToWhenAdded: false,
				isBaseLayer: false,
				unsupportedBrowsers: [],
				colorGroups: colorDatePairings,
				tileOptions: {
					// http://www.faqs.org/rfcs/rfc2616.html
					// This will cause any request larger than this many characters to be a POST
					maxGetUrlLength: 2048
				},
				title: title,
				singleTile: true,
				ratio: 1,
				groupByAttribute: Shorelines.groupingColumn,
				shorelines: shorelines,
				layerType: Shorelines.stage,
				displayInLayerSwitcher: false
			}),
			loadEnd = function (e) {
				e.object.events.unregister('loadend', null, loadEnd);
				Shorelines.updateFeatureTable(e);
				Shorelines.showFeatureTable();
				Shorelines.updateSortingSelectionControl();
			};

		if (CONFIG.tempSession.getDisabledShorelines(prefix).length) {
			wmsLayer.mergeNewParams({cql_filter: cqlFilter + " AND NOT(shoreline_id IN (" + CONFIG.tempSession.getDisabledShorelines(prefix) + "))"});
		}

		Shorelines.getShorelineIdControl().layers.push(wmsLayer);
		wmsLayer.events.register("loadend", wmsLayer, loadEnd);
		CONFIG.map.getMap().addLayer(wmsLayer);
		wmsLayer.redraw(true);
		return wmsLayer;
	},
	createSLDBody: function (args) {
		"use strict";
		var sldBody,
			colorDatePairings = args.colorDatePairings,
			groupColumn = args.groupColumn,
			layer = args.layer,
			layerName = args.layerName || layer.prefix + ':' + layer.name,
			layerPrefix = layerName.substring(0, layerName.indexOf(':')),
			scaleDenominatorFunction = '<ogc:Function name="min">' +
			'<ogc:Literal>6</ogc:Literal>' +
			'<ogc:Function name="max">' +
			'<ogc:Literal>2</ogc:Literal>' +
			'<ogc:Div>' +
			'<ogc:Literal>1000000</ogc:Literal>' +
			'<ogc:Function name="env">' +
			'<ogc:Literal>wms_scale_denominator</ogc:Literal>' +
			'</ogc:Function>' +
			'</ogc:Div>' +
			'</ogc:Function>' +
			'</ogc:Function>';
		if (!isNaN(colorDatePairings[0][1])) {
			LOG.info('Shorelines.js::?: Grouping will be done by number');
			// Need to first find out about the featuretype
			var createUpperLimitFilterSet = function (colorLimitPairs) {
				var filterSet = '';
				for (var pairsIndex = 0; pairsIndex < colorLimitPairs.length; pairsIndex++) {
					filterSet += '<ogc:Literal>' + colorLimitPairs[pairsIndex][0] + '</ogc:Literal>';
					filterSet += '<ogc:Literal>' + colorLimitPairs[pairsIndex][1] + '</ogc:Literal>';
				}
				return filterSet + '<ogc:Literal>' + Util.getRandomColor({
					fromDefinedColors: true
				}).capitalize(true) + '</ogc:Literal>';
			};
			sldBody = '<?xml version="1.0" encoding="ISO-8859-1"?>' +
				'<StyledLayerDescriptor version="1.1.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">' +
				'<NamedLayer>' +
				'<Name>#[layer]</Name>' +
				'<UserStyle>' +
				'<FeatureTypeStyle>' +
				'<Rule>' +
				'<PointSymbolizer>' +
				'<Graphic>' +
				'<Mark>' +
				'<WellKnownName>circle</WellKnownName>' +
				'<Fill>' +
				'<CssParameter name="fill">#FF0000</CssParameter>' +
				'</Fill>' +
				'<Stroke>' +
				'<CssParameter name="stroke">' +
				'<ogc:Function name="Categorize">' +
				'<ogc:PropertyName>' + groupColumn.trim() + '</ogc:PropertyName> '
				+ createUpperLimitFilterSet(colorDatePairings) +
				'</ogc:Function>' +
				'</CssParameter>' +
				'<CssParameter name="stroke-opacity">1</CssParameter>' +
				'<CssParameter name="stroke-width">1</CssParameter>' +
				'</Stroke>' +
				'</Mark>' +
				'<Size>' + scaleDenominatorFunction + '</Size>' +
				'</Graphic>' +
				'</PointSymbolizer>' +
				'</Rule>' +
				'</FeatureTypeStyle>' +
				'</UserStyle>' +
				'</NamedLayer>' +
				'</StyledLayerDescriptor>';
		} else if (!isNaN(Date.parse(colorDatePairings[0][1]))) {
			LOG.debug('Shorelines.js::?: Grouping will be done by year');
			var createRuleSets;
			LOG.debug('Shorelines.js::?: Geoserver date column is actually a string');
			createRuleSets = function (colorLimitPairs, workspace) {
				var html = '';

				for (var lpIndex = 0; lpIndex < colorLimitPairs.length; lpIndex++) {
					var date = colorLimitPairs[lpIndex][1];
					var disabledDates = CONFIG.tempSession.getDisabledDates();
					if (disabledDates.indexOf(date) === -1) {
						html += '<Rule>';
						html += '<ogc:Filter>';
						html += '<ogc:PropertyIsLike escape="!" singleChar="." wildCard="*">';
						html += '<ogc:PropertyName>';
						html += groupColumn.trim();
						html += '</ogc:PropertyName>';
						html += '<ogc:Literal>';
						html += colorLimitPairs[lpIndex][1];
						html += '</ogc:Literal>';
						html += '</ogc:PropertyIsLike>';
						html += '</ogc:Filter><PointSymbolizer><Graphic><Mark><WellKnownName>circle</WellKnownName><Fill><CssParameter name="fill">#FFFFFF</CssParameter></Fill><Stroke><CssParameter name="stroke">';
						html += colorLimitPairs[lpIndex][0];
						html += '</CssParameter></Stroke></Mark><Size>' + scaleDenominatorFunction + '</Size></Graphic></PointSymbolizer>'
						html += '</Rule>';
					}
				}

				// default rule 
				html += '<Rule><ElseFilter />';
				html += '<PointSymbolizer>';
				html += '<Graphic>';
				html += '<Mark>';
				html += '<WellKnownName>circle</WellKnownName>';
				html += '<Stroke>';
				html += '<CssParameter name="stroke-opacity">0</CssParameter>';
				html += '</Stroke>';
				html += '</Mark>';
				html += '</Graphic>';
				html += '</PointSymbolizer>';
				html += '</Rule>';
				return html;
			};
			sldBody = '<?xml version="1.0" encoding="ISO-8859-1"?>' +
				'<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">' +
				'<NamedLayer>' +
				'<Name>#[layer]</Name>' +
				'<UserStyle>' +
				'<FeatureTypeStyle> ' + createRuleSets(colorDatePairings, layerPrefix) + '</FeatureTypeStyle>' +
				'</UserStyle>' +
				'</NamedLayer>' +
				'</StyledLayerDescriptor>';
		}
		sldBody = sldBody.replace('#[layer]', layerName);
		return sldBody;
	},
	zoomToLayer: function () {
		"use strict";
		LOG.info('loadend event triggered on layer');
		var bounds = new OpenLayers.Bounds();
		var layers = CONFIG.map.getMap().getLayersBy('zoomToWhenAdded', true);
		$(layers).each(function (i, layer) {
			if (layer.zoomToWhenAdded) {
				var mapLayer = CONFIG.ows.getLayerByName({
					layerNS: layer.prefix,
					layerName: layer.name
				}),
					mlBbox,
					lbbox;
				if (mapLayer) {
					mlBbox = mapLayer.bbox[CONFIG.strings.epsg4326];
					if (mlBbox) {
						lbbox = mlBbox.bbox;
						bounds.extend(new OpenLayers.Bounds(lbbox[1], lbbox[0], lbbox[3], lbbox[2])
							.transform(new OpenLayers.Projection(CONFIG.strings.epsg4326), new OpenLayers.Projection(CONFIG.strings.epsg900913)));
						if (layer.events.listeners.loadend.length) {
							layer.events.unregister('added', layer, Shorelines.zoomToLayer);
						}
					} else {
						LOG.warn('Map layer does not have EPSG:3857 or EPSG:900913 bounding box designation. Could not zoom to layer.');
					}
				}
			}
		});
		if (bounds.left && bounds.right && bounds.top && bounds.bottom) {
			CONFIG.map.getMap().zoomToExtent(bounds, false);
		}
	},
	emptyFeatureTable: function () {
		"use strict";
		Shorelines.$shorelineFeatureTableContainer.find('.table-features-column-aux').remove();
		Shorelines.$shorelineFeatureTableContainer.find('table > tbody').empty();
	},
	updateFeatureTable: function (event) {
		"use strict";
		var layer = event.object,
			shorelines = layer.shorelines,
			colorMap = Util.createColorMap(layer.colorGroups),
			$switchCandidates,
			$tbody = Shorelines.$shorelineFeatureTableContainer.find('table > tbody');
		event.object.events.unregister('loadend', null, Shorelines.updateFeatureTable);
		for (var slIdx = 0; slIdx < shorelines.length; slIdx++) {
			var shoreline = shorelines[slIdx],
				id = shoreline.id,
				date = shoreline.date,
				source = shoreline.source,
				workspace = shoreline.workspace,
				color = colorMap[date],
				isEnabled = !CONFIG.tempSession.isShorelineDisabled(layer.prefix, id),
				row = Shorelines.featureTableRowTemplate({
					id: id,
					color: color,
					date: date,
					source: source,
					workspace: workspace,
					checked: isEnabled
				});
			$tbody.append($(row));
		}

		// Allows user to click on the date field in a row and select the row
		$tbody.find('tr td:nth-child(2)').off('click', Shorelines.featureTableRowClickCallback);
		$tbody.find('tr td:nth-child(2)').on('click', Shorelines.featureTableRowClickCallback);
		$switchCandidates = $('.switch>:not(.switch-animate)').parent();
		$switchCandidates.off('switch-change', Shorelines.featureTableSwitchChangeCallback);
		$switchCandidates.on('switch-change', Shorelines.featureTableSwitchChangeCallback);
		$switchCandidates.bootstrapSwitch();
		if (layer.prefix === CONFIG.tempSession.getCurrentSessionKey()) {
			Shorelines.getAvailableAuxillaryColumns();
		}
		Shorelines.setupTableSorting();
	},
	featureTableRowClickCallback: function (e) {
		"use strict";
		var $clickedRow = $(e.target).parent(),
			$table = $clickedRow.parent(),
			$tbody = Shorelines.$shorelineFeatureTableContainer.find('table > tbody'),
			$rows = $table.find('tr'),
			shiftPressed = e.shiftKey,
			altKeyPressed = e.altKey;
		if ((!shiftPressed && !altKeyPressed) || (shiftPressed && altKeyPressed)) {
			var wasAlreadyOn = $clickedRow.hasClass(Shorelines.selectedFeatureClass);
			$tbody.find('tr').removeClass(Shorelines.selectedFeatureClass);
			if (!wasAlreadyOn) {
				$clickedRow.toggleClass(Shorelines.selectedFeatureClass);
			}
		} else if (altKeyPressed) {
			$clickedRow.toggleClass(Shorelines.selectedFeatureClass);
		} else if (shiftPressed) {
			var firstSelectedRowIndex = $rows.index($table.find('.' + Shorelines.selectedFeatureClass)),
				clickedRowIndex = $rows.index($clickedRow[0]);
			if (firstSelectedRowIndex === clickedRowIndex) {
				$clickedRow.toggleClass(Shorelines.selectedFeatureClass);
			} else {
				if (firstSelectedRowIndex > clickedRowIndex) {
					// Flip the values of the variables
					clickedRowIndex = [firstSelectedRowIndex, firstSelectedRowIndex = clickedRowIndex] [0];
				}

				for (var a = firstSelectedRowIndex; a < clickedRowIndex + 1; a++) {
					$($rows[a]).addClass(Shorelines.selectedFeatureClass);
				}
			}
		}

		e.stopImmediatePropagation();
	},
	featureTableSwitchChangeCallback: function (event, data) {
		"use strict";
		var status = data.value,
			$element = data.el,
			$multiSelRows = Shorelines.$shorelineFeatureTableContainer.find('.' + Shorelines.selectedFeatureClass),
			id = $element.closest('tr').attr('data-shoreline-id'),
			ids = Object.extended();
		ids[id] = $element.closest('tr').attr('data-shoreline-workspace');

		if ($multiSelRows.length) {
			$multiSelRows.off('switch-change', Shorelines.featureTableSwitchChangeCallback);
			$multiSelRows.find('td:nth-child(1)>div').bootstrapSwitch('setState', status);
			$multiSelRows.on('switch-change', Shorelines.featureTableSwitchChangeCallback);
			$multiSelRows.toggleClass(Shorelines.selectedFeatureClass);
			$multiSelRows.each(function (i, r) {
				var $r = $(r);
				ids[$r.attr('data-shoreline-id')] = $r.attr('data-shoreline-workspace');
			});
		}

		Object.keys(ids, function (id, workspace) {
			var $button = $('.btn-shoreline-id-toggle[data-shoreline-id="' + id + '"]'),
				buttonClass = status ? 'btn-success' : 'btn-danger',
				buttonHTML = status ? 'Disable' : 'Enable';
			if ($button) {
				$button.removeClass('btn-danger btn-success');
				$button.addClass(buttonClass);
				$button.html(buttonHTML);
			}

			if (status) {
				CONFIG.tempSession.removeDisabledShoreline(workspace, id);
			} else {
				CONFIG.tempSession.addDisabledShoreline(workspace, id);
			}
		});
		CONFIG.tempSession.persistSession();
		var layers = CONFIG.map.getMap().getLayersBy('layerType', Shorelines.stage);
		layers.forEach(function (l) {
			var sldBody = Shorelines.createSLDBody({
				colorDatePairings: l.colorGroups,
				groupColumn: l.groupByAttribute,
				layerTitle: l.title,
				layerName: l.prefix + ':' + l.name
			}),
				selectedBounds = Shorelines.aoiBoundsSelected.clone().transform(new OpenLayers.Projection(CONFIG.strings.epsg900913), new OpenLayers.Projection(CONFIG.strings.epsg4326)).toArray(false).toString(),
				cqlFilter = 'BBOX(geom, ' + selectedBounds + ')';


			if (CONFIG.tempSession.getDisabledShorelines(l.prefix).length) {
				cqlFilter += " AND NOT(shoreline_id IN (" + CONFIG.tempSession.getDisabledShorelines(l.prefix) + "))";
			}


			l.mergeNewParams({
				SLD_BODY: sldBody,
				CQL_FILTER: cqlFilter
			});
			l.redraw(true);
		});
		$("table.tablesorter").trigger('update', false);
	},
	updateSortingColumnOnServer: function (args) {
		"use strict";
		args = args || {};
		var name = args.name,
			def = args.deferred || $.Deferred(),
			url = Shorelines.shorelinesServiceEndpoint
			+ 'action=updateAuxillaryName'
			+ '&workspace=' + CONFIG.tempSession.getCurrentSessionKey()
			+ '&name=' + name;
		$.ajax(url, {
			method: 'PUT',
			context: def,
			success: function () {
				this.resolve(arguments);
			},
			error: function () {
				this.fail(arguments);
			}});
	},
	updateSortingColumnOnTable: function () {
		"use strict";
		Shorelines.$shorelineFeatureTableContainer.find('.table-features-column-aux').remove();
		$.ajax(Shorelines.shorelinesServiceEndpoint, {
			context: this,
			data: {
				action: 'getDateToAuxValues',
				workspace: CONFIG.tempSession.getCurrentSessionKey()
			},
			success: function (e, status) {
				if (status === 'success' && e.success === 'true') {
					var dateToValue = JSON.parse(e.values);
					if (Object.keys(dateToValue).length) {
						// First clear the table of any possible auxillary already showing


						// Now add the new auxillary columns
						var $theadRow = Shorelines.$shorelineFeatureTableContainer.find('thead > tr'),
							$tbody = Shorelines.$shorelineFeatureTableContainer.find('tbody'),
							$rows = $tbody.find('tr'),
							// Build the header cell
							$th = $('<th />')
							.addClass('table-features-column-aux tablesorter-header')
							.attr({
								'data-column': '4'
							}),
							$thDiv = $('<div />').addClass('tablesorter-header-inner').html(e.name);
						$th.append($thDiv);
						// Build row by row for the table body
						$theadRow.append($th);
						$rows.each(function (i, row) {
							var $row = $(row),
								year = $row.children()[1].innerHTML,
								value = dateToValue[year],
								$td = $('<td />').addClass('table-features-column-aux').html(value);
							$row.append($td);
						});
					}

					// Sorting has to be reset
					Shorelines.setupTableSorting();
				}
			}
		});
	},
	setupTableSorting: function () {
		"use strict";
		$("table.tablesorter").trigger('destroy');
		$.tablesorter.addParser({
			id: 'visibility',
			is: function (s) {
				return false;
			},
			format: function (s, table, cell, cellIndex) {
				var toggleButton = $(cell).find('.switch')[0];
				return $(toggleButton).bootstrapSwitch('status') ? 1 : 0;
			},
			type: 'numeric'
		});
		$.tablesorter.addParser({
			id: 'dateSorter',
			is: function (s) {
				return false;
			},
			format: function (s) {
				return Date.create(s).getTime();
			},
			type: 'numeric'
		});
		Shorelines.$shorelineFeatureTableContainer.find("table").tablesorter({
			headers: {
				0: {sorter: 'visibility'},
				1: {sorter: 'dateSorter'}
			}
		});
	},
	initializeUploader: function (args) {
		"use strict";
		CONFIG.ui.initializeUploader($.extend({
			caller: Shorelines
		}, args));
	},
	getShorelineIdControl: function () {
		"use strict";
		var shorelineIdControl = CONFIG.map.getControlBy('title', Shorelines.CONTROL_IDENTIFY_ID);
		if (!shorelineIdControl) {
			shorelineIdControl = new OpenLayers.Control.WMSGetFeatureInfo({
				title: Shorelines.CONTROL_IDENTIFY_ID,
				layers: [],
				queryVisible: true,
				output: 'features',
				drillDown: true,
				maxFeatures: 1000,
				infoFormat: 'application/vnd.ogc.gml',
				vendorParams: {
					radius: 3
				}
			});
			shorelineIdControl.events.register("getfeatureinfo", this, CONFIG.ui.showShorelineInfo);
			CONFIG.map.addControl(shorelineIdControl);
		}
		return shorelineIdControl;
	},
	activateShorelineIdControl: function () {
		"use strict";
		var idControl = Shorelines.getShorelineIdControl();
		
		if (idControl) {
			LOG.debug('Shorelines.js::enterStage: Shoreline identify control found in the map. Activating.');
			idControl.activate();
		} else {
			LOG.warn('Shorelines.js::enterStage: Shoreline identify control not found. Creating one, adding to map and activating it.');
			Shorelines.wmsGetFeatureInfoControl.events.register("getfeatureinfo", this, CONFIG.ui.showShorelineInfo);
			CONFIG.map.addControl(Shorelines.wmsGetFeatureInfoControl);
		}
	},
	deactivateShorelineIdControl: function () {
		"use strict";
		var idControl = Shorelines.getShorelineIdControl();
		
		if (idControl) {
			LOG.debug('Shorelines.js::enterStage: Shoreline identify control found in the map.  Deactivating.');
			idControl.deactivate();
		}
	},
	closeShorelineIdWindows: function () {
		"use strict";
		$('#FramedCloud_close').trigger('click');
	},
	bindSelectAOIButton: function () {
		"use strict";
		this.$buttonSelectAOI.on('click', function (e) {
			if ($(e.target).hasClass('active')) {
				Shorelines.deactivateSelectAOIControl();
			} else {
				Shorelines.activateSelectAOIControl();
			}
		});
	},
	toggleBindSelectAOIButton: function (toggleOn) {
		"use strict";
		var isActive = Shorelines.$buttonSelectAOI.hasClass('active');
		if (toggleOn === true && !isActive) {
			Shorelines.$buttonSelectAOI.trigger('click');
		}

		if (!toggleOn === false && isActive) {
			Shorelines.$buttonSelectAOI.trigger('click');
		}
	},
	bindSelectAOIDoneButton: function () {
		"use strict";
		this.$buttonSelectAOIDone.on('click', function () {
			var boxLayer = Shorelines.getAOISelectionLayer();
			
			if (boxLayer) {
				if (boxLayer.features && boxLayer.features.length) {
					Shorelines.aoiBoundsSelected = boxLayer.features[0].geometry.bounds;
				} else {
					Shorelines.aoiBoundsSelected = null;
				}
			}

			Shorelines.$buttonSelectAOI.trigger('click');
			if (Shorelines.aoiBoundsSelected) {
				var filterFunc = function (l) {
					if (l.name.endsWith(Shorelines.stage)) {
						var layerBounds = OpenLayers.Bounds.fromArray(l.bbox[CONFIG.strings.epsg4326].bbox, true)
							.transform(new OpenLayers.Projection(CONFIG.strings.epsg4326), new OpenLayers.Projection(CONFIG.strings.epsg900913));
						return Shorelines.aoiBoundsSelected.intersectsBounds(layerBounds);
					}
					return false;
				},
					validPublishedLayers = CONFIG.ows.wmsCapabilities.published.capability.layers.filter(filterFunc),
					validSessionLayers = CONFIG.ows.wmsCapabilities[CONFIG.tempSession.getCurrentSessionKey()].capability.layers.filter(filterFunc),
					validLayers = validPublishedLayers.concat(validSessionLayers),
					bounds = Shorelines.aoiBoundsSelected.clone(),
					boundsString = bounds.transform(new OpenLayers.Projection(CONFIG.strings.epsg900913), new OpenLayers.Projection(CONFIG.strings.epsg4326)).toArray(false).toString(),
					getShorelinesUrl = function (layer) {
						var url = Shorelines.shorelinesServiceEndpoint,
							params = {
								action: 'getShorelinesWithBbox',
								workspace: layer.prefix,
								bbox: boundsString
							};
						return url + $.param(params);
					},
					ajaxCalls = validLayers.map(function (l) {
						var context = {
							layer: l,
							bounds: boundsString
						};
						return $.ajax(getShorelinesUrl(l), {context: context}).promise();
					}),
					showNothingFoundAlert = function () {
						CONFIG.ui.showAlert({
							message: 'There is no data for the area of interest you selected<br />Try uploading data.',
							caller: Shorelines,
							displayTime: 3000,
							style: {
								classes: ['alert-warn']
							}
						});
					};
				if (validLayers.length) {
					$.when.apply(this, ajaxCalls).done(function () {
						// I should have as many incoming arguments as there were 
						// ajax calls going out (probably 2: published and workspace).
						// I need to create a dates array here 
						for (var aIdx = 0; aIdx < arguments.length; aIdx++) {
							// If there are no published shorelines, arguments will be 
							// an array of a single ajax response (data, status, jqXHR).
							// But if there are published shorelines, arguments will be 
							// an array of ajax responses. 
							// 
							// TODO- Either be more clever about this or realize
							// that not having published shorelines is an edge case
							// in the infancy of the application and move on with life
							var response = Array.isArray(arguments[0]) ? arguments[aIdx][0] : arguments[0],
								shorelines = JSON.parse(response.shorelines),
								layerInfo = Array.isArray(this) ? this[aIdx].layer : this.layer,
								bounds = Array.isArray(this) ? this[aIdx].bounds : this.bounds;

							// For each argument I want to read the response into a features array
							// I check if this layer already exists in the map because 
							// the for-loop I am in will loop here three times if 
							// there are no published shorelines because of the 
							// reasons mentioned in previous comment
							if (shorelines.length > 0 && CONFIG.map.getMap().getLayersByName(layerInfo.name).length === 0) {
								var wmsLayer = Shorelines.addLayerToMap({
									name: layerInfo.name,
									prefix: layerInfo.prefix,
									title: layerInfo.title,
									bounds: bounds,
									shorelines: shorelines
								});
								Shorelines.setupTableSorting();
							}
						}
					});
					CONFIG.map.getMap().zoomToExtent(Shorelines.aoiBoundsSelected.clone(), true);
				} else {
					showNothingFoundAlert();
				}
			} else {
				CONFIG.ui.showAlert({
					message: 'You have not selected an area of interest',
					caller: Shorelines,
					displayTime: 3000,
					style: {
						classes: ['alert-warn']
					}
				});
			}
		});
	},
	activateSelectAOIControl: function () {
		"use strict";
		this.$descriptionAOI.removeClass('hidden');
		var drawBoxLayer = new OpenLayers.Layer.Vector(Shorelines.LAYER_AOI_NAME),
			aoiIdControl = new OpenLayers.Control.DrawFeature(drawBoxLayer,
				OpenLayers.Handler.RegularPolygon, {
					title: Shorelines.CONTROL_IDENTIFY_AOI_ID,
					handlerOptions: {
						sides: 4,
						irregular: true
					}
				});
				
		// I really only want one box on a layer at any given time
		drawBoxLayer.events.register('beforefeatureadded', null, function (e) {
			e.object.removeAllFeatures();
		});
		
		Shorelines.hideFeatureTable(true);
		Shorelines.removeShorelineLayers();
		
		CONFIG.map.getMap().addLayers([drawBoxLayer]);
		CONFIG.map.getMap().addControl(aoiIdControl);
		
		aoiIdControl.activate();
	},
	deactivateSelectAOIControl: function () {
		"use strict";
		this.$descriptionAOI.addClass('hidden');
		var shorelineIdAOIControl = CONFIG.map.getControlBy('title', Shorelines.CONTROL_IDENTIFY_AOI_ID),
			layer;
		if (shorelineIdAOIControl) {
			layer = shorelineIdAOIControl.layer;
			CONFIG.map.removeLayer(layer, false);
			Shorelines.hideFeatureTable(true);
			shorelineIdAOIControl.destroy();
		}
	},
	getAOISelectionLayer: function () {
		"use strict";
		var results = CONFIG.map.getMap().getLayersBy('name', Shorelines.LAYER_AOI_NAME);
		if (results.length) {
			return results[0];
		}
		return null;
	},
	showFeatureTable: function () {
		"use strict";
		Shorelines.$shorelineFeatureTableContainer.removeClass('hidden');
	},
	hideFeatureTable: function (clear) {
		"use strict";
		Shorelines.$shorelineFeatureTableContainer.addClass('hidden');
		if (clear) {
			Shorelines.$shorelineFeatureTableContainer.find('tbody').empty();
		}
	},
	removeShorelineLayers: function () {
		"use strict";
		var layers = CONFIG.map.getMap().getLayersBy('layerType', Shorelines.stage);
		layers.forEach(function (layer) {
			CONFIG.map.getMap().removeLayer(layer);
		});
	},
	getActive: function () {
		"use strict";
		return $('#shorelines-list').children(':selected').map(function (i, v) {
			return v.value;
		}).toArray();
	},
	uploadCallbacks: {
		onComplete: function (id, fileName, responseJSON) {
			"use strict";
			
			CONFIG.ui.hideSpinner();
			
			$('#application-alert').alert('close');
			
			if (responseJSON.success === 'true') {
				var token = responseJSON.token;
				Shorelines.getShorelineHeaderColumnNames({
					token: token,
					callbacks: {
						success: function (data) {
							var success = data.success,
								headers = data.headers,
								layerColumns = Object.extended(),
								foundAllRequiredColumns = true;
							
							if (success === 'true') {
								headers = headers.split(',');
								if (headers.length < Shorelines.mandatoryColumns.length) {
									LOG.warn('Shorelines.js::addShorelines: There ' + 
										'are not enough attributes in the selected ' +
										'shapefile to constitute a valid shoreline. ' +
										'Will be deleted. Needed: ' +
										Shorelines.mandatoryColumns.length +
										', Found in upload: ' + headers.length);
									
									Shorelines.removeResource();
									
									CONFIG.ui.showAlert({
										message: 'Not enough attributes in upload - Check Logs',
										caller: Shorelines,
										displayTime: 7000,
										style: {
											classes: ['alert-error']
										}
									});
								} else {
									// User needs to match columns to attributes
									for (var hIdx = 0; hIdx < headers.length; hIdx++) {
										layerColumns[headers[hIdx]] = '';
									}
									
									layerColumns = Util.createLayerUnionAttributeMap({
										caller: Shorelines,
										layerColumns: layerColumns
									});
									
									Shorelines.mandatoryColumns.each(function (mc) {
										if (layerColumns.values().indexOf(mc) === -1) {
											foundAllRequiredColumns = false;
										}
									});
									
									Shorelines.defaultingColumns.each(function (col) {
										if (layerColumns.values().indexOf(col.attr) === -1) {
											foundAllRequiredColumns = false;
										}
									});
									
									var doUpload = function () {
										$.ajax(Shorelines.uploadRequest.endpoint, {
											type: 'POST',
											data: {
												action: 'import',
												token: token,
												workspace: CONFIG.tempSession.session.id,
												columns: JSON.stringify(layerColumns)
											},
											success: function (data) {
												var layerName = data.layer,
													workspace = CONFIG.tempSession.session.id;
												CONFIG.ows.getWMSCapabilities({
													namespace: workspace,
													layerName: layerName,
													callbacks: {
														success: [
															function (args) {
																CONFIG.ui.showAlert({
																	message: 'Upload Successful',
																	caller: Shorelines,
																	displayTime: 3000,
																	style: {
																		classes: ['alert-success']
																	}
																});
																CONFIG.tempSession.updateLayersFromWMS(args);
																CONFIG.ui.populateFeaturesList({
																	caller: Shorelines
																});
																$('a[href="#shorelines-view-tab"]').tab('show');
																$('#shorelines-list').val(workspace + ':' + layerName).trigger('change');
															}
														]
													}
												});
											},
											error: function () {
												Shorelines.removeResource();
												CONFIG.ui.showAlert({
													message: 'There was an error performing a shoreline import - Check Logs',
													caller: Shorelines,
													displayTime: 7000,
													style: {
														classes: ['alert-error']
													}
												});
											}
										});
									};
									
									if (!foundAllRequiredColumns) {
										CONFIG.ui.buildColumnMatchingModalWindow({
											layerName: token,
											columns: layerColumns,
											caller: Shorelines,
											template: Shorelines.columnMatchingTemplate,
											continueCallback: function () {
												doUpload();
											},
											updateCallback: function () {
												doUpload();
											},
											cancelCallback: function () {
												// Call to delete the stored file
												// on the back-end via its token
												$.ajax(Shorelines.uploadRequest.endpoint + '?' + $.param({action: "delete-token", token: token}), {
													type: 'DELETE'
												});
											}
										});
									} else {
										// Ready to add to map
										doUpload();
									}
								}
							}
						},
						error: function () {
							Shorelines.removeResource();
							CONFIG.ui.showAlert({
								message: 'There was an error performing a shoreline upload - Check Logs',
								caller: Shorelines,
								displayTime: 7000,
								style: {
									classes: ['alert-error']
								}
							});
						}
					}
				});
			} else {
				var exception = responseJSON.exception;
				LOG.warn('UI.js::Uploader Error Callback: Import incomplete.');
				Shorelines.removeResource();
				CONFIG.ui.showAlert({
					message: 'Import incomplete. ' + (exception ? exception : ''),
					caller: Shorelines,
					displayTime: 3000,
					style: {
						classes: ['alert-error']
					}
				});
			}
		}
	},
	getShorelineHeaderColumnNames: function (args) {
		"use strict";
		args = args || {};
		var token = args.token,
			callbacks = args.callbacks || {
				success: function () {
				},
				error: function () {
				}
			};
		$.ajax(Shorelines.uploadRequest.endpoint, {
			'data': {
				'action': 'read-dbf',
				'token': token
			},
			success: callbacks.success,
			error: callbacks.error
		});
	}
};
