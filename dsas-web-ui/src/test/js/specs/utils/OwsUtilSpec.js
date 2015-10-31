/* global jasmine */
/* global expect */
define([
	"backbone",
	"sinon",
	'jquery',
	'utils/OwsUtil',
	'text!templates/wms_getcaps_response_example.xml'
], function (Backbone, sinon, $, OwsUtil, getCapsResponse) {
	"use strict";
	describe("OwsUtil", function () {
		beforeEach(function () {
			this.server = sinon.fakeServer.create();
			this.server.respondWith("GET", /.*ows.*request=GetCapabilities.*/, [
				200,
				{"Content-Type": "text/xml"},
				getCapsResponse
			]);
		});

		afterEach(function () {
			this.server.restore();
		});

		it("Get valid WMS GetCaps", function () {
			var getCapsDeferred = OwsUtil.getWMSCapabilities();
			getCapsDeferred.always($.proxy(function (response) {
				expect(response).not.toBe(null);
				expect(response.hasOwnProperty('capability')).toBeTruthy();
				expect(response.hasOwnProperty('service')).toBeTruthy();
				expect(response.service.title).toBe("GeoServer Web Map Service");
			}, this));
			this.server.respond();
		});
		
		it("Get 404", function () {
			var getCapsDeferred = OwsUtil.getWMSCapabilities({
				namespace : 'invalid'
			});
			getCapsDeferred.always($.proxy(function (data, text, statusText) {
				expect(text).toBe("error");
				expect(statusText).toBe("Not Found");
				expect(data.status).toBe(404);
			}, this));
			this.server.respond();
		});

	});
});