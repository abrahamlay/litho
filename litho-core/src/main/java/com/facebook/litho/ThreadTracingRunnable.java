/*
 * Copyright 2018-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.litho;

/**
 * ThreadTracingRunnable tries to help debugging crashes happening across threads showing the
 * stacktrace of the crash origin which scheduled this runnable.
 *
 * <p>A stacktrace, together with the origin thread name, is going to be "saved" at the time this
 * class is instantiated and appended as "cause" to whatever is thrown from the {@link #run()}
 * method.
 *
 * <p>To use this class, just extends it instead of implementing {@link Runnable}. Then just
 * implement {@link #tracedRun(Throwable)}.
 *
 * <p>If the runnable is created ahead of time and used somewhere else later, a new, more relevant,
 * stacktrace can be created calling {@link #resetTrace()}.
 *
 * <p>If you need to chain another Runnable from your run method, you can pass the Throwable
 * parameter from {@link #tracedRun(Throwable)} to the constructor {@link
 * ThreadTracingRunnable#tracedRun(Throwable)}. Therefore, the crash can now be tracked across
 * boundaries of multiple threads.
 */
public abstract class ThreadTracingRunnable implements Runnable {

  private final Throwable mTracingThrowable;

  public ThreadTracingRunnable() {
    Thread thread = Thread.currentThread();
    // This Throwable is saving the call stack which created this Runnable.
    mTracingThrowable =
        new Throwable(
            "Runnable instantiated on thread id: "
                + thread.getId()
                + ", name: "
                + thread.getName());
  }

  /**
   * If you are chaining multiple Runnable together that are going to schedule each other within
   * their run() method, use this constructor to track their stacktraces across threads. The
   * required parameter here is the argument coming from {@link #tracedRun(Throwable)}.
   */
  public ThreadTracingRunnable(Throwable prevTracingThrowable) {
    this();
    mTracingThrowable.initCause(prevTracingThrowable);
  }

  /**
   * Implement here your {@link Runnable#run()} code.
   *
   * @param tracingThrowable If this Runnable schedule another Runnable to another thread, pass this
   *     parameter to the new ThreadTracingRunnable class with {@link
   *     #ThreadTracingRunnable(Throwable)}.
   */
  public abstract void tracedRun(Throwable tracingThrowable);

  /**
   * Reset the stacktrace of this Runnable to this point. To be called right before the runnable is
   * scheduled to another thread, in case it was instantiated ahead of time with a different code
   * flow.
   */
  public void resetTrace() {
    mTracingThrowable.fillInStackTrace();
  }

  @Override
  public final void run() {
    try {
      tracedRun(mTracingThrowable);
    } catch (Throwable t) {
      Throwable lastThrowable = t;
      while (lastThrowable.getCause() != null) {
        lastThrowable = lastThrowable.getCause();
      }
      lastThrowable.initCause(mTracingThrowable);
      throw t;
    }
  }
}
