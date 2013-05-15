from string import join

class Test(object):
  """This is the python representation of a Reader++ test,
    as associated with each @Test method"""
  def __init__(self, static = [], dynamic = []):
    super(Test, self).__init__()
    self.static = list(static)
    self.dynamic = list(dynamic)

  def add_dynamic_assertion(self, assertion):
    self.dynamic.append(assertion)   

  def add_static_assertion(self, assertion):
    self.static.append(assertion)
    
  def dynamic_iterator(self):
    return self.dynamic

  def static_iterator(self):
    return self.static 

global uniqueID
uniqueID = 0

class Assertion(object):
  """Represents a reader++ assertion"""
  def __init__(self, target, statements = [], test = None):
    super(Assertion, self).__init__()
    self.target = target
    self.test = test
    self.statements = list(statements)
    global uniqueID
    self.id = uniqueID
    uniqueID += 1

  def dependencies(self):
    '''Returns all the dependencies of this assertion'''
    pass

class DynamicAssertion(Assertion):
  """docstring for DynamicAssertion"""
  def __init__(self, target, ret = None, statements = [], test = None):
    super(DynamicAssertion, self).__init__(target, statements = statements, test = test)
    self.ret = ret
    if test:
      test.add_dynamic_assertion(self)

  def belongs_to(self, test):
    self.test = test
    test.add_dynamic_assertion(self)

  def dependencies(self):
    '''Overrided'''
    out = []
    for stmt in self.statements:
      if stmt.action != "return":
        out.append(stmt.object)
        for modifier in stmt.modifiers:
          if modifier and isinstance(modifier[1], MethodSignature):
            # if not None
            out.append(modifier[1])

    return out

  def __str__(self):
    import string
    return string.replace(str(self.target) + "#" + str(self.id), '"', "")

class StaticAssertion(Assertion):
  """docstring for StaticAssertion"""
  def __init__(self, target, statements = [], test = None, isClass=True):
    super(StaticAssertion, self).__init__(target, statements = statements, test = test)
    if test:
      test.add_static_assertion(self)
    self.isClass = isClass
  def belongs_to(self, test):
    self.test = test
    test.add_static_assertion(self)

class StaticStatement(object):
  def __init__(self, action, object=None, modifiers=[], negated=False):
    super(StaticStatement, self).__init__()
    self.action = action
    self.object = object
    self.modifiers = list(modifiers)
    self.negated = negated

  def negate(self):
    self.negated = not self.negated

class DynamicStatement(object):
  """docstring for DynamicStatement"""
  def __init__(self, action, object = None, modifiers = [], negated = False):
    super(DynamicStatement, self).__init__()
    self.action = action
    self.object = object
    self.modifiers = list(modifiers)
    self.negated = negated

  def __str__(self):
    if self.object == None:
      self.object = ""
    if self.action == None:
      self.action = ""
    # import pdb; pdb.set_trace()
    modifiers = map(lambda x: str(x), self.modifiers[0]) if self.modifiers[0] and self.modifiers[0][0] else [" "]
    if self.negated:
      return "not {} {} {}".format(self.action,
                  str(self.object), join(modifiers, " "))
    return "{} {} {}".format(self.action,\
        str(self.object), join(modifiers, " "))
  def negate(self):
    self.negated = not self.negated
            
class MethodSignature(object):
  """docstring for MethodSignature"""
  def __init__(self, className, methodName,  args, modifiers = [],returnType=""):
    super(MethodSignature, self).__init__()
    self.className = className
    self.methodName = methodName
    self.args = args
    self.modifiers = list(modifiers)
    self.returnType = returnType

  def __str__(self):
    return "%s#%s(%s)" % (self.className, self.methodName, join(self.args, ","))

class Field(object):
  """ For field declaration. """
  def __init__(self, fieldName, modifiers=[], type=""):
    super(Field, self).__init__()
    self.fieldName = fieldName
    self.modifiers = modifiers
    self.type = type

# helper method used for flattening pattern
def flatten(stmt):
    if type(stmt) == AppliedPattern:
      return stmt.apply()
    else:
      return [stmt]

class Pattern(object):
  """docstring for Pattern"""
  def __init__(self, args, stmts): 
    super(Pattern, self).__init__()
    self.args = args
    self.stmts = stmts

  def belongs_to(self, test):
    # no need to register pattern since it will be processed in SDT
    pass
  
class AppliedPattern(object):
  """docstring for AppliedPattern"""
  def __init__(self, pattern, args):
    super(AppliedPattern, self).__init__()
    self.pattern = pattern
    self.args = args

  def flatten(self):
    out = []
