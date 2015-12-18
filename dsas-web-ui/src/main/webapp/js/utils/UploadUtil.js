/*global define*/
define([
	'underscore',
	'jquery',
	'utils/logger',
], function (_,
		$,
		log) {
	"use strict";

	var me = {
		pollProcess: function (args) {
			var location = args.location;
			var deferred = args.deferred || $.Deferred();
			var context = args.context || this;
			$.get(location)
					.done($.proxy(function (response, status, jqXHR) {
						switch (jqXHR.status) {
							case 200:

								switch (response.status) {
									case 'running':
										deferred.notifyWith(context, [response]);
										setTimeout(function () {
											context.pollProcess({
												location: location,
												deferred: deferred,
												context: context
											})
										}, 1000);
										break;
									case 'terminated':
										if (response.ranSuccessfully) {
											deferred.resolveWith(context, [response]);
										} else {
											deferred.rejectWith(context, [response]);
										}
										break;
								}


						}
					}, this));
			return deferred;
		}
	};

	return _.extend({}, me);
});