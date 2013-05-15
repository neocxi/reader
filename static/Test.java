package tests;

import org.junit.*;
import static org.junit.Assert.*;
import helloworld.*;

public class Test {

    @Test
    public void testPass() {
        ``
        class KVException should:
        be public;
        have public KVMessage getMsg(int, KVMessage);
        inherit class Exception;
        have static final long serialVersionUID;
        not have static final int gibberjabber;
        implement Qian.
        ``
    }

    @Test
    public void anotherPass() {
        ``
        class KVException#Peter should:
        be private;
        have private static int field;
        have public int fib(int, String).
        ``
    }

    @Test
    public void otherFail() {
        ``
        class KVException should:
        be private;
        have public KVMessage getMsg(int, KVMessage);
        inherit class Exception;
        have static final long serialVersionUID;
        not have static final int gibberjabber;
        implement Qian.
        ``
    }


    public static void main(String ignored[]) {
        org.junit.runner.JUnitCore.main("tests.Test");
    }
}