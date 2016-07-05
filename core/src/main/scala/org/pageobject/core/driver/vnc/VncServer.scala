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
package org.pageobject.core.driver.vnc

import java.io.File
import java.util.concurrent.atomic.AtomicInteger

import org.openqa.selenium.net.PortProber

import scala.sys.process.BasicIO
import scala.sys.process.Process
import scala.sys.process.ProcessLogger

private object VncServer {
  val threadGroup = new ThreadGroup("VncServer")
}

/**
 * trait to start and stop a VNC Server instance.
 */
trait VncServer {
  protected def startCommand: Option[String]

  protected def checkCommand: Option[String]

  protected def stopCommand: Option[String]

  protected def onTerminated: () => Unit

  protected def id: Int

  protected val logger: ProcessLogger = ProcessLogger(message => println(s"VNC $id $message"))

  protected def execute(cmd: String): Process = {
    Process(cmd, new File("..")).run(BasicIO(withIn = false, logger).daemonized)
  }

  def shutdown(): Unit = {
    stopCommand.foreach(execute(_))
  }

  def checkConnection(): Boolean = {
    checkCommand.forall(execute(_).exitValue == 0)
  }

  def start(): Unit = {
    val process = execute(startCommand.get)
    val thread = new Thread(VncServer.threadGroup, new Runnable {
      override def run(): Unit = {
        process.exitValue()
        onTerminated()
      }
    })
    thread.setName(s"VncServerThread-$id")
    thread.setDaemon(true)
    thread.start()
  }
}

/**
 * You can configure the port range used by VNC,
 * the default is to use ports from one upwards.
 */
object CountedId {
  private val idCounter = new AtomicInteger
}

trait CountedId {
  this: VncServer =>

  val id = CountedId.idCounter.incrementAndGet()
}

/**
 * A trait providing the URL used to connect to selenium running inside of the VNC Server.
 */
trait SeleniumVncServer extends VncServer {
  def seleniumPort: Int

  lazy val url = s"http://localhost:$seleniumPort/wd/hub"
}

/**
 * Tries to find an unused port for selenium server.
 */
trait FindFreeSeleniumPort {
  this: SeleniumVncServer =>

  val seleniumPort: Int = PortProber.findFreePort()
}

/**
 * The default selenium port is just a fixed offset added to the display id
 */
trait FixedSeleniumPort {
  this: SeleniumVncServer =>

  val seleniumPortOffset: Int = 14000
  val seleniumPort: Int = id + seleniumPortOffset
}