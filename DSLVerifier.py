from Common import *
from DSLParser import *
import lib.plyj.parser as plyj
import lib.plyj.model as model

parser = plyj.Parser()

TEST_RULE = yacc.parse('''\
    class KVException should:
        be public;
        have public KVMessage getMsg(int, KVMessage);
        extend Exception;
        have static final long serialVersionUID;
        not have static final int gibberjabber;
        implement Qian.
''')

RULE_PRIME = yacc.parse('''\
    class KVException#Peter should:
        be private;
        have private static int field;
        have public int fib(int, String).
''')
    


import re


def identify(t):
  if isinstance(t, model.Type):
    return t.name.value
  return t


def match(classHierarchy, declarations):
  if len(classHierarchy) == 0:
    return None
  if classHierarchy[0] == declarations.name:
    if len(classHierarchy) == 1:
      return declarations
    else:
      for d in declarations.body:
        if isinstance(d, model.ClassDeclaration):
          m = match(classHierarchy[1:], d)
          # return if we find match at any deeper level
          if m: return m
  return None

def static_class_check(target, assertion):
  returnLst = [assertion.target, []]
  for statement in assertion.statements:
    if statement.action == 'modifier':
      rc = True
      for m in statement.modifiers:
        if (not m in target.modifiers) and not statement.negated:
          returnLst[1].append(statement)
          rc = False
          break
      if rc and statement.negated:
        returnLst[1].append(statement)
    elif statement.action == 'field':
      # import pdb; pdb.set_trace()
      fields = [i for i in target.body
            if isinstance(i, model.FieldDeclaration)]
      rule = statement.object
      isMatched = False
      for field in fields:
        # First assume it follows the rule
        rc = True
        # check method modifers
        # check variable names
        temprc = False
        for var in field.variable_declarators:
          # import pdb; pdb.set_trace()
          if var.variable.name == rule.fieldName:
            temprc = True
            break
        if not temprc:
          rc = False
          # break
        for modifier in rule.modifiers:
          if not modifier in field.modifiers:
            rc = False
            break
        # check return type
        if field._type != rule.type:
          rc = False
        # method signature check
        if rc:
          isMatched = True
      if not(isMatched ^ statement.negated):
        returnLst[1].append(statement)
    elif statement.action == 'method':
      methods = [i for i in target.body
            if isinstance(i, model.MethodDeclaration)]
      signature = statement.object
      isMatched = False
      for m in methods:
        rc = True
        # check method modifers
        if m.name != signature.methodName:
          rc = False
        for modifier in signature.modifiers:
          if not modifier in m.modifiers:
            rc = False
        # check return type
        if identify(m.return_type) != signature.returnType:
          rc = False
        # check arguments
        args = [identify(j._type) for j in m.parameters]
        if (args != signature.args):
          print args, signature.args
          rc = False
        if rc:
          isMatched = True
          break
      if not(isMatched ^ statement.negated):
        returnLst[1].append(statement)
    elif statement.action == 'inherit':
      if target.extends == None or statement.object != target.extends.name.value:
        if not statement.negated:
          returnLst[1].append(statement)
      else:
        if statement.negated:
          returnLst[1].append(statement)
    elif statement.action == 'implement':
      if target.implements == None:
        if not statement.negated:
          returnLst[1].append(statement)
      else:
        implements = [x.name.value for x in target.implements]
        if target.implements == None or statement.object not in implements:
          if not statement.negated:
            import pdb; pdb.set_trace()
            returnLst[1].append(statement)
        else:
          if statement.negated:
            returnLst[1].append(statement)
  if len(returnLst[1]) > 0:
    return returnLst
  else:
    return []



def static_check(filePath, assertions):
  static = assertions
  tree = parser.parse_file(file(filePath))
  # Iterate through every static assertion
  flag = []
  for st in static:
    if st.isClass:
      classHierarchy = st.target.split('#')
      # Search through file according to class Hierachy for the target
      for ty in tree.type_declarations:
        # Try to match target if it's class declaration.
        if isinstance(ty, model.ClassDeclaration):
          checkTarget = match(classHierarchy, ty)
          if checkTarget: break
      if checkTarget:
        flag.append(static_class_check(checkTarget, st))
  return flag

# temp = static_check('static/KVException.java', TEST_RULE.static)
# print 'matching', temp
# temp = static_check('static/KVException.java', RULE_PRIME.static)
# print 'matching', temp
# import pdb; pdb.set_trace()

def pstatic_check(firectory, rule):
  'match python program staticly with a given rule'
  for root, dirs, files in os.walk(currentDir): # Walk directory tree
    javaPrograms = [f for f in files if '.java' in f]
    for source in javaPrograms:
      tree = parser.parse_file(file(directory + source))

            


            