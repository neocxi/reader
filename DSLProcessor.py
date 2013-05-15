import re

from DSLParser import *
from string import Template, join

DSL_RE = re.compile(r'``(.+?)``', flags=re.DOTALL)

def extractAndReplaceSegments(content, name = "Test"):
  '''
  This method will extract doubly backquoted segments from
  the content(String) and feed them one by one to processSegment
  which gives hooks to be placed back into content as well as
  some queries to be performed at the end.
  '''  
  match = DSL_RE.search(content)
  first = content[:match.start()] if match else ""
  rest = content[match.end():] if match else ""

  counter = 0
  tests = []

  while match:
    delta, test = processSegment(match.group(1), "%s-%i" % (name, counter))
    first += delta
    tests.append(test)
    counter += 1
    match = DSL_RE.search(rest)
    first += rest[:match.start()] if match else rest
    rest = rest[match.end():] if match else ""

  return first, tests


def processSegment(segment, name):
  test = DSLParse(segment)
  out = ""
  # A naive processor, only run the java program and check its output
  for assertion in test.dynamic:
    testName = str(assertion)
    out += setupEnvironment(testName, assertion.dependencies())
    if assertion.ret:
      out += "assertEquals(%s, %s);\n" % (assertion.target, assertion.ret)
    else:
      out += "%s;\n" % assertion.target
    out += destroyEnvironment(testName)

  return out, test


SETUP_TEMPLATE = Template(
                  'LogHelper.setupEnvironment("$name", new String[] { $dependencies });\n'
                  )
def setupEnvironment(name, dependencies):
  return SETUP_TEMPLATE.substitute(name = name,
                                   dependencies = join(map(lambda x: '"%s"' % x, dependencies), ", ")
                                  )

DESTROY_TEMPLATE = Template(
                    'LogHelper.destroyEnvironment("$name");\n'
                    )
def destroyEnvironment(name):
  return DESTROY_TEMPLATE.substitute(name = name)
