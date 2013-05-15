import plyj.parser as plyj

parser = plyj.Parser()

# parse a compilation unit from a file
tree = parser.parse_file(file('./test.java'))
import pdb; pdb.set_trace()

# parse a compilation unit from a string
tree = parser.parse_string('class Foo { }')

# parse expression from string
tree = parser.parse_expression('1 / 2 * (float) 3')