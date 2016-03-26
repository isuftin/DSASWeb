/* global $ */
/* global document */
/* global splashUpdate */
/* global initializeLogging */
/* global CONFIG */
/* global LOG */
/* global Session */
/* global UI */
/* global OWS */
/* global CCH */
/* global e */
$(document).ready(function () {
	"use strict";

	splashUpdate("Initializing Logging...");
	initializeLogging({
		LOG4JS_LOG_THRESHOLD: CONFIG.development ? 'debug' : 'info'
	});

	// Set up global jQuery AJAX properties
	$(document).ajaxStart(function () {
		LOG.trace('AJAX Call Started');
		$("#application-spinner").fadeIn();
	});
	$(document).ajaxStop(function () {
		LOG.trace('AJAX Call Finished');
		$("#application-spinner").fadeOut();
	});
	$(document).ajaxError(function (event, jqXHR, ajaxSettings, thrownError) {
		LOG.debug('AJAX Call Error: ' + thrownError);
		CONFIG.ui.showAlert({
			message: 'There was an error while communicating with the server. Check logs for more info. Please try again.',
			displayTime: 0
		});
		$("#application-spinner").fadeOut();
	});
	$.ajaxSetup({
		timeout: CONFIG.ajaxTimeout
	});

	// Utility class for the user interface
	splashUpdate("Initializing User Interface...");
	CONFIG.ui = new UI();

	splashUpdate("Initializing Sessions...");
	try {
		LOG.info('OnReady.js:: Initializing session objects');
		// Contains the pemanent session object which holds one or more sessions
		CONFIG.permSession = new CCH.Session('dsas', true);

		// Contains the non-permanent single-session object
		CONFIG.tempSession = new CCH.Session('dsas', false);

		var currentSessionKey = CONFIG.permSession.getCurrentSessionKey();
		LOG.info('OnReady.js:: Sessions created. User session list has ' + Object.keys(CONFIG.permSession.session.sessions).length + ' sessions.');
		LOG.info('OnReady.js:: Current session key: ' + currentSessionKey);
		CONFIG.tempSession.persistSession();
	} catch (e) {
		LOG.error('OnReady.js:: Session could not be read correctly');
		LOG.error(e.message);
		if (e.hasOwnProperty('func')) {
			e.func();
		}
		return;
	}

	// Map interaction object. Holds the map and utilities 
	splashUpdate("Initializing Map...");
	CONFIG.map = new Map();

	// Primarily a utility class
	splashUpdate("Initializing OWS services...");
	CONFIG.ows = new OWS();

	var finishLoadingApplication = function (data, textStatus, jqXHR) {
		CONFIG.ui.work_stages_objects.each(function (stage) {
				
			stage.appInit();
			
			if (typeof stage.populateFeaturesList === 'function') {
				stage.populateFeaturesList(data, textStatus, jqXHR);
			}

			CONFIG.tempSession.updateSessionFromWMS({
				stage: stage
			});
		});

		CONFIG.ui.precacheImages();
		
		// Update the last accessed time stamp on each session on the back end.
		// This prevents the sessions from being wiped
		$.each(CONFIG.permSession.session.sessions, function (i, session) {
			var sessionName = session.name;
			CONFIG.permSession.updateSession(sessionName)
					// Session not found on the back-end. Clean up both front and back ends
					.error($.proxy(function (resp) {
						if (resp.status === 404) {
							CONFIG.permSession.session.sessions = CONFIG.permSession.session.sessions.filter(function (a) {
								return a.id !== this.name;
							}, this);
							CONFIG.tempSession.persistSession();
						}
					}, this));
		});

		$('.qq-upload-button').addClass('btn btn-success');

		$('#application-overlay').fadeOut(2000, function () {
			$('#application-overlay').remove();
		});
	};

	var interrogateSessionResources = function () {
		CONFIG.ows.getWMSCapabilities({
			namespace: currentSessionKey,
			callbacks: {
				success: [
					CONFIG.tempSession.updateLayersFromWMS,
					finishLoadingApplication
				],
				error: [
					function () {
						var session = CONFIG.tempSession.createNewSession().sessions[0];
						CONFIG.permSession.session.sessions.push(session);
						CONFIG.permSession.session.currentSession = session.id;
						CONFIG.permSession.save();
						interrogateSessionResources();
					}
				]
			}
		});
	};

	var interrogatePublishedResources = function () {
		CONFIG.ows.getWMSCapabilities({
			namespace: CONFIG.name.published,
			callbacks: {
				success: [
					interrogateSessionResources,
					CONFIG.tempSession.updateLayersFromWMS
				],
				error: [interrogateSessionResources]
			}
		});
	};

	CONFIG.ows.getWFSCapabilities({
		callbacks: {
			success: [
				function () {
					CONFIG.ui.appInit();
					splashUpdate("Interrogating OWS server...");
					interrogatePublishedResources();
					splashUpdate("Working...");
				}
			],
			error: [
				function (responseObj) {
					// At this point, I can't properly run the application
					// so just show the error without removing the splash screen
					var errorMessage = '';

					if (responseObj && responseObj.data && responseObj.data.responseText) {
						errorMessage += '<br /> Error: ' + responseObj.data.responseText;
					}

					splashUpdate("The OWS server could not be contacted. The application cannot be loaded. " + errorMessage);
					CONFIG.ui.removeSplashSpinner();
				}
			]
		}
	});
});
