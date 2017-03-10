package com.satori.rtm.utils;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TrampolineExecutorService extends AbstractExecutorService
    implements ListeningExecutorService {
  private static final Logger LOG = LoggerFactory.getLogger(TrampolineExecutorService.class);

  private final BlockingQueue<Runnable> mQueue;
  /**
   * Lock used whenever accessing the state variables
   */
  private final Lock lock = new ReentrantLock();
  private final Condition termination = lock.newCondition();
  private final AtomicBoolean working;
  private boolean shutdown = false;

  public TrampolineExecutorService() {
    mQueue = new LinkedBlockingDeque<Runnable>();
    working = new AtomicBoolean(false);
  }

  @Override
  public void execute(Runnable command) {
    startTask();
    try {
      mQueue.add(command);
      while (0 < mQueue.size()) {
        if (!working.compareAndSet(false, true)) {
          return;
        }

        Runnable action = mQueue.poll();
        if (null == action) {
          continue;
        }
        action.run();

        working.set(false);
      }
    } finally {
      endTask();
    }
  }

  @Override
  public boolean isShutdown() {
    lock.lock();
    try {
      return shutdown;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void shutdown() {
    lock.lock();
    try {
      shutdown = true;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public List<Runnable> shutdownNow() {
    shutdown();
    return Collections.emptyList();
  }

  @Override
  public boolean isTerminated() {
    lock.lock();
    try {
      return shutdown && mQueue.size() == 0;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit)
      throws InterruptedException {
    long nanos = unit.toNanos(timeout);
    lock.lock();
    try {
      while (true) {
        if (isTerminated()) {
          return true;
        } else if (nanos <= 0) {
          return false;
        } else {
          nanos = termination.awaitNanos(nanos);
        }
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public ListenableFuture<?> submit(Runnable task) {
    return addExceptionLogging((ListenableFuture<?>) super.submit(task));
  }

  @Override
  public <T> ListenableFuture<T> submit(Runnable task, T result) {
    return addExceptionLogging((ListenableFuture<T>) super.submit(task, result));
  }

  @Override
  public <T> ListenableFuture<T> submit(Callable<T> task) {
    return addExceptionLogging((ListenableFuture<T>) super.submit(task));
  }

  private <T> ListenableFuture<T> addExceptionLogging(ListenableFuture<T> future) {
    FutureUtils.addExceptionLogging(future, "Dispatcher task is failed", LOG);
    return future;
  }

  private void startTask() {
    lock.lock();
    try {
      if (isShutdown()) {
        throw new RejectedExecutionException("Executor already shutdown");
      }
    } finally {
      lock.unlock();
    }
  }

  private void endTask() {
    lock.lock();
    try {
      if (isTerminated()) {
        termination.signalAll();
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  protected final <T> ListenableFutureTask<T> newTaskFor(Runnable runnable, T value) {
    return ListenableFutureTask.create(runnable, value);
  }

  @Override
  protected final <T> ListenableFutureTask<T> newTaskFor(Callable<T> callable) {
    return ListenableFutureTask.create(callable);
  }
}
