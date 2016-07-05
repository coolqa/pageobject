/*
 * Copyright 2016 agido GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageobject.core

import org.scalatest.FailedStatus
import org.scalatest.exceptions.NotAllowedException
import org.scalatest.exceptions.StackDepthException
import org.scalatest.exceptions.TestCanceledException
import org.scalatest.exceptions.TestFailedDueToTimeoutException
import org.scalatest.exceptions.TestFailedException
import org.scalatest.time.Span.convertDurationToSpan

import scala.concurrent.duration.FiniteDuration

class TestHelperImpl extends TestHelper {
  def failTest[T](message: String): T = {
    throw new TestFailedException(message, 1)
  }

  def failTest[T](throwable: Throwable): T = {
    throw new TestFailedException(throwable, 1)
  }

  def cancelTest[T](message: String): T = {
    throw new TestCanceledException(message, 1)
  }

  def cancelTest[T](throwable: Throwable): T = {
    throw new TestFailedException(throwable, 1)
  }

  def timeoutTest[T](message: String, timeout: FiniteDuration): T = {
    throw new TestFailedDueToTimeoutException((_: StackDepthException) => Some(message), None,
      Right((_: StackDepthException) => 1), None, timeout)
  }

  def notAllowed[T](message: String): T = {
    throw new NotAllowedException(message, 1)
  }

  def isFailedResult[T](result: T): Boolean = {
    result == FailedStatus
  }

  def isAssertionError(th: Throwable): Boolean = th match {
    case tfe: TestFailedException => tfe.getStackTrace.head.getMethodName == "newAssertionFailedException"
    case _ => false
  }

  def isTestAbortError(th: Throwable): Boolean = th match {
    case th: Throwable if isAssertionError(th) => false
    case tce: TestCanceledException => true
    case tfe: TestFailedException => true
    case _ => false
  }
}