/* global jasmine */
/* global expect */
define([
	'jquery',
	'utils/ShorelineUtil'
], function ($, ShorelineUtil) {
	"use strict";
	describe("ShorelineUtil", function () {

		it("Gets a consistent color from dates", function () {
			var dateArray = [
				"1855-07-01", "1857-07-01", "1858-07-01", "1863-07-01", "1867-07-01",
				"1868-07-01", "1869-03-01", "1869-07-01", "1870-07-01", "1899-07-01",
				"1900-07-01", "1910-07-01", "1913-07-01", "1919-10-20", "1924-08-01",
				"1924-09-01", "1924-10-01", "1924-11-01", "1925-02-01", "1925-05-01",
				"1933-06-01", "1933-09-01", "1933-10-01", "1933-11-01", "1933-12-01",
				"1951-07-01", "1952-02-01", "1964-03-01", "1971-03-01", "1971-11-01",
				"1973-09-01", "1973-10-01", "1978-03-01", "1979-02-01", "1999-10-10"];
			var expResult = [
				"#41a9d7", "#c31fa4", "#045b8a", "#9e5fec", "#a24c85", "#e3876b",
				"#a8f14f", "#24c351", "#bada1a", "#c149f5", "#a20f37", "#813c18",
				"#44eeca", "#ef3a3b", "#c3ca92", "#223f93", "#4c3f9d", "#abb39d",
				"#ca4b76", "#e7a877", "#a3d38c", "#c0308e", "#ea3098", "#49a598",
				"#a81999", "#3e2b83", "#a42067", "#643815", "#80b343", "#43e250",
				"#3ce412", "#66e41c", "#47528f", "#291975", "#c8a144"];
			var result = $.map(dateArray, function (d) {
				return ShorelineUtil.getColorForDateString(d);
			});

			for (var rIdx = 0; rIdx < result.length; rIdx++) {
				expect(expResult[rIdx]).toBe(result[rIdx]);
			}
		});

		it("Gets a consistent SLD from dates/workspace", function () {
			var dateArray = [
				"1855-07-01", "1857-07-01", "1858-07-01", "1863-07-01", "1867-07-01",
				"1868-07-01", "1869-03-01", "1869-07-01", "1870-07-01", "1899-07-01",
				"1900-07-01", "1910-07-01", "1913-07-01", "1919-10-20", "1924-08-01",
				"1924-09-01", "1924-10-01", "1924-11-01", "1925-02-01", "1925-05-01",
				"1933-06-01", "1933-09-01", "1933-10-01", "1933-11-01", "1933-12-01",
				"1951-07-01", "1952-02-01", "1964-03-01", "1971-03-01", "1971-11-01",
				"1973-09-01", "1973-10-01", "1978-03-01", "1979-02-01", "1999-10-10"];
			var workspace = "test";

			var result = ShorelineUtil.createSLDBody({
				dates: dateArray,
				workspace: workspace
			});
			
			expect(result).not.toBe(null);
			expect(result).toContain('<Name>test:test_shorelines</Name>');
			expect(result).toContain('<CssParameter name="stroke">#41a9d7</CssParameter>');
			expect(result).toContain('<ogc:Literal>1855-07-01</ogc:Literal>');
			
		});
	});
});