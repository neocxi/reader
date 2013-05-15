from Common import *
import time
import lib.xmltodict as xml

def verifyRuntimeConstraints(log, assertion):
  calls = xml.parse(log)[u"history"][u"call"]
  if not isinstance(calls, list):
    # hack to deal with xml quirk
    calls = [calls]
  processed = []
  for call in calls:
    invocation = (call[u"methodName"],
                  time.strptime(call[u"startTime"], "%Y/%m/%d %H:%M:%S"))
    processed.append(invocation)
  # sort it to enhance efficiency
  processed.sort(key = lambda x: x[1])

  offended = []
  # this is a sufficient bases
  for statement in assertion.statements:
    # print statement
    if statement.action == "return":
      continue
    failed = True
    for i in xrange(len(processed)):
      if str(statement.object) == processed[i][0]:
        satisfiedAll = True
        for modifier in statement.modifiers:
          # proceed only if it's a valid modifier
          if modifier:
            satisfied = False
            if modifier[0] == 'after':
              for j in xrange(0, i):
                if str(modifier[1]) == processed[j][0]:
                  satisfied = True
                  break
            elif modifier[0] == 'before':
              for j in xrange(i, len(processed)):
                if str(modifier[1]) == processed[j][0]:
                  satisfied = True
                  break
            elif modifier[0] == 'less' or modifier[0] == 'more':
              num = sum([1 for call in processed if str(statement.object) == call[0]])
              # print modifier, num
              if modifier[0] == 'less':
                satisfied = True if num < modifier[1] else satisfied
              else:
                satisfied = True if num > modifier[1] else satisfied

            if not satisfied:
              satisfiedAll = False

        if satisfiedAll:
          failed = False

    if failed ^ statement.negated:
      offended.append(statement)

  return offended

