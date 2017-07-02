package company.implementations;

import company.interfaces.IJob;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class JobATest {
    static IJob normalExecution;          // single instruction
    static IJob sleepExecution;           // with sleep instructions
    static IJob throwExecution;           // throw exception
    static IJob infiniteLoop;

    @BeforeClass
    public static void initJobA(){
        normalExecution = ()-> System.out.println("JOB: Single println");
        sleepExecution = ()-> {
            try {
                Thread.sleep(1500);
                System.out.println("JOB: Single println with thread sleep");
            } catch (InterruptedException e) {
                System.out.println("Task is interrupted");
            }
        };
        throwExecution = ()-> {
            System.out.println("It will throw an exception");
            try {
                Thread.sleep(100);
                throw new RuntimeException("JOB: throwing exception");
            } catch (InterruptedException e) {}
        };
        infiniteLoop = ()-> { while (true){} };
    }

    /**
     * Test the validation of thread number which can not be negative
     */
    @Test(expected = AssertionError.class)
    public void validateThreadNumCannotBeNegative(){
        new JobA(-10);
    }

    /**
     * Test the validation of thread number which can not more than 32767.
     * ForkJoinPool supported up to 32767 thread.
     */
    @Test(expected = AssertionError.class)
    public void validateThreadNumUpperLimit(){
        new JobA(32768);
    }

    /**
     * Test the validation of time out which can not be negative
     */
    @Test(expected = AssertionError.class)
    public void validateTimeOutCannotBeNegative(){
        new JobA(1,-1);
    }

    /**
     * Test the validation of time out which can not be zero
     */
    @Test(expected = AssertionError.class)
    public void validateTimeOutCannotBeZero(){
        new JobA(1,0);
    }

    /**
     * Test if empty list causes any exception
     */
    @Test
    public void emptyListNotFail(){
        try {
            new JobA(1).execute();
            assertTrue("Empty list works", true);
        } catch (Exception e){
            assertTrue("No exception expected", false);
        }

    }

    /**
     * This test case try to compare the single thread execution time with
     * the multi threaded. The goal of this case is to show that the configuration
     * of the thread number is working. Run the same set of IJobs against one core
     * and the whole available processors. Then compare the result, where multi threaded
     * has to faster. This test case only make sense running on a multi core machine.
     */
    @Test
    public void compareExecutionSingleVsMultiThread(){
        int availableProcessor = Runtime.getRuntime().availableProcessors();
        Assume.assumeTrue("The test machine has more than one available processor",
                availableProcessor > 1);

        // init
        JobA singleThreaded = new JobA(1);
        JobA multiThreaded = new JobA(availableProcessor);
        List<IJob> compareList = Arrays.asList(
                normalExecution,
                normalExecution,
                normalExecution,
                sleepExecution,
                sleepExecution);

        singleThreaded.getListExecute().addAll(compareList);
        multiThreaded.getListExecute().addAll(compareList);

        long singleExecutionTime = 0;
        long multiExecutionTime = 0;
        // single part
        try {
            long start = System.currentTimeMillis();
            singleThreaded.execute();
            long end = System.currentTimeMillis();

            singleExecutionTime = end - start;
            assertTrue("Without exception", true);
        } catch (RuntimeException e){
            assertTrue("No exception expected", false);
        }
        // multi part
        try {
            long start = System.currentTimeMillis();
            multiThreaded.execute();
            long end = System.currentTimeMillis();

            multiExecutionTime = end - start;
            assertTrue("Without exception", true);
        } catch (RuntimeException e){
            assertTrue("No exception expected", false);
        }

        // in this case the multi threaded has to be faster than the single one
        assertTrue("Multi threaded are faster", multiExecutionTime < singleExecutionTime);
    }

    /**
     * Test the time out on infinite loop
     */
    @Test
    public void avoidLongTasks(){
        JobA executor = new JobA(1, 1);
        executor.getListExecute().addAll(Arrays.asList(infiniteLoop));

        try {
            executor.execute();
            assertTrue("Timeout exception expected", false);
        } catch (RuntimeException e){
            assertTrue("Timeout".equals(e.getMessage()));
        }
    }

    /**
     * Test the time out on a long task
     */
    @Test
    public void avoidLongTasks2(){
        JobA executor = new JobA(1, 1);
        executor.getListExecute().addAll(Arrays.asList(sleepExecution));

        try {
            executor.execute();
            assertTrue("Timeout exception expected", false);
        } catch (RuntimeException e){
            assertTrue("Timeout".equals(e.getMessage()));
        }
    }

    /**
     * Test if one of the task throws exception, it get returns to the caller
     */
    @Test
    public void testIfOneTaskThrowsException(){
        JobA multiThreaded = new JobA(6);
        List<IJob> jobList = Arrays.asList(
                throwExecution,
                sleepExecution,
                sleepExecution,
                sleepExecution,
                sleepExecution,
                infiniteLoop);

        multiThreaded.getListExecute().addAll(jobList);
        try {
            multiThreaded.execute();
            assertTrue("Should not reach this point", false);
        } catch (RuntimeException e){
            assertTrue("Runtime exception has to cotain exception message",
                    "JOB: throwing exception".equals(e.getMessage()));
        }
    }

    /**
     * Test if all of the task throws exception, and the first one get returns to the caller.
     * In this case more threads enter the same block.
     */
    @Test
    public void allTaskThrowsException(){
        JobA multiThreaded = new JobA(3);
        List<IJob> jobList = Arrays.asList(
                throwExecution,
                throwExecution,
                throwExecution,
                throwExecution,
                throwExecution,
                throwExecution);

        multiThreaded.getListExecute().addAll(jobList);
        try {
            multiThreaded.execute();
            assertTrue("Should not reach this point", false);
        } catch (RuntimeException e){
            assertTrue("Runtime exception has to cotain exception message",
                    "JOB: throwing exception".equals(e.getMessage()));
        }
    }

    /**
     * Test after an exception occurred, other task gets killed. In this
     * case both are running parallel, so both are in the pool in the same
     * time.
     */
    @Test(timeout = 5000)
    public void checkIfExceptionInterruptsOtherTask() throws InterruptedException {
        final Resolver resolver = new Resolver();

        JobA multiThreaded = new JobA(2);
        List<IJob> jobList = Arrays.asList(
                throwExecution,
                ()-> {
                    try {
                        Thread.sleep(10000);
                        System.out.println("JOB: Single println with thread sleep");
                    } catch (InterruptedException e) {
                        resolver.setFlag(true);
                    }
                });

        multiThreaded.getListExecute().addAll(jobList);
        try {
            multiThreaded.execute();
        } catch (RuntimeException e){
            // tricky case, should wait until is resolver set
            // blocking wait
            while (true){
                Thread.sleep(100);
                if(resolver.isFlag()){
                    break;
                }
            }

            assertTrue("Task gets interrupted", resolver.isFlag());
        }
    }

    class Resolver {
        private boolean flag = false;

        public boolean isFlag() {
            return flag;
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }
    }
}