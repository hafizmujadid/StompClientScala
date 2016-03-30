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

import scala.collection.JavaConversions._
abstract sealed class StompCommand

case object CONNECT extends StompCommand
case object SEND extends StompCommand
case object SUBSCRIBE extends StompCommand
case object UNSUBSCRIBE extends StompCommand
case object BEGIN extends StompCommand
case object ABORT extends StompCommand
case object ACKNOWLEDGE extends StompCommand
case object COMMIT extends StompCommand
case object DISCONNECT extends StompCommand
case object MESSAGE extends StompCommand
case object RECEIPT extends StompCommand
case object ERROR extends StompCommand
case object DISCONNECTED extends StompCommand
case object CONNECTED extends StompCommand
object StompCommand {
	def fromString(value: String): StompCommand = value.toLowerCase() match {
		case "connect" => CONNECT
		case "connected" => CONNECTED
		case "disconnect" => DISCONNECT
		case "disconnected" => DISCONNECTED
		
		case "subscribe" => SUBSCRIBE
		case "unsubscribe" => UNSUBSCRIBE
		
		case "begin" => BEGIN
		case "abort" => ABORT
		
		case "send" => SEND
		case "ack" => ACKNOWLEDGE
		case "commit" => COMMIT
		
		case "message" => MESSAGE
		case "receipt" => RECEIPT
		case "error" => ERROR
		
		
	}
}