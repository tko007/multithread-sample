# Multithread-sample 
## Configure and using java8 common thread pool for executing sample jobs.

It is able to specify for the common thread pool, how many threads are maximum avaible. #
By default it is set to number of the cpu avaible threads. In this pool will be executed 
every jobs, implementing an IJob interface. It could happen, that a job throws an exception 
during its execution, then the whole pool will be shutting down immediately, 
so no more jobs will be executed. 

---

The cool thing about my solution is, it uses a java8 common thread pool, which used by streams as well. 
So it lets you enable to use the java8 parallstream feature.

```java
   ForkJoinTask<?> actualTask =
      pool.submit(()-> listExecute
         .parallelStream()
         .forEach(this::executeJobs));
```
