# Multithread-sample 

A set of predefined IJob interface tasks will be executed parallel.
---

Those tasks have to be independent from each other. In case of a long running
task a predefined timeout is set to prevent the system. Due to the timeout is
configurable, longer tasks could be also executed as well. For the most throughput
the parallel running thread number is configurable, but there is no guarantee to
get it, it depends on the tasks (blocking, non-blocking) and the available processor
cores as well.

Internally the ForkJoinPool framework is used, which is part of the java standard, no
other dependency needed. The given tasks will be submitted to the pool, which has
the predefined amount of workers, using parallel stream. In case of any exception
the pool is immediately shouted down, to prevent other threads from further execution.
If more threads throw exception in a parallel execution, always the first one will be
returned to the caller, others are ignored. In every execution (calling execute)
a new thread pool is created.

---
The cool thing about my solution is, it uses a java8 common thread pool, which used by streams as well. 
So it lets you enable to use the java8 parallstream feature.

```java
ForkJoinTask<?> actualTask =
  pool.submit(()-> listExecute
      .parallelStream()
      .forEach(this::executeJobs));
```

---
Test cases provided for covering all scenarios. Junit framework is used for providing these test cases
