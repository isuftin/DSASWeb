/*jslint browser: true */
define([
	'handlebars',
	'views/BaseView',
	'utils/logger',
	'text!templates/shoreline-view.html'
], function (Handlebars, BaseView, log, template) {
	"use strict";
	var view = BaseView.extend({
		// Defines what the default tab in the shorelines toolbox is
		DEFAULT_TAB: "tabpanel-shorelines-view",
		template: Handlebars.compile(template),
		events: {
			'click #tabs-shorelines a': 'tabToggled',
			'click #button-shorelines-aoi-toggle': 'toggleAoiSelection',
			'click #button-shorelines-aoi-done': 'aoiSelected',
			'click #button-shorelines-file-select': 'handleFileSelectClick',
			'click #button-shorelines-upload': 'handleUploadButtonClick',
			'change #input-shorelines-file': 'handleUploadContentChange'
		},
		/*
		 * Renders the object's template using it's context into the view's element.
		 * @returns {extended BaseView}
		 */
		render: function (options) {
			options = options || {};
			this.context.activeTab = options.activeTab || this.DEFAULT_TAB;
			BaseView.prototype.render.apply(this, [options]);
			return this;
		},
		tabToggled: function (e) {
			var clickedTab = $(e.target).attr('data-target');
			this.router.navigate('shorelines/' + clickedTab, {trigger: true});
		},
		toggleAoiSelection: function (e) {
			var activated = !$(e.target).hasClass('active');
			log.debug("AOI Selection Toggled " + (activated ? "on" : "off"));
			this.appEvents.trigger(this.appEvents.shorelines.aoiSelectionToggled, activated);

			this.$('#description-aoi').toggleClass('hidden');
		},
		aoiSelected: function () {
			log.debug("AOI Selected");
			this.appEvents.trigger(this.appEvents.shorelines.aoiSelected);
		},
		processAoiSelection: function (bounds) {
			if (bounds) {
				// TODO
			}
		},
		/*
		 * @constructs
		 * @param {Object} options
		 */
		initialize: function (options) {
			Handlebars.registerHelper('ifIsActiveTab', function (a, b, output) {
				if (a === b) {
					return output;
				}
				return null;
			});
			log.debug("DSASweb Shoreline view initializing");
			BaseView.prototype.initialize.apply(this, [options]);

			this.listenTo(this.appEvents, this.appEvents.map.aoiSelected, this.processAoiSelection);


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

			xhr.upload.addEventListener("load", function (e) {
				
			}, false);

			xhr.open("POST", "service/stage-shoreline?action=stage&workspace=" + workspace, true);
			xhr.send(formData);
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
		remove: function () {
			BaseView.prototype.remove.apply(this);
			return this;
		}
	});

	return view;
});