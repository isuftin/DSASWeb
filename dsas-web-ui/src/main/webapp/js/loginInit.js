/*jslint browser: true*/
/*global define*/
define([
	'controller/LoginRouter',
	'utils/logger',
	'backbone',
	'module',
	'jquery',
	'bootstrap', // Load up bootstrap to get it worked into jquery
	'jqueryCookie' // Same with JQuery Cookie
], function (
		Router,
		log,
		Backbone,
		module,
		$) {
	"use strict";
	
	this.router = new Router();
	
	$.removeCookie(module.config().authTokenLabel, { path: '/' });
	
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