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

	var contextPath = module.config().contextPath;
	if (!contextPath.slice(-1) === '/') {
		contextPath += '/';
	}
	
	Backbone.history.start({
		root: contextPath
	});
	
	log.info("DSASweb inititialized");

	return this.router;
});