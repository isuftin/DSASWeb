/*global define*/
define([
	'backbone'
], function (Backbone) {
	"use strict";
	var modalModel = Backbone.Model.extend({
		defaults : {
			subView : null, // The view to use for the content of the modal window
			title : 'I need a title',
			autoShow : false, // Should the modal window show up immediately after rendering?
			modalOptions : {} // http://getbootstrap.com/javascript/#modals-options
		}
	});
	return modalModel;
});