# Common dependency
from Common import *

# Ply
import lib.ply.lex as lex
import lib.ply.yacc as yacc

import re

# mapping for patterns
patternHash = {}

# Tokens
tokens = (
  'JAVAEXP', 'COLON', 'SEMICOLON', 'PERIOD', 'HASH',
  'SHOULD', 'NOT', 'CALL', 'RETURN', 'HAVE', 'AFTER', 'BEFORE',
  'FILENAME', 'CONTAIN', 'BE', 'INHERIT', 'IMPLEMENT', 'LESS', 'MORE', 'THAN', 'TIMES',
  'FILE', 'CLASS', 'MODIFIER', 'ID'
  )

# Note: doesn't support multiline javaexp
RE_EXTRACT_JAVAEXP = re.compile(r'`(.+?)`')
def t_JAVAEXP(t):
  r'`.+?`'
  t.value = RE_EXTRACT_JAVAEXP.match(t.value).group(1)
  return t

def t_NOT(t):
  r'not'
  return t

def t_CALL(t):
  r'call'
  return t

def t_BEFORE(t):
  r'before'
  return t

def t_AFTER(t):
  r'after'
  return t

def t_HAVE(t):
  r'have'
  return t

def t_BE(t):
  r'be'
  return t

def t_LESS(t):
  r'less'
  return t

def t_MORE(t):
  r'more'
  return t

def t_THAN(t):
  r'than'
  return t

def t_TIMES(t):
  r'times'
  return t

# t_JAVAEXP       = r'`.+?`'
t_COLON         = r':'
t_SEMICOLON     = r';'
t_PERIOD        = r'\.'
t_HASH          = r'\#'
t_SHOULD        = r'should'
t_RETURN        = r'return'
# These rules are pretty strict for now
t_FILE          = r'[Ff]ile'
t_CLASS         = r'[Cc]lass'
t_MODIFIER      = r'public|private|protected|static|final|abstract|synchronized|volatile'
t_FILENAME      = r'[\w]+?\.java'
# t_AFTER         = r'after'
# t_BEFORE        = r'before'
# t_BE            = r'be'
t_INHERIT       = r'extend'
t_IMPLEMENT     = r'implement'

t_ID            = r'[\w]+'

literals = ['(', ')', ',']
t_ignore = ' \t'

def t_newline(t):
  r'[\n\r]+'
  t.lexer.lineno += len(t.value)

def t_error(t):
  print "Illegal character '%s'" % t.value[0]
  t.lexer.skip(1)

# build the lexer
lex.lex(errorlog=yacc.NullLogger())

# Parsing rules 
start = 'test'

# This is the epsilon edge
def p_empty(p):
  'empty :'
  pass

def p_test_component(p):
  '''test : component
          | test component'''
  if len(p) == 2:
    # first prod
    out = Test()
    p[1].belongs_to(out)
    p[0] = out
  else:
    p[2].belongs_to(p[1])
    p[0] = p[1]


def p_component_dynamic(p):
  '''component : assertion'''
  p[0] = p[1]

def p_assert(p):
  '''assertion : dynamicAssertion
               | staticAssertion'''
  p[0] = p[1]

def p_dynamic_assert(p):
  'dynamicAssertion : JAVAEXP SHOULD COLON dynamicStatements PERIOD'
  # Note: we restrict return to appear only at the end, might not be apt
  # to be dealt with here
  newAssert = DynamicAssertion(p[1], statements = p[4])
  if p[4][-1].action == 'return':
    newAssert.ret = p[4][-1].object
  p[0] = newAssert

def p_static_classAssert(p):
  'staticAssertion : CLASS classnames SHOULD COLON staticStatements PERIOD'
  p[0] = StaticAssertion(p[2], statements=p[5])

def p_static_hierarchy(p):
  '''
  classnames : ID
             | classnames HASH ID
  '''
  if len(p) == 2:
    p[0] = p[1]
  else:
    p[0] = p[1] + '#' + p[3]

"""
def p_static_fileAssert(p):
  'staticAssertion : FILE FILENAME SHOULD COLON staticStatements PERIOD'
  p[0] = ['file', p[5]]
"""

def p_static_statements(p):
  '''staticStatements : staticStatement
                      | staticStatements SEMICOLON staticStatement'''
  if len(p) == 2:
    p[0] = [p[1]]
  else:
    p[0] = p[1] + [p[3]]

def p_static_statement_negation(p):
  '''staticStatement : NOT staticStatement
  '''
  p[2].negate()
  p[0] = p[2]


def p_static_statement_be(p):
  'staticStatement : BE modifiers'
  p[0] = StaticStatement('modifier', modifiers=p[2])

def p_static_statement_method(p):
  'staticStatement : HAVE method'
  p[0] = StaticStatement('method', object=p[2])

def p_static_statement_field(p):
  'staticStatement : HAVE field'
  p[0] = StaticStatement('field', object=p[2])

def p_static_inherit(p):
  '''staticStatement : INHERIT ID'''
  p[0] = StaticStatement('inherit', p[2])

def p_static_implement(p):
  '''staticStatement : IMPLEMENT ID'''
  p[0] = StaticStatement('implement', p[2])


def p_static_method(p):
  '''method : modifiers ID ID '(' args ')'
  '''
  p[0] = MethodSignature('__this__', p[3], p[5], modifiers = p[1], returnType=p[2])

def p_static_field(p):
  '''field : modifiers ID ID
  '''
  p[0] = Field(p[3], p[1], p[2])


def p_static_modifiers(p):
  '''modifiers :  MODIFIER
               |  modifiers MODIFIER
  '''
  if len(p) == 2:
    # first production
    p[0] = [p[1]]
  else:
    p[0] = p[1] + [p[2]]


def p_general_stmts(p):
  '''statements : statement
                | statements SEMICOLON statement'''
  if len(p) == 2:
    p[0] = [p[1]]
  else:
    p[0] = p[1] + [p[2]]

def p_general_stmt(p):
  '''statement : dynamicStatement'''
  # add static statement if we have time
  p[0] = p[1]

def p_args_empty(p):
  'args : empty'
  p[0] = []

def p_args(p):
  '''args : ID
          | args ',' ID'''
  if len(p) == 2:
    # first production
    p[0] = [p[1]]
  else:
    p[0] = p[1] + [p[3]]

def p_dynamic_statements(p):
  '''dynamicStatements : dynamicStatement
                       | dynamicStatements SEMICOLON dynamicStatement'''
  # Here we will flatten pattern into what they should be
  if len(p) == 2:
    # first production
    p[0] = flatten(p[1])
  else:
    p[0] = p[1] + flatten(p[3])

def p_dynamic_statment_call(p):
  '''dynamicStatement : CALL methodSignature postModifier
  '''
  # only support one post modifier for now
  p[0] = DynamicStatement('call', p[2], modifiers = [p[3]])

def p_dynamic_statement_return(p):
  '''dynamicStatement : RETURN JAVAEXP
  '''
  # also only support java expression as return value for now
  p[0] = DynamicStatement('return', p[2])

def p_dynamic_statement_negation(p):
  '''dynamicStatement : NOT dynamicStatement
  '''
  # Note: it's modified in place
  p[2].negate()
  p[0] = p[2]

def p_method_sig(p):
  '''methodSignature : ID HASH ID '(' args ')\''''
  # This is again a simplification
  p[0] = MethodSignature(p[1], p[3], p[5])

def p_post_mod(p):
  '''postModifier : empty
                  | BEFORE methodSignature
                  | AFTER methodSignature
                  | LESS THAN ID TIMES
                  | MORE THAN ID TIMES'''
  if len(p) == 2:
    p[0] = None
  elif len(p) == 3:
    p[0] = (p[1], p[2])
  elif len(p) == 5:
    p[0] = (p[1], int(p[3]))


def p_error(t):
  print("Syntax error at '%s'" % t.value)

# build the parser
yacc.yacc(errorlog=yacc.NullLogger())

# not perfect regex
RE_RXTRACT_PATTERN = re.compile(r'define pattern ([\w]+)\(([\w]+,?)*\):([\d\D]+?)\.')
RE_FOLLOW_PATTERN = re.compile(r'follow pattern ([\w]+)\(([\w]+,?)*\)([;.])')
def DSLParse(raw):
  # First extract all patterns definitions
  patterns = RE_RXTRACT_PATTERN.findall(raw)
  for pattern in patterns:
    patternHash[pattern[0]] = (pattern[1].split(","), pattern[2])
    # print (pattern[1].split(","), pattern[2])

  extracted = RE_RXTRACT_PATTERN.sub("", raw)
  # print "extracted", extracted

  # Then factor in all the patterns, iterate untill no more changes since
  # there might be nested patterns
  processed = extracted
  previous  = ""
  
  while processed != previous:
    previous = processed
    match = RE_FOLLOW_PATTERN.search(processed)
    if match:
      first = processed[:match.start()]
      rest = processed[match.end():]

      pattern = patternHash[match.group(1)]
      vals = match.group(2).split(",") if match.group(2) else []
      stmts = str(pattern[1])
      for arg, val in zip(pattern[0], vals):
        stmts = re.sub('%s' % arg, val, stmts)

      processed = first + stmts + match.group(3) + rest

  # print "processed", processed

  return yacc.parse(processed)