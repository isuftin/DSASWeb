/*jslint browser: true */
/*global define*/
define([
	'underscore',
	'handlebars',
	'views/BaseView',
	'models/FileUploadModel',
	'utils/logger',
	'text!templates/file-upload-view.html'
], function (
		_,
		Handlebars,
		BaseView,
		FileUploadModel,
		log,
		template
		) {
	"use strict";

	var view = BaseView.extend({
		model: FileUploadModel,
		events: {
			'click #button-file-select': 'handleFileSelectClick',
			'change #input-file': 'handleUploadContentChange',
			'click #button-file-upload': 'handleUploadButtonClick'
		},
		template: Handlebars.compile(template),
		render: function (args) {
			args = args || {};
			BaseView.prototype.render.apply(this, arguments);
			return this;
		},
		initialize: function (args) {
			log.debug("DSASweb file upload view initializing");
			args = args || {};

			this.model = args.uploadModel ? args.uploadModel : this.model;
			args.context = this.model.attributes;
			BaseView.prototype.initialize.apply(this, [args]);
		},
		wireFileControls: function () {

		},
		handleFileSelectClick: function () {
			this.$('#input-file').prop('value', '');
			this.$('#container-file-info').addClass('hidden');
			this.$('#input-file').click();
		},
		handleUploadContentChange: function (e) {
			var chosenFile = e.target.files[0];
			var name = chosenFile.name;
			var size = chosenFile.size;
			var type = chosenFile.type;
			var lastModified = chosenFile.lastModified;
			var $infoContainer = this.$('#container-file-info');
			var $nameContainer = this.$('#container-file-info-filename');
			var $sizeContainer = this.$('#container-file-info-filesize');
			var $typeContainer = this.$('#container-file-info-filetype');
			var $typeLastModified = this.$('#container-file-info-filelastmod');

			$infoContainer.addClass('hidden');

			var validFileType = _.find(this.model.get('allowedFileTypes'), function (t) {
				return name.indexOf(t) !== -1;
			}) !== undefined;

			if (!validFileType) {
				log.debug("File must be one of: " + this.model.get('allowedFileTypes').join(","));
			} else if (size > this.model.get('maxFileSize')) {
				log.debug("File too large");
			} else {
				// Update the info container with file information
				$nameContainer.html(name);
				$sizeContainer.html(size);
				$typeContainer.html(type);
				$typeLastModified.html(new Date(lastModified).toLocaleDateString() + " " + new Date(lastModified).toLocaleTimeString());
				$infoContainer.removeClass('hidden');
			}
			return chosenFile;
		},
		handleUploadButtonClick: function () {
			var file = this.$('#input-file')[0].files[0];
			var formData = new FormData();
			formData.append("fileType", this.model.get('fileType'));
			formData.append("file", file);

			this.model.get('xhr').upload.addEventListener("progress", function (e) {
				if (e.lengthComputable) {
					var percentage = Math.round((e.loaded * 100) / e.total);
					log.info(percentage);
				}
			}, false);

			this.model.get('xhr').onreadystatechange = function (e) {
				var target = e.currentTarget;
				var status = target.status;
				var readyState = target.readyState;
				var responseString = target.response;
				var token;

				// I'm expecting an 'Accepted' response with a location header
				if (readyState === 2 && status === 202) {
					var token = target.getResponseHeader('Location').split('/').slice(-1);
					this.model.set('token', token);
					this.scope.$('#container-file-info').addClass('hidden');
					this.scope.handleFileStaged(token);
				} else if (readyState === 4 && responseString) {
					switch (status) {
						case 404:
							break;
						case 500:
							break;
					}
					this.scope.$('#container-file-info').addClass('hidden');
				}
			};

			this.model.get('xhr').scope = this.callbackScope || this;
			this.model.get('xhr').open("POST", this.model.get('uploadEndpoint'), true);
			this.model.get('xhr').send(formData);
			return this.model.get('xhr');
		},
		getColumnsFromStagedUpload: function (token) {
			return $.ajax(this.SHORELINE_STAGE_ENDPOINT, {
				'data': {
					'action': 'read-dbf',
					'token': token
				}
			});
		},
		handleFileStaged: function (token) {
			var token = this.model.get('token');
			this.getColumnsFromStagedUpload(token)
					.done()
					.fail()
			
			ShorelineUtil.getShorelineHeaderColumnNames(token)
					.done($.proxy(function (response) {
						var headers = response.headers.split(","),
								foundAllRequiredColumns = false,
								// Returns an object with headers for keys and blank strings 
								// for values: {'a': '', 'b': '', 'c': ''}
								layerColumns = _.object(headers, Array.apply(null, Array(headers.length))
										.map(function () {
											return '';
										}));

						if (headers.length < ShorelineUtil.MANDATORY_COLUMNS.length) {
							log.warn('Shorelines.js::addShorelines: There ' +
									'are not enough attributes in the selected ' +
									'shapefile to constitute a valid shoreline. ' +
									'Will be deleted. Needed: ' +
									ShorelineUtil.MANDATORY_COLUMNS.length +
									', Found in upload: ' + headers.length);
						} else {
							layerColumns = ShorelineUtil.createLayerUnionAttributeMap({
								layerColumns: layerColumns
							});

							// Do we have all the columns we need?
							_.each(ShorelineUtil.MANDATORY_COLUMNS, function (mc) {
								if (_.values(layerColumns).indexOf(mc) === -1) {
									foundAllRequiredColumns = false;
								}
							}, this);

							_.each(ShorelineUtil.DEFAULT_COLUMNS, function (col) {
								if (_.values(layerColumns).indexOf(col.attr) === -1) {
									foundAllRequiredColumns = false;
								}
							}, this);

							if (!foundAllRequiredColumns) {
								// User needs to match columns 
								var columnMatchingModel = new ColumnMatchingModel({
									layerColumns: layerColumns,
									layerName: SessionUtil.getCurrentSessionKey() + "_shorelines",
									defaultColumns: ShorelineUtil.DEFAULT_COLUMNS,
									columnKeys: _.keys(layerColumns),
									mandatoryColumns: ShorelineUtil.MANDATORY_COLUMNS
								}),
										columnMatchingView = new ColumnMatchingView({
											model: columnMatchingModel,
											router: this.router
										}),
										modalView = new ModalWindowView({
											model: new ModalModel({
												title: 'Column Information Required',
												view: columnMatchingView,
												autoShow: true
											})
										}).render();

								$(modalView.el).on('shown.bs.modal', $.proxy(function () {
									this.moveKnownColumns();
								}, columnMatchingView));

								this.listenToOnce(this.appEvents, this.appEvents.shorelines.columnsMatched, function () {
									modalView.remove();
									ShorelineUtil
											.importShorelineFromToken({
												token: token,
												workspace: SessionUtil.getCurrentSessionKey(),
												layerColumns: columnMatchingModel.get('layerColumns'),
												context: this
											})
											.done(this.handleImportDone)
											.fail(this.handleImportFail);
								});

							} else {
								ShorelineUtil.
										importShorelineFromToken({
											token: token,
											workspace: SessionUtil.getCurrentSessionKey(),
											layerColumns: layerColumns,
											context: this
										})
										.done(this.handleImportDone)
										.fail(this.handleImportFail);
							}

						}
					}, this))
					.fail(function () {
						// TODO
					});
		},
	});

	return view;
});