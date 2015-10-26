/*jslint browser: true*/
/*global define*/
define([
	'controller/AppRouter',
	'utils/logger',
	'backbone',
	'module',
	'underscore',
	'bootstrap' // Load up bootstrap to get it worked into jquery
], function (Router, log, Backbone, module, _) {
	"use strict";
	var router = new Router();
	
	Backbone.history.start({root: module.config().contextPath});

	log.info("DSASweb inititialized");

	return router;
});