/*jslint browser: true */
define([
	'utils/logger',
	'utils/OwsUtil',
	'underscore'
], function (log, OwsUtil, _) {
	"use strict";
	var self = {};

	self.MAX_SESSION_ID_LENGTH = 34;

	self.getRandomUUID = function () {
		"use strict";
		var s = [], itoh = '0123456789ABCDEF';

		// Make array of random hex digits. The UUID only has 32 digits in it, but we
		// allocate an extra items to make room for the '-'s we'll be inserting.
		for (var i = 0; i < 36; i++) {
			s[i] = Math.floor(Math.random() * 0x10);
		}

		// Conform to RFC-4122, section 4.4
		s[14] = 4;  // Set 4 high bits of time_high field to version
		s[19] = (s[19] & 0x3) | 0x8;  // Specify 2 high bits of clock sequence

		// Convert to hex chars
		for (var j = 0; j < 36; j++) {
			s[j] = itoh[s[j]];
		}

		// Insert '-'s
		s[8] = s[13] = s[18] = s[23] = '-';

		return s.join('');
	};

	return {
		updateSessionUsingWMSGetCapabilitiesResponse: function (session, context) {
			var deferred = $.Deferred();
			OwsUtil.getWMSCapabilities({
				namespace: session.id,
				context: {
					deferred: deferred,
					session: session,
					context: context || this
				}
			}).done(function (capabilities) {
				_.each(capabilities.capability.layers, function (l) {
					var layerName = l.name;
					var layerBoundsArray = l.llbbox;
					var layerStage = l.title.substring(l.title.indexOf('_') + 1);
					var session = this.session.session;
					var sessionStage = session.get(layerStage);
					var stageBboxArray = sessionStage.bbox;
					// Check that it's a valid layer by testing the bbox
					// TODO - Figure out a better way of checking for a valid layer
					// or find out why some layers have a broken bbox like this
					var isValidLayer = _.reduce(layerBoundsArray, function (a, b) {
						return parseInt(a) + parseInt(b);
					}) !== -2;

					// Only add the layer to the session if it isn't already in there
					if (!_.contains(sessionStage.layers, layerName) && isValidLayer) {
						if (stageBboxArray.length === 0) {
							sessionStage.bbox = layerBoundsArray;
						} else {
							var stageBounds = OwsUtil.getBoundsFromArray({
								array: stageBboxArray
							});
							var incomingBounds = OwsUtil.getBoundsFromArray({
								array: layerBoundsArray
							});
							var extendedBounds = OwsUtil.extendBounds(stageBounds, incomingBounds);
							sessionStage.bbox = extendedBounds.toArray();
						}

						sessionStage.layers.push(layerName);
					}
					session.set(layerStage, sessionStage);
				}, this);
				deferred.resolveWith(this.context, [this.session, capabilities]);
			});
			return deferred;
		},
		prepareSession: function () {
			// - A session has not yet been created for perm storage. Probably the first
			// run of the application or a new browser with no imported session. Because 
			// the session is used in the namespace for WFS-T, it needs to 
			// not have a number at the head of it so add a random letter. I need 
			// to remove any dashes from the random id and lowercase the entire
			// id. This is due to a back-end id length limitation
			var randID = String.fromCharCode(97 + Math.round(Math.random() * 25)) + self.getRandomUUID().replace(/-/g, '').toLowerCase();

			// Prepare the session on the OWS server
			return $.get('service/session', {
				action: 'prepare',
				workspace: randID
			}).done(function () {
				log.info('Session.js::init: A workspace has been prepared on the OWS server with the name of ' + randID);
			}).fail(function () {
				log.error('Session.js::init: A workspace could not be created on the OWS server with the name of ' + randID);
			});
		},
		getCurrentSessionKey: function () {
			return localStorage.dsas;
		}
	};
});