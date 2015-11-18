/*jslint browser: true */
/*global define*/
define([
	'jquery',
	'handlebars',
	'views/BaseView',
	'views/FileUploadView',
	'utils/logger',
	'text!templates/pdb-management-view.html'
], function ($,
		Handlebars,
		BaseView,
		FileUploadView,
		log,
		template) {
	"use strict";

	var view = BaseView.extend({
		template: Handlebars.compile(template),
		render: function () {
			BaseView.prototype.render.apply(this, arguments);

			this.fileUploadView = new FileUploadView({
				context: {
					fileTypeName: 'Proxy Datum Bias'
				},
				allowedFileTypes : [".zip"],
				fileType: 'pdb',
				uploadEndpoint: 'service/shapefile/stage'
			}).render();
			$(this.fileUploadView.el).appendTo(this.$('#pdb-management-upload'));
			this.fileUploadView.wireFileControls();

			return this;
		},
		initialize: function (args) {
			args = args || {};
			
			log.debug("DSASweb Proxy Datum Bias management view initializing");
			BaseView.prototype.initialize.apply(this, arguments);
		}
	});

	return view;
});