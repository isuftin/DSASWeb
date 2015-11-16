/*jslint browser: true*/
/*global define*/
define([
	'controller/AppRouter',
	'utils/logger',
	'backbone',
	'module',
	'bootstrap', // Load up bootstrap to get it worked into jquery
	'jquery'
], function (
		Router,
		log,
		Backbone,
		module) {
	"use strict";
	this.router = new Router();

	Backbone.history.start({
		root: module.config().contextPath
	});
	log.info("DSASweb inititialized");

	return this.router;
});