from DSLProcessor import *
from DSLVerifier import *
from JavaProcessor import *
from DynamicVerifier import *
from subprocess import call
import shutil
import sys
import os

def stripFileName(path):
    lst = path.split('/')
    return lst[-1]

def stripPackageName(path):
    lst = path.split('/')
    return lst[-2]

def stripClassName(path):
    lst = path.split('/')
    return lst[-1].split('.')[0]

def findFirstClass(s):
    return s.split('#')[0]


def interpretDynamic(allHash, offendingHash, offset = 0):
    for assertion in allHash.keys():
        all = allHash[assertion]
        offending = offendingHash[assertion] if assertion in offendingHash else None

        successful = set(all) - set(offending) if offending else all
        if len(successful) > 1 and list(successful)[0].action != "return":
            print "\t" * offset + str(assertion) + " passed tests:"
            for s in successful:
                if s.action == "return":
                    continue
                print "\t" * (offset + 1) + str(s)

        if not offending:
            print "\t" * offset + str(assertion) + " passes all tests"
            continue
        else:
            print "\t" * offset + str(assertion) + " failed tests:"
            for o in offending:
                print "\t" * (offset + 1) + str(o)

def interpretStatic(output):
    # print output[0][1]
    if len(output) == 0:
        return True
    else:
        for assertion in output:
            print 'Failed test: class',\
                assertion[0], 'does not:'
            # print len(assertion[1])
            for st in assertion[1]:
                if st.action == 'method':
                    temp = ''
                    modifiers = join(st.object.modifiers, ' ')
                    print '\tdefine %s method' % modifiers,\
                        "%s(%s)" % (st.object.methodName, join(st.object.args, ", "))
                elif st.action == 'inherit':
                    print "\textend class", st.object
                elif st.action == 'implement':
                    print "\timplement interface", st.object
                elif st.action == 'modifier':
                    print "\thave %s modifiers" % join(st.modifiers, ' ')
                elif st.action == 'field':
                    rule = st.object
                    print "\tdefine {} {} field {}".format(join(rule.modifiers, " "), rule.type, rule.fieldName)
                else:
                    print "\tunknown statement", st.action

    return False


if __name__ == "__main__":
    if len(sys.argv) != 4:
        print '''\
        ERR: Too many or too few arguments. Three arguments needed:
            rule file
            source directory
            output directory
        '''
        sys.exit(0)

    junitFileName = stripFileName(sys.argv[1])
    ruleFile = open(sys.argv[1], 'r')



    sourceDir = sys.argv[2]
    if sourceDir[-1] != '/':
        sourceDir += '/'
    outputDir = sys.argv[3]
    if outputDir[-1] != '/':
        outputDir += '/'

    packagename = stripPackageName(sourceDir)
    try:
        shutil.rmtree(outputDir)
    except OSError:
        pass
    shutil.copytree(sourceDir, outputDir + packagename)


    ruleContent = ruleFile.read()
    first, tests = extractAndReplaceSegments(ruleContent)

    # print first, tests

    outputFile = open(outputDir + junitFileName, 'w')
    outputFile.write(first)
    outputFile.close()

    # print "TEST", tests

    fileBucket = {}
    for test in tests:
        for static in test.static:
            fileName = findFirstClass(static.target) + '.java'
            # print os.walk(sourceDir)
            for root, dirs, files in os.walk(sourceDir): # Walk directory tree
                javaPrograms = [f for f in files if '.java' in f]
                for source in javaPrograms:
                    if source == fileName:
                        if fileName in fileBucket.keys():
                            fileBucket[fileName].append(static)
                            break
                        else:
                            fileBucket[fileName] = [static]

    allOffendingStaticStatements = []
    for fileName in fileBucket.keys():
        # print sourceDir + fileName
        allOffendingStaticStatements += \
                static_check(sourceDir + fileName, fileBucket[fileName])


    methodBucket = {}
    rewritten = set()
    for test in tests:
        for dynamic in test.dynamic:
            dependencies = dynamic.dependencies()
            for d in dependencies:
                className = d.className
                if className in methodBucket.keys():
                    methodBucket[className].append(d)
                else:
                    methodBucket[className] = [d]
    portDir = outputDir + packagename + '/'
    for className in methodBucket.keys():
        fileName = className + '.java'
        # print sourceDir + fileName,
        try:
            rc = open(sourceDir + fileName, 'r')
        except IOError:
            print "class name not exist", className
            continue
        content = rc.read()
        # print content
        # import pdb; pdb.set_trace()
        for signature in methodBucket[className]:
            if str(signature) not in rewritten:
                content = rewriteOriginalDeclaration(content, signature)
                rewritten.add(str(signature))
        for root, dirs, files in os.walk(portDir): # Walk directory tree
            javaPrograms = [f for f in files if f==fileName]
            if len(javaPrograms) == 0:
                continue
            else:
                fileTemp = open(portDir + javaPrograms[0], 'w')
                fileTemp.write(content)
                fileTemp.close()
    log = open('./lib/LogHelper.java', 'r')
    helperContent = 'package ' + packagename + ';\n' + log.read()
    log = open(outputDir + packagename + '/' + 'LogHelper.java', 'w')
    log.write(helperContent)
    log.close()
    os.chdir(outputDir)
    os.system(r"javac -cp ../lib/junit.jar:. **.java")
    os.system(r"java -cp $$CLASSPATH:../lib/hamcrest-all-1.3.jar:../lib/junit.jar:. " + 
                stripClassName(junitFileName) + 
                " 1>runtimeOutput 2>&1")

    allOffendingDynamicStatements = {}
    allDynamicStatements = {}
    for test in tests:
        for dynamic in test.dynamic:
            allDynamicStatements[dynamic] = list(dynamic.statements)
            try:
                log = open(str(dynamic), 'r')
            except IOError:
                if dynamic.dependencies() != []:
                    print 'not found: %s test failed in junit!' % str(dynamic)
                continue
            allOffendingDynamicStatements[dynamic] = verifyRuntimeConstraints(log, dynamic)
            # interpretDynamic(failedStatements)

    print "================ Results ================"
    print "** Static Properties Verification:"
    interpretStatic(allOffendingStaticStatements)
    print "** Dynamic Properties Verification:"
    interpretDynamic(allDynamicStatements, allOffendingDynamicStatements)
    print "** Runtime Output & Other JUnit Verification:"
    print open("runtimeOutput", "r").read()
"""
    print 'STATIC TEST'
    fileBucket = {}
    for test in tests:
        for static in test.static:
            fileName = findFirstClass(static.target) + '.java'
            print os.walk(sourceDir)
            for root, dirs, files in os.walk(sourceDir): # Walk directory tree
                javaPrograms = [f for f in files if '.java' in f]
                for source in javaPrograms:
                    if source == fileName:
                        if fileName in fileBucket.keys():
                            fileBucket[fileName].append(static)
                            break
                        else:
                            fileBucket[fileName] = [static]
    print 'file bucket', fileBucket
    for fileName in fileBucket.keys():
        print sourceDir + fileName
        rt = static_check(sourceDir + fileName, fileBucket[fileName])
        print 'failed test', rt
        isSuccess = interpretOutput(rt)
"""



