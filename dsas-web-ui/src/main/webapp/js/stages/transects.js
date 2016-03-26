/*global CONFIG */
/*global OpenLayers */
/*global LOG*/
/*global Handlebars */
/*global Baseline */
/*global Calculation */
/*global Results */
/*global Shorelines */
/*global ProxyDatumBias */
"use strict";
var Transects = {
	stage: 'transects',
	suffixes: ['_lt', '_st', '_transects'],
	reservedColor: '#D95F02',
	createTransectsAndIntersectionsWPSTemplate: undefined,
	DEFAULT_SPACING: 50,
	DEFAULT_SMOOTHING: 2500.0,
	NAME_CONTROL_SNAP: 'snap-control',
	NAME_CONTROL_EDIT: 'transects-edit-control',
	NAME_CONTROL_HIGHLIGHT: 'transects-highlight-control',
	NAME_CONTROL_SELECT: 'transects-select-control',
	NAME_CONTROL_CROP: 'transects-crop-control',
	NAME_LAYER_ANGLE: 'transects-angle-layer',
	NAME_LAYER_EDIT: 'transects-edit-layer',
	$buttonDownload: $('#transects-downloadbutton'),
	$buttonToggleEdit: $('#transect-edit-form-toggle'),
	$buttonTransectsCreateToggle: $('#create-transects-toggle'),
	$buttonTransectsCreateInput: $('#create-transects-input-button'),
	$buttonTransectsCreate: $('#create-transects-button'),
	$buttonTransectsAddButton: $('#transects-edit-add-button'),
	$buttonTransectsCropButton: $('#transects-edit-crop-button'),
	$buttonTransectsSave: $('#transects-edit-save-button'),
	$containerTransectsEdit: $("#transects-edit-container"),
	$transectListbox: $("#transects-list"),
	description: {
		'stage': '<p>Transects are cast perpendicular to the workspace baseline, at user-defined intervals.<br /> The intersections between the transects and shorelines are used to calculate erosion and deposition rates.</p><p>Add a pre-cast group of transects to your workspace with the selection box above or upload your own zipped shapefile containing a set of transects with the Manage tab. </p><p>Alternatively, create or modify transects with the editing and transect casting tools that are located within the Manage tab.</p><hr />Select existing transects, or generate new transects from the workspace baseline. Transects are rays that are projected from the baseline, and the intersections between shorelines and transects are used to calculate rates of erosion and deposition.',
		'view-tab': 'Select a published collection of shorelines to add to the workspace.',
		'manage-tab': 'Upload a new collection of transects to the workspace, generate new transects, or edit existing transects.',
		'upload-button': 'Upload a zipped shapefile which contains a collection of transects.',
		'calculate-button': 'Choose transect spacing and generate a new transects layer from the workspace baseline.'
	},
	appInit: function () {
		Transects.$buttonToggleEdit.on('click', Transects.editButtonToggled);
		Transects.$buttonTransectsCreateToggle.on('click', Transects.createTransectsButtonToggled);
		Transects.$buttonTransectsCreateInput.on('click', Transects.createTransectSubmit);
		Transects.$buttonTransectsAddButton.on('click', Transects.addTransectButtonToggled);
		Transects.$buttonTransectsCropButton.on('click', Transects.cropTransectsButtonToggled);
		Transects.$buttonTransectsCreate.popover({
			title: Transects.stage.capitalize() + ' Generate',
			content: $('<div />')
				.append($('<div />').html(Transects.description['calculate-button']))
				.html(),
			html: true,
			placement: 'bottom',
			trigger: 'hover',
			delay: {
				show: CONFIG.popupHoverDelay
			}
		});
		Transects.initializeUploader();
		CONFIG.map.addControl(new OpenLayers.Control.SelectFeature([], {
			title: Transects.NAME_CONTROL_HIGHLIGHT,
			autoActivate: false,
			hover: true,
			highlightOnly: true
		}));
		CONFIG.map.addControl(new OpenLayers.Control.SelectFeature([], {
			title: Transects.NAME_CONTROL_SELECT,
			autoActivate: false,
			box: false,
			onSelect: function (feature) {
				LOG.debug('Transects.js::SelectFeature.onSelect(): A feature was selected');
				var modifyControl = CONFIG.map.getMap().getControlsBy('id', Transects.NAME_CONTROL_EDIT)[0];
				modifyControl.selectFeature(feature);
				var selectedFeature = modifyControl.feature.clone();
				var angleGeometry1 = selectedFeature.clone().geometry.components[0].resize(100, selectedFeature.geometry.components[0].getCentroid(), 1);
				var angleGeometry2 = selectedFeature.clone().geometry.components[0].resize(-100, selectedFeature.geometry.components[0].getCentroid(), 1);
				var angleLayer = new OpenLayers.Layer.Vector(Transects.NAME_LAYER_ANGLE, {
					renderers: CONFIG.map.getRenderer(),
					type: 'angle-guide',
					style: {
						strokeColor: '#A1A1A1',
						strokeOpacity: 0.25
					},
					displayInLayerSwitcher: false
				});
				selectedFeature.geometry.addComponents([angleGeometry1, angleGeometry2]);
				angleLayer.addFeatures([selectedFeature]);
				angleLayer.type = Transects.stage;
				CONFIG.map.getMap().addLayer(angleLayer);
				var snapControl = CONFIG.map.getMap().getControlsBy('id', Transects.NAME_CONTROL_SNAP)[0];
				snapControl.addTargetLayer(angleLayer);
			},
			onUnselect: function (feature) {
				LOG.debug('Transects.js::SelectFeature.onSelect(): A feature was unselected');
				var modifyControl = CONFIG.map.getMap().getControlsBy('id', Transects.NAME_CONTROL_EDIT)[0];
				Transects.removeAngleLayer();
				modifyControl.unselectFeature(feature);
			}
		}));
		Transects.$buttonDownload.on('click', function () {
			CONFIG.ows.downloadLayerAsShapefile(Transects.$transectListbox.val());
		});
		$.get('templates/wps-request-createtransectsandintersections.mustache').done(function (data) {
			Transects.createTransectsAndIntersectionsWPSTemplate = Handlebars.compile(data);
			Handlebars.registerHelper('printDefaultValue', function () {
				if (this.defaultValue) {
					return new Handlebars.SafeString(' Default: "' + this.defaultValue + '"');
				} else {
					return '';
				}
			});
		});
	},
	enterStage: function () {
		LOG.debug('Transects.js::enterStage');
		CONFIG.ui.switchTab({
			stage: Transects.stage,
			tab: 'view'
		});
		if ($('#shorelines-list').val() && Baseline.getActive()) {
			Transects.enableUploadButton();
		} else {
			Transects.disableUploadButton();
		}
	},
	leaveStage: function () {
		LOG.debug('Transects.js::leaveStage');
		if (Transects.$buttonToggleEdit.hasClass('active')) {
			Transects.$buttonToggleEdit.trigger('click');
		}
		Transects.removeEditControl();
		Transects.removeSnapControl();
		Transects.removeDrawControl();
		Transects.deactivateSelectControl();
		Transects.deactivateHighlightControl();
		Transects.removeAngleLayer();
	},
	activateEditing : function () {
		LOG.debug('Transects.js::editButtonToggled: Edit form was toggled on');
		Transects.disableUpdateTransectsButton();
		if (Transects.$buttonTransectsCreateToggle.hasClass('active')) {
			Transects.$buttonTransectsCreateToggle.trigger('click');
		}

		LOG.debug('Transects.js::editButtonToggled: Adding cloned layer to map');
		var clonedLayer = Transects.cloneActiveLayer();
		CONFIG.map.getMap().addLayer(clonedLayer);
		LOG.debug('Transects.js::editButtonToggled: Create baseline snap control');
		var baselineSnapControl = Transects.createBaselineSnapControl(clonedLayer);
		CONFIG.map.getMap().addControl(baselineSnapControl);
		LOG.debug('Transects.js::editButtonToggled: Adding highlight control to map');
		var highlightControl = Transects.getHighlightControl();
		highlightControl.setLayer([clonedLayer]);
		highlightControl.activate();
		LOG.debug('Transects.js::editButtonToggled: Adding select control to map');
		var selectControl = Transects.getSelectControl();
		selectControl.setLayer([clonedLayer]);
		selectControl.activate();
		LOG.debug('Transects.js::editButtonToggled: Adding modify control to map');
		var modifyFeatureControl = Transects.createModifyFeatureControl(clonedLayer);
		CONFIG.map.getMap().addControl(modifyFeatureControl);
		modifyFeatureControl.activate();
		modifyFeatureControl.handlers.keyboard.activate();
		Transects.$containerTransectsEdit.removeClass('hidden');
		Transects.$buttonTransectsSave.unbind('click', Transects.saveEditedLayer);
		Transects.$buttonTransectsSave.on('click', Transects.saveEditedLayer);
	},
	deactivateEditing : function () {
		LOG.debug('Transects.js::editButtonToggled: Edit form was toggled off');
		Transects.$containerTransectsEdit.addClass('hidden');
		Transects.removeEditControl();
		Transects.removeSnapControl();
		Transects.removeDrawControl();
		Transects.removeAngleLayer();
		Transects.deactivateSelectControl();
		Transects.deactivateHighlightControl();
		if (Transects.getActiveLayer()) {
			Transects.getActiveLayer().refresh({force: true});
		}
	},
	editButtonToggled: function (event) {
		LOG.info('Transects.js::editButtonToggled');
		var toggledOn = $(event.currentTarget).hasClass('active') ? false : true;
		if (toggledOn) {
			Transects.activateEditing();
		} else {
			Transects.deactivateEditing();
		}
	},
	cloneActiveLayer: function () {
		var clonedOriginalLayer = Transects.getActiveLayer();
		var oLayerName = clonedOriginalLayer.name;
		var oLayerPrefix = oLayerName.split(':')[0];
		var oLayerTitle = oLayerName.split(':')[1];
		var clonedLayer = new OpenLayers.Layer.Vector(Transects.NAME_LAYER_EDIT, {
			strategies: [new OpenLayers.Strategy.Fixed(), new OpenLayers.Strategy.Save()],
			protocol: new OpenLayers.Protocol.WFS({
				version: "1.1.0",
				url: "geoserver/" + oLayerPrefix + "/wfs",
				featureType: oLayerTitle,
				featureNS: CONFIG.namespace[oLayerPrefix],
				geometryName: "the_geom",
				schema: "geoserver/" + oLayerPrefix + "/wfs/DescribeFeatureType?version=1.1.0&outputFormat=GML2&typename=" + oLayerName,
				srsName: CONFIG.map.getMap().getProjection()
			}),
			cloneOf: oLayerName,
			renderers: CONFIG.map.getRenderer(),
			displayInLayerSwitcher: false
		});
		clonedLayer.addFeatures(clonedOriginalLayer.features.clone());
		clonedLayer.styleMap.styles['default'].defaultStyle.strokeWidth = 4;
		clonedLayer.type = Transects.stage;
		return clonedLayer;
	},
	createModifyFeatureControl: function (cloneLayer) {
		var mfControl = new OpenLayers.Control.ModifyFeature(
			cloneLayer,
			{
				id: Transects.NAME_CONTROL_EDIT,
				deleteCodes: [8, 46, 48, 68],
				standalone: true,
				createVertices: false,
				onModification: function (feature) {
					var baseLayer = Baseline.getActiveLayer();
					var baseLayerFeatures = baseLayer.features;
					var vertices = feature.geometry.components[0].components;
					var connectedToBaseline = false;
					Transects.enableUpdateTransectsButton();
					baseLayerFeatures.each(function (f) {
						var g = f.geometry;
						vertices.each(function (vertex) {
							if (parseInt(g.distanceTo(vertex), 10) <= 5) {
								connectedToBaseline = true;
							}
						});
					});
					if (connectedToBaseline) {
						feature.state = OpenLayers.State.UPDATE;
						feature.style = {
							strokeColor: '#0000FF'
						};
					} else {
						feature.state = OpenLayers.State.DELETE;
						feature.style = {
							strokeColor: '#D3D3D3'
						};
					}
					feature.layer.redraw();
				},
				handleKeypress: function (evt) {
					var code = evt.keyCode;
					if (this.feature && OpenLayers.Util.indexOf(this.deleteCodes, code) !== -1) {
						var fid = this.feature.fid;
						var originalLayer = Transects.getActiveLayer();
						var cloneLayer = Transects.getEditLayer();
						var originalFeature = originalLayer.getFeatureBy('fid', fid);
						var cloneFeature = cloneLayer.getFeatureBy('fid', fid);
						cloneFeature.state = OpenLayers.State.DELETE;
						cloneFeature.style = {
							strokeColor: '#D3D3D3'
						};
						originalFeature.style = {
							strokeOpacity: 0
						};
						originalFeature.layer.redraw();
						cloneFeature.layer.redraw();
						Transects.enableUpdateTransectsButton();
					}
				}
			});
		return mfControl;
	},
	getActiveLayer: function () {
		return CONFIG.map.getMap().getLayersByName(Transects.getActive())[0];
	},
	getEditLayer: function () {
		return CONFIG.map.getMap().getLayersByName(Transects.NAME_LAYER_EDIT)[0];
	},
	removeEditLayer: function () {
		var editLayer = CONFIG.map.getMap().getLayersByName(Transects.NAME_LAYER_EDIT);
		if (editLayer.length) {
			$.each(editLayer, function (i, l) {
				CONFIG.map.removeLayer(l);
			});
		}
	},
	createBaselineSnapControl: function (cloneLayer) {
		var baselineLayer = Baseline.getActiveLayer();
		var snapControl = new OpenLayers.Control.Snapping({
			id: Transects.NAME_CONTROL_SNAP,
			layer: cloneLayer,
			targets: [baselineLayer],
			greedy: true,
			tolerance: 1
		});
		snapControl.activate();
		return snapControl;
	},
	cropTransectsButtonToggled: function () {
		if (!$(this).hasClass('active')) {
			Transects.removeDrawControl();
			var cloneLayer = Transects.getEditLayer(),
				saveStrategy = cloneLayer.strategies.find(function (s) {
					return s.CLASS_NAME === 'OpenLayers.Strategy.Save';
				});
			Transects.deactivateSelectControl();
			// This is a horrible hack but Geoserver currently has a bug 
			// ( https://jira.codehaus.org/browse/GEOS-6367 ) which prevents us
			// from updating more than one transect in one call. Because of this,
			// I have to call WFS-T update once per transect in recursive fashion

			// This flag tells the success callback that the save call was to
			// update. The success callback branches on whether this flag is set and
			// if there are more transects left in the array
			saveStrategy.updating = true;
			// Memoize the original save function because I'll be updating the 
			// actual save function. Only do this if innerSave is not already defined.
			// This may already be defined if the user clicks on "Crop" a second
			// time without having performed an update on the transects. This causes
			// a recursion issue.
			if (!saveStrategy.hasOwnProperty('innerSave')) {
				saveStrategy.innerSave = saveStrategy.save;
			}
			
			saveStrategy.save = function () {
				// Set up a strategy-object scope array of transects that need to
				// be updated via WFS. Do this the first time save() is called. I
				// will be picking off from the top of the array in every iteration
				// of this function call until there's nothing left.
				if (!this.updateFeatures) {
					this.updateFeatures = this.layer.features.filter(function (f) {
						return f.state === 'Update';
					});
				}

				// Grab the next transect off the top of the stack
				var nextTransect = this.updateFeatures.shift();
				// Set a flag telling the success callback whether I'm done or not
				this.hasMoreFeatures = this.updateFeatures.length > 0;
				// Call the original save function
				this.innerSave([nextTransect]);
			};
			var cropTransectsControl = new OpenLayers.Control.Split({
				layer: cloneLayer,
				mutual: true,
				deferDelete: true,
				eventListeners: {
					split: function (event) {
						var splitTransects = event.features,
							originalTransect = event.original,
							baselineFeatureGeometry = Baseline.getActiveLayer().features[0].geometry,
							// This function will reorder the split transects based
							// on the closest to the baseline in ascending distance 
							// order
							sortedSplitTransects = splitTransects.sortBy(function (t) {
								baselineFeatureGeometry.distanceTo(t.geometry);
							}),
							// Grab the transect that touches the baseline, pulling
							// it out of the array
							baselineConnectedTransect = sortedSplitTransects.shift(),
							tIdx, transect;
						// Tag the selected transect for use in the afterSplit event
						baselineConnectedTransect.fid = originalTransect.fid;
						baselineConnectedTransect.style = {
							strokeColor: '#00FF00'
						};
						// For the transect piece that will be cut off, change the 
						// stroke color to red
						for (tIdx = 0; tIdx < sortedSplitTransects.length; tIdx++) {
							transect = sortedSplitTransects[tIdx];
							transect.style = {
								strokeColor: '#D3D3D3'
							};
						}

						originalTransect.layer.redraw();
						event.object.layer.redraw();
					},
					aftersplit: function (event) {
						var cloneLayer = event.object.layer,
							// The slice control sets affected transects to the
							// delete state. 
							affectedTransects = cloneLayer.features.filter(function (t) {
								return t.state === 'Delete';
							}),
							// The slice control also wants to add the split
							// features to the layer
							splitTransects = cloneLayer.features.filter(function (t) {
								return t.state === 'Insert';
							}),
							tIdx,
							transect,
							croppedTransect;
						// For each of the affected transects, change their state 
						// and set the geometry to the cropped transect geometry
						// created by the split. 
						for (tIdx = 0; tIdx < affectedTransects.length; tIdx++) {
							transect = affectedTransects[tIdx];
							croppedTransect = splitTransects.find(function (t) {
								return transect.fid === t.fid;
							});
							transect.state = OpenLayers.State.UPDATE;
							transect.renderIntent = 'default';
							transect.geometry = croppedTransect.geometry;
							transect.bounds = croppedTransect.geometry.bounds;
						}

						// Update each of the split transects that the split control
						// added to the clone layer to set the state to null
						// so they don't get updated in the WFS-T update when
						// the layer is saved 
						$.each(splitTransects, function (i, t) {
							t.state = null;
						});
						cloneLayer.redraw();
						Transects.$buttonTransectsCropButton.click();
						Transects.enableUpdateTransectsButton();
						Transects.removeCropControl();
					}
				}
			});
			cropTransectsControl.id = Transects.NAME_CONTROL_CROP;
			CONFIG.map.addControl(cropTransectsControl);
			cropTransectsControl.activate();
		} else {
			Transects.removeCropControl();
		}
	},
	addTransectButtonToggled: function () {
		if (!$(this).hasClass('active')) {
			Transects.removeCropControl();
			var cloneLayer = Transects.getEditLayer();
			Transects.deactivateSelectControl();
			var drawControl = new OpenLayers.Control.DrawFeature(
				cloneLayer,
				OpenLayers.Handler.Path,
				{
					id: 'transects-draw-control',
					multi: true,
					handlerOptions: {
						maxVertices: 2,
						dblclick: function () {
							// We do not want to begin drawing another transect
							// on click. Therefore, when a double click does occur,
							// destroy the point the first click made and get out
							// of draw mode
							this.destroyFeature(true);
							return false;
						}
					},
					featureAdded: function (addedFeature) {
						LOG.debug('Transects.js::featureAdded: A new transect has been added');
						var baseline = Baseline.getActiveLayer();
						var editLayer = Transects.getEditLayer();
						LOG.trace('Transects.js::featureAdded: Trying to figure out how many transects we have at this point');
						var editFeatureCount = editLayer.features.length;
						editLayer.highestFid = editLayer.highestFid ? editLayer.highestFid + 1 : editFeatureCount + 1;
						LOG.trace('Transects.js::featureAdded: The transect being added is # ' + editLayer.highestFid);
						LOG.trace('Transects.js::featureAdded: Trying to find the baseline segment that the new transect is touching');
						var baselineFeature = baseline.features.find(
							function (baselineFeature) {
								return baselineFeature.geometry.distanceTo(addedFeature.geometry) === 0;
							}
						);
						addedFeature.attributes.TransectID = parseInt(editLayer.highestFid);
						if (baselineFeature) {
							LOG.trace('Transects.js::featureAdded: It looks like the new transect added does touch a baseline section');
							LOG.trace('Transects.js::featureAdded: Grab info from the baseline feature to add to the transect');
							addedFeature.attributes.Orient = baselineFeature.attributes.Orient;
							addedFeature.attributes.BaselineID = baselineFeature.fid;
							Transects.enableUpdateTransectsButton();
							LOG.trace('Transects.js::featureAdded: Between the two points on the transect, figure out which point touches the baseline');
							var transectPoint0 = addedFeature.geometry.components[0].components[0];
							var transectPoint1 = addedFeature.geometry.components[0].components[1];
							var blDist0 = baselineFeature.geometry.components[0].distanceTo(transectPoint0);
							var blDist1 = baselineFeature.geometry.components[0].distanceTo(transectPoint1);
							if (blDist0 > blDist1) {
								addedFeature.geometry.components[0].components.reverse();
							}
							var workingPoint = addedFeature.geometry.components[0].components[0];
							LOG.trace('Transects.js::featureAdded: The WFS getcaps response holds the native SRS proj of the transects layer, which is needed');
							var workspaceNS = CONFIG.ows.featureTypeDescription[baseline.name.split(':')[0]][baseline.name.split(':')[1]].targetNamespace;
							var transectLayerInfo = CONFIG.ows.wfsCapabilities.featureTypeList.featureTypes.find(function (ft) {
								return ft.featureNS === workspaceNS && ft.name === editLayer.cloneOf.split(':')[1];
							});
							var transectSRID = transectLayerInfo.srs.from(transectLayerInfo.srs.lastIndexOf(':') + 1);
							var baselineSRID = baseline.projection.projCode.split(':')[1];
							LOG.trace('Transects.js::featureAdded: We need to move the point connected on the baseline to a baseline in the UTM projection');
							CONFIG.ows.projectPointOnLine({
								'workspaceNS': workspaceNS,
								'layer': baseline.name,
								'point': 'SRID=' + baselineSRID + ';POINT(' + workingPoint.x + ' ' + workingPoint.y + ')',
								'transectSRID': transectSRID,
								callbacks: {
									'success': [function (data) {
											// If the data is in string format, this means the process succeeded
											// TODO - Handle when the process did not succeed
											if (typeof data === 'string') {
												var slicedData = data.from(data.indexOf('(') + 1);
												var newPoint = slicedData.substring(0, slicedData.length - 1).split(' ');
												workingPoint.x = newPoint[0];
												workingPoint.y = newPoint[1];
												workingPoint.clearBounds();
											}
										}]
								}
							});
						} else {
							// The baseline was not hit. This feature needs 
							// to be removed from the features array
							addedFeature.destroy();
							CONFIG.ui.showAlert({
								message: 'Intersection did not touch baseline',
								displayTime: 0,
								caller: Transects
							});
						}

					}
				});
			CONFIG.map.addControl(drawControl);
			drawControl.activate();
		} else {
			Transects.removeDrawControl();
		}
	},
	saveEditedLayer: function () {
		LOG.debug('Baseline.js::saveEditedLayer: Edit layer save button clicked');
		var layer = Transects.getEditLayer(),
			intersectsLayer = layer.cloneOf.replace('transects', 'intersects'),
			resultsLayer = layer.cloneOf.replace('transects', 'rates'),
			updatedFeatures = layer.features.filter(function (f) {
				return f.state;
			})
			.map(function (f) {
				return f.attributes.TransectID;
			});
		// This will be a callback from WPS
		var editCleanup = function (data) {
			LOG.debug('Transects.js::saveEditedLayer: Receieved response from updateTransectsAndIntersections WPS');
			Calculation.clear();
			Results.clear();
			$.each([Transects.$buttonTransectsAddButton, Transects.$buttonTransectsCropButton], function (i, $button) {
				if ($button.hasClass('active')) {
					$button.trigger('click');
				}
			});
			var intersectionsList = CONFIG.ui.populateFeaturesList({
				caller: Calculation,
				stage: 'intersections'
			});
			var resultsList = CONFIG.ui.populateFeaturesList({
				caller: Results
			});
			if ($(data).find('ows\\:ExceptionReport').length) {
				LOG.debug('Transects.js::saveEditedLayer: UpdateTransectsAndIntersections WPS failed. Removing Intersections layer');
				CONFIG.ui.showAlert({
					message: 'Automatic intersection generation failed.',
					displayTime: 0,
					caller: Transects
				});
				Transects.refreshFeatureList({
					selectLayer: layer.cloneOf
				});
				CONFIG.map.removeLayerByName(layer.cloneOf);
				intersectionsList.val('');
				resultsList.val('');
				Transects.$buttonToggleEdit.trigger('click');
				Results.clear();
			} else {
				LOG.debug('Transects.js::saveEditedLayer: Removing associated results layer');
				Transects.$buttonToggleEdit.trigger('click');
				var workspace = CONFIG.tempSession.getCurrentSessionKey();
				$.ajax('service/layer/workspace/' + workspace + '/store/ch-output/' + resultsLayer.split(':')[1],
						{
							type: 'DELETE',
							context: this
						})
						.done(function (data, textStatus, jqXHR) {
							CONFIG.ows.getWMSCapabilities({
								namespace: CONFIG.tempSession.getCurrentSessionKey(),
								callbacks: {
									success: [
										CONFIG.tempSession.updateLayersFromWMS,
										function () {
											Transects.refreshFeatureList({
												selectLayer: layer.cloneOf
											});
											CONFIG.map.removeLayerByName(layer.cloneOf);
											if (Transects.$buttonToggleEdit.hasClass('active')) {
												Transects.$buttonToggleEdit.trigger('click');
											}
											CONFIG.map.removeLayerByName(layer.cloneOf);
											intersectionsList.val(intersectsLayer);
											resultsList.val(resultsLayer);
											LOG.debug('Transects.js::saveEditedLayer: WMS Capabilities retrieved for your session');
											Results.clear();
										}
									],
									error: [function () {
											LOG.warn('Transects.js::saveEditedLayer: There was an error in retrieving the WMS capabilities for your session. This is probably be due to a new session. Subsequent loads should not see this error');
										}]
								}
							});
						});
			}

			Transects.enableUpdateTransectsButton();
			intersectionsList.trigger('change');
			resultsList.trigger('change');
		};
		var saveStrategy = layer.strategies.find(function (n) {
			return n.CLASS_NAME === 'OpenLayers.Strategy.Save';
		});
		saveStrategy.events.remove('success');
		saveStrategy.events.register('success', null, function () {
			// If I'm coming here from a transect crop call, these flags will be
			// set. If there are more transects to crop, recurse back into the 
			// save function (This is due to a bug in Geoserver).
			if (this.updating && this.hasMoreFeatures) {
				this.save();
			} else {
				LOG.debug('Baseline.js::saveEditedLayer: Transects layer was updated on OWS server. Refreshing layer list');
				LOG.debug('Transects.js::saveEditedLayer: Removing associated intersections layer');
				LOG.debug('Transects.js::saveEditedLayer: Calling updateTransectsAndIntersections WPS');
				CONFIG.ows.updateTransectsAndIntersections({
					shorelines: Shorelines.getActive(),
					baseline: Baseline.getActive(),
					transects: Transects.getActive(),
					intersections: Calculation.getActive(),
					transectId: updatedFeatures,
					farthest: $('#create-intersections-nearestfarthest-list').val(),
					callbacks: {
						success: [
							function (data, textStatus, jqXHR) {
								editCleanup(data, textStatus, jqXHR);
							}
						],
						error: [
							function (data, textStatus, jqXHR) {
								editCleanup(data, textStatus, jqXHR);
							}
						]
					}
				});
			}
		});
		saveStrategy.save();
	},
	refreshFeatureList: function (args) {
		LOG.info('Transects.js::refreshFeatureList: Will cause WMS GetCapabilities call to refresh current feature list');
		var selectLayer = args.selectLayer;
		var namespace = selectLayer.split(':')[0];
		CONFIG.ows.getWMSCapabilities({
			namespace: namespace,
			callbacks: {
				success: [
					CONFIG.tempSession.updateLayersFromWMS,
					function (caps) {
						LOG.info('Transects.js::refreshFeatureList: WMS GetCapabilities response parsed');
						Transects.populateFeaturesList(caps);
						if (selectLayer) {
							LOG.info('Transects.js::refreshFeatureList: Auto-selecting layer ' + selectLayer);
							Transects.$transectListbox.children().each(function (i, v) {
								if (v.value === selectLayer) {
									LOG.debug('Triggering "select" on featurelist option');
									Transects.$transectListbox.val(v.value);
								}
							});
						} else {
							Transects.$transectListbox.val('');
						}
						Transects.$transectListbox.trigger('change');
					}
				],
				error: [
					LOG.warn('Transects.js::refreshFeatureList: WMS GetCapabilities could not be attained')
				]
			}
		});
	},
	addTransects: function (args) {
		var transects = new OpenLayers.Layer.Vector(args.name, {
			strategies: [new OpenLayers.Strategy.BBOX()],
			protocol: new OpenLayers.Protocol.WFS.v1_1_0({
				version: '1.1.0',
				outputFormat: 'gml2',
				url: "geoserver/" + args.name.split(':')[0] + "/wfs",
				featureType: args.name.split(':')[1],
				featureNS: CONFIG.namespace[args.name.split(':')[0]],
				geometryName: "the_geom",
				srsName: CONFIG.map.getMap().getProjection()
			}),
			styleMap: new OpenLayers.StyleMap({
				"default": new OpenLayers.Style({
					strokeColor: Transects.reservedColor,
					strokeWidth: 2
				})
			}),
			type: Transects.stage,
			displayInLayerSwitcher: false
		});
		CONFIG.map.getMap().addLayer(transects);
		CONFIG.tempSession.getStage(Transects.stage).viewing = args.name;
		CONFIG.tempSession.persistSession();
	},
	removeResource: function (args) {
		args = args || {};
		var layer = args.layer || Transects.$transectListbox.find('option:selected')[0].text;
		var store = args.store || 'ch-input';
		var callbacks = args.callbacks || [
			function () {
				CONFIG.ui.showAlert({
					message: 'Transect removed',
					caller: Transects,
					displayTime: 4000,
					style: {
						classes: ['alert-success']
					}
				});
				Transects.$transectListbox.val('');
				CONFIG.ui.switchTab({
					caller: Transects,
					tab: 'view'
				});
				Transects.refreshFeatureList();
			}
		];
		try {
			CONFIG.tempSession.removeResource({
				store: store,
				layer: layer,
				callbacks: callbacks
			});
		} catch (ex) {
			CONFIG.ui.showAlert({
				message: 'Remove Failed - ' + ex,
				caller: Transects,
				displayTime: 0,
				style: {
					classes: ['alert-error']
				}
			});
		}
	},
	removeTransects: function () {
		CONFIG.map.getMap().getLayersBy('type', 'transects').each(function (layer) {
			CONFIG.map.getMap().removeLayer(layer, false);
			CONFIG.tempSession.getStage(Transects.stage).viewing = layer.name;
			CONFIG.tempSession.persistSession();
		});
	},
	populateFeaturesList: function () {
		CONFIG.ui.populateFeaturesList({
			caller: Transects
		});
	},
	clear: function () {
		Transects.$transectListbox.val('');
		Transects.listboxChanged();
	},
	clearSubsequentStages: function () {
		Calculation.clear();
		Results.clear();
	},
	listboxChanged: function () {
		LOG.info('Transects.js::listboxChanged: Transect listbox changed');
		Transects.disableEditButton();
		Transects.disableDownloadButton();
		Transects.$transectListbox.find("option:not(:selected)").each(function (index, option) {
			var layers = CONFIG.map.getMap().getLayersBy('name', option.value);
			if (layers.length) {
				$(layers).each(function (i, l) {
					CONFIG.map.getMap().removeLayer(l, false);
					CONFIG.tempSession.getStage(Transects.stage).viewing = l.name;
					CONFIG.tempSession.persistSession();
				});
			}
		});
		var selectedValue = Transects.$transectListbox.find("option:selected")[0].value;
		if (selectedValue) {
			Transects.addTransects({
				name: selectedValue
			});
			CONFIG.tempSession.getStage(Transects.stage).viewing = selectedValue;
			CONFIG.tempSession.persistSession();
			Transects.enableEditButton();
			Transects.enableDownloadButton();
			// Update the interesects listbox in calculations if available
			var intersections = selectedValue.substring(0, selectedValue.lastIndexOf('_') + 1) + 'intersects';
			Calculation.updateListboxWithValue(intersections);
		}
	},
	disableDownloadButton: function () {
		this.$buttonDownload.attr('disabled', 'disabled');
	},
	enableDownloadButton: function () {
		this.$buttonDownload.removeAttr('disabled');
	},
	enableEditButton: function () {
		$('#transect-edit-form-toggle').removeAttr('disabled');
	},
	disableEditButton: function () {
		if ($('#transect-edit-form-toggle').hasClass('active')) {
			$('#transect-edit-form-toggle').trigger('click');
			if ($('#transects-edit-add-button').hasClass('active')) {
				$('#transects-edit-add-button').hasClass('active').trigger('click');
			}
		}
		$('#transect-edit-form-toggle').attr('disabled', 'disabled');
	},
	enableUpdateTransectsButton: function () {
		Transects.$buttonTransectsSave.removeAttr('disabled');
	},
	disableUpdateTransectsButton: function () {
		Transects.$buttonTransectsSave.attr('disabled', 'disabled');
	},
	enableCreateTransectsButton: function () {
		LOG.info('Transects.js::enableCreateTransectsButton: Baseline has been added to the map. Enabling create transect button');
		$('#create-transects-toggle').removeAttr('disabled');
	},
	disableCreateTransectsButton: function () {
		LOG.info('Transects.js::disableCreateTransectsButton: No valid baseline on the map. Disabling create transect button');
		if ($('#create-transects-toggle').hasClass('active')) {
			$('#create-transects-toggle').trigger('click');
		}
		$('#create-transects-toggle').attr('disabled', 'disabled');
	},
	deactivateSelectControl: function () {
		var control = CONFIG.map.getMap().getControlsBy('title', Transects.NAME_CONTROL_SELECT);
		if (control.length) {
			control[0].deactivate();
		}
	},
	removeDrawControl: function () {
		var controlArr = CONFIG.map.getMap().getControlsBy('id', 'transects-draw-control');
		if (controlArr.length) {
			controlArr[0].destroy();
		}
		CONFIG.map.removeControl({
			id: 'transects-draw-control'
		});
	},
	removeCropControl: function () {
		var controlArr = CONFIG.map.getMap().getControlsBy('id', Transects.NAME_CONTROL_CROP);
		if (controlArr.length) {
			controlArr[0].deactivate();
			controlArr[0].destroy();
		}
		CONFIG.map.removeControl({
			id: Transects.NAME_CONTROL_CROP
		});
	},
	removeEditControl: function () {
		Transects.removeEditLayer();
		var controlArr = CONFIG.map.getMap().getControlsBy('id', Transects.NAME_CONTROL_EDIT);
		if (controlArr.length) {
			controlArr[0].deactivate();
			controlArr[0].destroy();
		}

		CONFIG.map.removeControl({
			id: Transects.NAME_CONTROL_EDIT
		});
	},
	removeSnapControl: function () {
		var controlArr = CONFIG.map.getMap().getControlsBy('id', Transects.NAME_CONTROL_SNAP);
		if (controlArr.length) {
			controlArr[0].destroy();
		}
		CONFIG.map.removeControl({
			id: Transects.NAME_CONTROL_SNAP
		});
	},
	removeAngleLayer: function () {
		var layerArr = CONFIG.map.getMap().getLayersBy('name', Transects.NAME_LAYER_ANGLE);
		if (layerArr.length) {
			var layer = layerArr[0];
			var snapControlArr = CONFIG.map.getMap().getControlsBy('id', Transects.NAME_CONTROL_SNAP);
			if (snapControlArr.length) {
				var snapControl = snapControlArr[0];
				snapControl.removeTargetLayer(layer);
			}
			CONFIG.map.removeLayerByName(Transects.NAME_LAYER_ANGLE);
		}
	},
	getSelectControl: function () {
		var ca = CONFIG.map.getMap().getControlsBy('title', Transects.NAME_CONTROL_SELECT);
		if (ca.length) {
			return ca[0];
		} else {
			return null;
		}
	},
	getHighlightControl: function () {
		var ca = CONFIG.map.getMap().getControlsBy('title', Transects.NAME_CONTROL_HIGHLIGHT);
		if (ca.length) {
			return ca[0];
		} else {
			return null;
		}
	},
	deactivateHighlightControl: function () {
		var control = Transects.getHighlightControl();
		if (control) {
			control.deactivate();
		}
	},
	activateHighlightControl: function () {
		var control = Transects.getHighlightControl();
		if (control) {
			control.activate();
		}
	},
	createTransectsButtonToggled: function (event) {
		LOG.info('Transects.js::createTransectsButtonToggled: Transect creation Button Clicked');
		var toggledOn = $(event.currentTarget).hasClass('active') ? false : true;
		if (toggledOn && $('#transect-edit-form-toggle').hasClass('active')) {
			$('#transect-edit-form-toggle').trigger('click');
		}
		$('#create-transects-panel-well').toggleClass('hidden');
		$('#intersection-calculation-panel-well').toggleClass('hidden');
		$('#create-transects-input-button').toggleClass('hidden');
	},
	createTransectSubmit: function () {
		Transects.clearSubsequentStages();
		var shorelines = CONFIG.tempSession.getStage(Shorelines.stage).layers,
			selectedBounds = Shorelines.aoiBoundsSelected,
			baseline = Baseline.getActive(),
			biasRef = ProxyDatumBias.usePdbInProcessing ? ProxyDatumBias.overrideWorkspace + ":" + ProxyDatumBias.overrideLayer : null,
			spacing = $('#create-transects-input-spacing').val() || 0,
			length = $('#create-transects-input-length').val(),
			layerName = $('#create-transects-input-name').val(),
			farthest = $('#create-intersections-nearestfarthest-list').val(),
			smoothing = parseFloat($('#create-transects-input-smoothing').val());
		if (!selectedBounds) {
			CONFIG.ui.showAlert({
				message: 'You\'ve not selected shorelines to build transects against',
				caller: Transects,
				displayTime: 0,
				style: {
					classes: ['alert-info']
				}
			});
			return;
		}
		
		if (!length.length) {
			length = null;
		}

		if (isNaN(smoothing)) {
			smoothing = Transects.DEFAULT_SMOOTHING;
		} else {
			smoothing = smoothing <= 0.0 ? 0.0 : smoothing;
		}

		var request = Transects.createWPScreateTransectsAndIntersectionsRequest({
			shorelines: shorelines,
			biasRef: biasRef,
			baseline: baseline,
			spacing: spacing,
			length: length,
			farthest: farthest,
			smoothing: smoothing,
			workspace: CONFIG.tempSession.getCurrentSessionKey(),
			store: 'ch-input',
			transectLayer: layerName + '_transects',
			intersectionLayer: layerName + '_intersects'
		});
		var wpsProc = function () {
			CONFIG.ows.executeWPSProcess({
				processIdentifier: 'gs:CreateTransectsAndIntersections',
				request: request,
				context: this,
				callbacks: [
					// TODO- Error Checking for WPS process response!
					function (data) {
						if (typeof data === 'string') {
							var transectLayer = data.split(',')[0];
							var intersectionLayer = data.split(',')[1];
							CONFIG.ows.getWMSCapabilities({
								namespace: CONFIG.tempSession.getCurrentSessionKey(),
								callbacks: {
									success: [
										CONFIG.ows.getWFSCapabilities,
										Transects.populateFeaturesList,
										Calculation.populateFeaturesList,
										function () {
											// Remove previous transects and intersection layers
											if (CONFIG.map.getMap().getLayersBy('type', 'transects').length) {
												CONFIG.map.getMap().removeLayer(CONFIG.map.getMap().getLayersBy('type', 'transects')[0]);
											}
											if (CONFIG.map.getMap().getLayersBy('type', 'intersects').length) {
												CONFIG.map.getMap().removeLayer(CONFIG.map.getMap().getLayersBy('type', 'intersects')[0]);
											}

											Transects.$transectListbox.val(transectLayer);
											$('#intersections-list').val(intersectionLayer);
											Transects.$transectListbox.trigger('change');
											$('#intersections-list').trigger('change');
											$('#stage-select-tablist a[href="#calculation"]').trigger('click');
											CONFIG.ui.showAlert({
												message: 'Intersection calculation succeeded.',
												displayTime: 7500,
												caller: Calculation,
												style: {
													classes: ['alert-success']
												}
											});
										}
									]
								}
							});
						} else {
							var errorText = $(data).find('ows\\:ExceptionText').first().text();
							var msg = 'Transect calculation failed. Check logs.';
							LOG.error(errorText);
							// This is a dopey way of doing this...
							if (errorText.toLowerCase().has('baselines cannot intersect shorelines')) {
								msg = 'Transect calculation failed. Baselines cannot intersect shorelines.';
							}

							CONFIG.ui.showAlert({
								message: msg,
								displayTime: 0,
								caller: Transects,
								style: {
									classes: ['alert-error']
								}
							});
						}
					}
				]
			});
		};
		// Check if transects already exists in the select list
		if (Transects.$transectListbox.find('option[value="' + CONFIG.tempSession.getCurrentSessionKey() + ':' + layerName + '_transects"]').length ||
			$('#intersections-list option[value="' + CONFIG.tempSession.getCurrentSessionKey() + ':' + layerName + '_intersects"]').length) {
			CONFIG.ui.createModalWindow({
				context: {
					scope: this
				},
				headerHtml: 'Resource Exists',
				bodyHtml: 'A resource already exists with the name ' + layerName + ' in your session. Would you like to overwrite this resource?',
				buttons: [
					{
						text: 'Overwrite',
						callback: function () {
							$.when(
									$.ajax('service/layer/workspace/' + CONFIG.tempSession.getCurrentSessionKey() + '/store/ch-input/' + layerName + '_transects',
											{
												type: 'DELETE',
												context: this
											}),
									$.ajax('service/layer/workspace/' + CONFIG.tempSession.getCurrentSessionKey() + '/store/ch-input/' + layerName + '_intersects',
											{
												type: 'DELETE',
												context: this
											})
									)
									.then(wpsProc)
						}
					}
				]
			});
		} else {
			wpsProc();
		}
	},
	createWPScreateTransectsAndIntersectionsRequest: function (args) {
		var shorelines = args.shorelines;
		var baseline = args.baseline;
		var biasRef = args.biasRef;
		var biasrefNS = biasRef ? biasRef.split(':')[0] + '="' + CONFIG.namespace.proxydatumbias + '"' : '';
		var baselineNS = baseline.split(':')[0] + '="gov.usgs.cida.ch.' + baseline.split(':')[0] + '"';
		var spacing = args.spacing ? args.spacing : Transects.DEFAULT_SPACING;
		var smoothing = args.smoothing || 0.0;
		var farthest = args.farthest;
		var workspace = args.workspace;
		var transectLayer = args.transectLayer;
		var intersectionLayer = args.intersectionLayer;
		var length = args.length;
		var store = args.store;
		var bounds = Shorelines.aoiBoundsSelected.clone().transform(new OpenLayers.Projection(CONFIG.strings.epsg900913), new OpenLayers.Projection(CONFIG.strings.epsg4326)).toArray(false);
		var shorelineInfo = [];
		shorelines.forEach(function (shoreline) {
			var shorelineWorkspace = shoreline.split(':')[0];
			this.push({
				name: shoreline,
				excludedShorelines: CONFIG.tempSession.getDisabledShorelines(shorelineWorkspace),
				workspace: shorelineWorkspace
			});
		}, shorelineInfo);
		
		var request = Transects.createTransectsAndIntersectionsWPSTemplate({
			bounds: bounds,
			shorelineInfo: shorelineInfo,
			workspace: workspace,
			biasRef: biasRef,
			biasrefNS: biasrefNS,
			baseline: baseline,
			baselineNS: baselineNS,
			spacing: spacing,
			length: length,
			smoothing: smoothing,
			farthest: farthest,
			store: store,
			transectLayer: transectLayer,
			intersectionLayer: intersectionLayer
		});
		
		return request;
	},
	enableUploadButton: function () {
		$('#transects-triggerbutton').removeAttr('disabled');
	},
	disableUploadButton: function () {
		$('#transects-triggerbutton').attr('disabled', 'disabled');
	},
	initializeUploader: function (args) {
		CONFIG.ui.initializeUploader($.extend({
			caller: Transects
		}, args));
	},
	getActive: function () {
		return Transects.$transectListbox.find("option:selected").first().val();
	}
};
