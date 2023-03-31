/*
 * -\-\-
 * Mobius
 * --
 * Copyright (c) 2017-2020 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */
package com.spotify.mobius;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a connection that queues up messages until a delegate to consume them is available.
 * Useful for setting up circular dependencies safely. All methods are synchronized for ease of
 * implementation.
 */
class QueuingConnection<I> implements Connection<I> {

  private CompletableFuture<Connection<I>> delegate = new CompletableFuture<>();

  void setDelegate(Connection<I> delegate) {
    final CompletableFuture<Connection<I>> thisDelegate = this.delegate;
    if (thisDelegate == null) {
      return;
    }
    thisDelegate.complete(checkNotNull(delegate));
    if (thisDelegate.getNow() != delegate) {
      throw new IllegalStateException("Attempt at setting delegate twice");
    }
  }

  @Override
  public void accept(I value) {
    final CompletableFuture<Connection<I>> thisDelegate = this.delegate;
    if (thisDelegate == null) {
      return;
    }
    thisDelegate.thenAccept(d -> d.accept(value));
  }

  @Override
  public void dispose() {
    final CompletableFuture<Connection<I>> thisDelegate = this.delegate;
    if (thisDelegate == null) {
      return;
    }
    thisDelegate.dispose();
    delegate = null;
  }
}

