/*jslint browser: true */
/*global define*/
define([
	'handlebars',
	'views/BaseView',
	'views/NavigationView',
	'views/MapView',
	'views/NotificationView',
	'utils/logger',
	'text!templates/home-view.html'
], function (Handlebars, BaseView, NavigationView, MapView, NotificationView, log, template) {
	"use strict";

	var view = BaseView.extend({
		template: Handlebars.compile(template),
		el: '#page-content-container',
		subViews: {},
		/*
		 * Renders the object's template using it's context into the view's element.
		 * @returns {extended BaseView}
		 */
		render: function () {
			BaseView.prototype.render.apply(this, arguments);

			this.subViews.navView.setElement(this.$('#navigation-span')).render();
			this.subViews.mapView.setElement(this.$('#map-span')).render();

			this.subViews.notificationView.createNotification({
				notification: "Welcome",
				severity: this.subViews.notificationView.severity.INFO,
				displayTime: 1000
			});

			return this;
		},
		/*
		 * @constructs
		 * @param {Object} options
		 *		@prop collection {ModelCollection instance}
		 *      @prop el {Jquery element} - render view in $el.
		 */
		initialize: function (options) {
			log.debug("DSASweb Home view initializing");
			
			BaseView.prototype.initialize.apply(this, arguments);

			this.appEvents = options.appEvents;
			
			var subViewParams = {
				parent: this,
				router: options.router,
				appEvents: this.appEvents,
				session : this.session
			};
			
			this.subViews.navView = new NavigationView(subViewParams);
			this.subViews.mapView = new MapView(subViewParams);
			this.subViews.notificationView = new NotificationView(subViewParams);
			this.subViews.notificationView.setElement(this.$('#notification-span'));
		},
		remove: function () {
			BaseView.prototype.remove.apply(this, arguments);
			return this;
		}
	});

	return view;
});