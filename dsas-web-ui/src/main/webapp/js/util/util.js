/* global Baseline, Transects, Calculation, Results, LOG */
var Util = {
	noopFunction: function () {
		"use strict";
	},
	getRandomColor: function (args) {
		"use strict";
		args = args || {};
		var randomColor = '';
		var createRandomColor;
		if (!args.fromDefinedColors) {
			// http://paulirish.com/2009/random-hex-color-code-snippets/
			createRandomColor = function () {
				return '#' + Math.floor(Math.random() * 16777215).toString(16);
			};
		} else {
			createRandomColor = function () {
				return '#' + Util.getColorSets()[Math.floor(Math.random() * Util.getColorSets().length)];
			};
		}

		while (randomColor.length !== 7 ||
			randomColor.toLowerCase() === Baseline.reservedColor.toLowerCase() ||
			randomColor.toLowerCase() === Transects.reservedColor.toLowerCase() ||
			randomColor.toLowerCase() === Calculation.reservedColor.toLowerCase() ||
			randomColor.toLowerCase() === Results.reservedColor.toLowerCase()) {
			randomColor = createRandomColor();
		}
		return randomColor;
	},
	randomGUID: function () {
		"use strict";
		var S4 = function () {
			return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
		};

		return (S4() + S4() + "-" + S4() + "-" + S4() + "-" + S4() + "-" + S4() + S4() + S4());
	},
	randomUUID: function () {
		"use strict";
		var s = [], itoh = '0123456789ABCDEF';

		// Make array of random hex digits. The UUID only has 32 digits in it, but we
		// allocate an extra items to make room for the '-'s we'll be inserting.
		for (var i = 0; i < 36; i++) {
			s[i] = Math.floor(Math.random() * 0x10);
		}

		// Conform to RFC-4122, section 4.4
		s[14] = 4;  // Set 4 high bits of time_high field to version
		s[19] = (s[19] & 0x3) | 0x8;  // Specify 2 high bits of clock sequence

		// Convert to hex chars
		for (var j = 0; j < 36; j++) {
			s[j] = itoh[s[j]];
		}

		// Insert '-'s
		s[8] = s[13] = s[18] = s[23] = '-';

		return s.join('');
	},
	makeGroups: function (args) {
		"use strict";
		var groupItems = args.groupItems;
		var range = args.range;
		var preserveDate = args.preserveDate;
		LOG.info("Util.js::makeGroups:");
		var groupItem = groupItems[0];

		if (!isNaN(Date.parse(groupItem))) {
			if (range) {
				LOG.info("Util.js::makeGroups: Grouping by date range");
				var dateBegin = Date.create(groupItem);
				var dateEnd = Date.create(groupItem);
				$(groupItems.unique()).each(function (i, dateItem) {
					var date = Date.create(dateItem);
					if (date.isBefore(dateBegin)) {
						dateBegin = date;
					}

					if (date.isAfter(dateEnd)) {
						dateEnd = date;
					}
				});

				return Date.range(dateBegin, dateEnd).every('1 year');
			} else {
				LOG.info("Util.js::makeGroups: Grouping by available dates");
				var yearSet = [];
				var sortedUniqueGroupItems = groupItems.unique().sortBy(function (n) {
					return n.split('/')[2];
				});
				$(sortedUniqueGroupItems).each(function (i, dateItem) {
					var date;
					if (preserveDate) {
						date = Date.create(dateItem);
					} else {
						date = Date.create('01/01/' + dateItem.split('/')[2]);
					}
					yearSet.push(date);
				});
				return yearSet.unique();
			}
		}

		else if (!isNaN(groupItem)) {
			LOG.info("Grouping by number");
			var groups = groupItems.sortBy();
			$(groups).each(function (i, v) {
				groups[i] = Number.ceil(v);
			});
			return groups.unique();

		} else if (typeof firstGroupItem === 'string') {
			LOG.info("Grouping by string");
			return null;
		}
		return null;
	},
	createLayerUnionAttributeMap: function (args) {
		"use strict";
		var caller = args.caller;
		var attributes = args.attributes;
		var layerColumns = args.layerColumns;

		if (!layerColumns) {
			layerColumns = Object.extended();
			attributes.map(function (property) {
				return property.name;
			})
				.each(function (property) {
					layerColumns[property] = '';
				});
		}

		layerColumns.keys(function (columnName) {
			var eqColName = caller.mandatoryColumns.find(function (column) {
				return column.toLowerCase() === columnName.toLowerCase();
			});

			if (!eqColName && caller.defaultingColumns) {
				var defaultingColumnMatch = caller.defaultingColumns.find(function (column) {
					return column.attr.toLowerCase() === columnName.toLowerCase();
				});
				if (defaultingColumnMatch) {
					eqColName = defaultingColumnMatch.attr;
				}
			}

			if (eqColName) {
				layerColumns[columnName] = eqColName;
			}
		});

		return layerColumns;
	},
	createColorMap: function (colorGroups) {
		var colorMap = {};
		colorGroups.forEach(function (cg) {
			this[cg[1]] = cg[0];
		}, colorMap);
		return colorMap;
	},
	createColorGroups: function (groups) {
		"use strict";
		LOG.info('Util.js::createColorGroups: Creating color groups');
		var colorGroups = [];
		$(groups).each(function (i, group) {
			var color = CONFIG.colorGroups[group];

			if (!color) {
				color = Util.getRandomColor({
					fromDefinedColors: true
				}).toUpperCase(true);

				// Make sure that we don't already have this color in the colorGroups or white or black
				while (colorGroups.find(function (n) {
					return n[0] === color;
				}) || color === '#FFFFFF' || color === '#000000') {
					color = Util.getRandomColor({
						fromDefinedColors: false
					}).toUpperCase();
				}

				CONFIG.colorGroups[group] = color;
			}
			colorGroups.push([color, group]);
		});
		LOG.info('Util.js::createColorGroups: Created ' + colorGroups.length + ' color groups');
		return colorGroups;
	},
	getColorSets: function () {
		"use strict";
		return [
			"FFFFCC", "FFFF99", "FFFF66", "FFFF33", "FFFF00", "003366",
			"FFCCFF", "FFCCCC", "FFCC99", "FFCC66", "FFCC33", "FFCC00",
			"FF99FF", "FF99CC", "FF9999", "FF9966", "FF9933", "FF9900",
			"FF66FF", "FF66CC", "FF6699", "FF6666", "FF6633", "FF6600",
			"FF33FF", "FF33CC", "FF3399", "FF3366", "FF3333", "FF3300",
			"FF00FF", "FF00CC", "FF0099", "FF0066", "FF0033", "FF0000",
			"CCFFFF", "CCFFCC", "CCFF99", "CCFF66", "CCFF33", "CCFF00",
			"CCCCFF", "CCCCCC", "CCCC99", "CCCC66", "CCCC33", "CCCC00",
			"CC99FF", "CC99CC", "CC9999", "CC9966", "CC9933", "CC9900",
			"CC66FF", "CC66CC", "CC6699", "CC6666", "CC6633", "CC6600",
			"CC33FF", "CC33CC", "CC3399", "CC3366", "CC3333", "CC3300",
			"CC00FF", "CC00CC", "CC0099", "CC0066", "CC0033", "CC0000",
			"99FFFF", "99FFCC", "99FF99", "99FF66", "99FF33", "99FF00",
			"99CCFF", "99CCCC", "99CC99", "99CC66", "99CC33", "99CC00",
			"9999FF", "9999CC", "999999", "999966", "999933", "999900",
			"9966FF", "9966CC", "996699", "996666", "996633", "996600",
			"9933FF", "9933CC", "993399", "993366", "993333", "993300",
			"9900FF", "9900CC", "990099", "990066", "990033", "990000",
			"66FFFF", "66FFCC", "66FF99", "66FF66", "66FF33", "66FF00",
			"66CCFF", "66CCCC", "66CC99", "66CC66", "66CC33", "66CC00",
			"6699FF", "6699CC", "669999", "669966", "669933", "669900",
			"6666FF", "6666CC", "666699", "666666", "666633", "666600",
			"6633FF", "6633CC", "663399", "663366", "663333", "663300",
			"6600FF", "6600CC", "33FFFF", "33FFCC", "33FF99", "33FF66",
			"33FF33", "33FF00", "33CCFF", "33CCCC", "33CC99", "33CC66",
			"33CC33", "33CC00", "3399FF", "3399CC", "339999", "339966",
			"339933", "339900", "3366FF", "3366CC", "336699", "336666",
			"336633", "336600", "3333FF", "3333CC", "333399", "00FFFF",
			"00FFCC", "00FF99", "00FF66", "00FF33", "00FF00", "00CCFF",
			"00CCCC", "00CC99", "00CC66", "00CC33", "00CC00", "0099FF",
			"0099CC", "009999", "009966", "009933", "009900", "0066FF",
			"0066CC", "006699", "006666", "006633", "006600", "0033FF",
			"0033CC", "003399", "0B57A4", "B8D0E8", "2A82D7", "148AA5",
			"3714A4", "964B00", "A50516", "FB3C8F", "1B4F15", "A51497",
			"686868", "3AA03A", "FF0080", "FEE233", "8BBDEB", "FC6A6C",
			"C1FD33", "2BFD2F", "FC1CAD", "7F2B14", "000066", "2B4726",
			"FD7222", "FC331C", "FFE5B4", "FC5AB8", "AF31F2", "FC0D1B",
			"D7462C", "F69E94", "F2DDBF", "2B8A6D", "6B28CE", "6041FA",
			"FFFFFF", "FEEE35", "DEFD35", "FEC42E"
		];
	},
	getRandomLorem: function () {
		"use strict";
		var loremArray = ["quisque", "libero", "ligula", "consectetuer", "rhoncus",
			"nullam", "velit", "semper", "lacinia", "vitae", "sodales", "pellentesque",
			"ultricies", "dignissim", "lacus", "aliquam", "rutrum", "lorem", "risus",
			"morbi", "metus", "vivamus", "euismod", "lobortis", "felis", "ullamcorper",
			"viverra", "maecenas", "iaculis", "aliquet", "auctor", "tristique",
			"eleifend", "mauris", "nulla", "integer", "molestie", "dapibus", "volutpat",
			"ornare", "egestas", "feugiat", "placerat", "varius", "porttitor", "scelerisque",
			"neque", "malesuada", "fringilla", "turpis", "proin", "augue", "tincidunt",
			"justo", "faucibus", "lectus", "sollicitudin", "massa", "suspendisse",
			"vehicula", "magna", "phasellus", "dolor", "facilisis", "bibendum", "laoreet",
			"vestibulum", "dictum", "consequat", "curabitur", "tempor", "donec", "adipiscing",
			"luctus", "tellus", "interdum", "purus", "elementum", "accumsan", "convallis",
			"fusce", "tempus", "ipsum", "tortor", "sagittis", "aenean", "hendrerit", "mattis",
			"ultrices", "gravida", "blandit", "sociis", "natoque", "penatibus", "magnis",
			"parturient", "montes", "nascetur", "ridiculus", "etiam", "vulputate", "praesent",
			"cursus", "commodo", "pretium", "sapien", "imperdiet", "suscipit", "venenatis",
			"fermentum", "pharetra", "congue", "condimentum", "primis", "posuere", "cubilia",
			"curaeundefined", "mollis", "nonummy", "porta", "habitant", "senectus", "netus", "fames"];
		return loremArray[Math.floor(Math.random() * loremArray.length)];
	},
	getLayerDateFormatFromFeaturesArray: function (args) {
		"use strict";
		var featureArray = args.featureArray;
		var groupingColumn = args.groupingColumn;
		var dateFormat = CONFIG.dateFormat.nonPadded;

		// First check the months
		featureArray.each(function (l) {
			if (l.data[groupingColumn].substring(0, 1) === '0' || l.data[groupingColumn].split('-')[1].substring(0, 1) === '0') {
				dateFormat = CONFIG.dateFormat.padded;
			}
		});

		return dateFormat;
	}
};




