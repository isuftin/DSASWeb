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
	
	Backbone.history.start({
		root: module.config().contextPath
	});
	log.info("DSASweb inititialized");

	return this.router;
});