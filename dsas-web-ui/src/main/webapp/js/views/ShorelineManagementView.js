/*jslint browser: true */
/*global define*/
define([
	'jquery',
	'underscore',
	'handlebars',
	'utils/AppEvents',
	'utils/logger',
	'utils/ShorelineUtil',
	'utils/SessionUtil',
	'views/BaseView',
	'views/ShorelineColumnMatchingView',
	'views/ModalWindowView',
	'views/FileUploadView',
	'models/FileUploadModel',
	'models/ColumnMatchingModel',
	'models/ModalModel',
	'text!templates/shoreline-management-view.html'
], function (
		$,
		_,
		Handlebars,
		AppEvents,
		log,
		ShorelineUtil,
		SessionUtil,
		BaseView,
		ColumnMatchingView,
		ModalWindowView,
		FileUploadView,
		FileUploadModel,
		ColumnMatchingModel,
		ModalModel,
		template) {
	"use strict";
	var view = BaseView.extend({
		events: {
			'click #button-shorelines-file-select': 'handleFileSelectClick',
			'click #button-shorelines-upload': 'handleUploadButtonClick',
			'change #input-shorelines-file': 'handleUploadContentChange'
		},
		SHORELINE_STAGE_ENDPOINT: 'service/stage-shoreline',
		template: Handlebars.compile(template),
		initialize: function (options) {
			options = options || {};
			options.appEvents = AppEvents;
			BaseView.prototype.initialize.apply(this, [options]);
			log.debug("DSASweb Shoreline management view initializing");
			return this;
		},
		render: function (options) {
			options = options || {};
			BaseView.prototype.render.apply(this, [options]);
			$(this.el).appendTo(options.el);
			
			this.fileUploadView = new FileUploadView({
				model : new FileUploadModel({
					'fileType' : 'Shoreline',
					'allowedFileTypes' : [".zip"],
					'mandatoryColumns' : ShorelineUtil.MANDATORY_COLUMNS,
					'defaultColumns' : ShorelineUtil.DEFAULT_COLUMNS
				})
			}).render();
			$(this.fileUploadView.el).appendTo(this.$('#shoreline-file-upload-container'));
			
			return this;
		},
		handleImportDone: function () {
			this.appEvents.trigger(this.appEvents.shorelines.layerImportSuccess, arguments);
		},
		handleImportFail: function () {
			this.appEvents.trigger(this.appEvents.shorelines.layerImportFail, arguments);
		}
	});

	return view;
});