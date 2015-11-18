/*jslint browser: true*/
/*global define*/
/*global session*/
define([
	'jquery',
	'controller/AppRouter',
	'utils/logger',
	'backbone',
	'module',
	'collections/SessionCollection',
	'models/sessionModel',
	'utils/SessionUtil',
	'bootstrap', // Load up bootstrap to get it worked into jquery
	'jqueryui', // Do the same with JQuery UI
	'bootstrapToggle', // and bootstrap toggle
	'tablesorter' // and tablesorter
], function (
		$,
		Router,
		log,
		Backbone,
		module,
		SessionCollection,
		SessionModel,
		SessionUtil) {
	"use strict";
	this.router = new Router();

	// TODO- Simplify session creation a bit. Getting a bit complicated

	this.sessionUpdateComplete = function () {
		this.router.session = this.session;
		Backbone.history.start({root: module.config().contextPath});
		log.info("DSASweb inititialized");
	};

	this.prepareSession = function (workspaceId) {
		SessionUtil
				.prepareSession(workspaceId)
				.done($.proxy(function (response) {
					var workspace = response.workspace;

					this.session.create(new SessionModel({
						id: workspace
					}));

					SessionUtil.updateSessionUsingWMSGetCapabilitiesResponse({
						session: this.session.get(SessionUtil.getCurrentSessionKey()),
						context: this
					}).done(this.sessionUpdateComplete);
				}, this))
				.error(function (response) {
					// TODO - What happens if I can't create a session on
					// the server? If I can't do that, I have to bail out 
					// of the application because the user can't upload
					// any files or really do much of anything. Send the
					// user to a 500 Error page?
					log.error("Could not create a session on the workspace.");
				});
	};

	// Create a new session collection, check if it exists in localstorage. 
	// If so, I'm done. Otherwise, call out to the server to create a workspace
	// for this session and then create the session based on the workspace name. 
	this.session = new SessionCollection();
	this.session.fetch();
	if (this.session.models.length === 0) {
		// There has not been a session created yet. Do so now. 
		this.prepareSession();
	} else {
		// There is one or more sessions in localStorage
		SessionUtil
				.updateSessionUsingWMSGetCapabilitiesResponse({
					session: this.session.get(SessionUtil.getCurrentSessionKey()),
					context: this
				})
				// The WMS GetCapabilities call for this workspace came back 
				.done(this.sessionUpdateComplete)
				// This workspace probably doesn't exist on the server. Use the 
				// workspace ID to (re)create it. 
				.fail(function (response) {
					// The server actually did not have this session created. It may
					// have been wiped. A new session should be created. 
					var responseCode = response.status;

					switch (responseCode) {
						case 404:
							this.prepareSession(SessionUtil.getCurrentSessionKey());
							break;
						default:
							break;
					}
				});
	}

	$.tablesorter.addParser({
		id: 'vis',
		is: function () {
			return false;
		},
		format: function (s, table, cell) {
			return $(cell).find('input').prop('checked') ? 1 : 0;
		},
		type: 'numeric'
	});

	return this.router;
});
