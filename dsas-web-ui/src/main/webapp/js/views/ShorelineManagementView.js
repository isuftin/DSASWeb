/*jslint browser: true */
define([
	'handlebars',
	'views/BaseView',
	'utils/logger',
	'text!templates/shoreline-management-view.html'
], function (Handlebars, BaseView, log, template) {
	"use strict";
	var view = BaseView.extend({
		events: {
			'click #button-shorelines-file-select': 'handleFileSelectClick',
			'click #button-shorelines-upload': 'handleUploadButtonClick',
			'change #input-shorelines-file': 'handleUploadContentChange'
		},
		template: Handlebars.compile(template),
		initialize: function (options) {
			BaseView.prototype.initialize.apply(this, [options]);
			return this;
		},
		render: function (options) {
			options = options || {};
			BaseView.prototype.render.apply(this, [options]);
			$(this.el).appendTo(options.el);
			return this;
		},
		handleFileSelectClick: function () {
			this.$('#input-shorelines-file').click();
		},
		handleUploadButtonClick: function () {
			var file = document.getElementById('input-shorelines-file').files[0],
					xhr = new XMLHttpRequest(),
					workspace = localStorage.dsas;

			var formData = new FormData();
			formData.append("file", file);

			xhr.upload.addEventListener("progress", function (e) {
				if (e.lengthComputable) {
					var percentage = Math.round((e.loaded * 100) / e.total);
					log.info(percentage);
				}
			}, false);

			xhr.onreadystatechange = function (e) {
				var status = e.currentTarget.status,
					readyState = e.currentTarget.readyState,
					responseString = e.currentTarget.response,
					response,
					token;
			
				if (readyState === 4 && responseString) {
					switch (status) {
						case 200:
							response = JSON.parse(responseString);
							token = response.token;
							this.scope.handleFileStaged(token);
							break;
						case 404:
							break;
						case 500:
							break;
					}
					this.scope.$('#container-shorelines-file-info').addClass('hidden');
				}
			};
			
			xhr.scope = this;
			xhr.open("POST", "service/stage-shoreline?action=stage&workspace=" + workspace, true);
			xhr.send(formData);
			return xhr;
		},
		handleUploadContentChange: function (e) {
			var chosenFile = e.target.files[0],
				name = chosenFile.name,
				size = chosenFile.size,
				$infoContainer = this.$('#container-shorelines-file-info'),
				$nameContainer = this.$('#container-shorelines-file-info-filename'),
				$sizeContainer = this.$('#container-shorelines-file-info-filesize');

			$infoContainer.addClass('hidden');

			if (!name.endsWith(".zip")) {
				// TODO - Display alert
				log.debug("Not a zip");
			} else if (size > Number.MAX_VALUE) {
				// TODO - Figure out intelligent max size for a file
				log.debug("File too large");
			} else {
				// Update the info container with file information
				$nameContainer.html(name);
				$sizeContainer.html(size);
				$infoContainer.removeClass('hidden');
			}
		},
		handleFileStaged: function (token) {
			
		}
	});

	return view;
});