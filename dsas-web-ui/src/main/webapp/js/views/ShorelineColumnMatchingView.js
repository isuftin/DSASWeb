/*jslint browser: true */
/*global define*/
define([
	'handlebars',
	'views/BaseView',
	'utils/logger',
	'underscore',
	'text!templates/shoreline-column-matching-view.html'
], function (Handlebars, BaseView, log,  _, template) {
	"use strict";
	var view = BaseView.extend({
		template: Handlebars.compile(template),
		render : function (options) {
			options = options || {};
			BaseView.prototype.render.apply(this, [options]);
			$(this.el).appendTo($('body'));
			return this;
		},
		initialize: function (options) {
			
			this.layerColumns = options.layerColumns;
			
			Handlebars.registerHelper('printDefaultValue', function () {
				if (this.defaultValue) {
					return new Handlebars.SafeString(' Default: "' + this.defaultValue + '"');
				} else {
					return '';
				}
			});
			
			BaseView.prototype.initialize.apply(this, [options]);
			
			log.debug("DSASweb Shoreline column matching view initializing");
			
			return this.render();
		}
	});
	
	return view;
});