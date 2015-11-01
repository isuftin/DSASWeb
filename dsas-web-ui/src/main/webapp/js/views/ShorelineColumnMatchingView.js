/*jslint browser: true */
/*global define*/
define([
	'handlebars',
	'views/BaseView',
	'utils/logger',
	'underscore',
	'jquery',
	'text!templates/shoreline-column-matching-view.html'
], function (Handlebars, BaseView, log, _, $, template) {
	"use strict";
	var view = BaseView.extend({
		events: {
			'drop .drop-holder': 'dropHandler'
		},
		template: Handlebars.compile(template),
		render: function (options) {
			options = options || {};
			this.context = this.model.toJSON();
			BaseView.prototype.render.apply(this, [options]);
			this.initializeDragDrop();
			return this;
		},
		initialize: function (options) {
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
		},
		initializeDragDrop: function () {
			
			var layerName = this.model.get("layerName");

			this.$('.' + layerName + '-drag-item').draggable({
				containment: '#' + layerName + '-drag-drop-row',
				scroll: false,
				snap: '.drop-holder',
				snapMode: 'inner',
				cursor: 'move',
				revert: 'invalid',
				stack: '.' + layerName + '-drag-item'
			});

			this.$('.drop-holder').droppable({
				greedy: true,
				activeClass: 'ui-state-highlight',
				hoverClass: 'drop-hover',
				tolerance: 'intersect'
			});
		},
		dropHandler: function (e, ui) {
			var draggable = ui.draggable;
			var dragId = draggable.attr('id');
			var dropId = e.target.id;
			var layerAttribute = dragId.substr(0, dragId.indexOf('-drag-item'));
			var layerMappingAttribute = dropId.substr(0, dropId.indexOf('-drop-item'));
			var mapping = this.model.get('layerColumns');
			var mandatoryColumns = this.model.get('mandatoryColumns');

			// Figure out if we are in a drag or drop well
			if ($(e.target).hasClass('right-drop-holder')) {
				// right column, add to map
				mapping[layerAttribute] = layerMappingAttribute;
			} else { 
				// left column, remove from map
				mapping[layerAttribute] = '';
			}

			// Check that all of the required columns are mapped properly
			var readyToUpdate = _.difference(mandatoryColumns, _.values(mapping)).length === 0;
			
			if (readyToUpdate) {
				this.$('#button-shorelines-update-columns').removeAttr('disabled');
			} else {
				this.$('#button-shorelines-update-columns').prop('disabled', true);
			}
		}
	});

	return view;
});