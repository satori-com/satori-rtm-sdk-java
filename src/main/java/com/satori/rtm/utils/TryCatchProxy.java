package com.satori.rtm.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class TryCatchProxy {
  @SuppressWarnings("unchecked")
  public static <T> T wrap(final Object target, Class<T> contract) {
    final Logger logger = LoggerFactory.getLogger(target.getClass());
    return (T) Proxy.newProxyInstance(
        target.getClass().getClassLoader(),
        new Class<?>[]{contract},
        new InvocationHandler() {
          @Override
          public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
            try {
              return method.invoke(target, objects);
            } catch (InvocationTargetException targetException) {
              logger.error("Suppress exception", targetException.getCause());
            } catch (Exception exception) {
              logger.error("Proxy exception", exception);
            }
            return null;
          }
        });
  }
}
