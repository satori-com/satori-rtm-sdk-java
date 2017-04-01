package com.satori.rtm.utils;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.satori.rtm.Callback;
import com.satori.rtm.model.PduException;
import org.slf4j.Logger;
import java.util.concurrent.CancellationException;

public class FutureUtils {
  public static <T> void delegateTo(ListenableFuture<T> task,
                                    final SettableFuture<T> settableFuture) {
    Futures.addCallback(task, new FutureCallback<T>() {
      public void onSuccess(T result) {
        settableFuture.set(result);
      }

      public void onFailure(Throwable t) {
        if (t instanceof CancellationException) {
          settableFuture.cancel(false);
        } else {
          settableFuture.setException(t);
        }
      }
    });
  }

  public static <T> void addExceptionLogging(ListenableFuture<T> task, final String message,
                                             final Logger logger) {
    Futures.addCallback(task, new FutureCallback<T>() {
      public void onSuccess(T result) { }

      public void onFailure(Throwable t) {
        if (t instanceof CancellationException) {
          return;
        }
        if (t instanceof PduException) {
          // in case of negative response just print the negative PDU without stacktrace
          logger.warn(message + ": " + t.getMessage());
        } else {
          logger.warn(message, t);
        }
      }
    });
  }

  public static <T> Callback<T> addExceptionLogging(final Callback<T> callback,
                                                    final String message, final Logger logger) {
    return new Callback<T>() {
      @Override
      public void onResponse(T result) {
        callback.onResponse(result);
      }

      @Override
      public void onFailure(Throwable t) {
        if (t instanceof PduException) {
          logger.warn(message + ": " + t.getMessage());
        } else {
          logger.warn(message, t);
        }
        callback.onFailure(t);
      }
    };
  }
}
