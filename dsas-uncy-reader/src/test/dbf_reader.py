import sys
import pkg_resources

# pkg_resources.require("dbf >= 0.95")
import dbf

def reader(fn):
	t = dbf.Table(fn)
	t.open()

	for ri, r in enumerate(t):
		print("record %d" % ri)

		for f in t.field_names:
			try:
				print("%s:\t%s" % (f, r[f]))
			except ValueError as e:
				print("%s:\tValueError %s" % (f,e))
		print

	t.close()

if __name__ == '__main__':
	reader(sys.argv[1])

