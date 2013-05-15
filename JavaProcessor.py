import re
from string import join

def rewriteOriginalDeclaration(content, signature):
  className = signature.className
  methodName = signature.methodName
  args = signature.args
  matchRegex = r'^(.*?%s)[\s]*(\(%s\))(.*?{)' % \
    (methodName, join(
               map(lambda arg: r'%s[\s]+[\w]+' % arg, args),
               r'[\s]*,[\s]*'))
  typeRegex = r'^.+?([\w]+)[\s]+%s[\s]*\(' % methodName
  retType = re.search(typeRegex, content, re.MULTILINE)
  if retType:
    retType = retType.group(1)
  else:
    print "%s cannot be found." % str(signature)
    raise Exception("method not found")
    
  # assume no more than # of asciis
  proxyArgs = join([arg + " " + chr(97 + i) for (arg, i) in zip(args, xrange(100))], ",")
  return re.sub(matchRegex,
                r'\1(' + proxyArgs + r')\3' + 
                  proxyMethod(className, methodName, args, retType) + 
                  r'} \1Prime\2\3',
                content,
                flags = re.MULTILINE)

def proxyMethod(className, methodName, args, retType):
  out = ""
  compoundName = "%s#%s(%s)" % (className, methodName, join(args, ","))
  # First we need to construct the serialized arguments
  out += 'String args = "";'
  for arg in [chr(97 + i) for i in xrange(len(args))]:
    if arg != 'a':
      out += 'args += ",";'
    out += 'args += String.valueOf(%s);' % arg

  # log start
  out += 'LogHelper.invocationBegin("%s", %s);' % (compoundName, "args")
  if retType == "void":
    out +=  ("%sPrime(" % methodName) + join([chr(97 + i) for i in xrange(len(args))], ",") + ');'
    out += ('LogHelper.invocationEnd("%s", "void");' % compoundName)
  else:
    # delegate to actual call
    out += ('%s ret = ' % retType) + ("%sPrime(" % methodName) + join([chr(97 + i) for i in xrange(len(args))], ",") + ');'
    # log end 
    out += ('LogHelper.invocationEnd("%s", String.valueOf(ret));' % compoundName)
    out += "return ret;"

  return out