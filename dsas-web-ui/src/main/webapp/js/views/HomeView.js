/*jslint browser: true */
define([
	'handlebars',
	'views/BaseView',
	'views/NavigationView',
	'views/MapView',
	'utils/logger',
	'text!templates/home.html'
], function (Handlebars, BaseView, NavigationView, MapView, log, template) {
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
			
			return this;
		},
		/*
		 * @constructs
		 * @param {Object} options
		 *		@prop collection {ModelCollection instance}
		 *      @prop el {Jquery element} - render view in $el.
		 */
		initialize: function () {
			log.debug("DSASweb Home view initializing");
			
			this.subViews.navView = new NavigationView();
			this.subViews.mapView = new MapView();
			
			BaseView.prototype.initialize.apply(this, arguments);
		},
		
		remove: function () {
			BaseView.prototype.remove.apply(this, arguments);
			return this;
		}
	});

	return view;
});