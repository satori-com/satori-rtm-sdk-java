package com.satori.rtm.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class DispatcherProxy {
  @SuppressWarnings("unchecked")
  public static <T> T wrap(T underlying, ExecutorService pool) {
    final ClassLoader classLoader = underlying.getClass().getClassLoader();
    final Class<T> intf = (Class<T>) underlying.getClass().getInterfaces()[0];
    return (T) Proxy.newProxyInstance(
        classLoader,
        new Class<?>[]{intf},
        new ProxyHandler<T>(underlying, pool));
  }

  static class ProxyHandler<T> implements InvocationHandler {

    private final T underlying;
    private final ExecutorService pool;

    ProxyHandler(T underlying, ExecutorService pool) {
      this.underlying = underlying;
      this.pool = pool;
    }

    @Override
    public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
      final Future<Object> future = pool.submit(new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          return method.invoke(underlying, args);
        }
      });
      return handleResult(method, future);
    }

    private Object handleResult(Method method, Future<Object> future) throws Throwable {
      if (method.getReturnType() == void.class) { return null; }
      try {
        return future.get();
      } catch (ExecutionException e) {
        throw e.getCause();
      }
    }
  }
}
