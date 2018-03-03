/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A future which forwards all its method calls to another future. Subclasses should override one or
 * more methods to modify the behavior of the backing future as desired per the <a href=
 * "http://en.wikipedia.org/wiki/Decorator_pattern">decorator pattern</a>.
 *
 * <p>Most subclasses can simply extend {@link SimpleForwardingCheckedFuture}.
 *
 * @param <V> The result type returned by this Future's {@code get} method
 * @param <X> The type of the Exception thrown by the Future's {@code checkedGet} method
 * @author Anthony Zana
 * @since 9.0
 * @deprecated {@link CheckedFuture} cannot properly support the chained operations that are the
 *     primary goal of {@link ListenableFuture}. {@code CheckedFuture} also encourages users to
 *     rethrow exceptions from one thread in another thread, producing misleading stack traces.
 *     Additionally, it has a surprising policy about which exceptions to map and which to leave
 *     untouched. Guava users who want a {@code CheckedFuture} can fork the classes for their own
 *     use, possibly specializing them to the particular exception type they use. We recommend that
 *     most people use {@code ListenableFuture} and perform any exception wrapping themselves. This
 *     class is scheduled for removal from Guava in February 2018.
 */
@Beta
@Deprecated
@GwtIncompatible
public abstract class ForwardingCheckedFuture<V, X extends Exception>
    extends ForwardingListenableFuture<V> implements CheckedFuture<V, X> {


  @Override
  public V checkedGet() throws X {
    return delegate().checkedGet();
  }


  @Override
  public V checkedGet(long timeout, TimeUnit unit) throws TimeoutException, X {
    return delegate().checkedGet(timeout, unit);
  }

  @Override
  protected abstract CheckedFuture<V, X> delegate();

  // TODO(cpovirk): Use Standard Javadoc form for SimpleForwarding*
  /**
   * A simplified version of {@link ForwardingCheckedFuture} where subclasses can pass in an already
   * constructed {@link CheckedFuture} as the delegate.
   *
   * @since 9.0
   * @deprecated {@link CheckedFuture} cannot properly support the chained operations that are the
   *     primary goal of {@link ListenableFuture}. {@code CheckedFuture} also encourages users to
   *     rethrow exceptions from one thread in another thread, producing misleading stack traces.
   *     Additionally, it has a surprising policy about which exceptions to map and which to leave
   *     untouched. Guava users who want a {@code CheckedFuture} can fork the classes for their own
   *     use, possibly specializing them to the particular exception type they use. We recommend
   *     that most people use {@code ListenableFuture} and perform any exception wrapping
   *     themselves. This class is scheduled for removal from Guava in February 2018.
   */
  @Beta
  @Deprecated
  public abstract static class SimpleForwardingCheckedFuture<V, X extends Exception>
      extends ForwardingCheckedFuture<V, X> {
    private final CheckedFuture<V, X> delegate;

    protected SimpleForwardingCheckedFuture(CheckedFuture<V, X> delegate) {
      this.delegate = Preconditions.INSTANCE.checkNotNull(delegate);
    }

    @Override
    protected final CheckedFuture<V, X> delegate() {
      return delegate;
    }
  }
}
