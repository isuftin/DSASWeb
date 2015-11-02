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

		it("Gets a valid WMS GetCaps using getWMSCapabilities", function () {
			var getCapsDeferred = OwsUtil.getWMSCapabilities();
			getCapsDeferred.always($.proxy(function (response) {
				expect(response).not.toBe(null);
				expect(response.hasOwnProperty('capability')).toBeTruthy();
				expect(response.hasOwnProperty('service')).toBeTruthy();
				expect(response.service.title).toBe("GeoServer Web Map Service");
			}, this));
			this.server.respond();
		});
		
		it("Gets a 404 when calling getWMSCapabilities", function () {
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
		
		it("Properly creates bounds using 4326 array", function () {
			var sourceArray = ["-159.35177778318248", "21.957387826403924", "-159.29265037686733", "22.174701746065356"];
			var result = OwsUtil.getBoundsFromArray({
				array : sourceArray
			});
			
			// OpenLayers does not get precision exact after so many decimal places
			expect(result.left).toBeCloseTo(sourceArray[0]);
			expect(result.bottom).toBeCloseTo(sourceArray[1]);
			expect(result.right).toBeCloseTo(sourceArray[2]);
			expect(result.top).toBeCloseTo(sourceArray[3]);
		});
		
		it("Properly creates bounds using 4326 array and flip", function () {
			var sourceArray = ["-159.35177778318248", "21.957387826403924", "-159.29265037686733", "22.174701746065356"];
			var result = OwsUtil.getBoundsFromArray({
				array : sourceArray,
				flipAxis : true
			});
			
			expect(result.left).toBeCloseTo(sourceArray[1]);
			expect(result.bottom).toBeCloseTo(sourceArray[0]);
			expect(result.right).toBeCloseTo(sourceArray[3]);
			expect(result.top).toBeCloseTo(sourceArray[2]);
		});
		
		it("Properly creates bounds and transforms to 900913", function () {
			var sourceArray = ["-159.35177778318248", "21.957387826403924", "-159.29265037686733", "22.174701746065356"];
			var expectedResult = [-17738958.7573572, 2506409.9023636496, -17732376.72459576, 2532513.2277615];
			var result = OwsUtil.getBoundsFromArray({
				array : sourceArray,
				epsgToCode : "EPSG:900913"
			});
			
			expect(result.left).toBeCloseTo(expectedResult[0]);
			expect(result.bottom).toBeCloseTo(expectedResult[1]);
			expect(result.right).toBeCloseTo(expectedResult[2]);
			expect(result.top).toBeCloseTo(expectedResult[3]);
		});
	});
});