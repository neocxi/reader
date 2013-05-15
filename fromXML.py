import xml.etree.ElementTree as ET

def fromXML(filename):
	tree = ET.parse(filename)
	root = tree.getroot()
	data = {}

	for child in root:
		#print child.tag, child.text
		data[child.tag] = child.text

	return data

