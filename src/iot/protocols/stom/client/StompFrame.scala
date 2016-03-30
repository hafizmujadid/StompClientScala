/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package iot.protocols.stom.client
import java.util.HashMap
import java.util.Map
import StompFrame._
import scala.collection.JavaConversions._
object StompFrame {

  def parse(raw: String): StompFrame = {
    val frame = new StompFrame()
    val commandheaderSections = raw.split("\n\n")(0)
    val headerLines = commandheaderSections.split("\n")
    frame.command = StompCommand.fromString(headerLines(0))
    for (i <- 1 until headerLines.length) {
      val key = headerLines(i).split(":")(0)
      frame.header.put(key, headerLines(i).substring(key.length + 1))
    }
    frame.body = raw.substring(commandheaderSections.length + 2)
    frame
  }
}

class StompFrame {

  var command: StompCommand = _

  var header: Map[String, String] = new HashMap[String, String]()

  var body: String = _

  def this(command: StompCommand) {
    this()
    this.command = command
  }

  override def toString(): String = {
    String.format("command: %s, header: %s, body: %s", this.command, this.header.toString, this.body)
  }

  def getBytes(): Array[Byte] = {
    var frame = this.command.toString + '\n'
    for (key <- this.header.keySet) {
      frame += key + ":" + this.header.get(key) + '\n'
    }
    frame += '\n'
    if (this.body != null) {
      frame += this.body
    }
    frame += "\0"
    frame.getBytes
  }
}