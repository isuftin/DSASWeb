/*jslint browser: true */
/*global define*/
define([
	'underscore',
	'handlebars',
	'views/BaseView',
	'utils/logger',
	'text!templates/file-upload-view.html'
], function (
		_,
		Handlebars,
		BaseView,
		log,
		template
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
				var location;
				var token;
				var targetReadyState = 4; // http://www.w3schools.com/ajax/ajax_xmlhttprequest_onreadystatechange.asp
				
				if (readyState === targetReadyState) {
					switch (status) {
						case 202:
							location = this.getResponseHeader("location");
							// TODO: Not yet implemented the functionality after staging PDB
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
		}
	});

	return view;
});