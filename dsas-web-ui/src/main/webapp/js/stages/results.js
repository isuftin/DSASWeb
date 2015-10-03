var Results = {
    stage : 'results',
    
    viewableResultsColumns : ['base_dist', 'LRR', 'LCI', 'WLR', 'WCI', 'SCE', 'NSM', 'EPR', 'ECI'],
    
    suffixes : ['_rates','_results','_clip','_lt'],
    
    reservedColor: '#0061A3',
    
    description : {
        'stage' : '<p>Erosion and deposition rates are contained within the results layer of DSASweb processing.</p><p>View existing results by selecting layers from the list above. Use the Manage tab to export results information onto your desktop.</p><p>The results map, plot, and table are all interactive. Users can view the map location which corresponds to the line plot, or use the table to view map locations.</p><hr />View results from the workspace calculations or view published results from other studies.',
        'view-tab' : '',
        'manage-tab' : '',
        'upload-button' : ''
    },
    
    appInit : function() {
        $('#results-form-name').val(Util.getRandomLorem());
        $("#create-results-btn").on("click", function() {
            var ci = $('#results-form-ci').val();
            var ciNum = parseFloat(ci);
            if (isNaN(ci)) {
                CONFIG.ui.showAlert({
                    message : 'Confidence Interval needs to be numeric.',
                    displayTime : 3000,
                    caller : Calculation
                });
            } else if (ciNum < 50 || ciNum > 100) {
                CONFIG.ui.showAlert({
                    message : 'Confidence Interval needs to be in range 50 - 100.',
                    displayTime : 3000,
                    caller : Calculation
                });
            } else {
                Results.calcResults();
            }
        });
        $('#download-shapefile-btn').click(Results.retrieveResultsShapefile);
        $('#download-spreadsheet-btn').click(Results.retrieveResultsSpreadsheet);
        $('#download-plot-btn').click(Results.retrieveRSquigglePlotPNG);
    },
    leaveStage : function() {
        LOG.debug('Results.js::leaveStage');
        CONFIG.map.getMap().removeControl(CONFIG.map.getMap().getControlsBy('id','results-select-control')[0]);
    },
    enterStage : function() {
        LOG.debug('Results.js::enterStage');
        CONFIG.ui.switchTab({
            caller : Results,
            tab : 'view'
        });
    },
    populateFeaturesList : function() {
        CONFIG.ui.populateFeaturesList({
            caller : Results
        });
    },
    calcResults : function() {
        if (!$('#transects-list :selected').length  
            || !$('#intersections-list :selected').length) {
            CONFIG.ui.showAlert({
                message : 'Missing transects or intersections.',
                displayTime : 7500,
                caller : Calculation
            });
            return;
        }
        Results.clear();
        LOG.info('Results.js::calcResults');
        var transects = $('#transects-list :selected')[0].value;
        var intersects = $('#intersections-list :selected')[0].value;
        var ci = parseFloat($('#results-form-ci').val()) / 100;
        var resultsLayerName = $('#results-form-name').val() ? $('#results-form-name').val() + '_rates' : transects.replace('_transects', Results.suffixes[0]); 
        var request = Results.createWPSCalculateResultsRequest({
            transects : transects,
            intersects : intersects,
            ci : ci
        })
        
        var wpsProc = function() {
            CONFIG.ows.executeWPSProcess({
                processIdentifier : 'gs:CreateResultsLayer',
                request : request,
                context : this,
                callbacks : [
                function(data, textStatus, jqXHR, context) {
                    if (typeof data == 'string') {
                        CONFIG.ows.getWMSCapabilities({
                            namespace : CONFIG.tempSession.getCurrentSessionKey(),
                            callbacks : {
                                success : [
                                Results.populateFeaturesList,
                                function() {
                                    var results = CONFIG.tempSession.session.results;
                                    if (!results) {
                                        results = Object.extended();
                                        CONFIG.tempSession.session.results = results;
                                    }
                                    
                                    var selectedShorelines = CONFIG.tempSession.getStage(Shorelines.stage).viewing;
                                    var selectedBaseline = CONFIG.tempSession.getStage(Baseline.stage).viewing;
                                    var selectedTransects = CONFIG.tempSession.getStage(Baseline.stage).viewing;
                                    var selectedIntersections = CONFIG.tempSession.getStage('intersections').viewing;
                                    
                                    results[data] = {
                                        shorelines : selectedShorelines,
                                        baseline : selectedBaseline,
                                        transects : selectedTransects,
                                        intersections : selectedIntersections
                                    };
                                    
                                    CONFIG.tempSession.session.results = results;
                                    CONFIG.tempSession.persistSession();
                                    
                                    $('#results-list').val(data);
                                    Results.listboxChanged();
                                    CONFIG.ui.displayStage(Results);
                                    $('a[href="#' + Results.stage + '-view-tab"]').tab('show');
                                    CONFIG.ui.showAlert({
                                        message : 'Results were created successfully.',
                                        displayTime : 7500,
                                        caller : Results,
                                        style: {
                                            classes : ['alert-success']
                                        }
                                    })
                                }      
                                ]
                            }
                        })
                    } else {
                        LOG.error($(data).find('ows\\:ExceptionText').first().text());
                        CONFIG.ui.showAlert({
                            message : 'Results creation failed. Check logs.',
                            displayTime : 7500,
                            caller : Results,
                            style: {
                                classes : ['alert-error']
                            }
                        })
                    }
                }
                ]
            })
        }
        
        if ($('#results-list option[value="'+ CONFIG.tempSession.getCurrentSessionKey() + ':' + resultsLayerName + '"]').length) {
            CONFIG.ui.createModalWindow({
                context : {
                    scope : this
                },
                headerHtml : 'Resource Exists',
                bodyHtml : 'A resource already exists with the name ' + resultsLayerName + ' in your session. Would you like to overwrite this resource?',
                buttons : [
                {
                    text : 'Overwrite',
                    callback : function(event) {
                        $.get('service/session', {
                            action : 'remove-layer',
                            workspace : CONFIG.tempSession.getCurrentSessionKey(),
                            store : 'ch-output',
                            layer : resultsLayerName
                        },
                        function(data, textStatus, jqXHR) {
                            wpsProc();
                        }, 'json')
                    }           
                }
                ]
            })
        } else {
            wpsProc();
        }
    },
    clear : function() {
        $("#results-list").val('');
        Results.listboxChanged();
    },
    listboxChanged : function() {
        var value = $("#results-list :selected").val();
        
        var downloadButtons = $('#results-manage-tab button');
        if('' === value){
            downloadButtons.attr('disabled', 'disabled');
            
        }
        else{
            downloadButtons.removeAttr('disabled', '');
        }
        
        LOG.info('Results.js::listboxChanged: A result was selected from the select list');

        $('#results-table-navtabs').children().remove();
        $('#results-tabcontent').children().remove();
        CONFIG.map.removeControl({
            id : 'results-select-control'
        });
        
        CONFIG.tempSession.getStage(Results.stage).viewing = '';
        $("#results-list option:not(:selected)").each(function (index, option) {
            var layers = CONFIG.map.getMap().getLayersBy('name', option.value);
            if (layers.length) {
                $(layers).each(function(i,l) {
                    CONFIG.map.getMap().removeLayer(l, false);
                })
            }
        });
        
        if ($("#results-list option:selected")[0].value) {
            var selectedResult = $("#results-list option:selected")[0];
            var selectedResultText = selectedResult.text
            var selectedResultValue = selectedResult.value
            
            LOG.debug('Results.js::listboxChanged: A result ('+selectedResultText+') was selected from the select list');
            var layer = CONFIG.ows.getLayerByName({
                layerNS: selectedResultValue.split(':')[0],
                layerName : selectedResultValue.split(':')[1]
            });
            CONFIG.tempSession.getStage(Transects.stage).viewing = selectedResultValue;
            
            Results.addLayerToMap({
                layer : layer
            });
             
            Results.displayResult({
                result : layer
            });
            
            CONFIG.map.getMap().getLayersBy('name', layer.prefix + ':' + layer.name)[0].redraw();
        } 
        CONFIG.tempSession.persistSession();
    },
    
	/**
     * Uses a OWS DescribeFeatureType response to add a layer to a map,
     * pop up a plotter and table and sets highlighting rules on the layer
     * that ties into the table and plotter
	 * 
	 * @param {Object} args { layer : { name : String, prefix : String}  }
	 * @returns {undefined}
	 */
    addLayerToMap : function(args) {
        LOG.info('Results.js::addLayerToMap');
        
        LOG.trace('Results.js::addLayerToMap: Creating WMS layer that will hold the heatmap style');
        var resultsWMS = Results.createRasterLayer(args);
            
        LOG.trace('Results.js::addLayerToMap: Creating Vector layer that will be used for highlighting');
        var resultsVector = Results.createVectorLayer(args);
	
		// TODO - Figure out a way to bind scrolling across the vector features and 
		// highlight it in the plotter
        var featureHighlighted = function(event) {
            LOG.trace('Results.js::addLayerToMap: A results feature is being highlighted');
            var xValue = event.feature.attributes.base_dist;
            
			// Figure out which tab the user is looking at
			var activeTab = $('#results-table-navtabs li[class=active] a').attr('id');
			if (activeTab === 'nav-tab-plot-link') { // User is looking @ plot
				LOG.trace('Results.js::addLayerToMap: Highlighting the feature in the plot');
				var xPlotIdx = CONFIG.graph.plot.rawData_.findIndex(function(o){
					return o[0] === parseFloat(xValue);
				});
				CONFIG.graph.plot.setSelection(xPlotIdx);
			} else if (activeTab === 'nav-tab-table-link') { // User is looking @ table
				 LOG.trace('Results.js::addLayerToMap: Highlighting the feature in the table');
				$('#results-table tbody>tr').removeClass('warning');
				var tableRow = $('#results-table tbody>tr').toArray().find(function(tr){
					return $(tr).data().base_dist === parseFloat(xValue);
				});

				LOG.trace('Results.js::addLayerToMap: Scrolling the table into view and highlighting the correct row');
				$(tableRow).addClass('warning'); // Highlight in yellow
			}
        };
		
        LOG.debug('Shorelines.js::addLayerToMap: (re?)-adding vector selector control for new results set');
        CONFIG.map.getMap().removeControl(CONFIG.map.getMap().getControlsBy('id','results-select-control')[0]);
        var selectFeatureControl = new OpenLayers.Control.SelectFeature(resultsVector, {
            id : 'results-select-control',
            hover: true,
            highlightOnly: true,
            renderIntent: "temporary",
            eventListeners: {
                featurehighlighted: featureHighlighted,
                featureunhighlighted: featureHighlighted
            }
        });
            
        LOG.debug('Shorelines.js::addLayerToMap: Adding results WMS layer to the map');
		Results.removeRasterLayer();
        CONFIG.map.getMap().addLayer(resultsWMS);
		
        LOG.debug('Shorelines.js::addLayerToMap: Adding results Vector layer to the map');
        Results.removeVectorLayer();
        CONFIG.map.getMap().addLayer(resultsVector);
		
        LOG.debug('Shorelines.js::addLayerToMap: Adding select feature control to map and activating');
        CONFIG.map.getMap().addControl(selectFeatureControl);
        selectFeatureControl.activate();
    },
	/**
	 * Removes the results raster layer, if it exists
	 * @returns {Boolean} Removed
	 */
	removeRasterLayer : function() {
		var rLayerArr = CONFIG.map.getMap().getLayersBy('type','results');
		
		if (rLayerArr.length) {
			CONFIG.map.getMap().removeLayer(rLayerArr[0]);
			return true;
		}
		return false;
	},
	/**
	 * Removes the results vector layer, if it exists
	 * @returns {Boolean} Removed
	 */
	removeVectorLayer : function() {
		var vLayerArr = CONFIG.map.getMap().getLayersBy('type','highlight');
		
		if (vLayerArr.length) {
			CONFIG.map.getMap().removeLayer(vLayerArr[0]);
			return true;
		}
		return false;
	},
	/**
	 * Creates a results raster layer to display on the map. Typically this 
	 * will be a heatmap
	 * 
	 * @param {Object} args { layer : { name : String, prefix : String}  }
	 * 
	 * @returns {OpenLayers.Layer.WMS} Results raster
	 */
    createRasterLayer: function(args) {
		LOG.info('Results.js::createRasterLayer');
		var layer = args.layer;
		var layerName = layer.name;
		var layerPrefix = layer.prefix;
		
		var raster = new OpenLayers.Layer.WMS(layerPrefix + ':' + layerName,
				'geoserver/' + layerPrefix + '/wms',
				{
					layers: layerName,
					transparent: true,
					styles: 'ResultsRaster',
					env: 'attribute:' + CONFIG.graph.enabled + ";invert:" + CONFIG.graph.displayMap[CONFIG.graph.enabled].invert
				},
		{
			prefix: layerPrefix,
			zoomToWhenAdded: true, // Include this layer when performing an aggregated zoom
			isBaseLayer: false,
			unsupportedBrowsers: [],
			tileOptions: {
				// http://www.faqs.org/rfcs/rfc2616.html
				// This will cause any request larger than this many characters to be a POST
				maxGetUrlLength: 2048
			},
			ratio: 1,
			singleTile: true,
			displayInLayerSwitcher : false
		});
		raster.type="results";
		return raster;
	},
	/**
	 * Creates a results vector layer to display on the map. Typically this 
	 * will be the highlight later
	 * 
	 * @param {Object} args { layer : { name : String, prefix : String}  }
	 * 
	 * @returns {OpenLayers.Layer.Vector} Results raster
	 */
	createVectorLayer: function(args) {
		LOG.info('Results.js::createVectorLayer');
		var layer = args.layer;
		var layerName = layer.name;
		var layerPrefix = layer.prefix;

		var vector = new OpenLayers.Layer.Vector(layerPrefix + ':' + layerName, {
			strategies: [new OpenLayers.Strategy.Fixed()],
			protocol: new OpenLayers.Protocol.WFS({
				version: '1.1.0',
				url: "geoserver/" + layerPrefix + "/wfs",
				featureType: layerName,
				featureNS: CONFIG.namespace[layerPrefix],
				geometryName: "the_geom",
				srsName: CONFIG.map.getMap().getProjection()
			}),
			displayInLayerSwitcher : false,
			styleMap: new OpenLayers.StyleMap({
				"default": new OpenLayers.Style({
					strokeColor: Results.reservedColor,
					strokeWidth: 2,
					strokeOpacity: 0
				}),
				"temporary": new OpenLayers.Style({
					strokeColor: Results.reservedColor,
					strokeOpacity: 1,
					strokeWidth: 2,
					fillColor: Results.reservedColor,
					fillOpacity: 1,
					cursor: "pointer"
				})
			})
		});
		vector.type="highlight";
		return vector;
	},
	/**
	 * 
	 * @param {Object} args { result : { name : String, prefix : String}  }
	 * @returns {undefined}
	 */
    displayResult : function(args) {
        var result = args.result;
        var resultsColumns = this.viewableResultsColumns.clone();
        
        CONFIG.ows.getFilteredFeature({ 
            layerPrefix : result.prefix,
            layerName : result.name,
            propertyArray : [],
            scope : result,
            callbacks : {
                success : [
                function (features, scope) {
					CONFIG.graph.features = features;
					
                    var resultsTable = Results.createTable({
                        features : features,
                        layer : result,
                        resultsColumns : resultsColumns
                    });
                    
                    Results.createResultsTabs({
                        layer : result,
                        table : resultsTable
                    });
                    
                    Results.createPlot({
                        layer : result
                    });
					
					Results.bindPlotControls();
                    
                    $('#results-table tbody>tr').hover( 
                        function(event) {
                            var baseDist = $(this).data().base_dist;
                            var selectionControl = CONFIG.map.getMap().getControlsBy('id','results-select-control')[0];
                            var hlFeature = CONFIG.map.getMap().getLayersBy('type', 'highlight')[0].features.find(function(f){
                                return f.attributes.base_dist === baseDist;
                            });
                            selectionControl.select(hlFeature);
                            $(this).addClass('warning');
                            event.stopImmediatePropagation();
                            
                        },
                        function() {
                            var selectionControl = CONFIG.map.getMap().getControlsBy('id','results-select-control')[0];
							if (selectionControl) {
								selectionControl.unselectAll();
								$(this).removeClass('warning');
							}
                        });
                }
                ],
                error : []
            }
        });
    },
	bindPlotControls : function() {
		var container = $('<div />').addClass('container-fluid').attr('id', 'plot-controls-container');
		var row = $('<div />').addClass('row-fluid').attr('id', 'plot-controls-row');
		var commonButtonControls = 'btn plot-ctrl-btn';
		row.append(
			$('<div />').addClass('btn-group').attr('data-toggle', 'buttons-radio')
			.append($('<button />').addClass(commonButtonControls).attr('id', 'lrr-btn').html('LRR +/- LCI'))
			.append($('<button />').addClass(commonButtonControls).attr('id', 'wlr-btn').html('WLR +/- WCI'))
			.append($('<button />').addClass(commonButtonControls).attr('id', 'sce-btn').html('SCE'))
			.append($('<button />').addClass(commonButtonControls).attr('id', 'nsm-btn').html('NSM'))
			.append($('<button />').addClass(commonButtonControls).attr('id', 'epr-btn').html('EPR +/- ECI')));
			container.append(row);
		
		$('#plot-menu-icon').popover({
			animation : true,
			html : true,
			title : 'Select statistic to display',
			placement : 'top',
			trigger : 'click',
		    content : container.html()
		}).bind({
			'shown' : function() {
				// Hiding the popover detaches it from the DOM. This resets the HTML inside
				// the popover. So the next time the popover gets opened, it doesn't have 
				// the button the user clicked on enabled. This is annoying but I think this 
				// is a bootstrap issue so I deal with it here
				var enabled = CONFIG.graph.enabled;
				var enabledId = '#' + enabled.toLowerCase() + '-btn';
				var enabledButton = $(enabledId);
				$(enabledButton).addClass('active');
				
				$('.plot-ctrl-btn').click(function(event) {
					var attribute = $(event.target).html().substring(0,3);
					var fullAttribute = $(event.target).html();
					CONFIG.graph.enabled = attribute;
					
					// Change the tab title
					$('#tab-stat-description').html(fullAttribute);
					
					// Show the plot
					Results.createPlot();
					
					// Display a new heatmap
					Results.removeRasterLayer();
					var selectedLayer = $('#results-list :selected').val();
					var resultsWMS = Results.createRasterLayer({
						layer : {
							prefix : selectedLayer.split(':')[0],
							name : selectedLayer.split(':')[1]
						}
					});
					CONFIG.map.getMap().addLayer(resultsWMS);
					
					// Hide the popover
					$('#plot-menu-icon').popover('hide');
				});
			}
		});
	},
	preparePlotData : function() {
		var features = CONFIG.graph.features;
		var enabled = CONFIG.graph.enabled;
		var uncertainty = CONFIG.graph.displayMap[enabled].uncertainty;
		
		var data = features.map(function(n){
            var baseDist = parseFloat(n.data['base_dist']);
            var series = parseFloat(n.data[enabled]);
			if (uncertainty) {
				// Include error bars [distance, [series, ABS(error bars)], error bars]
				return [baseDist, [series, parseFloat(Math.abs(n.data[uncertainty]))], [parseFloat(n.data[uncertainty]), null]];
			} else {
				return [baseDist, series];
			}
        }).sortBy(function(n) {
            return n[0];
        });
        
        // Find missing data
        var fidBreaks = [];
        features.each(function(feature, index, features) {
            if (index !== 0) {
                var previousFid = features[index - 1].attributes.BaselineID;
                if (previousFid !== feature.attributes.BaselineID) {
                    fidBreaks.push(index);
                }
            }
        });
        fidBreaks.each(function(i) {
			var noData = null;
			if (uncertainty) {
				noData = [null,null];
				data.insert([[null, noData, [null, null]]], i);
			} else {
				data.insert([[null,noData]], i);
			}
        });
		
		return data;
	},
    createPlot: function() {
		var enabled = CONFIG.graph.enabled;
		var uncertaintyText = CONFIG.graph.displayMap[enabled].uncertainty;
		var uncertainty =  uncertaintyText ? true : false;
		var plotDiv = $('#results-plot').get()[0];
		var plotLegendDiv =  $('#results-plot-legend').get()[0];
		var labels = ['D', enabled];
		var xLabel = 'Distance (m)';
		var yLabel = CONFIG.graph.displayMap[enabled].longName + '<br />(' + CONFIG.graph.displayMap[enabled].units + ')';
		var data = Results.preparePlotData();
		var seriesOptions = {
			strokeWidth: 1.0
		};
		seriesOptions[enabled] = {
			strokeWidth: 1.0,
			highlightCircleSize: 3,
			rangeSelectorPlotStrokeColor : '#00AA00'
			
		};
		
		if (uncertainty) {
			labels.push(uncertaintyText);
			seriesOptions[uncertaintyText] = {
				strokeWidth: 0.0,
				highlightCircleSize: 0,
				fillAlpha : 0.5
			};
		}
		
		if (CONFIG.graph.plot) {
			LOG.debug('results.js:createPlot():: Removing previous plot');
			CONFIG.graph.plot.destroy();
		}
		
		var options = Object.extended({
					labelsDiv : plotLegendDiv,
					labels: labels,
					errorBars: uncertainty,
					showRangeSelector: true,
					labelsSeparateLines : true,
					xlabel : xLabel,
					ylabel :  yLabel,	
					legend : 'always',
					colors : ['#00AA00', '#00AA00'],
					underlayCallback: function(canvas, area, dygraph) {
						var w = $('#results-plot-container').width();
						var h = $('#results-plot').height();
						if (w !== dygraph.width || h !== dygraph.height) {
							dygraph.resize(w,h);
						}
					},
					highlightCallback: function(e, x, pts, row) {
						var selectionControl = CONFIG.map.getMap().getControlsBy('id', 'results-select-control')[0];
						selectionControl.unselectAll();
						var hlFeature = CONFIG.map.getMap().getLayersBy('type', 'highlight')[0].features.find(function(f) {
							return parseFloat(f.attributes.base_dist) === x;
						});
						selectionControl.select(hlFeature);
					}
				});
		options.merge(seriesOptions);
		
		CONFIG.graph.plot = new Dygraph(plotDiv, data, options);
		return plotDiv;
	},
    createTable : function(args) {
        LOG.debug('Results.js::createResultsTable:: Creating results table header');
        var columns = this.viewableResultsColumns;
        var features = args.features || CONFIG.graph.features;
        var tableDiv = $('<div />').attr('id','results-table-container');
        var table = $('<table />').addClass('table table-bordered table-condensed tablesorter results-table').attr('id','results-table');
        var thead = $('<thead />');
        var theadRow = $('<tr />');
        var tbody = $('<tbody />');
        
        columns.each(function(c) {
            if (features[0].attributes[c]) {
                theadRow.append($('<td />').html(c));
            }
        });
        thead.append(theadRow);
        table.append(thead);
        
        LOG.debug('Results.js::createResultsTable:: Creating results table body');
		features.each(function(feature) {
			var tbodyRow = $('<tr />')
					.data({
				base_dist: feature.attributes.base_dist
			});
			columns.each(function(c) {
				if (feature.attributes[c]) {
					var tbodyData;
					var data = feature.data[c];
					if (c === 'base_dist') {
						tbodyData = $('<td />').html(data);
					} else {
						var floatData = parseFloat(data);
						var fixedFloatData = floatData.toPrecision(3);
						tbodyData = $('<td />').html(fixedFloatData);

					}
					tbodyRow.append(tbodyData);
				}
			});
			tbody.append(tbodyRow);
		});
        table.append(tbody);
        tableDiv.append(table);
        LOG.debug('Results.js::createResultsTable:: Results table created');
        
        return tableDiv;
    },
    createResultsTabs : function(args) {
        LOG.info('Results.js::createResultsTable:: Creating table for results');
        var navTabs = 	$('#results-table-navtabs');
        var tabContent = $('#results-tabcontent');
        
        var layer = args.layer;
        var table = args.table;
        
        LOG.debug('Results.js::createResultsTable:: Creating new tab for new results table. Removing old result tabs');
        navTabs.children().each(function(i,navTab) {
            $(navTab).remove();
        });
        tabContent.children().each(function(i, tc) {
            $(tc).remove();
        });

        var navTabPlot = $('<li />').addClass('active');
        var navTabTable = $('<li />');
        var navTabPlotLink = $('<a />').attr({
			'href' : '#results-plot-tabpane',
			'id' : 'nav-tab-plot-link'
		}).attr('data-toggle', 'tab').html('<span id="tab-stat-description">LRR + LCI</span> Rates Plot &nbsp;&nbsp;&nbsp;').append($('<i />').attr('id','plot-menu-icon').addClass('icon-cogs'));
        var navTabTableLink = $('<a />').attr({
			'id' : 'nav-tab-table-link',
			'href' : '#results-' + layer.title + '-table'
		}).attr('data-toggle', 'tab').html('Rates Table');
        
		navTabTable.append(navTabTableLink);
        navTabPlot.append(navTabPlotLink);
        navTabs.append(navTabPlot);
        navTabs.append(navTabTable);
		
		var plotContainer = $('<div />').addClass('container-fluid').attr('id', 'results-plot-container');//.attr('id', 'results-plot');
        var plotRow = $('<div />').addClass('row-fluid').attr('id', 'results-plot');
		var plotLegendRow = $('<div />').addClass('row-fluid').attr('id', 'results-plot-legend');
		plotContainer.append(plotRow).append(plotLegendRow);
		
        LOG.debug('Results.js::createResultsTable:: Adding results table to DOM');
        var tabContentPlotDiv = $('<div />').addClass('tab-pane active').attr('id', 'results-plot-tabpane');
        var tabContentTableDiv = $('<div />').addClass('tab-pane').attr('id', 'results-' + layer.title + '-table');
		tabContentPlotDiv.append(plotContainer);
        tabContentTableDiv.append(table);
        tabContent.append(tabContentPlotDiv);
        tabContent.append(tabContentTableDiv);
                        
        $("table.tablesorter").tablesorter();
    },
    createWPSCalculateResultsRequest : function(args) {
        var transects = args.transects;
        var intersects = args.intersects;
        var ci = args.ci;
        var geoserverEndpoint = CONFIG.geoServerEndpoint.endsWith('/') ? CONFIG.geoServerEndpoint : CONFIG.geoServerEndpoint + '/';
        var wps = '<?xml version="1.0" encoding="UTF-8"?><wps:Execute version="1.0.0" service="WPS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://www.opengis.net/wps/1.0.0" xmlns:ogc="http://www.opengis.net/ogc" xmlns:wfs="http://www.opengis.net/wfs" xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml" xmlns:wcs="http://www.opengis.net/wcs/1.1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd">' + 
        '<ows:Identifier>gs:CreateResultsLayer</ows:Identifier>' + 
        '<wps:DataInputs>'+
        '<wps:Input>' + 
        '<ows:Identifier>results</ows:Identifier>' +         
        '<wps:Reference mimeType="text/xml" xlink:href="'+CONFIG.n52Endpoint+'/WebProcessingService" method="POST">' + 
        '<wps:Body><![CDATA[<?xml version="1.0" encoding="UTF-8"?>' + 
        '<wps:Execute xmlns:wps="http://www.opengis.net/wps/1.0.0" xmlns:wfs="http://www.opengis.net/wfs" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" service="WPS" version="1.0.0" xsi:schemaLocation="http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsExecute_request.xsd">' + 
        '<ows:Identifier>org.n52.wps.server.r.DSAS_stats</ows:Identifier>' + 
        '<wps:DataInputs>' +
        '<wps:Input>' + 
        '<ows:Identifier>input</ows:Identifier>' + 
        '<wps:Reference xlink:href="' + geoserverEndpoint + CONFIG.tempSession.getCurrentSessionKey() + '/wfs" mimeType="text/xml">' + 
        '<wps:Body>' + 
        '<wfs:GetFeature service="WFS" version="1.1.0" outputFormat="GML2" xmlns:ogc="http://www.opengis.net/ogc">' + 
        '<wfs:Query typeName="'+intersects+'" />' + 
        '<ogc:PropertyName>TransectID</ogc:PropertyName>' + 
        '<ogc:PropertyName>Distance</ogc:PropertyName>' + 
        '<ogc:PropertyName>date</ogc:PropertyName>' + 
        '<ogc:PropertyName>Uncy</ogc:PropertyName>' + 
        '</wfs:GetFeature>' + 
        '</wps:Body>' + 
        '</wps:Reference>' + 
        '</wps:Input>' + 
        
        '<wps:Input>' + 
        '<ows:Identifier>ci</ows:Identifier>' + 
        '<wps:Data>' + 
        '<wps:LiteralData>'+ci+'</wps:LiteralData>' + 
        '</wps:Data>' + 
        '</wps:Input>' + 
        '</wps:DataInputs>' + 
        '<wps:ResponseForm>' + 
        '<wps:RawDataOutput>' + 
        '<ows:Identifier>output</ows:Identifier>' + 
        '</wps:RawDataOutput>' + 
        '</wps:ResponseForm>' + 
        '</wps:Execute>]]></wps:Body>' + 
        '</wps:Reference>' +         
        '</wps:Input>'+
        
        '<wps:Input>' + 
        '<ows:Identifier>transects</ows:Identifier>' + 
        '<wps:Reference mimeType="text/xml; subtype=wfs-collection/1.0" xlink:href="http://geoserver/wfs" method="POST">' + 
        '<wps:Body>' + 
        '<wfs:GetFeature service="WFS" version="1.0.0" outputFormat="GML2" xmlns:'+transects.split(':')[0]+'="gov.usgs.cida.ch.'+transects.split(':')[0]+'">' + 
        '<wfs:Query typeName="'+transects+'" />' + 
        '</wfs:GetFeature>' + 
        '</wps:Body>' + 
        '</wps:Reference>' + 
        '</wps:Input>' + 
		
		'<wps:Input>' + 
        '<ows:Identifier>intersects</ows:Identifier>' + 
        '<wps:Reference mimeType="text/xml; subtype=wfs-collection/1.0" xlink:href="http://geoserver/wfs" method="POST">' + 
        '<wps:Body>' + 
        '<wfs:GetFeature service="WFS" version="1.0.0" outputFormat="GML2" xmlns:'+intersects.split(':')[0]+'="gov.usgs.cida.ch.'+intersects.split(':')[0]+'">' + 
        '<wfs:Query typeName="'+intersects+'" />' + 
        '</wfs:GetFeature>' + 
        '</wps:Body>' + 
        '</wps:Reference>' + 
        '</wps:Input>' +  
        
        '<wps:Input>' + 
        '<ows:Identifier>workspace</ows:Identifier>' + 
        '<wps:Data>' + 
        '<wps:LiteralData>'+CONFIG.tempSession.getCurrentSessionKey()+'</wps:LiteralData>' + 
        '</wps:Data>' + 
        '</wps:Input>' +     
        
        '<wps:Input>' + 
        '<ows:Identifier>store</ows:Identifier>' + 
        '<wps:Data>' + 
        '<wps:LiteralData>ch-output</wps:LiteralData>' + 
        '</wps:Data>' + 
        '</wps:Input>' +      
        
        '<wps:Input>' + 
        '<ows:Identifier>layer</ows:Identifier>' + 
        '<wps:Data>' + 
        '<wps:LiteralData>'+transects.split(':')[1].replace('_transects', '') + Results.suffixes[0] +'</wps:LiteralData>' + 
        '</wps:Data>' + 
        '</wps:Input>' +    
        
        '</wps:DataInputs>' + 
        '<wps:ResponseForm>' + 
        '<wps:RawDataOutput>' + 
        '<ows:Identifier>intersections</ows:Identifier>' + 
        '</wps:RawDataOutput>' + 
        '</wps:ResponseForm>' + 
        '</wps:Execute>';

        return wps;
    },
    retrieveRSquigglePlotPNG : function() {
        var val = $("#results-list option:selected")[0].value;
        if (val) {
            var layerName = val.split(':')[1];
            var workspaceNS = 'gov.usgs.cida.ch.' + CONFIG.tempSession.getCurrentSessionKey();
            var exportForm = $('<form />').attr({
                'id' : 'export-form',
                'style' : 'display:none;visibility:hidden;',
                'method' : 'POST'
            }).
            append(
                $('<input />').attr({
                    'type' : 'hidden',
                    'name' : 'filename'
                }).val(layerName + '.png')).
            append(
                $('<input />').attr({
                    'type' : 'hidden',
                    'name' : 'layer'
                }).val(val)).
            append(
                $('<input />').attr({
                    'type' : 'hidden',
                    'name' : 'workspaceNS'
                }).val(workspaceNS)).
            append(
                $('<input />').attr({
                    'type' : 'hidden',
                    'name' : 'output'
                }).val('output')).
            append(
                $('<input />').attr({
                    'type' : 'hidden',
                    'name' : 'type'
                }).val('image/png')).
            append(
                $('<input />').attr({
                    'type' : 'hidden',
                    'name' : 'shortName'
                }).val(CONFIG.graph.enabled));
            $('body').append(exportForm);
            exportForm.attr('action', 'service/export/squiggle');
            exportForm.submit();
            exportForm.remove();
            
        }
    },
    retrieveResultsShapefile: function(){
        var layerName = $('#results-list').val();
        
        if(!layerName){
			CONFIG.ui.showAlert({
                message : 'Please select a result from the list.',
                displayTime : 7500,
                caller : Results
            });
        } else {
			var geoserverEndpoint = CONFIG.ows.geoserverProxyEndpoint.endsWith('/') ? CONFIG.ows.geoserverProxyEndpoint : CONFIG.ows.geoserverProxyEndpoint + '/';
			var url = geoserverEndpoint + 'wfs?' +
			'service=wfs&'+
			'version=2.0.0&'+
			'request=GetFeature&'+
			'typeName=' + escape(layerName) + '&' +
			'outputFormat=SHAPE-ZIP';
			window.open(url);
		}
        
    },
    retrieveResultsSpreadsheet: function(){
        var layerName = $('#results-list').val();
        
        if(!layerName){
			CONFIG.ui.showAlert({
                message : 'Please select a result from the list.',
                displayTime : 7500,
                caller : Results
            });
        } else {
			CONFIG.ows.getDescribeFeatureType({
				layerNS: layerName.split(':')[0],
				layerName: layerName.split(':')[1],
				callbacks: [
					function(describeFeatureResponse) {
						var propertyNames = describeFeatureResponse.featureTypes[0].properties.map(function(ft) {
							return ft.name;
						});
						var propertyNamesToExclude = ['the_geom', 'NSD'];

						//remove each excluded attribute name from the array
						propertyNamesToExclude.each(function(nameToExclude) {
							propertyNames.remove(nameToExclude);
						});

						var stringPropertyNames = escape(propertyNames.join(','));

						var url = CONFIG.ows.geoserverProxyEndpoint + 'wfs?' +
								'service=wfs&' +
								'version=2.0.0&' +
								'request=GetFeature&' +
								'typeName=' + layerName + '&' +
								'outputFormat=csv&' +
								'propertyName=' + stringPropertyNames;
						//reset it to blank in case the user downloads the same file again
						$('#download').attr('src', '').attr('src', url);
					}
				]
			});
		}
    }
};
