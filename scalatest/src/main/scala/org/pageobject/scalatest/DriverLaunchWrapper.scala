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
package org.pageobject.scalatest

import com.typesafe.config.ConfigFactory
import org.pageobject.core.TestHelper
import org.pageobject.core.driver.DefaultDriverFactoryList
import org.pageobject.core.driver.DriverFactories
import org.pageobject.core.driver.DriverFactory
import org.pageobject.core.driver.DriverFactoryHolder
import org.pageobject.core.driver.RunWithDrivers
import org.pageobject.core.page.UnexpectedPagesFactory
import org.pageobject.core.tools.Limit.TestLimit
import org.pageobject.core.tools.LimitProvider
import org.scalatest.Args
import org.scalatest.DoNotDiscover
import org.scalatest.DynaTags
import org.scalatest.Filter
import org.scalatest.PageObjectHelper
import org.scalatest.Status
import org.scalatest.Suite
import org.scalatest.Suites
import org.scalatest.tools.AnnotationHelper

import scala.util.Try

/**
 * Helper Object to get the corresponding DriverFactories
 */
object DriverLaunchWrapper {
  def getDriverFactories(clazz: Class[_]): DriverFactories = {
    AnnotationHelper.find(classOf[RunWithDrivers], clazz)
      .map(_.value())
      .orElse(
        sys.env.get("RUN_WITH_DRIVERS")
          .orElse(Try(ConfigFactory.load().getString("org.pageobject.run-with-drivers")).toOption)
          .map(Class.forName(_).asInstanceOf[Class[DriverFactories]])
      )
      .orElse(sys.env.get("IGNORE_DEFAULT_DRIVER") match {
        case None | Some("0") | Some("false") =>
          Some(classOf[DefaultDriverFactoryList])
        case Some(_) => None
      })
      .getOrElse(TestHelper.notAllowed("Missing RUN_WITH_DRIVERS environment variable!"))
      .newInstance()
  }
}

/**
 * This class will wrap the "real" test suite.
 *
 * A list of driver factories is queried from <code>&#064;RunWithDrivers</code>
 *
 * For each driver factory an instance of the "real" test suite is created.
 **/
@DoNotDiscover
class DriverLaunchWrapper(clazz: Class[_ <: DriverLauncher with Suite])
  extends Suites /*with ParallelTestExecution with ConfigureableParallelTestLimit*/ with LimitProvider {
  // TODO see https://github.com/agido/pageobject/issues/3

  private val currentMock = DriverFactory.currentMock

  private val runWith = DriverLaunchWrapper.getDriverFactories(clazz)

  private def createBrowserSuiteInstance(driverFactory: DriverFactory) = {
    DriverFactoryHolder.withValue(Some(driverFactory)) {
      DriverFactory.withWebDriverMock(currentMock) {
        clazz.getConstructor().newInstance()
      }
    }
  }

  override def limit = TestLimit

  override val nestedSuites = UnexpectedPagesFactory.withMaybeUnexpectedPages(runWith) {
    runWith.drivers().map(config => createBrowserSuiteInstance(config.asInstanceOf[DriverFactory])).toIndexedSeq
  }

  private def patchFilter(filter: Filter): Filter = {
    val suiteTags = filter.dynaTags.suiteTags
    val testTags = filter.dynaTags.testTags.flatMap {
      case (_, tags) => nestedSuites.map(suite => (suite.suiteId, tags))
    }
    Filter(filter.tagsToInclude, filter.tagsToExclude, excludeNestedSuites = false, DynaTags(suiteTags, testTags))
  }

  override def expectedTestCount(filter: Filter): Int = {
    super.expectedTestCount(patchFilter(filter))
  }

  override def suiteName = PageObjectHelper.suiteName(clazz)

  override def suiteId = clazz.getName

  override def run(testName: Option[String], args: Args): Status = {
    if (runWith.drivers().isEmpty) {
      TestHelper.failTest("no browsers selected, have a look at the documentation about org.pageobject.core.tools.Limit")
    }
    UnexpectedPagesFactory.withMaybeUnexpectedPages(runWith) {
      super.run(testName, args.copy(filter = patchFilter(args.filter)))
    }
  }
}
