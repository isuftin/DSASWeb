/*global LOG */
/*global CONFIG */
/*global OpenLayers */
/*global Shorelines */
var OWS = function (endpoint) {
	"use strict";
	LOG.info('OWS.js::constructor: OWS class is initializing.');
	var me = (this === window) ? {} : this;

	me.importEndpoint = 'service/import/shapefile';
	me.geoserverEndpoint = endpoint ? endpoint : CONFIG.geoServerEndpoint;
	me.geoserverProxyEndpoint = 'geoserver/';
	me.wfsGetCapsUrl = me.geoserverProxyEndpoint + 'ows?service=wfs&version=1.1.0&request=GetCapabilities';
	me.wfsGetFeature = me.geoserverProxyEndpoint + 'ows?service=wfs&version=1.0.0&request=GetFeature';
	me.wfsCapabilities = Object.extended();
	me.wmsCapabilities = Object.extended();
	me.wpsExecuteRequestPostUrl = me.geoserverProxyEndpoint + 'ows?service=wps&version=1.0.0&request=execute';

	// An object to hold the return from WFS DescribeFeatureType
	me.featureTypeDescription = Object.extended();

	// An object to hold the return of a filtered WFS getFeature response
	me.filteredFeature = Object.extended();

	LOG.debug('OWS.js::constructor: OWS class initialized.');
	return $.extend(me, {
		importFile: function (args) {
			LOG.info('OWS.js::importFile: Importing file into OWS resource');
			$.ajax(me.importEndpoint, {
				context: args.context || this,
				method: 'POST',
				data: {
					'file-token': args['file-token'],
					'feature-name': args.importName,
					'workspace': args.workspace,
					'store': args.store || 'ch-input',
					'extra-column': args.extraColumn
				},
				success: function (data) {
					var scope = this;
					$(args.callbacks).each(function (index, callback) {
						callback(data, scope);
					});
				}
			});
		},
		downloadLayerAsShapefile: function (layer) {
			var workspace = layer.split(':')[0];

			window.location = 'geoserver/' +
					workspace +
					'/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=' +
					layer +
					'&outputFormat=shape-zip';
		},
		getUTMZoneCount: function (args) {
			var layerPrefix = args.layerPrefix;
			var layerName = args.layerName;
			var layerFullName = layerPrefix + ':' + layerName;
			var callbacks = args.callbacks || [];
			var context = args.context || this;
			var identifier = 'gs:UTMZoneCount';
			if (!layerPrefix || !layerName) {
				return;
			}

			var request = '<?xml version="1.0" encoding="UTF-8"?>' +
					'<wps:Execute version="1.0.0" service="WPS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.opengis.net/wps/1.0.0" xmlns:wfs="http://www.opengis.net/wfs" xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" xmlns:wcs="http://www.opengis.net/wcs/1.1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">' +
					'<ows:Identifier>' + identifier + '</ows:Identifier>' +
					'<wps:DataInputs>' +
					'<wps:Input>' +
					'<ows:Identifier>features</ows:Identifier>' +
					'<wps:Reference mimeType="text/xml" xlink:href="http://geoserver/wfs" method="POST">' +
					'<wps:Body>' +
					'<wfs:GetFeature service="WFS" version="1.0.0" outputFormat="GML2" xmlns:' + layerPrefix + '="gov.usgs.cida.ch.' + layerPrefix + '">' +
					'<wfs:Query typeName="' + layerFullName + '"/>' +
					'</wfs:GetFeature>' +
					'</wps:Body>' +
					'</wps:Reference>' +
					'</wps:Input>' +
					'</wps:DataInputs>' +
					'<wps:ResponseForm>' +
					'<wps:RawDataOutput>' +
					'<ows:Identifier>count</ows:Identifier>' +
					'</wps:RawDataOutput>' +
					'</wps:ResponseForm>' +
					'</wps:Execute>';

			CONFIG.ows.executeWPSProcess({
				processIdentifier: identifier,
				request: request,
				callbacks: callbacks,
				context: context
			});

		},
		getWMSCapabilities: function (args) {
			LOG.info('OWS.js::getWMSCapabilities');
			var namespace = args.namespace || 'ows';
			var url = me.geoserverProxyEndpoint + namespace + '/wms?service=wms&version=1.3.0&request=GetCapabilities&cb=' + new Date().getTime();
			var callbacks = args.callbacks || {};
			var sucessCallbacks = callbacks.success || [];
			var errorCallbacks = callbacks.error || [];
			sucessCallbacks.push(function () {
				LOG.debug('OnReady.js:: WMS Capabilities retrieved for your session');
			});
			errorCallbacks.push(function () {
				LOG.warn('OnReady.js:: There was an error in retrieving the WMS capabilities for your session. This is probably be due to a new session. Subsequent loads should not see this error');
			});

			LOG.debug('OWS.js::getWMSCapabilities: A request is being made for GetCapabilities for the namespace: ' + namespace);
			return $.ajax(url, {
				context: args,
				success: function (data, textStatus, jqXHR) {
					var getCapsResponse = new OpenLayers.Format.WMSCapabilities.v1_3_0().read(data);

					// Fixes an issue with prefixes not being parsed correctly from response
					getCapsResponse.capability.layers.each(function (n) {
						n.prefix = namespace;
					});
					me.wmsCapabilities[namespace] = getCapsResponse;
					getCapsResponse.capability.layers.each(function (layer) {
						CONFIG.tempSession.addLayerToSession(layer);
					});
					CONFIG.tempSession.persistSession();

					sucessCallbacks.each(function (callback) {
						callback({
							wmsCapabilities: getCapsResponse,
							data: data,
							textStatus: textStatus,
							jqXHR: jqXHR,
							context: args
						});
					});
				},
				error: function (data, textStatus, jqXHR) {
					// TODO - This really should be moved to the session object
					if (this.namespace === CONFIG.tempSession.getCurrentSessionKey() && jqXHR.toLowerCase() === 'not found') {
						CONFIG.ui.showAlert({
							message: 'Current session was not found on server. Attempting to initialize session on server.',
							displayTime: 0
						});
						// First clean up everything on the server, then create a session with this key.
						$.ajax('service/session/' + this.namespace, {type: 'DELETE'})
								.then($.proxy(function () {
									$.ajax('service/session/' + this.namespace,
											{
												type: 'POST',
												success: function (data, textStatus, jqXHR) {
													LOG.info('Session.js::init: A workspace has been prepared on the OWS server with the name of ' + CONFIG.tempSession.getCurrentSessionKey());
													CONFIG.ui.showAlert({
														message: 'Your session has been created on the server',
														displayTime: 7500,
														style: {
															classes: ['alert-info']
														}
													});
													$(errorCallbacks).each(function (index, callback) {
														callback({
															data: data,
															textStatus: textStatus,
															jqXHR: jqXHR
														});
													});
												},
												error: function () {
													LOG.error('Session.js::init: A workspace could not be created on the OWS server');
													CONFIG.ui.showAlert({
														message: 'No session could be found. A new session could not be created on server. This application may not function correctly.',
														style: {
															classes: ['alert-error']
														}
													});
												}
											})
								}, this));
					} else {
						errorCallbacks.each(function (callback) {
							callback({
								data: data,
								textStatus: textStatus,
								jqXHR: jqXHR,
								context: args
							});
						});
					}
				}
			});
		},
		getWFSCapabilities: function (args) {
			LOG.info('OWS.js::getWFSCapabilities');
			args = args || {};

			var callbacks = args.callbacks || {},
					sucessCallbacks = callbacks.success || [],
					errorCallbacks = callbacks.error || [];

			$.ajax(me.wfsGetCapsUrl, {
				context: args,
				success: function (data) {
					var getCapsResponse = new OpenLayers.Format.WFSCapabilities.v1_1_0().read(data);
					me.wfsCapabilities = getCapsResponse;
					$.each(sucessCallbacks, function (index, callback) {
						callback(getCapsResponse, this);
					});
				},
				error: function (data, textStatus, jqXHR) {
					$.each(errorCallbacks, function (i, callback) {
						callback({
							data: data,
							textStatus: textStatus,
							jqXHR: jqXHR,
							context: args
						});
					});
				}
			});
		},
		getFeatureByName: function (name) {
			return me.wfsCapabilities.featureTypeList.featureTypes.find(function (featureType) {
				return featureType.name === name;
			});
		},
		getLayerByName: function (args) {
			var ns = args.layerNS;
			var name = args.layerName;
			return me.wmsCapabilities[ns].capability.layers.find(function (layer) {
				return layer.name === name;
			});
		},
		getLayerPropertiesFromWFSDescribeFeatureType: function (args) {
			LOG.info('OWS.js::getLayerPropertiesFromWFSDescribeFeatureType');
			LOG.debug('OWS.js::getLayerPropertiesFromWFSDescribeFeatureType: Parsing WFS describe feature type response for properties');

			var describeFeatureType = args.describeFeatureType;
			var includeGeom = args.includeGeom || false;
			var result = new Object.extended();

			LOG.debug('OWS.js::getLayerPropertiesFromWFSDescribeFeatureType: Will attempt to parse ' + describeFeatureType.featureTypes.length + ' layers');
			$(describeFeatureType.featureTypes).each(function (i, featureType) {

				// For each layer, initilize a property array for it in the result object
				result[featureType.typeName] = [];

				LOG.trace('OWS.js::getLayerPropertiesFromWFSDescribeFeatureType: Will attempt to parse ' + featureType.properties.length + ' layer properties');
				$(featureType.properties).each(function (i, property) {

					if (!includeGeom) {
						// Pulling down geometries is not required and can make the document huge 
						// So grab everything except the geometry object(s)
						if (property.type !== "gml:MultiLineStringPropertyType" && property.type !== "gml:MultiCurvePropertyType" && property.name !== 'the_geom') {
							result[featureType.typeName].push(property.name);
						}
					} else {
						result[featureType.typeName].push(property.name);
					}
				});
			});
			return result;
		},
		getDescribeFeatureType: function (args) {
			LOG.info('OWS.js::getDescribeFeatureType: WFS featureType requested for feature ' + args.layerName);
			var layerNS = args.layerNS;
			var layerName = args.layerName;
			var url = me.geoserverProxyEndpoint + layerNS + '/wfs?service=wfs&version=2.0.0&request=DescribeFeatureType&typeName=' + layerNS + ':' + layerName;
			$.ajax(url, {
				context: args.scope || this,
				success: function (data) {
					LOG.info('OWS.js::getDescribeFeatureType: WFS featureType response received.');
					var gmlReader = new OpenLayers.Format.WFSDescribeFeatureType();
					var describeFeaturetypeRespone = gmlReader.read(data);
					
					// Make sure that I don't have an error. If there's an error,
					// I want to skip doing anything more here and pass that response
					// immediately back to any callbacks waiting on this and not 
					// try to further parse this response.
					if (!describeFeaturetypeRespone.hasOwnProperty('error')) {
						var prefix = args.layerNS;//describeFeaturetypeRespone.featureTypes[0].targetPrefix;
						var namespace = describeFeaturetypeRespone.targetNamespace;
						if (!me.featureTypeDescription[prefix]) {
							me.featureTypeDescription[prefix] = Object.extended();
						}
						me.featureTypeDescription[prefix][describeFeaturetypeRespone.featureTypes[0].typeName] = describeFeaturetypeRespone;
						CONFIG.tempSession.namespace[prefix] = namespace;
					}
					
					$(args.callbacks || []).each(function (index, callback) {
						callback(describeFeaturetypeRespone, this);
					});
				}
			});
		},
		getFeatureCount: function (args) {
			var layerPrefix = args.layerPrefix;
			var layerName = args.layerName;
			return $.get(me.geoserverProxyEndpoint + layerPrefix + '/wfs?service=wfs&version=1.1.0&outputFormat=GML2&request=GetFeature&resultType=hits&typeName=' + layerPrefix + ':' + layerName);
		},
		getFilteredFeature: function (args) {
			LOG.info('OWS.js::getFilteredFeature');
			LOG.debug('OWS.js::getFilteredFeature: Building request for WFS GetFeature (filtered)');

			var layerPrefix = args.layerPrefix;
			var layerName = args.layerName;
			var scope = args.scope;
			var propertyArray = args.propertyArray;
			var callbacks = args.callbacks;

			var url = me.geoserverProxyEndpoint + layerPrefix + '/wfs?service=wfs&version=1.1.0&outputFormat=GML2&request=GetFeature&typeName=' + layerPrefix + ':' + layerName + '&propertyName=';
			url += (propertyArray || []).join(',');

			$.ajax(url, {
				context: scope || this,
				success: function (data) {
					LOG.trace('OWS.js::getFilteredFeature: Successfully received WFS GetFeature response.');
					var gmlReader = new OpenLayers.Format.GML.v3();
					var getFeatureResponse = gmlReader.read(data);
					LOG.debug('OWS.js::getFilteredFeature: WFS GetFeature parsed .');
					if (!me.featureTypeDescription[layerPrefix]) {
						me.featureTypeDescription[layerPrefix] = Object.extended();
					}
					me.featureTypeDescription[layerPrefix][layerName] = getFeatureResponse;

					LOG.trace('OWS.js::getFilteredFeature: Executing ' + callbacks.success + 'callbacks');
					$(callbacks.success || []).each(function (index, callback) {
						LOG.trace('OWS.js::getFilteredFeature: Executing callback ' + index);
						callback(getFeatureResponse, this);
					});
				},
				error: function (data) {
					$(callbacks.error || []).each(function (index, callback) {
						callback(data, this);
					});
				}
			});
		},
		updateFeatureTypeAttribute: function (featureType, attribute, value, callback) {

			var updateTransaction =
					'<?xml version="1.0"?>' +
					'<wfs:Transaction xmlns:ogc="http://www.opengis.net/ogc" ' +
					'xmlns:wfs="http://www.opengis.net/wfs" ' +
					'xmlns:gml="http://www.opengis.net/gml" ' +
					'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" ' +
					'version="1.1.0" service="WFS" ' +
					'xsi:schemaLocation="http://www.opengis.net/wfs ../wfs/1.1.0/WFS.xsd">' +
					'<wfs:Update typeName="' + featureType + '">' +
					'<wfs:Property>' +
					'<wfs:Name>' + attribute + '</wfs:Name>' +
					'<wfs:Value>' + value + '</wfs:Value>' +
					'</wfs:Property>' +
					'</wfs:Update>' +
					'</wfs:Transaction>';

			$.ajax({
				url: me.geoserverProxyEndpoint + 'ows/',
				type: 'POST',
				contentType: 'application/xml',
				data: updateTransaction,
				success: function (data) {
					callback(data);
				}
			});
		},
		executeWPSProcess: function (args) {
			LOG.info('OWS.js::executeWPSProcess: Calling WPS execute process');
			var processIdentifier = args.processIdentifier;
			var processUrl = args.url || this.wpsExecuteRequestPostUrl + '&' + processIdentifier;
			var request = args.request;
			var callbacks = args.callbacks || [];
			var successCallbacks = callbacks.success ? callbacks.success : callbacks;
			var errorCallbacks = callbacks.error ? callbacks.error : callbacks;
			var context = args.context || this;

			return $.ajax({
				url: processUrl,
				type: 'POST',
				contentType: 'application/xml',
				data: request,
				context: context || this,
				success: function (data, textStatus, jqXHR) {
					successCallbacks.each(function (callback) {
						callback(data, textStatus, jqXHR, this);
					});
				},
				error: function (data, textStatus, jqXHR) {
					errorCallbacks.each(function (callback) {
						callback(data, textStatus, jqXHR, this);
					});
				}
			});
		},
		clearFeaturesOnServer: function (args) {
			var layerName = args.layer.split(':')[1];
			var context = args.context || this;
			var callbacks = args.callbacks || [];
			var errorCallbacks = args.errorCallbacks || [];
			if (args.layer.split(':')[0] === CONFIG.tempSession.getCurrentSessionKey()) {
				var url = me.geoserverProxyEndpoint + CONFIG.tempSession.getCurrentSessionKey() + '/wfs';
				var wfst = '<wfs:Transaction service="WFS" version="1.1.0" xmlns:ogc="http://www.opengis.net/ogc" xmlns:wfs="http://www.opengis.net/wfs" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd">' +
						'<wfs:Delete typeName="feature:' + layerName + '">' +
						'<ogc:Filter>' +
						'<ogc:BBOX>' +
						'<ogc:PropertyName>the_geom</ogc:PropertyName>' +
						'<gml:Envelope srsName="' + CONFIG.strings.epsg900913 + '" xmlns:gml="http://www.opengis.net/gml">' +
						'<gml:lowerCorner>-20037508.34 -20037508.34</gml:lowerCorner>' +
						'<gml:upperCorner>20037508.34 20037508.34</gml:upperCorner>' +
						'</gml:Envelope>' +
						'</ogc:BBOX>' +
						'</ogc:Filter>' +
						'</wfs:Delete>' +
						'</wfs:Transaction>';
				$.ajax({
					url: url,
					type: 'POST',
					contentType: 'application/xml',
					data: wfst,
					context: context || this,
					success: function (data, textStatus, jqXHR) {
						callbacks.each(function (callback) {
							callback(data, textStatus, jqXHR, context);
						});
					},
					error: function (data, textStatus, jqXHR) {
						errorCallbacks.each(function (callback) {
							callback(data, textStatus, jqXHR, context);
						});
					}
				});
			}
		},
		cloneLayer: function (args) {
			var originalLayer = args.originalLayer;
			var newLayer = args.newLayer;

			var wps = '<?xml version="1.0" encoding="UTF-8"?><wps:Execute version="1.0.0" service="WPS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.opengis.net/wps/1.0.0" xmlns:wfs="http://www.opengis.net/wfs" xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" xmlns:wcs="http://www.opengis.net/wcs/1.1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">' +
					'<ows:Identifier>gs:Import</ows:Identifier>' +
					'<wps:DataInputs>' +
					'<wps:Input>' +
					'<ows:Identifier>features</ows:Identifier>' +
					'<wps:Reference mimeType="text/xml; subtype=wfs-collection/1.0" xlink:href="http://geoserver/wfs" method="POST">' +
					'<wps:Body>' +
					'<wfs:GetFeature service="WFS" version="1.0.0" outputFormat="GML2" xmlns:' + CONFIG.name.published + '="' + CONFIG.namespace.published + '">' +
					'<wfs:Query typeName="' + originalLayer + '"/>' +
					'</wfs:GetFeature>' +
					'</wps:Body>' +
					'</wps:Reference>' +
					'</wps:Input>' +
					'<wps:Input>' +
					'<ows:Identifier>workspace</ows:Identifier>' +
					'<wps:Data>' +
					'<wps:LiteralData>' + CONFIG.tempSession.getCurrentSessionKey() + '</wps:LiteralData>' +
					'</wps:Data>' +
					'</wps:Input>' +
					'<wps:Input>' +
					'<ows:Identifier>store</ows:Identifier>' +
					'<wps:Data>' +
					'<wps:LiteralData>ch-input</wps:LiteralData>' +
					'</wps:Data>' +
					'</wps:Input>' +
					'<wps:Input>' +
					'<ows:Identifier>name</ows:Identifier>' +
					'<wps:Data>' +
					'<wps:LiteralData>' + newLayer + '</wps:LiteralData>' +
					'</wps:Data>' +
					'</wps:Input>' +
					'<wps:Input>' +
					'<ows:Identifier>srsHandling</ows:Identifier>' +
					'<wps:Data>' +
					'<wps:LiteralData>REPROJECT_TO_DECLARED</wps:LiteralData>' +
					'</wps:Data>' +
					'</wps:Input>' +
					'</wps:DataInputs>' +
					'<wps:ResponseForm>' +
					'<wps:RawDataOutput>' +
					'<ows:Identifier>layerName</ows:Identifier>' +
					'</wps:RawDataOutput>' +
					'</wps:ResponseForm>' +
					'</wps:Execute>';

			CONFIG.ows.executeWPSProcess({
				processIdentifier: 'gs:Import',
				request: wps,
				callbacks: args.callbacks || [],
				context: args.context || this
			});
		},
		appendAttributesToLayer: function (args) {
			var wps = CONFIG.ows.createAppendAttributesToLayerWPSXML(args);
			CONFIG.ows.executeWPSProcess({
				processIdentifier: 'gs:AppendColumnsToLayer',
				request: wps,
				callbacks: args.callbacks || [],
				context: args.context || this
			});
		},
		createAppendAttributesToLayerWPSXML: function (args) {
			var layer = args.layer;
			var workspace = args.workspace;
			var store = args.store;
			var columns = args.columns;
			var wps = '<?xml version="1.0" encoding="UTF-8"?><wps:Execute version="1.0.0" service="WPS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.opengis.net/wps/1.0.0" xmlns:wfs="http://www.opengis.net/wfs" xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" xmlns:wcs="http://www.opengis.net/wcs/1.1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">' +
					'<ows:Identifier>gs:AppendColumnsToLayer</ows:Identifier>' +
					'<wps:DataInputs>' +
					'<wps:Input>' +
					'<ows:Identifier>layer</ows:Identifier>' +
					'<wps:Data>' +
					'<wps:LiteralData>' + layer + '</wps:LiteralData>' +
					'</wps:Data>' +
					'</wps:Input>' +
					'<wps:Input>' +
					'<ows:Identifier>workspace</ows:Identifier>' +
					'<wps:Data>' +
					'<wps:LiteralData>' + workspace + '</wps:LiteralData>' +
					'</wps:Data>' +
					'</wps:Input>' +
					'<wps:Input>' +
					'<ows:Identifier>store</ows:Identifier>' +
					'<wps:Data>' +
					'<wps:LiteralData>' + store + '</wps:LiteralData>' +
					'</wps:Data>' +
					'</wps:Input>';

			columns.each(function (column) {
				wps += '<wps:Input>' +
						'<ows:Identifier>column</ows:Identifier>' +
						'<wps:Data>' +
						'<wps:LiteralData>' + column + '</wps:LiteralData>' +
						'</wps:Data>' +
						'</wps:Input>';
			});

			wps += '</wps:DataInputs>' +
					'<wps:ResponseForm>' +
					'<wps:RawDataOutput>' +
					'<ows:Identifier>layerName</ows:Identifier>' +
					'</wps:RawDataOutput>' +
					'</wps:ResponseForm>' +
					'</wps:Execute>';

			return wps;
		},
		createResultsRasterSLD: function (args) {
			args = args || {};
			var attribute = args.attribute || 'LRR';
			var layerName = args.layerName || 'ResultsRaster';

			var sld = '<?xml version="1.0" encoding="ISO-8859-1"?>' +
					'<StyledLayerDescriptor version="1.1.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">' +
					'<NamedLayer>' +
					'<Name>#[layer]</Name>' +
					'<UserStyle>' +
					'<FeatureTypeStyle>' +
					'<Transformation>' +
					'<ogc:Function name="gs:ResultsRaster">' +
					'<ogc:Function name="parameter">' +
					'<ogc:Literal>features</ogc:Literal>' +
					'</ogc:Function>' +
					'<ogc:Function name="parameter">' +
					'<ogc:Literal>attribute</ogc:Literal>' +
					'<ogc:Literal>' + attribute + '</ogc:Literal>' +
					'</ogc:Function>' +
					'<ogc:Function name="parameter">' +
					'<ogc:Literal>bbox</ogc:Literal>' +
					'<ogc:Function name="env">' +
					'<ogc:Literal>wms_bbox</ogc:Literal>' +
					'</ogc:Function>' +
					'</ogc:Function>' +
					'<ogc:Function name="parameter">' +
					'<ogc:Literal>width</ogc:Literal>' +
					'<ogc:Function name="env">' +
					'<ogc:Literal>wms_width</ogc:Literal>' +
					'</ogc:Function>' +
					'</ogc:Function>' +
					'<ogc:Function name="parameter">' +
					'<ogc:Literal>height</ogc:Literal>' +
					'<ogc:Function name="env">' +
					'<ogc:Literal>wms_height</ogc:Literal>' +
					'</ogc:Function>' +
					'</ogc:Function>' +
					'</ogc:Function>' +
					'</Transformation>' +
					'<Rule>' +
					'<RasterSymbolizer>' +
					'<Geometry>' +
					'<ogc:PropertyName>the_geom</ogc:PropertyName>' +
					'</Geometry>' +
					'<Opacity>1</Opacity>' +
					'</RasterSymbolizer>' +
					'</Rule>' +
					'</FeatureTypeStyle>' +
					'</UserStyle>' +
					'</NamedLayer>' +
					'</StyledLayerDescriptor>';
			return sld.replace('#[layer]', layerName);
		},
		updateTransectsAndIntersections: function (args) {
			var wps = CONFIG.ows.createUpdateTransectsAndIntersectionsWPSXML(args);
			CONFIG.ows.executeWPSProcess({
				processIdentifier: 'gs:UpdateTransectsAndIntersections',
				request: wps,
				callbacks: args.callbacks || [],
				context: args.context || this
			});
		},
		createUpdateTransectsAndIntersectionsWPSXML: function (args) {
			var transects = args.transects;
			var intersections = args.intersections;
			var baseline = args.baseline;
			var shorelines = args.shorelines;
			var transectId = args.transectId || [];
			var farthest = args.farthest || 'false';

			var wps = '<?xml version="1.0" encoding="UTF-8"?>' +
					'<wps:Execute version="1.0.0" service="WPS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.opengis.net/wps/1.0.0" xmlns:wfs="http://www.opengis.net/wfs" xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" xmlns:wcs="http://www.opengis.net/wcs/1.1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">' +
					'<ows:Identifier>gs:UpdateTransectsAndIntersections</ows:Identifier>' +
					'<wps:DataInputs>' +
					'<wps:Input>' +
					'<ows:Identifier>transectLayer</ows:Identifier>' +
					'<wps:Data>' +
					'<wps:LiteralData>' + transects + '</wps:LiteralData>' +
					'</wps:Data>' +
					'</wps:Input>' +
					'<wps:Input>' +
					'<ows:Identifier>intersectionLayer</ows:Identifier>' +
					'<wps:Data>' +
					'<wps:LiteralData>' + intersections + '</wps:LiteralData>' +
					'</wps:Data>' +
					'</wps:Input>' +
					'<wps:Input>' +
					'<ows:Identifier>baselineLayer</ows:Identifier>' +
					'<wps:Data>' +
					'<wps:LiteralData>' + baseline + '</wps:LiteralData>' +
					'</wps:Data>' +
					'</wps:Input>';

			shorelines.each(function (shoreline) {
				var excludedDates = CONFIG.tempSession.getDisabledDates(shoreline);
				var prefix = shoreline.split(':')[0];
				wps += '<wps:Input>' +
						'<ows:Identifier>shorelines</ows:Identifier>' +
						'<wps:Reference mimeType="text/xml; subtype=wfs-collection/1.0" xlink:href="http://geoserver/wfs" method="POST">' +
						'<wps:Body>' +
						'<wfs:GetFeature service="WFS" version="1.1.0" outputFormat="GML2" xmlns:' + prefix + '="gov.usgs.cida.ch.' + prefix + '">' +
						(function (args) {
							var filter = '';
							if (excludedDates) {
								var property = args.shoreline.substring(0, args.shoreline.indexOf(':') + 1) + CONFIG.tempSession.getStage(Shorelines.stage).groupingColumn;

								filter += '<wfs:Query typeName="' + shoreline + '"  srsName="' + CONFIG.strings.epsg4326 + '">' +
										'<ogc:Filter>' +
										'<ogc:And>';

								excludedDates.each(function (date) {
									filter += '<ogc:Not>' +
											'<ogc:PropertyIsLike  wildCard="*" singleChar="." escape="!">' +
											'<ogc:PropertyName>' + property + '</ogc:PropertyName>' +
											'<ogc:Literal>' + date + '</ogc:Literal>' +
											'</ogc:PropertyIsLike>' +
											'</ogc:Not>';
								});

								filter += '</ogc:And>' +
										'</ogc:Filter>' +
										'</wfs:Query>';
							} else {
								filter += '<wfs:Query typeName="' + shoreline + '"  srsName="' + CONFIG.strings.epsg4326 + '" />';
							}
							return filter;
						}({
							shoreline: shoreline
						})) +
						'</wfs:GetFeature>' +
						'</wps:Body>' +
						'</wps:Reference>' +
						'</wps:Input>';
			});

			transectId.each(function (tid) {
				wps += '<wps:Input>' +
						'<ows:Identifier>transectID</ows:Identifier>' +
						'<wps:Data>' +
						'<wps:LiteralData>' + tid + '</wps:LiteralData>' +
						'</wps:Data>' +
						'</wps:Input>';
			});

			wps += '<wps:Input>' +
					'<ows:Identifier>farthest</ows:Identifier>' +
					'<wps:Data>' +
					'<wps:LiteralData>' + farthest + '</wps:LiteralData>' +
					'</wps:Data>' +
					'</wps:Input>' +
					'</wps:DataInputs>' +
					'<wps:ResponseForm>' +
					'<wps:RawDataOutput mimeType="text/xml; subtype=wfs-collection/1.0">' +
					'<ows:Identifier>intersections</ows:Identifier>' +
					'</wps:RawDataOutput>' +
					'</wps:ResponseForm>' +
					'</wps:Execute>';
			return wps;
		},
		projectPointOnLine: function (args) {
			var wps = CONFIG.ows.createProjectPointOnLineWPSXML(args);
			CONFIG.ows.executeWPSProcess({
				'processIdentifier': 'gs:NearestPointOnLine',
				'request': wps,
				'callbacks': args.callbacks || [],
				'context': args.context || this
			});
		},
		createProjectPointOnLineWPSXML: function (args) {
			var workspaceNS = args.workspaceNS;
			var layer = args.layer;
			var workspace = layer.split(':')[0];
			var point = args.point;
			var transectSRID = args.transectSRID;

			return '<?xml version="1.0" encoding="UTF-8"?>' +
					'<wps:Execute version="1.0.0" service="WPS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.opengis.net/wps/1.0.0" xmlns:wfs="http://www.opengis.net/wfs" xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" xmlns:wcs="http://www.opengis.net/wcs/1.1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">' +
					'<ows:Identifier>gs:NearestPointOnLine</ows:Identifier>' +
					'<wps:DataInputs>' +
					'<wps:Input>' +
					'<ows:Identifier>lines</ows:Identifier>' +
					'<wps:Reference mimeType="text/xml" xlink:href="http://geoserver/wfs" method="POST">' +
					'<wps:Body>' +
					'<wfs:GetFeature service="WFS" version="1.0.0" outputFormat="GML2" xmlns:' + workspace + '="' + workspaceNS + '">' +
					'<wfs:Query typeName="' + layer + '" srsName="EPSG:' + transectSRID + '"/>' +
					'</wfs:GetFeature>' +
					' </wps:Body>' +
					'</wps:Reference>' +
					'</wps:Input>' +
					'<wps:Input>' +
					'<ows:Identifier>point</ows:Identifier>' +
					'<wps:Data>' +
					'<wps:LiteralData>' + point + '</wps:LiteralData>' +
					'</wps:Data>' +
					'</wps:Input>' +
					'</wps:DataInputs>' +
					'<wps:ResponseForm>' +
					'<wps:RawDataOutput>' +
					'<ows:Identifier>point</ows:Identifier>' +
					'</wps:RawDataOutput>' +
					'</wps:ResponseForm>' +
					'</wps:Execute>';
		},
		/**
		 * Requests the hit count of features from all published shorelines as well
		 * as session shorelines
		 * 
		 * @param {Object} opts
		 * @param {Object} opts.context - The context to use when resolving the AJAX request.
		 * @param {OpenLayers.Bounds} opts.bbox - An OpenLayers Bounds object to specify the bounding box for this request
		 * @returns {jqXHR}
		 */
		requestShorelineLayerFeatureCountForBBox: function (opts) {
			opts = opts || {};
			return $.ajax({
				url: me.geoserverProxyEndpoint + 'ows/',
				context: opts.context || this,
				data: {
					service: "wfs",
					version: "2.0.0",
					request: "GetFeature",
					typeName: CONFIG.tempSession.getStage('shorelines').layers.join(','),
					resultType: "hits",
					srsName: "EPSG:900913",
					bbox: opts.bbox.toString()
				}
			});

		}
	});
};
