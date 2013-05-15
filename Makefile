all:
	javac -cp lib/junit.jar helloworld/*.java tests/*.java
clean:
	rm -f helloworld/*.class tests/*.class
hello:
	javac -cp lib/junit.jar helloworld/*.java
test:
	javac -cp lib/junit.jar tests/*.java
unit:
	java -cp $$CLASSPATH:lib/hamcrest-all-1.3.jar:lib/junit.jar tests.FibTest
