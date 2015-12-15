/*jslint browser: true */
/*global define*/
define([
	'underscore',
	'handlebars',
	'views/BaseView',
	'utils/logger',
	'utils/PdbUtil',
	'views/ModalWindowView',
	'views/ColumnMatchingView',
	'models/ColumnMatchingModel',
	'models/ModalModel',
	'text!templates/file-upload-view.html',
	'utils/AppEvents'
], function (
		_,
		Handlebars,
		BaseView,
		log,
		PdbUtil,
		ModalWindowView,
		ColumnMatchingView,
		ColumnMatchingModel,
		ModalModel,
		template,
		AppEvents
		) {
	"use strict";

	var view = BaseView.extend({
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
			log.debug("DSASweb Proxy Datum Bias management view initializing");
			args = args || {};

			this.fileType = args.fileType;
			this.maxFileSize = args.maxFileSize || Number.MAX_VALUE;
			this.allowedFileTypes = args.allowedFileTypes || [];
			this.uploadEndpoint = args.uploadEndpoint;
			this.xhr = new XMLHttpRequest();
			this.callbackScope = args.callbackScope || this;

			BaseView.prototype.initialize.apply(this, arguments);
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

			var validFileType = _.find(this.allowedFileTypes, function (t) {
				return name.indexOf(t) !== -1;
			}) !== undefined;

			if (!validFileType) {
				log.debug("File must be one of: " + this.allowedFileTypes.join(","));
			} else if (size > this.maxFileSize) {
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
		renderColumnMatchingView: function (args) {
			args = args || {};
			var tokenLocation = args.tokenLocation;

		},
		handleUploadButtonClick: function () {
			var file = this.$('#input-file')[0].files[0];
			var formData = new FormData();
			formData.append("fileType", this.fileType);
			formData.append("file", file);

			this.xhr.upload.addEventListener("progress", function (e) {
				if (e.lengthComputable) {
					var percentage = Math.round((e.loaded * 100) / e.total);
					log.info(percentage);
				}
			}, false);

			this.xhr.onreadystatechange = function (e) {
				var status = e.currentTarget.status;
				var readyState = e.currentTarget.readyState;
				var targetReadyState = 4; // http://www.w3schools.com/ajax/ajax_xmlhttprequest_onreadystatechange.asp

				if (readyState === targetReadyState) {
					switch (status) {
						case 202:
							this.scope.handleFileStaged({
								location: this.getResponseHeader("location")
							});
							break;
						case 404:
							log.error("NOT FOUND");
							break;
						case 500:
							log.error("ERROR");
							break;
					}
					this.scope.$('#container-file-info').addClass('hidden');
				}
			};

			this.xhr.scope = this.callbackScope || this;
			this.xhr.open("POST", this.uploadEndpoint, true);
			this.xhr.send(formData);
			return this.xhr;
		},
		handleFileStaged: function (args) {
			args = args || {};
			var location = args.location;
			var token = location.substr(location.lastIndexOf('/') + 1);
			$.get('..' + location + '/columns')
					.done($.proxy(function (response) {
						var headers = JSON.parse(arguments[0].headers);
						var foundAllRequiredColumns = false;
						var layerColumns = _.object(headers, Array.apply(null, Array(headers.length))
								.map(function () {
									return '';
								}));
						if (headers.length < PdbUtil.MANDATORY_COLUMNS.length) {
							log.warn('There are not enough attributes in the selected ' +
									'shapefile to constitute a valid shoreline. ' +
									'Will be deleted. Needed: ' +
									PdbUtil.MANDATORY_COLUMNS.length +
									', Found in upload: ' + headers.length);

						} else {
							layerColumns = PdbUtil.createLayerUnionAttributeMap({
								layerColumns: layerColumns
							});

							_.each(PdbUtil.MANDATORY_COLUMNS, function (mc) {
								if (_.values(layerColumns).indexOf(mc) === -1) {
									foundAllRequiredColumns = false;
								}
							}, this);

							if (!foundAllRequiredColumns) {
								// User needs to match columns 
								var columnMatchingModel = new ColumnMatchingModel({
									layerColumns: layerColumns,
									layerName: "pdb",
									columnKeys: _.keys(layerColumns),
									mandatoryColumns: PdbUtil.MANDATORY_COLUMNS
								});
								var columnMatchingView = new ColumnMatchingView({
									model: columnMatchingModel
								});
								var modalView = new ModalWindowView({
									model: new ModalModel({
										title: 'Column Information Required',
										view: columnMatchingView,
										autoShow: true
									})
								}).render();

								$(modalView.el).on('shown.bs.modal', $.proxy(function () {
									this.moveKnownColumns();
								}, columnMatchingView));

								this.listenToOnce(AppEvents, AppEvents.staging.columnsMatched, function () {
									modalView.remove();
									PdbUtil
											.importFromToken({
												token: token,
												workspace: 'published',
												location: location,
												layerColumns: columnMatchingModel.get('layerColumns'),
												context: this
											})
											.done(function () {
											//	debugger;
											})
											.fail(function () {
											//	debugger;
											});
								});

							} else {
								PdbUtil
										.importFromToken({
											token: token,
											layerColumns: columnMatchingModel.get('layerColumns'),
											context: this
										})
										.done(function () {
										//	debugger;
										})
										.fail(function () {
										//	debugger;
										});
							}
						}

					}, this))
					.fail(function (response) {
						log.error(response)
					});
		}
	});

	return view;
});