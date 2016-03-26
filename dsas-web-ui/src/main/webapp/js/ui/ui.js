/* global CONFIG */
/* global Map */
/* global LOG */
/* global ProxyDatumBias */
/* global Shorelines */
/* global Baseline */
/* global Transects */
/* global Calculation */
/* global TransectVerification */
/* global Results */
/* global qq */
/* global OpenLayers */
/* global bootstro */
/* global Util */

var UI = function () {
	"use strict";
	LOG.info('UI.js::constructor: UI class is initializing.');

	var me = (this === window) ? {} : this;

	me.work_stages = ['bias', 'shorelines', 'baseline', 'transect_verification', 'transects', 'calculation', 'results'];
	me.work_stages_objects = [ProxyDatumBias, Shorelines, Baseline, TransectVerification, Transects, Calculation, Results];
	me.base_name = undefined;//init to undefined. Update in baselines
	me.precachedImages = [
		'images/introduction_images/BaselineDraw.gif',
		'images/introduction_images/EditTransects.gif',
		'images/workflow_figures/baseline.png',
		'images/workflow_figures/baseline_past.png',
		'images/workflow_figures/calculation.png',
		'images/workflow_figures/calculation_past.png',
		'images/workflow_figures/transects.png',
		'images/workflow_figures/transects_past.png',
		'images/workflow_figures/results.png',
		'images/workflow_figures/results_past.png',
		'images/workflow_figures/shorelines_future.png',
		'images/workflow_figures/shorelines_past.png'
	];
	me.clickIdTableTemplate = undefined;
	$('#manage-sessions-btn').on('click', function () {
		CONFIG.tempSession.createSessionManagementModalWindow();
	});

	$('.collapsibleHelp').accordion({
		collapsible: true,
		heightStyle: 'content'
	});

	// Set up on-click functionality for stage buttons
	$('.nav-stacked>li>a').each(function (index, ele) {
		$(ele).on('click', function () {
			me.switchStage(index);
		});
	});

	LOG.debug('UI.js::constructor: UI class initialized.');
	return $.extend(me, {
		appInit: function () {
			// bias stage not shown when not admin
			// TODO - I've disabled the login requirement to show the BIAS stage
			// This should be re-enabled when we've landed on a login solution
			if (false) {
				me.work_stages.shift();
				me.work_stages_objects.shift();
			}

			this.bindWindowResize();
			this.addIntroContent();
			$(window).resize();
			// Header fix
			$('#ccsa-area').find('br').first().remove();
		},
		/**
		 * Removes the spinner for the splash page. Useful if a severe error 
		 * happens during load (e.g. can't find the backing OWS service), 
		 * I want to show a message on the splash page and get rid of the spinner
		 * because the rest of the application is not going to continue loading
		 */
		removeSplashSpinner: function () {
			$('#splash-spinner').fadeOut();
		},
		displayStage: function (caller) {
			$('#stage-select-tablist a[href="#' + caller.stage + '"]').trigger('click');
		},
		bindWindowResize: function () {
			$(window).resize(function () {
				var mapViewport = $(CONFIG.map.getMap().getViewport());
				var contentRowHeight = $(window).height() - $('#header-row').height() - $('#footer-row').height() - $('#alert-row').height() - 40;
				$('#content-row').css('min-height', contentRowHeight);
				$('#nav-list').css('min-height', contentRowHeight);
				$('#toolbox-span').css('min-height', contentRowHeight);
				$('#map-span').css('min-height', contentRowHeight);
				CONFIG.map.$mapDiv.css('height', contentRowHeight);
				CONFIG.map.getMap().updateSize();

				// Move the zoom control over to the right
				$('.olControlZoom').css('left', mapViewport.width() - $('.olControlZoom').width() - 20);
				// Move the layer switcher control down a bit to make room for zoom control
				$('.olControlLayerSwitcher').css('top', 60);
			});
		},
		bindSignInImageMouseEvents: function () {
			$('#sign-in-img').on({
				'mouseenter': function () {
					$('#sign-in-img').attr('src', 'images/OpenID/White-signin_Medium_hover_44dp.png');
				},
				'mouseleave': function () {
					$('#sign-in-img').attr('src', 'images/OpenID/White-signin_Medium_base_44dp.png');
				},
				'mousedown': function () {
					$('#sign-in-img').attr('src', 'images/OpenID/White-signin_Medium_press_44dp.png');
				},
				'mouseup': function () {
					$('#sign-in-img').attr('src', 'images/OpenID/White-signin_Medium_base_44dp.png');
				}
			});
		},
		precacheImages: function () {
			var tempImage = [];
			for (var x = 0; x < this.precachedImages.length; x++) {
				var tempImage = new Image();
				tempImage.src = this.precachedImages[x];
			}
		},
		createModalWindow: function (args) {
			var headerHtml = args.headerHtml || '';
			var bodyHtml = args.bodyHtml || '';
			var buttons = args.buttons || [];
			var includeCancelButton = args.includeCancelButton || true;
			var doneButtonText = args.doneButtonText || 'Done';
			var callbacks = args.callbacks || [];

			$('#application-overlay').fadeOut();
			$('#modal-window-label').html(headerHtml);
			$('#modal-body-content').html(bodyHtml);
			$('#modal-window>.modal-footer').html('');

			buttons.each(function (button) {
				var text = button.text;
				var callback = button.callback;
				var modalButton = $('<button />')
					.attr('id', button.id || '')
					.addClass('btn')
					.addClass(button.type)
					.html(text)
					.on('click', callback)
					.on('click', function () {
						$("#modal-window").modal('hide');
					});
				$('#modal-window>.modal-footer').append(modalButton);
			});

			if (includeCancelButton) {
				$('#modal-window>.modal-footer').append(
					$('<button />')
					.addClass('btn')
					.attr({
						'id': 'cancel-button',
						'data-dismiss': 'modal',
						'aria-hidden': 'true'
					})
					.html(doneButtonText));
			}

			callbacks.each(function (callback) {
				callback();
			});

			$('#modal-window').modal('show');
		},
		lockBaseNameTo: function (name) {
			me.base_name = name;
			$('#results-form-name').val(name).trigger('change');
			$('#create-transects-input-name').val(name).trigger('change');
		},
		switchStage: function (stage) {
			LOG.info('UI.js::switchImage: Changing application context to ' + me.work_stages[stage]);
			
			var caller = me.work_stages_objects[stage];
			me.work_stages_objects.filter(function (stage) {
				return stage !== caller;
			}).each(function (stage) {
				stage.leaveStage();
			});

			caller.enterStage();

			for (var stageIndex = 0; stageIndex < me.work_stages.length; stageIndex++) {
				var workStage = me.work_stages[stageIndex];
				var imgId = '#' + workStage + '_img';
				if (stageIndex < stage) {
					$(imgId).attr('src', 'images/workflow_figures/' + workStage + '_past.png');
				} else if (stageIndex === stage) {
					$(imgId).attr('src', 'images/workflow_figures/' + workStage + '.png');
				} else {
					$(imgId).attr('src', 'images/workflow_figures/' + workStage + '_future.png');
				}
			}
		},
		initializeUploader: function (args) {
			args = args || {};

			var caller = args.caller,
				context = caller.stage,
				request = caller.uploadRequest || {
					endpoint: 'service/upload',
					paramsInBody: false,
					forceMultipart: false,
					params: {
						'response.encoding': 'json',
						'filename.param': 'qqfile',
						'overwrite.existing.layer': caller.overrideOverwriteExistingLayer || 'true',
						'workspace': caller.overrideWorkspace || CONFIG.tempSession.getCurrentSessionKey(),
						'store': caller.overrideStore || 'ch-input',
						'srs': CONFIG.map.getMap().getProjection(),
						'use.crs.failover': 'true',
						'projection.policy': 'reproject',
						'layer': caller.overrideLayer || '' // Use the file name for the name
					}
				},
			callbacks = $.extend({},
				{
					onSubmit: function (id, name) {
						CONFIG.ui.showSpinner();

						CONFIG.ui.showAlert({
							close: false,
							message: 'Upload beginning',
							style: {
								classes: ['alert-info']
							}
						});

						if (!this._options.request.params.layer) {

							// Test to see if the upload name ends with an 
							// underscore and the stage name we are in. If not, add it
							if (!name.endsWith(caller.stage + '.zip')) {
								this._options.request.params.layer = name.substring(0, name.length - 4) + '_' + caller.stage;
							}

							// Test to see if the first character in the layer is a digit. 
							// If so, prepend an underscore. Otherwise we get big 
							// fails working with the layer later on
							if (/^[0-9]/.test(this._options.request.params.layer)) {
								this._options.request.params.layer = '_' + this._options.request.params.layer;
							}
						}
					},
					onCancel: function (id, name) {
						CONFIG.ui.hideSpinner();
						$('#application-alert').alert('close');
					},
					onError: function (id, name, errorReason, xhr) {
						CONFIG.ui.hideSpinner();
						$('#application-alert').alert('close');
					},
					onProgress: function (id, name, uploadBytes, totalBytes) {
						$('#application-alert #message').html('Uploading <b>' + uploadBytes + '<b /> of <b>' + totalBytes + '<b /> total bytes');
					},
					onComplete: function (id, fileName, responseJSON) {
						CONFIG.ui.hideSpinner();
						$('#application-alert').alert('close');
						var success = responseJSON.success;
						var layerName = responseJSON.name;
						var workspace = responseJSON.workspace;

						if (success === 'true') {
							LOG.info("UI.js::initializeUploader: Upload complete");
							LOG.info('UI.js::initializeUploader: Import complete. Will now call WMS GetCapabilities to refresh session object and ui.');
							CONFIG.ows.getWMSCapabilities({
								namespace: workspace,
								layerName: layerName,
								callbacks: {
									success: [
										function (args) {
											CONFIG.ui.showAlert({
												message: 'Upload Successful',
												caller: caller,
												displayTime: 3000,
												style: {
													classes: ['alert-success']
												}
											});
											CONFIG.tempSession.updateLayersFromWMS(args);
											CONFIG.ui.populateFeaturesList({
												caller: caller
											});
											$('a[href="#' + caller.stage + '-view-tab"]').tab('show');
											$('#' + caller.stage + '-list')
												.val(layerName)
												.trigger('change');
										}
									],
									error: [
										function (args) {
											LOG.info('UI.js::Uploader Error Callback: Import incomplete.');
											CONFIG.ui.showAlert({
												message: 'Import incomplete',
												caller: caller,
												displayTime: 0,
												style: {
													classes: ['alert-error']
												}
											});
										}
									]
								}
							});
						} else {
							LOG.warn('UI.js::Uploader Error Callback: Import incomplete.');
							CONFIG.ui.showAlert({
								message: 'Import incomplete. ' + (responseJSON.exception ? responseJSON.exception : ''),
								caller: caller,
								displayTime: 0,
								style: {
									classes: ['alert-error']
								}
							});
						}
					}
				}, caller.uploadCallbacks);

			LOG.info('UI.js::initializeUploader: Initializing uploader for the ' + context + ' context');

			var uploader = new qq.FineUploader({
				element: document.getElementById(context + '-uploader'),
				multiple: false,
				request: request,
				validation: {
					allowedExtensions: ['zip']
				},
				autoUpload: true,
				caller: caller,
				text: {
					uploadButton: '<i class="icon-upload icon-white"></i>Upload'
				},
				classes: {
					success: 'alert alert-success',
					fail: 'alert alert-error'
				},
				callbacks: callbacks
			});
			$('#' + context + '-triggerbutton').on('click', function () {
				$('#' + context + '-uploader input').fineUploader().trigger('click');
			});
			return uploader;
		},
		populateFeaturesList: function (args) {
			var wmsCapabilities = CONFIG.ows.wmsCapabilities;
			var caller = args.caller;
			var suffixes = caller.suffixes || [];
			var stage = args.stage || caller.stage;

			LOG.info('UI.js::populateFeaturesList:: Populating feature list for ' + stage);
			$('#' + stage + '-list').children().remove();

			// Add a blank spot at the top of the select list
			$('#' + stage + '-list')
				.append($("<option />")
					.attr("value", '')
					.text(''));

			wmsCapabilities.keys().each(function (layerNS) {
				var cap = wmsCapabilities[layerNS];
				var layers = cap.capability.layers;
				var sessionLayerClass = 'session-layer';
				var publishedLayerClass = 'published-layer';

				layers.each(function (layer) {
					var currentSessionKey = CONFIG.tempSession.getCurrentSessionKey();
					var title = layer.title;
					// Add the option to the list only if it's from the published namespace or
					// if it's from the input namespace and in the current session
					if (layerNS === CONFIG.name.published || layerNS === CONFIG.name.proxydatumbias || layerNS === currentSessionKey) {
						var type = title.substr(title.lastIndexOf('_'));
						if (suffixes.length === 0 || suffixes.indexOf(type.toLowerCase()) !== -1) {
							LOG.debug('UI.js::populateFeaturesList: Found a layer to add to the ' + stage + ' listbox: ' + title);
							var layerFullName = layer.prefix + ':' + layer.name;
							var sessionStage = CONFIG.tempSession.getStage(stage);
							var lIdx = sessionStage.layers.findIndex(function (l) {
								return l === layerFullName;
							});
							if (lIdx === -1) {
								sessionStage.layers.push(layerFullName);
							}
							CONFIG.tempSession.persistSession();
							var option = $("<option />")
								.attr({
									"value": layerNS + ':' + layer.name
								})
								.addClass(layerNS === CONFIG.name.published ? publishedLayerClass : sessionLayerClass)
								.text(layer.title);

							$('#' + stage + '-list')
								.append(option);
						}
					}
				});
			});

			CONFIG.tempSession.persistSession();
			LOG.debug('UI.js::populateFeaturesList: Re-binding select list');
			$('#' + stage + '-list').unbind('change');
			$('#' + stage + '-list').change(function (index, option) {
				caller.listboxChanged(index, option);
			});

			return  $('#' + stage + '-list');
		},
		showShorelineInfo: function (event) {
			LOG.debug('UI.js::showShorelineInfo: The map was clicked and a response from the OWS resource was received');

			Shorelines.closeShorelineIdWindows();

			if (event.features.length) {
				LOG.debug('UI.js::showShorelineInfo: Features were returned from the OWS resource. Parsing and creating table to display');
				LOG.debug('UI.js::showShorelineInfo: Creating table for ' + event.features.length + ' features');
				// The click event brings back many points of the same feature id.
				// I only want unique feature IDs in this table
				var features = event.features.unique(function (f) {
					return f.data.shoreline_id;
				}),
					layerTitle = features[0].fid.split('.')[0],
					layerName = features[0].gml.featureNSPrefix + ':' + layerTitle,
					shorelineIdContainer = $('<div />').attr('id', layerName + '-id-container').addClass('shoreline-id-container'),
					shorelineIdTable = $('<table />').attr('id', layerName + '-id-table').addClass('shoreline-id-table table table-striped table-condensed'),
					thead = $('<thead />'),
					theadTr = $('<tr />'),
					tbody = $('<tbody />');

				LOG.trace('UI.js::showShorelineInfo: Closing any other open identify windows');
				$('.olPopupCloseBox').each(function (i, v) {
					v.click();
				});

				Shorelines.clickToIdColumnNames.forEach(function (v) {
					theadTr.append($('<th />').append(v));
				});
				thead.append(theadTr);

				features.each(function (feature) {
					var atts = feature.attributes,
						shorelineId = atts.shoreline_id,
						workspace = atts.workspace,
						tbodyTr = $('<tr />').attr('data-shoreline-id', shorelineId);

					Shorelines.clickToIdColumnNames.forEach(function (v) {
						tbodyTr.append($('<td />').append(atts[v]));
					});

					var date = Date.create(Shorelines.groupingColumn).format(Shorelines.dateFormat);
					var isVisible = !CONFIG.tempSession.isDateDisabled(date);
					
					var $button = $('<button />')
						.addClass('btn btn-shoreline-id-toggle')
						.attr({
							type: 'button',
							layer: layerName,
							'data-shoreline-workspace': workspace,
							'data-shoreline-id': shorelineId
						})
						.html(isVisible ? 'Disable' : 'Enable');

					if (isVisible) {
						$button.addClass('btn-success');
					} else {
						$button.addClass('btn-danger');
					}

					tbodyTr.append($('<td />').append($button));
					tbody.append(tbodyTr);
				});

				shorelineIdTable.append(thead);
				shorelineIdTable.append(tbody);
				shorelineIdContainer.append(shorelineIdTable);

				LOG.debug('UI.js::showShorelineInfo: Table created, displaying new identify window');
				CONFIG.map.getMap().addPopup(new OpenLayers.Popup.FramedCloud(
					"FramedCloud",
					CONFIG.map.getMap().getLonLatFromPixel(event.xy),
					null,
					shorelineIdContainer.html(),
					null,
					true
					));

				$('.btn-shoreline-id-toggle').click(function (event) {
					var shorelineId = $(event.target).attr('data-shoreline-id'),
						toggle = Shorelines.$shorelineFeatureTableContainer
						.find('table > tbody tr[data-shoreline-id="' + shorelineId + '"]');
					toggle.bootstrapSwitch('toggleState');
				});

			} else {
				LOG.debug('UI.js::showShorelineInfo: No features were found at point of mouse click');
				CONFIG.ui.showAlert({
					message: 'No shorelines found',
					caller: Shorelines,
					displayTime: 0,
					style: {
						classes: ['alert-info']
					}
				});
			}
		},
		showAlert: function (args) {
			var caller = args.caller || {
				stage: 'application'
			};
			var message = args.message || '';
			var style = args.style || {
				classes: []
			};
			var alertContainer = $('#' + caller.stage + '-alert-container');
			var alertDom = $('<div />').attr('id', caller.stage + '-alert');
			var close = args.close || true;
			var displayTime = args.displayTime || 0;

			CONFIG.alertQueue[caller.stage].unshift({
				message: message,
				style: style,
				displayTime: displayTime,
				close: close
			});

			var createAlert = function (args) {
				var nextMessageObj = CONFIG.alertQueue[args.caller.stage].pop();
				if (nextMessageObj.hasOwnProperty('message')) {
					var alertContainer = args.alertContainer;
					var style = nextMessageObj.style;
					var close = nextMessageObj.close;
					var message = nextMessageObj.message;
					var displayTime = nextMessageObj.displayTime;
					var createAlertFn = args.createAlertFn;
					var queueLength = CONFIG.alertQueue[args.caller.stage].length;

					alertDom.addClass('alert fade in');
					if (style.classes) {
						alertDom.addClass(style.classes.join(' '));
					}

					if (close) {
						alertDom.append($('<button />')
							.attr({
								'type': 'button',
								'data-dismiss': 'alert',
								'href': '#'
							})
							.addClass('close')
							.html('&times;'));
					}

					if (queueLength) {
						alertDom.append($('<div />').addClass('alert-queue-notifier').html(queueLength + ' more'));
					}

					alertDom.append($('<div id="message"/>').html(message));
					alertContainer.append(alertDom);

					alertDom.on('closed', function () {
						if (CONFIG.alertQueue[args.caller.stage].length) {
							createAlertFn({
								alertDom: alertDom,
								alertContainer: alertContainer,
								createAlertFn: createAlertFn,
								caller: args.caller
							});
						}
					});

					alertDom.alert();

					if (displayTime) {
						setTimeout(function () {
							alertDom.alert('close');
						}, displayTime);
					}
				}
			};

			// The container is empty so go ahead and fire the alert
			if (alertContainer.children().length === 0) {
				createAlert({
					alertDom: alertDom,
					alertContainer: alertContainer,
					createAlertFn: createAlert,
					caller: caller
				});
			}
		},
		switchTab: function (args) {
			var caller = args.caller;
			var stage = caller ? caller.stage : args.stage || '';
			var tab = args.tab;
			if (tab === 'view') {
				$('#action-' + stage + '-tablist a[href="#' + stage + '-view-tab"]').trigger('click');
			} else if (tab === 'manage') {
				$('#action-' + stage + '-tablist a[href="#' + stage + '-manage-tab"]').trigger('click');
			}
		},
		buildColumnMatchingModalWindow: function (args) {
			LOG.debug('UI.js::buildColumnMatchingModalWindow: Could not automatically map all layer attributes. Need help');
			var layerName = args.layerName,
				columns = args.columns,
				caller = args.caller,
				template = args.template,
				cancelCallback = args.cancelCallback ? args.cancelCallback : Util.noopFunction,
				continueCallback = args.continueCallback,
				updateCallback = args.updateCallback;

			var html = template({
				stage: caller.stage,
				defaultingColumns: caller.defaultingColumns,
				layerName: layerName,
				columnKeys: columns.keys(),
				mandatoryColumns: caller.mandatoryColumns,
				defaultColumns: caller.defaultingColumns
			});

			CONFIG.ui.createModalWindow({
				headerHtml: 'Please Assign Required and Optional Attribute Fields',
				bodyHtml: html,
				doneButtonText: 'Cancel',
				buttons: [{
						id: 'modal-continue-button',
						text: 'Continue With Defaults',
						type: 'btn-success',
						callback: continueCallback
					}, {
						id: 'modal-update-button',
						text: 'Update',
						type: 'btn-success',
						callback: updateCallback
					}],
				callbacks: [
					function () {
						$('#modal-update-button').attr('disabled', 'disabled');
						$('#modal-continue-button').attr('disabled', 'disabled');

						// When cancel button is clicked, remove the layer on the server as well
						$('#cancel-button').click(cancelCallback);

						$('#' + layerName + '-drag-drop-row').data('mapping', columns);

						$('.' + layerName + '-drag-item').draggable({
							containment: '#' + layerName + '-drag-drop-row',
							scroll: false,
							snap: '.' + layerName + '-drop-holder',
							snapMode: 'inner',
							cursor: 'move',
							revert: 'invalid',
							stack: '.' + layerName + '-drag-item'
						});

						$('.' + layerName + '-drop-holder').droppable({
							greedy: true,
							activeClass: 'ui-state-highlight',
							hoverClass: 'drop-hover',
							tolerance: 'fit',
							drop: function (event, ui) {
								var draggable = ui.draggable;
								var dragId = draggable.attr('id');
								var dropId = this.id;
								var layerAttribute = dragId.substr(0, dragId.indexOf('-drag-item'));
								var layerMappingAttribute = dropId.substr(0, dropId.indexOf('-drop-item'));
								var mapping = $('#' + layerName + '-drag-drop-row').data('mapping');

								// Figure out if we are in a drag or drop well
								if ($(this).closest('.well').attr('id').indexOf('drop-container')) {
									mapping[layerAttribute] = layerMappingAttribute;
								} else { // left column, remove from map
									mapping[layerAttribute] = '';
								}

								var readyToUpdate = true;
								caller.mandatoryColumns.each(function (mc) {
									if (!mapping.values().filter(mc).length) {
										readyToUpdate = false;
									}
								});
								if (readyToUpdate) {
									$('#modal-continue-button').removeAttr('disabled');
									$('#modal-update-button').removeAttr('disabled');
								} else {
									$('#modal-continue-button').attr('disabled', 'disabled');
									$('#modal-update-button').attr('disabled', 'disabled');
								}

							}
						});

						var moveDraggable = function (draggable, droppable) {
							var dragTop = draggable.position().top;
							var dragLeft = draggable.position().left;
							var dropTop = droppable.position().top;
							var dropLeft = droppable.position().left;
							var horizontalMove = dropLeft - dragLeft;
							var verticalMove = dropTop - dragTop + 5; // 5 = margin-top
							draggable.animate({
								left: horizontalMove
							}, {
								queue: 'fx',
								duration: 1000
							}).animate({
								top: verticalMove
							},
							{
								queue: 'fx',
								duration: 1000,
								complete: function () {
									this.style.zIndex = 9999;
								}
							});
						};

						var showCallback = function () {
							$("#modal-window").unbind('shown', showCallback);
							
							// Move stuff over if the layers are already mapped
							columns.keys().each(function (key) {
								if (columns[key]) {
									var draggable = $('#' + key + '-drag-item').draggable('widget');
									var droppable = $('#' + columns[key] + '-drop-item').droppable('widget');
									draggable.queue("fx");
									moveDraggable(draggable, droppable);
								}
							});

							//if all mandatory columns are mapped, we can allow continuing
							var allMandatoryColumnsSet = true;
							Shorelines.mandatoryColumns.each(function (mc) {
								if (!columns.values().filter(mc).length) {
									allMandatoryColumnsSet = false;
								}
							});
							if (allMandatoryColumnsSet) {
								$('#modal-continue-button').removeAttr('disabled'); //allowed to continue if all mandatory columns are already mapped
							}
						};
						$("#modal-window").on('shown', showCallback);
						$("#modal-window").on('hidden', function () {
							$('#' + layerName + '-drag-drop-row').data('mapping', undefined);
						});
					}]
			});
		},
		/**
		 * Displays the AJAX animated spinner graphic
		 * 
		 * @returns {undefined}
		 */
		showSpinner: function () {
			$("#application-spinner").fadeIn();
		},
		/**
		 * Hides the AJAX animated spinner graphic
		 * 
		 * @returns {undefined}
		 */
		hideSpinner: function () {
			$("#application-spinner").fadeOut();
		},
		/**
		 * Given the response from the geocoding service, build the popup on the map
		 * to display the results
		 * 
		 * @param {Object} {
		 *		"currentLocationIndex" : 0, // An index into the locations dropdown 
		 *		"locations" : $('#location-container').data('locations')
		 *	}
		 * @returns {undefined}
		 */
		buildGeocodingPopup: function (args) {
			var map = CONFIG.map.getMap();
			var currentLocationIndex = args.currentLocationIndex || 0;
			var locations = args.locations || $('#location-container').data('locations');
			var currentLocation = locations[currentLocationIndex];
			var xmax = currentLocation.extent.xmax;
			var xmin = currentLocation.extent.xmin;
			var ymax = currentLocation.extent.ymax;
			var ymin = currentLocation.extent.ymin;
			var x = currentLocation.feature.geometry.x;
			var y = currentLocation.feature.geometry.y;
			var locAttr = currentLocation.feature.attributes;
			var select = $('<select />').attr('id', 'alt-location-list');

			// Build Marker
			var markerLayer = CONFIG.map.getGeocodingMarkerLayer();
			var iconSize = new OpenLayers.Size(32, 32);
			var icon = new OpenLayers.Icon('js/openlayers/img/BulbGrey.png', iconSize, new OpenLayers.Pixel(-(iconSize.w / 2), -iconSize.h));
			var marker = new OpenLayers.Marker(new OpenLayers.LonLat(x, y), icon);

			// Build HTML
			var container = $('<div />').addClass('container-fluid').attr('id', 'location-container');
			var table = $('<table />').addClass('table table-hover table-condensed');
			table.append(
				$('<thead>').append(
				$('<tr />').attr('colspan', '2').append(
				$('<th />').attr('id', 'location-popup-title').html(locAttr.Match_addr))));
			var tbody = $('<tbody />');
			if (locAttr.Type) {
				tbody.append('<tr><td>Address Type</td><td>' + locAttr.Addr_type + ' : ' + locAttr.Type + '</td></tr>');
			}
			if (locAttr.Country) {
				tbody.append('<tr><td>Country</td><td>' + locAttr.Country + '</td></tr>');
			}
			if (locAttr.Loc_name) {
				tbody.append('<tr><td>Source</td><td>' + locAttr.Loc_name + '</td></tr>');
			}
			table.append(tbody);
			container.append($('<div />').addClass('row-fluid span12').append(table));

			select.append($('<option />').attr('value', '-1').html(''));

			for (var lInd = 0; lInd < locations.length; lInd++) {
				if (lInd !== currentLocationIndex) {
					var loc = locations[lInd];
					var addr = loc.feature.attributes.Match_addr;
					var country = loc.feature.attributes.Country;
					var type = loc.feature.attributes.Type;
					var type2 = loc.feature.attributes.Addr_type;
					var typeDesc = ' (Type: ' + type + ', ' + type2 + ')';
					select.append($('<option />').attr('value', lInd).html(addr + ', ' + country + typeDesc));
				}
			}

			if (locations.length > 1) {
				container.append($('<div />').addClass('row-fluid span12').html("Did you mean... ")).append($('<div />').addClass('fluid-row span12').append(select));
			}

			markerLayer.addMarker(marker);

			map.zoomToExtent([xmin, ymin, xmax, ymax], true);

			var popup = new OpenLayers.Popup.FramedCloud("geocoding-popup",
				new OpenLayers.LonLat(x, y),
				new OpenLayers.Size(200, 200),
				$('<div />').append(container).html(),
				icon,
				true,
				function () {
					markerLayer.removeMarker(marker);
					map.removePopup(this);
				});

			if (map.popups.length) {
				map.popups[0].closeDiv.click();
			}

			map.addPopup(popup);

			$('#alt-location-list').change(function (event) {
				var index = parseInt(event.target.value);
				if (index !== -1) {
					CONFIG.ui.buildGeocodingPopup({
						currentLocationIndex: index,
						locations: locations
					});
				}
			});
		},
		/**
		 * Binds the search input text box to submit a query to the geolocation
		 * service and handle the response
		 * 
		 * @returns {undefined}
		 */
		bindSearchInput: function () {
			$('#app-navbar-search-form').submit(function () {
				var query = $('#app-navbar-search-input').val();
				if (query) {
					$.ajax({
						type: 'GET',
						url: 'http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer/find',
						data: {
							text: query,
							maxLocations: '20',
							outFields: '*',
							f: 'pjson',
							outSR: '3785'
						},
						async: false,
						contentType: 'application/json',
						dataType: 'jsonp',
						success: function (json) {
							if (json.locations[0]) {
								CONFIG.ui.buildGeocodingPopup({
									locations: json.locations
								});
							} else {
								CONFIG.ui.showAlert({
									close: false,
									message: query + ' not found',
									displayTime: 0,
									style: {
										classes: ['alert-info']
									}
								});
							}
						}
					});
				}

			});
		},
		/**
		 * Special handling is required for the previous/next steps. Binds the steps 
		 * to what stage/view should be opened at each one
		 * 
		 * @returns {undefined}
		 */
		bindIntroPrevNextButtons: function () {
			bootstro.onStepFunc = function (args) {
				switch (args.idx) {
					case 4 :
					case 5 :
						$('#stage-select-tablist >li a[href="#shorelines"]').click();
						$('#action-shorelines-tablist > li a[href="#shorelines-view-tab"]').click();
						break
					case 6 :
						$('#stage-select-tablist >li a[href="#shorelines"]').click();
						$('#action-shorelines-tablist > li a[href="#shorelines-manage-tab"]').click();
						break
					case 7 :
						$('#stage-select-tablist >li a[href="#shorelines"]').click();
						$('#action-shorelines-tablist > li a[href="#shorelines-view-tab"]').click();
						break
					case 8 :
					case 9 :
						$('#stage-select-tablist >li a[href="#baseline"]').click();
						$('#action-baseline-tablist > li a[href="#baseline-view-tab"]').click();
						break
					case 10 :
						$('#stage-select-tablist >li a[href="#baseline"]').click();
						$('#action-baseline-tablist > li a[href="#baseline-manage-tab"]').click();
						break
					case 11 :
					case 12 :
						$('#stage-select-tablist >li a[href="#transects"]').click();
						$('#action-transects-tablist > li a[href="#transects-view-tab"]').click();
						break
					case 13 :
						$('#stage-select-tablist >li a[href="#transects"]').click();
						$('#action-transects-tablist > li a[href="#transects-manage-tab"]').click();
						break
					case 14 :
					case 15 :
						$('#stage-select-tablist >li a[href="#calculation"]').click();
						$('#action-calculation-tablist > li a[href="#calculation-view-tab"]').click();
						break
					case 16 :
						$('#stage-select-tablist >li a[href="#calculation"]').click();
						$('#action-calculation-tablist > li a[href="#calculation-manage-tab"]').click();
						break
					case 17 :
					case 18 :
						$('#stage-select-tablist >li a[href="#results"]').click();
						$('#action-results-tablist > li a[href="#results-view-tab"]').click();
						break
					case 19 :
						$('#stage-select-tablist >li a[href="#results"]').click();
						$('#action-results-tablist > li a[href="#results-manage-tab"]').click();
						break
				}
			};
		},
		/**
		 * Binds the introductory menu items to the click event
		 * 
		 * @returns {undefined}
		 */
		bindIntroMenuItems: function () {
			$('#nav-menu-intro').on('click', function () {
				bootstro.start(undefined, {
					finishButton: '<button class="btn btn-mini btn-success bootstro-finish-btn"><i class="icon-ok" ></i> Finish</button>'
				});
			});
			$('#nav-menu-shorelines').on('click', function () {
				bootstro.start(undefined, {
					finishButton: '<button class="btn btn-mini btn-success bootstro-finish-btn"><i class="icon-ok" ></i> Finish</button>'
				});
				$('#stage-select-tablist >li a[href="#shorelines"]').click();
				$('#action-shorelines-tablist > li a[href="#shorelines-view-tab"]').click();
				bootstro.go_to(4);
			});
			$('#nav-menu-baseline').on('click', function () {
				bootstro.start(undefined, {
					finishButton: '<button class="btn btn-mini btn-success bootstro-finish-btn"><i class="icon-ok" ></i> Finish</button>'
				});
				$('#stage-select-tablist >li a[href="#baseline"]').click();
				$('#action-baseline-tablist > li a[href="#baseline-view-tab"]').click();
				bootstro.go_to(8);
			});
			$('#nav-menu-transects').on('click', function () {
				bootstro.start(undefined, {
					finishButton: '<button class="btn btn-mini btn-success bootstro-finish-btn"><i class="icon-ok" ></i> Finish</button>'
				});
				$('#stage-select-tablist >li a[href="#transects"]').click();
				$('#action-transects-tablist > li a[href="#transects-view-tab"]').click();
				bootstro.go_to(11);
			});
			$('#nav-menu-calculation').on('click', function () {
				bootstro.start(undefined, {
					finishButton: '<button class="btn btn-mini btn-success bootstro-finish-btn"><i class="icon-ok" ></i> Finish</button>'
				});
				$('#stage-select-tablist >li a[href="#calculation"]').click();
				$('#action-calculation-tablist > li a[href="#calculation-view-tab"]').click();
				bootstro.go_to(14);
			});
			$('#nav-menu-results').on('click', function () {
				bootstro.start(undefined, {
					finishButton: '<button class="btn btn-mini btn-success bootstro-finish-btn"><i class="icon-ok" ></i> Finish</button>'
				});
				$('#stage-select-tablist >li a[href="#results"]').click();
				$('#action-results-tablist > li a[href="#results-view-tab"]').click();
				bootstro.go_to(17);
			});
		},
		/**
		 * Adds content to the introductory sections
		 * @returns {undefined}
		 */
		addIntroContent: function () {
			this.bindIntroPrevNextButtons();
			this.bindIntroMenuItems();

			$('#app-navbar-container').addClass('bootstro').attr({
				'data-bootstro-title': 'Welcome to USGS DSAS',
				'data-bootstro-content': 'This web-based Digital Shoreline Analysis System (DSASweb) is a software application that enables a user to calculate shoreline rate-of-change statistics from multiple historical shoreline positions.' +
					'<br /><br />A user-friendly interface of simple buttons and menus guides the user through the major steps of shoreline change analysis.' +
					'<br /><br />You can use our current database of shorelines, or upload your own.' +
					'<br /><br />DSASweb is a convenient, web-based version of the original USGS DSAS analysis tool.',
				'data-bootstro-placement': 'bottom',
				'data-bootstro-html': true,
				'data-bootstro-step': 0
			});

			$('#stage-select-tablist').addClass('bootstro').attr({
				'data-bootstro-title': 'Stage Selection',
				'data-bootstro-content': 'Each stage in the DSASweb workflow can be accessed by clicking one of these navigation buttons.',
				'data-bootstro-placement': 'right',
				'data-bootstro-html': true,
				'data-bootstro-step': 1
			});

			$('#toolbox-well').addClass('bootstro').attr({
				'data-bootstro-title': 'Toolbox',
				'data-bootstro-content': 'The workspace for the active stage is designed to allow users to edit, define parameters, and interact with various DSASweb selection options.',
				'data-bootstro-placement': 'right',
				'data-bootstro-html': true,
				'data-bootstro-step': 2
			});

			$('#map-span').addClass('bootstro').attr({
				'data-bootstro-title': 'Map',
				'data-bootstro-content': 'The map view provides an interactive view of active DSASweb geospatial elements.<br />These elements include shorelines, baselines, transects, intersections, and results.<br /><br />' +
					'Use the +/- buttons  in the upper left to change the zoom level of the map, or double click to zoom in.<br />To quickly zoom in, draw a bounding box with the mouse by holding down the shift key on your keyboard.',
				'data-bootstro-placement': 'left',
				'data-bootstro-html': true,
				'data-bootstro-step': 3
			});

			$('#stage-select-tablist >li a[href="#shorelines"]').addClass('bootstro').attr({
				'data-bootstro-title': 'Shorelines',
				'data-bootstro-content': 'Shorelines are geospatial polylines which represent the location of the shoreline at different times.<br />DSASweb uses the difference between these shorelines to calculate metrics of shoreline change.',
				'data-bootstro-placement': 'right',
				'data-bootstro-html': true,
				'data-bootstro-step': 4
			});

			$('#action-shorelines-tablist > li a[href="#shorelines-view-tab"]').addClass('bootstro').attr({
				'data-bootstro-title': 'Shorelines View',
				'data-bootstro-content': 'A existing set of shorelines selected from the view menu will be added to the active workspace.' +
					'Use the visibility toggles, or click on shorelines in the map to disable shorelines.',
				'data-bootstro-placement': 'right',
				'data-bootstro-html': true,
				'data-bootstro-step': 5
			});

			$('#action-shorelines-tablist > li a[href="#shorelines-manage-tab"]').addClass('bootstro').attr({
				'data-bootstro-title': 'Shorelines Manage',
				'data-bootstro-content': 'A set of shorelines can be added or removed from the view menu using the manage menu.',
				'data-bootstro-placement': 'top',
				'data-bootstro-html': true,
				'data-bootstro-step': 6
			});

			$('#shorelines-list').addClass('bootstro').attr({
				'data-bootstro-title': 'Individual Shorelines',
				'data-bootstro-content': 'Individual shorelines can be disabled, which will result in those shorelines being ignored during DSASweb calculations. ',
				'data-bootstro-placement': 'top',
				'data-bootstro-html': true,
				'data-bootstro-step': 7
			});

			$('#stage-select-tablist >li a[href="#baseline"]').addClass('bootstro').attr({
				'data-bootstro-title': 'Baseline',
				'data-bootstro-content': 'The baseline provides a local frame of reference for calculating metrics of shoreline change.',
				'data-bootstro-placement': 'right',
				'data-bootstro-html': true,
				'data-bootstro-step': 8
			});

			$('#action-baseline-tablist > li a[href="#baseline-view-tab"]').addClass('bootstro').attr({
				'data-bootstro-title': 'Baseline View',
				'data-bootstro-content': 'A baseline selected from the view menu will be added to the active workspace.',
				'data-bootstro-placement': 'right',
				'data-bootstro-html': true,
				'data-bootstro-step': 9
			});

			$('#action-baseline-tablist > li a[href="#baseline-manage-tab"]').addClass('bootstro').attr({
				'data-bootstro-title': 'Baseline Manage',
				'data-bootstro-content': '<div><div style="float:left;">A baseline can be added or removed<br />from the view menu using the manage menu.<br /><br />The manage tab also provides tools to draw <br />new baselines or clone and edit existing baselines.</div><img src="images/introduction_images/BaselineDraw.gif" /></div>',
				'data-bootstro-placement': 'bottom',
				'data-bootstro-html': true,
				'data-bootstro-step': 10
			});

			$('#stage-select-tablist >li a[href="#transects"]').addClass('bootstro').attr({
				'data-bootstro-title': 'Transects',
				'data-bootstro-content': 'Transects are cast perpendicular to the workspace baseline, and their intersections with shorelines are used to calculate metrics of shoreline change.',
				'data-bootstro-placement': 'right',
				'data-bootstro-html': true,
				'data-bootstro-step': 11
			});

			$('#action-transects-tablist > li a[href="#transects-view-tab"]').addClass('bootstro').attr({
				'data-bootstro-title': 'Transects View',
				'data-bootstro-content': 'An existing set of transects selected from the view menu will be added to the active workspace.',
				'data-bootstro-placement': 'right',
				'data-bootstro-html': true,
				'data-bootstro-step': 12
			});

			$('#action-transects-tablist > li a[href="#transects-manage-tab"]').addClass('bootstro').attr({
				'data-bootstro-title': 'Transects Manage',
				'data-bootstro-content': '<div><div style="float:left">A set of transects can be added<br />or removed from the view menu using the manage menu.<br /><br />The manage tab also provides tools to cast new transects<br />at user defined intervals or to edit existing transects.</div><img src="images/introduction_images/EditTransects.gif" /></div>',
				'data-bootstro-placement': 'bottom',
				'data-bootstro-html': true,
				'data-bootstro-step': 13
			});

			$('#stage-select-tablist >li a[href="#calculation"]').addClass('bootstro').attr({
				'data-bootstro-title': 'Review/Calculation',
				'data-bootstro-content': ' Review the workspace elements and specify parameters for the calculations. Calculate metrics of shoreline change.',
				'data-bootstro-placement': 'right',
				'data-bootstro-html': true,
				'data-bootstro-step': 14
			});

			$('#action-intersections-tablist > li a[href="#intersections-view-tab"]').addClass('bootstro').attr({
				'data-bootstro-title': 'Review/Calculation View',
				'data-bootstro-content': ' An existing set of intersections can be selected from the view menu will be added to the active workspace.',
				'data-bootstro-placement': 'right',
				'data-bootstro-html': true,
				'data-bootstro-step': 15
			});

			$('#action-intersections-tablist > li a[href="#intersections-manage-tab"]').addClass('bootstro').attr({
				'data-bootstro-title': 'Review/Calculation  Manage',
				'data-bootstro-content': 'Specify parameters for calculations, and begin DSASweb calculation algorithms. ',
				'data-bootstro-placement': 'right',
				'data-bootstro-html': true,
				'data-bootstro-step': 16
			});

			$('#stage-select-tablist >li a[href="#results"]').addClass('bootstro').attr({
				'data-bootstro-title': 'Results',
				'data-bootstro-content': 'Visualize and/or download metrics of shoreline change resulting from processing of DSASweb workspace elements as defined by the user.',
				'data-bootstro-placement': 'right',
				'data-bootstro-html': true,
				'data-bootstro-step': 17
			});

			$('#action-result-tablist > li a[href="#results-view-tab"]').addClass('bootstro').attr({
				'data-bootstro-title': 'Results View',
				'data-bootstro-content': 'An existing set of rates can be selected from the view menu will be added to the active workspace.<br />View the interactive plot for metrics of shoreline change.',
				'data-bootstro-placement': 'right',
				'data-bootstro-html': true,
				'data-bootstro-step': 18
			});

			$('#action-result-tablist > li a[href="#results-manage-tab"]').addClass('bootstro').attr({
				'data-bootstro-title': 'Results Manage',
				'data-bootstro-content': 'Download images, csv files, or shapefiles for the DSASweb calculation results.',
				'data-bootstro-placement': 'right',
				'data-bootstro-html': true,
				'data-bootstro-step': 19
			});
		},
		/**
		 * A user wishes to publish metadata. Create the form that allows the 
		 * uploading of this metadata
		 * 
		 * @returns {undefined}
		 */
		createMetadataUploadForm: function () {
			var container = $('<div />').addClass('container-fluid');

			var explanationRow = $('<div />').addClass('row-fluid').attr('id', 'md-explanation-row');
			var explanationWell = $('<div />').addClass('well well-small').attr('id', 'md-explanation-well');
			var explanationDiv = $('<div />').html('Using the metadata upload functionality you are able to quickly enter metadata associated with published resources');
			container.append(explanationWell.append(explanationRow.append(explanationDiv)));

			var formRow = $('<div />').addClass('row-fluid').attr('id', 'md-form-row');
			var formWell = $('<div />').addClass('well well-small').attr('id', 'md-form-well');
			var form = $('<form />').attr({
				id: 'md-form',
				action: 'service/publish',
				method: 'post',
				enctype: 'multipart/form-data',
				target: ''
			});

			var select = $('<select />').attr({
				'id': 'md-layers-select',
				'name': 'md-layers-select'
			});
			form.append(select);

			var options = $('.feature-list option[class="session-layer"]').toArray();
			for (var oIdx = 0; oIdx < options.length; oIdx++) {
				$(select).append(options[oIdx]);
			}

			form.append('Metadata XML ').append($('<input />').attr({
				'id': 'form-file-input',
				'type': 'file',
				'name': 'metadata',
				'size': '40'
			}));

			form.after('<br />');

			container.append(formRow.append(formWell.append(form)));

			CONFIG.ui.createModalWindow({
				headerHtml: 'Metadata Publish',
				doneButtonText: 'Cancel',
				buttons: [{
						text: 'Submit',
						callback: function () {
							var formData = new FormData();
							formData.append('md-layers-select', $('#md-layers-select').val());
							formData.append('metadata', $('#form-file-input').val());
							$.each($('#form-file-input')[0].files, function (i, file) {
								formData.append('metadata', file);
							});
							$.ajax({
								type: 'POST',
								url: 'service/publish',
								cache: false,
								contentType: false,
								processData: false,
								data: formData,
								dataType: 'json',
								success: function () {
									CONFIG.ows.getWMSCapabilities({
										callbacks: {
											success: [
												CONFIG.tempSession.updateLayersFromWMS,
												function (data, textStatus, jqXHR) {
													CONFIG.ui.work_stages_objects.each(function (stage) {
														stage.appInit();
														stage.populateFeaturesList(data, textStatus, jqXHR);
													});
												}
											],
											error: [
												// TODO: What do we do no error here?
											]
										}
									});
								}
							});
						}
					}
				],
				bodyHtml: container.html(),
				callbacks: []
			});
		},
		createMetadataEntryForm: function () {
			var sessionId = $('#session-management-session-list :selected').val();
			var session = CONFIG.permSession.session.sessions.find(function (s) {
				return s.id === sessionId;
			});

			var container = $('<div />').addClass('container-fluid');
			var explanationRow = $('<div />').addClass('row-fluid').attr('id', 'explanation-row');
			var explanationWell = $('<div />').addClass('well').attr('id', 'explanation-well');
			explanationWell.html('Provide a name and some optional metadata for this session<br /><br />Session: ' + session.id);
			container.append(explanationRow.append(explanationWell));

			var nameRow = $('<div />').addClass('row-fluid').attr('id', 'name-row');
			var nameWell = $('<div />').addClass('well').attr('id', 'name-well');
			var nameInputLabel = $('<label />').attr({
				'id': 'name-input-label',
				'for': 'name-input'
			}).html('Name:');
			var nameInput = $('<input>').attr({
				'id': 'name-input',
				'name': 'name-input',
				'type': 'text',
				'style': 'width:100%;',
				'placeholder': session.name
			});
			container.append(nameRow.append(nameWell.append(nameInputLabel, nameInput)));

			var metadataRow = $('<div />').addClass('row-fluid').attr('id', 'metadata-row');
			var metadataWell = $('<div />').addClass('well').attr('id', 'metadata-well');
			var metadataInputLabel = $('<label />').attr({
				'id': 'metadata-input-label',
				'for': 'metadata-input'
			}).html('Metadata:');
			var metadataInput = $('<input>').attr({
				'id': 'metadata-input',
				'name': 'metadata-input',
				'type': 'textarea',
				'style': 'width:100%;height:5em;',
				'maxLength': '4000',
				'rows': '10',
				'placeholder': session.metadata
			});
			container.append(metadataRow.append(metadataWell.append(metadataInputLabel, metadataInput)));

			CONFIG.ui.createModalWindow({
				bodyHtml: container.html(),
				buttons: [{
						id: 'save-metadata-button',
						type: 'btn-success',
						text: 'Save',
						callback: function () {
							var name = $('#name-input').val();
							var metadata = $('#metadata-input').val();
							session.name = name;
							session.metadata = metadata;
							CONFIG.permSession.save();
							CONFIG.tempSession.persistSession();
							CONFIG.tempSession.createSessionManagementModalWindow();
						}
					}],
				callbacks: [
					function () {
						$('#cancel-button').on('click', function () {
							CONFIG.tempSession.createSessionManagementModalWindow();
						});
					}
				]
			});
		}
	});
};
