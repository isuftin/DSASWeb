define([
	'loglevel',
	'module'
], function (ll, module) {
	"use strict";
	
	// LogLevel is set to only log warn level by default to keep a clean console
	// on production tiers. Figure out if what tier I'm on here and set logging 
	// levels appropriately
	if (module.config().isDevelopment) {
		ll.enableAll();
	}

	return ll;
});