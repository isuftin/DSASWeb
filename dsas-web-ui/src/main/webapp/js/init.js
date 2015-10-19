/*jslint browser: true*/
/*global define*/
define([
	'controller/appRouter',
	'utils/logger',
	'backbone',
	'module'
], function (Router, log, Backbone, module) {
	"use strict";
	var router = new Router();

	Backbone.history.start({root: module.config().contextPath});

	log.info("DSASweb inititialized");

	return router;
});