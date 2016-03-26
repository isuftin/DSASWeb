/*global Util, LOG, CONFIG, Shorelines */
var CCH = window.CCH || {};
CCH.Session = function (name, isPerm) {
	"use strict";
	var me = (this === window) ? {} : this;
	me.MAX_SESSION_ID_LENGTH = 34;
	me.SESSION_OBJECT_NAME = name;
	me.newStage = Object.extended({
		shorelines: Object.extended({
			layers: [],
			viewing: [],
			groupingColumn: 'date',
			view: Object.extended({
				layer: Object.extended({
					'dates-disabled': []
				})
			})
		}),
		baseline: Object.extended({
			layers: [],
			viewing: ''
		}),
		transect_verification: Object.extended({
			layers: [],
			viewing: ''
		}),
		transects: Object.extended({
			layers: [],
			viewing: ''
		}),
		bias: Object.extended({
			layers: [],
			viewing: ''
		}),
		intersections: Object.extended({
			layers: [],
			viewing: ''
		}),
		calculation: Object.extended({
			layers: [],
			viewing: ''
		}),
		results: Object.extended({
			layers: [],
			viewing: ''
		})
	});
	me.isPerm = isPerm;
	me.sessionObject = isPerm ? localStorage : sessionStorage;
	me.session = isPerm ? $.parseJSON(me.sessionObject.getItem(me.SESSION_OBJECT_NAME)) : Object.extended();
	me.createNewSession = function () {
		// - A session has not yet been created for perm storage. Probably the first
		// run of the application or a new browser with no imported session

		// - Because the session is used in the namespace for WFS-T, it needs to 
		// not have a number at the head of it so add a random letter

		// - I need to remove any dashes from the random id and lowercase the entire
		// id. This is due to a back-end id length limitation
		var randID = String.fromCharCode(97 + Math.round(Math.random() * 25)) + Util.randomUUID().remove(/-/g).toLowerCase();
		// Prepare the session on the OWS server
		$.ajax('service/session/' + randID, {
			type: 'POST'
		}).done(function () {
			LOG.info('Session.js::init: A workspace has been prepared on the OWS server with the name of ' + randID);
			CONFIG.ui.showAlert({
				message: 'No session could be found. A new session has been created',
				displayTime: 0,
				style: {
					classes: ['alert-info']
				}
			});
		})
		.fail(function () {
			LOG.error('Session.js::init: A workspace could not be created on the OWS server with the name of ' + randID);
			CONFIG.ui.showAlert({
				message: 'No session could be found. A new session could not be created on server. This application may not function correctly.',
				style: {
					classes: ['alert-error']
				}
			});
		});

		var newSession = Object.extended();
		newSession.sessions = [];

		// This will constitute a new session object
		var session = Object.extended();
		session.id = randID;
		session.created = new Date().toString();
		session.stage = me.newStage;
		session.layers = [];
		session.results = Object.extended();
		session.name = randID;
		session.metadata = '';

		newSession.sessions.push(session);
		newSession.currentSession = randID;
		return newSession;
	};

	me.verifySession = function (sessionsObj) {
		var verifySessionId = function (sessionId) {
			// length check
			if (sessionId.length > me.MAX_SESSION_ID_LENGTH || sessionId.length === 0) {
				return false;
			}

			// legacy sessions contained '-' which we can't use anymore
			if (sessionId.indexOf('-') !== -1) {
				return false;
			}

			// case check - must all be lowercase
			for (var cIdx = 0; cIdx < sessionId.length; cIdx++) {
				var sessionIdChar = sessionId.charAt(cIdx);
				if (isNaN(sessionIdChar) && sessionIdChar === sessionIdChar.toUpperCase()) {
					return false;
				}
			}

			return true;
		};
		return verifySessionId(sessionsObj.id);
	};

	if (isPerm) {
		if (!me.session) {
			me.session = me.createNewSession();
		}

		var invalidSessions = [];
		$.each(me.session.sessions, function (i, session) {
			if (!me.verifySession(session)) {
				invalidSessions.push(session.id);
			}
		});

		if (invalidSessions.length) {
			throw {
				message: 'Invalid Sessions Found',
				func: function () {
					// Session will need to be removed
					$.get('templates/remove-session-modal.mustache')
						.done(function (data) {
							var modalId = 'modal-session-invalid-remove',
								template = Handlebars.compile(data),
								modalHtml = template({
									modalId: modalId,
									sessionList: invalidSessions,
									includesCurrentId: invalidSessions.indexOf(me.session.currentSession) !== -1
								});
							$('#application-overlay').fadeOut();
							$('body').append($(modalHtml));
							$('#modal-session-invalid-remove').modal();
							$('#btn-session-remove').click(function () {
								var permSession = $.parseJSON(me.sessionObject[me.SESSION_OBJECT_NAME]),
									cleanedSessions = [];

								for (var sIdx in permSession.sessions) {
									var session = permSession.sessions[sIdx];
									if (invalidSessions.indexOf(session.id) === -1) {
										cleanedSessions.push(session);
									}
								}
								permSession.sessions = cleanedSessions;

								if (permSession.sessions.indexOf(permSession.currentSession) === -1) {
									permSession.currentSession = '';
								}
								me.sessionObject.setItem(me.SESSION_OBJECT_NAME, JSON.stringify(permSession));
								localStorage.removeItem('dsas');
								location.reload(true);
							});

						});
				}
			};
		}

	} else {
		LOG.info('Session.js::constructor:Removing previous temp session');
		me.sessionObject.removeItem(me.SESSION_OBJECT_NAME);

		LOG.info('Session.js::constructor:Saving new temp session');
		me.session = CONFIG.permSession.session.sessions.find(function (session) {
			return session.id === CONFIG.permSession.session.currentSession;
		});

		me.sessionObject.setItem(me.SESSION_OBJECT_NAME, JSON.stringify(me.session));

		me.namespace = Object.extended();

		/**
		 * Persist the temp session to the appropriate location in the permanent session 
		 */
		me.persistSession = function () {
			LOG.info('Session.js::persistSession: Persisting temp session to perm session');
			var permSession = CONFIG.permSession;
			var sessionIndex = permSession.session.sessions.findIndex(function (session) {
				return session.id === me.session.id;
			});

			permSession.session.currentSession = me.session.id;
			if (sessionIndex === -1) {
				permSession.session.sessions.push(me.session);
			} else {
				permSession.session.sessions[sessionIndex] = me.session;
			}

			permSession.save();
			me.save();
		};

		me.getStage = function (stage) {
			//for backward compatibility, if existing sessions don't have the requested stage, try to attach it from the newStage object
			if (!me.session.stage[stage]) {
				me.session.stage[stage] = me.newStage[stage];
			}
			return me.session.stage[stage];
		};

		me.setStage = function (args) {
			if (!args) {
				return null;
			}
			var stage = args.stage;
			me.session[stage] = args.obj;
		};

		me.getConfig = function (args) {
			if (!args) {
				return null;
			}
			var stage = args.stage;
			var config = me.getStage(stage);

			return config;
		};

		me.setConfig = function (args) {
			if (!args) {
				args = Object.extended();
			}
			var config = args.config || 'default';
			var stage = args.stage || 'default';
			me.session[stage][config.name] = config;
			me.persistSession();
			return me.session[stage][config.name];
		};

		me.updateSessionFromWMS = function (args) {
			var wmsCapabilities = CONFIG.ows.wmsCapabilities,
				currentSessionKey = CONFIG.tempSession.getCurrentSessionKey(),
				stage = args.stage,
				suffixes = stage.suffixes,
				sessionStage = CONFIG.tempSession.getStage(stage.stage);
		
			wmsCapabilities.keys().each(function (layerNS) {
				var cap = wmsCapabilities[layerNS];
				var layers = cap.capability.layers;

				if (layers.length) {
					layers.each(function (layer) {
						var title = layer.title;
						if (layerNS === CONFIG.name.published || layerNS === CONFIG.name.proxydatumbias || layerNS === currentSessionKey) {
							var type = title.substr(title.lastIndexOf('_'));
							if (suffixes.length === 0 || suffixes.indexOf(type.toLowerCase()) !== -1) {
								var layerFullName = layer.prefix + ':' + layer.name;
								var lIdx = sessionStage.layers.findIndex(function (l) {
									return l === layerFullName;
								});
								if (lIdx === -1) {
									sessionStage.layers.push(layerFullName);
								}
								CONFIG.tempSession.persistSession();
							}
						}
					});
				} else {
					sessionStage.layers.remove(function (l) {
						return l.substring(0, l.indexOf(':')) === layerNS;
					});
					CONFIG.tempSession.persistSession();
				}
			});
		};

		me.updateLayersFromWMS = function (args) {
			LOG.debug('Session.js::updateLayersFromWMS');

			var wmsCapabilities = args.wmsCapabilities;
			var jqXHR = args.jqXHR;

			if (jqXHR.status !== 200) {
				LOG.warn('Session.js::updateLayersFromWMS: Client was unable to attain WMS capabilities');
			}

			if (wmsCapabilities && wmsCapabilities.capability.layers.length) {
				LOG.trace('Session.js::updateLayersFromWMS: Updating session layer list from WMS Capabilities');

				var wmsLayers = wmsCapabilities.capability.layers;
				var namespace = wmsLayers[0].prefix;
				var sessionLayers = me.session.layers.filter(function (n) {
					return n.prefix === namespace;
				});

				if (namespace === me.getCurrentSessionKey() || namespace === CONFIG.name.published) {
					LOG.trace('Session.js::updateLayersFromWMS: Scanning session for expired/missing layers in the ' + namespace + ' prefix');
					sessionLayers.each(function (sessionLayer, index) {
						if (sessionLayer.name.indexOf(me.getCurrentSessionKey() > -1)) {
							var foundLayer = wmsLayers.find(function (wmsLayer) {
								return wmsLayer.name === sessionLayer.name;
							});

							if (!foundLayer) {
								LOG.info('Session.js::updateLayersFromWMS: Removing layer ' + sessionLayer.name + ' from session object. This layer is not found on the OWS server');
								me.session.layers[index] = null;
							}
						}
					});

					// Removes all undefined or null from the layers array
					me.session.layers = me.session.layers.compact();

					LOG.trace('Session.js::updateLayersFromWMS: Scanning layers on server for layers in this session');
					var ioLayers = wmsLayers.findAll(function (wmsLayer) {
						return (wmsLayer.prefix === 'ch-input' || wmsLayer.prefix === 'ch-output') &&
							wmsLayer.name.indexOf(me.getCurrentSessionKey() !== -1);
					});

					$.each(ioLayers, function (index, layer) {
						LOG.info('Session.js::updateLayersFromWMS: Remote layer found. Adding it to current session');
						var incomingLayer = {
							name: layer.name,
							title: layer.title,
							prefix: layer.prefix,
							bbox: layer.bbox
						};

						var foundLayerAtIndex = me.session.layers.findIndex(function (l) {
							return l.name === layer.name;
						});

						if (foundLayerAtIndex !== -1) {
							LOG.trace('Session.js::updateLayersFromWMS: Layer ' +
								'provided by WMS GetCapabilities response already in session layers. ' +
								'Updating session layers with latest info.');
							me.session.layers[foundLayerAtIndex] = incomingLayer;
						} else {
							LOG.info('Session.js::updateLayersFromWMS: Layer ' +
								'provided by WMS GetCapabilities response not in session layers. ' +
								'Adding layer to session layers.');
							me.addLayerToSession(incomingLayer);
						}
					});
				}
				
				// Special case for Shorelines 
				var sessionKey = me.getCurrentSessionKey();
				var sessionShorelineLayer = me.session.layers.find(function (l) {
					return l.name === sessionKey + "_shorelines";
				});
				if (sessionShorelineLayer) {
					me.session.shorelines.layers.push(sessionShorelineLayer.prefix + ":" + sessionShorelineLayer.name);
					me.session.shorelines.layers = me.session.shorelines.layers.unique();
				}
			} else {
				LOG.info('Session.js::updateLayersFromWMS: Could not find any layers for this session. Removing any existing layers in session object');
				if (args.context && args.context.namespace) {
					var removalCandidates = me.session.layers.filter(function (l) {
						return l.prefix === args.context.namespace;
					});
					if (removalCandidates.length) {
						removalCandidates.forEach(function (c) {
							me.session.layers.remove(c);
						});
					}
				}
			}

			me.persistSession();
		};

		me.addLayerToSession = function (args) {
			LOG.debug('Session.js::addLayerToSession:Adding layer to session');
			var layer = args.layer;
			var sessionLayer = Object.extended({
				name: args.name || layer.name,
				title: args.title || layer.title,
				prefix: args.prefix || layer.prefix,
				bbox: args.bbox || layer.bbox,
				keywords: args.keywords || layer.keywords
			});

			var lIndex = me.session.layers.findIndex(function (l) {
				return l.name === sessionLayer.name;
			});

			if (lIndex !== -1) {
				me.session.layers[lIndex] = sessionLayer;
			} else {
				me.session.layers.push(sessionLayer);
			}
		};

		/**
		 * Replace the current temp session 
		 * @param {type} key
		 * @param {type} session
		 * @returns {undefined}
		 */
		me.setCurrentSession = function (key, session) {
			LOG.info('Replacing current session');
			if (session) {
				me.session = session;
			} else {
				me.session = CONFIG.permSession.session.sessions.find(function (session) {
					return session.id === CONFIG.permSession.session.currentSession;
				});
			}
			me.save();
		};

		me.getDisabledShorelines = function (workspace) {
			return me.session.stage[Shorelines.stage].slDisabled[workspace];
		};
		me.addDisabledShoreline = function (workspace, id) {
			if (!me.isShorelineDisabled(workspace, id)) {
				me.session.stage[Shorelines.stage].slDisabled[workspace].push(id);
			}
			return me.session.stage[Shorelines.stage].slDisabled[workspace];
		};
		me.addDisabledShorelines = function (workspace, ids) {
			for (var idIdx = 0;idIdx < ids.length;idIdx++) {
				me.addDisabledShoreline(workspace, ids[idIdx]);
			}
			return me.session.stage[Shorelines.stage].slDisabled[workspace];
		};
		me.removeDisabledShoreline = function (workspace, id) {
			me.session.stage[Shorelines.stage].slDisabled[workspace].remove(id);
			return me.session.stage[Shorelines.stage].slDisabled[workspace];
		};
		me.isShorelineDisabled = function (workspace, id) {
			return me.session.stage[Shorelines.stage].slDisabled[workspace].indexOf(id) !== -1;
		};

		me.getDisabledDates = function () {
			return me.session.stage[Shorelines.stage].datesDisabled;
		};
		me.setDisabledDates = function (dates) {
			if (Array.isArray(dates)) {
				me.session.stage[Shorelines.stage].datesDisabled = dates;
			}
			return me.getDisabledDates();
		};
		me.addDisabledDate = function (date) {
			if (me.session.stage[Shorelines.stage].datesDisabled.indexOf(date) === -1) {
				me.session.stage[Shorelines.stage].datesDisabled.push(date);
			}
		};
		me.removeDisabledDate = function (date) {
			me.session.stage[Shorelines.stage].datesDisabled.remove(date);
		};
		me.isDateDisabled = function (date) {
			return me.session.stage[Shorelines.stage].datesDisabled.indexOf(date) !== -1;
		};

	}

	return $.extend(me, {
		/**
		 * Creates the session management window. Makes a call to the server to get 
		 * OpenID session information.
		 * 
		 * @returns {undefined}
		 */
		createSessionManagementModalWindow: function () {
			var container = $('<div />').addClass('container-fluid');
			var menuNavBar = $('<div />').addClass('navbar navbar-static');
			var innerNavBar = $('<div />').addClass('navbar-inner');
			var navBarItem = $('<ul />')
				.addClass('nav')
				.attr({
					'role': 'navigation'
				});
			var fileDropDown = $('<li />').addClass('dropdown');
			var fileDropDownLink = $('<a />').attr({
				'id': 'file-drop-down',
				'data-toggle': 'dropdown',
				'role': 'button',
				'href': '#'
			})
				.html('File')
				.addClass('dropdown-toggle')
				.append($('<b />').addClass('caret'));
			container.append(menuNavBar.append(innerNavBar.append(navBarItem.append(fileDropDown.append(fileDropDownLink)))));

			var sessionDropDown = $('<li />').addClass('dropdown');
			var sessionDropDownLink = $('<a />').attr({
				'id': 'session-drop-down',
				'data-toggle': 'dropdown',
				'role': 'button',
				'href': '#'
			})
				.html('Session')
				.addClass('dropdown-toggle')
				.append($('<b />').addClass('caret'));
			container.append(menuNavBar.append(innerNavBar.append(navBarItem.append(sessionDropDown.append(sessionDropDownLink)))));

			var fileDropDownList = $('<ul />')
				.addClass('dropdown-menu')
				.attr({
					'aria-labelledby': 'file-drop-down'
				});

			var importli = $('<li />').attr('role', 'presentation')
				.append($('<a />').attr({
					'tabindex': '-1',
					'role': 'menuitem',
					'id': 'file-menu-item-import'
				}).html('Import'));

			var exportli = $('<li />').attr('role', 'presentation')
				.append($('<a />').attr({
					'tabindex': '-1',
					'role': 'menuitem',
					'id': 'file-menu-item-export'
				}).html('Export'));
			fileDropDownList.append(importli, exportli);
			fileDropDown.append(fileDropDownList);

			var sessionDropDownList = $('<ul />')
				.addClass('dropdown-menu')
				.attr({
					'aria-labelledby': 'session-drop-down',
					'id': 'session-drop-down-list'
				});
			var createli = $('<li />').attr('role', 'presentation')
				.append($('<a />').attr({
					'tabindex': '-1',
					'role': 'menuitem',
					'id': 'session-menu-item-create'
				}).html('Create New'));
			var clearAllli = $('<li />').attr('role', 'presentation')
				.append($('<a />').attr({
					'tabindex': '-1',
					'role': 'menuitem',
					'id': 'session-menu-item-clear-all'
				}).html('Clear All'));
			var setCurrentli = $('<li />').attr('role', 'presentation')
				.append($('<a />').attr({
					'tabindex': '-1',
					'role': 'menuitem',
					'id': 'session-menu-item-set-current'
				}).html('Set Current'));
			var setMetadatatli = $('<li />').attr('role', 'presentation')
				.append($('<a />').attr({
					'tabindex': '-1',
					'role': 'menuitem',
					'id': 'session-menu-item-set-metadata'
				}).html('Provide Metadata'));
			sessionDropDownList.append(createli, clearAllli, setCurrentli, setMetadatatli);
			sessionDropDown.append(sessionDropDownList);

			var explanationRow = $('<div />').addClass('row-fluid').attr('id', 'explanation-row');
			var explanationWell = $('<div />').addClass('well well-small').attr('id', 'explanation-well');
			var explanationDiv = $('<div />').html('In the session management section, you are able to maniupulate your current session set, export single sessions and import a new session set<br />While it isn\'t mandatory to do so, it is strongly suggested to reload the application after switching sessions or creating a new session');
			container.append(explanationWell.append(explanationRow.append(explanationDiv)));

			var sessionListWell = $('<div />').addClass('well well-small');
			var sessionListRow = $('<div />').attr('id', 'session-management-session-list-row').addClass('row-fluid');
			var sessionList = $('<select />').attr({
				'style': 'width:100%;',
				'id': 'session-management-session-list'
			});
			CONFIG.permSession.session.sessions.each(function (session) {
				sessionList.append(
					$('<option />').attr({
					'value': session.id
				}).html(session.id));
			});
			container.append(sessionListWell.append(sessionListRow.append(sessionList)));

			var importDescriptionWell = $('<div />').addClass('well well-small');
			var importDescriptionRow = $('<div />').attr('id', 'session-management-session-description-row').addClass('row-fluid');
			container.append(importDescriptionWell.append(importDescriptionRow));

			CONFIG.ui.createModalWindow({
				headerHtml: 'Session Management',
				bodyHtml: container.html(),
				callbacks: [
					function () {
						CONFIG.ui.bindSignInImageMouseEvents();

						$('#file-menu-item-import').on('click', function () {
							CONFIG.tempSession.importSession();
						});
						$('#file-menu-item-export').on('click', function () {
							CONFIG.tempSession.exportSession();
						});
						$('#session-menu-item-create').on('click', function () {
							var session = CONFIG.tempSession.createNewSession().sessions[0];
							CONFIG.permSession.session.sessions.push(session);
							CONFIG.permSession.session.currentSession = session.id;
							CONFIG.permSession.save();
							CONFIG.tempSession.createSessionManagementModalWindow();
						});
						$('#session-menu-item-clear-all').on('click', CONFIG.tempSession.clearSessions);
						$('#session-menu-item-set-current').on('click', function () {
							var id = $('#session-management-session-list').val();
							CONFIG.permSession.session.currentSession = id;
							CONFIG.permSession.save();
							CONFIG.tempSession.createSessionManagementModalWindow();
						});
						$('#session-menu-item-set-metadata').on('click', function () {
							CONFIG.ui.createMetadataEntryForm();
						});

					},
					function () {
						var sessionList = $('#session-management-session-list');
						sessionList.on('change', function () {
							var key = this.value;
							var importDescriptionRow = $('#session-management-session-description-row');
							importDescriptionRow.html('');
							var session = CONFIG.permSession.session.sessions.find(function (s) {
								return s.id === key;
							});
							var sessionLayers = CONFIG.permSession.session.sessions.find(function (s) {
								return s.id === key;
							}).layers.filter(function (l) {
								return l.prefix === key;
							});
							var html = 'Session Information' +
								'<br />Name: ' + (session.name || '') +
								'<br />Created: ' + session.created +
								'<br />Is Current: ' + (key === CONFIG.permSession.session.currentSession ? 'true' : 'false') +
								'<br />Layers: ' + sessionLayers.length +
								'<br />Results: ' + Object.values(session.results).length +
								'<br />Metadata: ' + (session.metadata || '');

							importDescriptionRow.html(html);
						});
						sessionList.val(CONFIG.permSession.session.currentSession);
						sessionList.trigger('change');
					}
				]
			});
		},
		importSession: function () {
			if (window.File && window.FileReader) {
				var container = $('<div />').addClass('container-fluid');
				var explanationRow = $('<div />').addClass('row-fluid').attr('id', 'explanation-row');
				var explanationWell = $('<div />').addClass('well').attr('id', 'explanation-well');
				explanationWell.html('Import a session filepreviously exported from this application');
				container.append(explanationRow.append(explanationWell));

				var selectionRow = $('<div />').addClass('row-fluid').attr('id', 'file-upload-row');
				var uploadForm = $('<form />');
				var fileInput = $('<input />').attr({
					'id': 'file-upload-input',
					'name': 'file-upload-input',
					'type': 'file'
				});
				container.append(selectionRow.append(uploadForm.append(fileInput)));

				var importWell = $('<div />').addClass('well').attr('id', 'import-well');
				var importRow = $('<div />').addClass('row-fluid').attr('id', 'import-row');
				container.append(importWell.append(importRow));

				CONFIG.ui.createModalWindow({
					headerHtml: 'Import A Session File',
					bodyHtml: container.html(),
					callbacks: [
						function () {
							$('#file-upload-input').on('change', function (event) {
								var fileObject = event.target.files[0];
								if (fileObject.type.match('json')) {
									var reader = new FileReader();
									var importWell = $('#import-well');
									var importRow = $('#import-row');
									var resultObject;
									importRow.empty();
									reader.onloadend = function (event) {
										try {
											resultObject = $.parseJSON(event.target.result); // This will not work with 
											var importDisplay = $('<div />').addClass('span12');
											var currentId = resultObject.currentSession;
											if (!currentId) {
												importRow.html('Imported session object has no current session');
												return;
											}

											var session = resultObject.sessions.find(function (s) {
												return s.id === currentId;
											});
											if (!session) {
												importRow.html('Imported session has a nonexistent session marked as current');
												return;
											}

											importDisplay.append('Session File Information');
											importDisplay.append('<br />Sessions found: ' + resultObject.sessions.length);
											importDisplay.append('<br />Current session key: ' + currentId);
											importDisplay.append('<br />Session created: ' + session.created);
											importDisplay.append('<br />Layers found: ' + session.layers.length + '<br />');
											importDisplay.append(
												$('<button />')
												.attr('id', 'import-all-session-button')
												.addClass('btn btn-success span6')
												.html('Import Sessions'));

											importRow.append(importDisplay);
											importWell.append(importRow);
											$('#import-all-session-button').on('click', function () {
												var chObj = JSON.parse(localStorage.getItem(me.SESSION_OBJECT_NAME));
												resultObject.sessions.each(function (ros) {
													var foundSession = chObj.sessions.find(function (s) {
														return s.id === ros.id;
													});
													if (!foundSession) {
														chObj.sessions.push(ros);
														CONFIG.permSession.session.sessions.push(ros);
													}
												});

												localStorage.setItem(me.SESSION_OBJECT_NAME, JSON.stringify(chObj)); //TODO- This will not work with IE8 and below. No JSON object
												CONFIG.tempSession.createSessionManagementModalWindow();
											});
										} catch (ex) {
											importRow.html('Your file could not be read: ' + ex);
											return;
										}
									};

									try {
										reader.readAsText(fileObject);
									} catch (ex) {
										importRow.html('Your file could not be read: ' + ex);
										return;
									}
								} else {
									// Not a json file
									$('#file-upload-input').val('');
									$('#import-row').html('Your file could not be read');
								}
							});
						}
					]
				});
			} else {
				CONFIG.ui.showAlert({
					message: 'Functionality not yet supported for non-HTML5 browsers'
				});
			}
		},
		exportSession: function () {
			CONFIG.tempSession.persistSession();
			var exportForm = $('<form />').attr({
				'id': 'export-form',
				'style': 'display:none;visibility:hidden;',
				'method': 'POST'
			}).append(
				$('<input />').attr({
				'type': 'hidden',
				'name': 'filename'
			}).val('cch_session_' + me.getCurrentSessionKey() + '.json'))
				.append(
					$('<input />').attr({
					'type': 'hidden',
					'name': 'encoding'
				}).val('UTF-8'))
				.append(
					$('<input />').attr({
					'type': 'hidden',
					'name': 'data'
				}).val(localStorage[me.SESSION_OBJECT_NAME]));
			$('body').append(exportForm);
			exportForm.attr('action', 'service/export');
			exportForm.submit();
			exportForm.remove();
		},
		save: function () {
			LOG.info('Session.js::save:Saving session object to storage');
			me.sessionObject.setItem(me.SESSION_OBJECT_NAME, JSON.stringify(me.session));
		},
		load: function (name) {
			LOG.info('Session.js::load:Loading session object from storage');
			$.parseJSON(me.sessionObject.getItem(name ? name : me.SESSION_OBJECT_NAME));
		},
		getCurrentSessionKey: function () {
			if (me.isPerm) {
				return me.session.currentSession;
			} else {
				return me.session.id;
			}
		},
		getCurrentSession: function () {
			return me.session;
		},
		removeSession: function (sessionId) {
			return $.ajax('service/session/' + sessionId, {
				type : 'DELETE'
			});
		},
		updateSession: function (sessionId) {
			return $.ajax('service/session/' + sessionId, {
				type : 'PUT'
			});
		},
		clearSessions: function (type) {
			type = type || '';
			var workspaces = JSON.parse(localStorage.getItem('dsas')).sessions.map(function (a) {
				return a.id;
			});
			var deleteCalls = [];
			for (var wIdx = 0;wIdx < workspaces.length;wIdx++) {
				deleteCalls.push($.ajax('service/session/' + workspaces[wIdx], {type: 'DELETE'}));
			}

			$.when
					.apply($, deleteCalls)
					.then(function () {
						location.reload(true);
					});
			localStorage.removeItem(me.SESSION_OBJECT_NAME);
			sessionStorage.removeItem(me.SESSION_OBJECT_NAME);
			LOG.warn('UI.js::Cleared ' + type + ' session. Reloading application.');
			
		},
		removeResource: function (args) {
			var store = args.store,
				layer = args.layer,
				callbacks = args.callbacks || [],
				workspace = args.session || CONFIG.tempSession.getCurrentSessionKey(),
				extraParams = args.extraParams || {},
				params = $.extend({}, {
					type: 'DELETE',
					context : this
				}, extraParams);

			if (workspace.toLowerCase() === CONFIG.name.published) {
				throw 'Workspace cannot be read-only (Ex.: ' + CONFIG.name.published + ')';
			}

			$.ajax('service/layer/workspace/' + workspace + '/store/' + store + '/' + layer,
					params)
					.done(function (data, textStatus, jqXHR) {
						callbacks.each(function (callback) {
							callback(data, textStatus, jqXHR);
						});
					});
		}
	});
};
