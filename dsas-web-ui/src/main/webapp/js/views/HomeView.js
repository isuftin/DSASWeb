/*jslint browser: true */
/*global define*/
define([
	'handlebars',
	'views/BaseView',
	'views/NavigationView',
	'views/MapView',
	'views/NotificationView',
	'utils/logger',
	'utils/sessionUtil',
	'collections/sessionCollection',
	'models/sessionModel',
	'text!templates/home-view.html'
], function (Handlebars, BaseView, NavigationView, MapView, NotificationView, log, SessionUtil, SessionCollection, SessionModel, template) {
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

			this.subViews.navView = new NavigationView({
				parent: this,
				router: options.router,
				appEvents : options.appEvents
			});

			this.subViews.mapView = new MapView({
				parent: this,
				router: options.router,
				appEvents : options.appEvents
			});

			this.subViews.notificationView = new NotificationView({
				parent: this,
				router: options.router,
				appEvents : options.appEvents
			});

			this.subViews.notificationView.setElement(this.$('#notification-span'));

			// Create a new session collection, check if it exists in localstorage. 
			// If so, I'm done. Otherwise, call out to the server to create a workspace
			// for this session and then create the session based on the workspace
			// name. 
			this.collection = new SessionCollection();
			this.collection.fetch();
			if (this.collection.models.length === 0) {
				SessionUtil
						.prepareSession()
						.done($.proxy(function (response) {
							var workspace = response.workspace;
							this.collection.create(new SessionModel({
								id: workspace
							}));
						}, this))
						.error(function (response) {
							// TODO - What happens if I can't create a session on
							// the server? If I can't do that, I have to bail out 
							// of the application because the user can't upload
							// any files or really do much of anything. Send the
							// user to a 500 Error page?
							log.error("Could not create a session on the workspace.");
						});
			}
			BaseView.prototype.initialize.apply(this, arguments);
		},
		remove: function () {
			BaseView.prototype.remove.apply(this, arguments);
			return this;
		}
	});

	return view;
});