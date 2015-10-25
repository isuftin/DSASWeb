/* global jasmine */
/* global expect */

define([
	"backbone",
	"sinon",
	'jquery',
	'controller/AppRouter'
], function (Backbone, sinon, $, Router) {
	"use strict";
	describe("Router", function () {
		beforeEach(function () {
			this.$div = $('<div />')
					.prop({
						id: "page-content-container",
						role: "application"
					}).addClass("container-fluid");
			$('body').append(this.$div);
			
			this.server = sinon.fakeServer.create();
			this.server.respondWith("GET", /service\/session*/, [
				200,
				{"Content-Type": "application/json"},
				JSON.stringify({"success":"true"})
			]);
			
			this.router = new Router();
			this.routeSpy = sinon.spy();
			this.router.bind("route:displayShorelineToolset", this.routeSpy);
			
			try {
				Backbone.history.start({silent: true});
			} catch (e) {
			}
			
			this.router.navigate("elsewhere");
		});

		it("does not fire for unknown paths", function () {
			this.router.navigate("unknown", {trigger : true});
			this.server.respond();
			expect(this.routeSpy.notCalled).toBeTruthy();
		});
		

		afterEach(function () {
			this.$div.remove();
			this.server.restore();
		});
	});
});