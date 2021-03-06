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
package org.pageobject.howto.maven

import org.pageobject.core.BrowserErrorPage
import org.pageobject.core.page.EmptyUnexpectedPagesFactory
import org.pageobject.core.page.UnexpectedPagesFactory
import org.pageobject.core.page.UrlPage
import org.pageobject.scalatest.PageObjectSuite
import org.scalatest.FunSpec

class MavenHowToExample extends FunSpec with PageObjectSuite {
  it("should detect browsers connection refused page") {
    via(UrlPage("http://localhost:65534/"))
    UnexpectedPagesFactory.withUnexpectedPages(EmptyUnexpectedPagesFactory()) {
      at(BrowserErrorPage())
    }
  }
}
