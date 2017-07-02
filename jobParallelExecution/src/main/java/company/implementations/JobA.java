package company.implementations;

import company.interfaces.IJob;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * JobA implementation class let define a set of IJob tasks for execution.
 * Those tasks have to be independent from each other. In case of a long running
 * task a predefined timeout is set to prevent the system. Due to the timeout is
 * configurable, longer tasks could be also executed as well. For the most throughput
 * the parallel running thread number is configurable, but there is no guarantee to
 * get it, it depends on the tasks (blocking, non-blocking) and the available processor
 * cores as well.
 *
 * Internally the ForkJoinPool framework is used, which is part of the java standard, no
 * other dependency needed. The given tasks will be submitted to the pool, which has
 * the predefined amount of workers, using parallel stream. In case of any exception
 * the pool is immediately shouted down, to prevent other threads from further execution.
 * If more threads throw exception in a parallel execution, always the first one will be
 * returned to the caller, others are ignored. In every execution (calling execute)
 * a new thread pool is created.
 *
 */
public class JobA implements IJob{

    private static long DEFAULT_TIME_OUT = 1000;        // in sec

    private ForkJoinPool pool;
    private List<IJob> listExecute;
    private volatile RuntimeException reason;
    private int threadNum;
    private long timeOut;                               // in sec

    public JobA(int threadNum){
        this(threadNum, DEFAULT_TIME_OUT);
    }

    public JobA(int threadNum, long timeOut){
        // validate threadnum and timeout
        assert threadNum > 0 && threadNum <= 32767;
        assert timeOut > 0;

        this.listExecute = new ArrayList<>();
        this.threadNum = threadNum;
        this.timeOut = timeOut;

        reason = null;
    }

    public List<IJob> getListExecute() {
        return listExecute;
    }

    @Override
    public void execute() {
        // if there is no task configured
        if(listExecute == null || listExecute.isEmpty()){
            return;
        }

        // init pool for execution
        pool = new ForkJoinPool(threadNum);
        // set reason to null
        reason = null;

        try {
            // register tasks for execution
            ForkJoinTask<?> actualTask =
                    pool.submit(()-> listExecute
                        .parallelStream()
                        .forEach(this::executeJobs));

            // avoid infinite loops + long running task
            actualTask.get(timeOut, TimeUnit.SECONDS);
        } catch (CancellationException e){
            throw reason;
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupt");
        } catch (ExecutionException e) {
            throw new RuntimeException("Execution");
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout");
        } finally {
            if(!pool.isShutdown()){
                pool.shutdown();
            }
        }
    }

    private void executeJobs(IJob task){
        try {
            task.execute();
        } catch (RuntimeException e){
            synchronized(this){
                // if already not shouted down
                if(!pool.isShutdown()){
                    // cancel all other task in the pool
                    reason = e;
                    pool.shutdownNow();
                }
            }
        }
    }
}
