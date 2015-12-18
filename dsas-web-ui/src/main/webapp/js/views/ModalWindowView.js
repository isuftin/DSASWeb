// A view that constructs a modal window. 
// The content is provided via a view using the 'contentView' option.

/*jslint browser: true */
/*global define*/
define([
	'handlebars',
	'views/BaseView',
	'utils/logger',
	'text!templates/modal-window-view.html'
], function (Handlebars, BaseView, log, template) {
	"use strict";
	var view = BaseView.extend({
		events: {
			'click #window-modal-button-close': 'remove',
			'hidden.bs.modal #window-modal': 'remove'
		},
		template: Handlebars.compile(template),
		render: function (options) {
			options = options || {};
			BaseView.prototype.render.apply(this, [options]);

			$(this.el).appendTo($('body'));

			$(this.model.get('view').el).appendTo(this.$('.modal-body'));

			if (this.model.get('autoShow')) {
				this.show();
			}
			return this;
		},
		initialize: function (options) {
			options = options || {};
			this.modalOptions = options.modalOptions || {};
			this.autoShow = options.autoShow || false;
			this.contentView = options.contentView;

			BaseView.prototype.initialize.apply(this, [options]);

			this.context.title = this.model.get("title");

			log.debug("DSASweb modal window view initializing");

			return this;
		},
		show: function () {
			this.$('#window-modal').modal(this.modalOptions);
			return this;
		},
		remove: function () {
			$('.modal-backdrop').fadeOut(function () {
				$(this).remove();
			});
			BaseView.prototype.remove.apply(this, arguments);
			return this;
		}
	});

	return view;
});