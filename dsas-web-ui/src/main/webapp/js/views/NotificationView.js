/*jslint browser: true */
define([
	'handlebars',
	'views/BaseView',
	'utils/logger',
	'text!templates/notification-view.html'
], function (Handlebars, BaseView, log, template) {
	"use strict";

	var view = BaseView.extend({
		template: Handlebars.compile(template),
		/*
		 * Renders the object's template using it's context into the view's element.
		 * @returns {extended BaseView}
		 */
		render: function () {
			BaseView.prototype.render.apply(this, arguments);
			return this;
		},
		/*
		 * @constructs
		 * @param {Object} options
		 *		@prop collection {ModelCollection instance}
		 *      @prop el {Jquery element} - render view in $el.
		 */
		initialize: function (options) {
			log.debug("DSASweb Notification view initializing");
			BaseView.prototype.initialize.apply(this, [options]);
			return this;
		},
		/**
		 * 
		 * @param {Object} opts
		 *		@prop notification {String} The message to display
		 *		@prop severity {NotificationView.severity} The visual severity of the message
		 *		@prop displayTime {Number} Time, in milliseconds, to display the notification. -1 for permanent.
		 * @returns {undefined}
		 */
		createNotification: function (opts) {
			var notification = opts.notification,
					severity = opts.severity,
					displayTime = opts.displayTime;

			// TODO: Create notification
			log.debug(JSON.stringify(opts));
		},
		severity: {
			DEBUG: "debug",
			INFO: "info",
			WARN: "warn"
		}
	});

	return view;
});