/*jslint browser: true */
/*global define*/
define([
	'jquery',
	'handlebars',
	'views/BaseView',
	'views/FileUploadView',
	'utils/logger',
	'text!templates/management-view.html',
	'backbone',
	'underscore',
	'module'
], function ($,
		Handlebars,
		BaseView,
		FileUploadView,
		log,
		template,
		Backbone,
		_,
		module) {
	"use strict";

	var view = BaseView.extend({
		template: Handlebars.compile(template),
		render: function () {
			BaseView.prototype.render.apply(this, arguments);

			this.fileUploadView = new FileUploadView({
				context: {
					fileTypeName: 'Proxy Datum Bias'
				},
				allowedFileTypes: [".zip"],
				fileType: 'pdb',
				uploadEndpoint: this.model.get('paths').pdbStaging
			}).render();
			$(this.fileUploadView.el).appendTo(this.$('#pdb-management-upload'));
			this.fileUploadView.wireFileControls();

			return this;
		},
		initialize: function (args) {
			args = args || {};
			var model = Backbone.Model.extend({
				constructor: function (args) {
					var paths = _.mapObject(args.paths, function (p) {
						if (p.substr(0, 1) === '/') {
							return p.substr(1);
						}
						return p;
					});
					var options = {
						paths : paths
					};
					Backbone.Model.apply(this, [options]);
				}
			});
			this.model = new model({
				paths: module.config().paths
			});
			log.debug("DSASweb management view initializing");
			BaseView.prototype.initialize.apply(this, arguments);
		}
	});

	return view;
});