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

import java.io.IOException
import java.io.InputStream
import java.net.Socket
import java.net.URI
import java.net.URISyntaxException
import java.util.Map
import javax.net.SocketFactory
import javax.net.ssl.SSLSocketFactory
import scala.collection.JavaConversions._

abstract class StompClient(private val uri: URI) {

	private var socket: Socket = _

	private var sessionId: String = _

	private var readerThread: Thread = _

	@volatile private var running: Boolean = true

	def this(url: String) {
		this(new URI(url))
	}
	def this(host:String,port:String){
		this(s"tcp://$host:$port")
	}

	def onConnected(sessionId: String): Unit

	def onDisconnected(): Unit

	def onMessage(messageId: String, body: String): Unit

	def onReceipt(receiptId: String): Unit

	def onError(message: String, description: String): Unit

	def onCriticalError(e: Exception): Unit

	def connect() {
		try {
			if (uri.getScheme == "tcp") {
				socket = new Socket(this.uri.getHost, this.uri.getPort)
			} else if (uri.getScheme == "tcps") {
				val socketFactory = SSLSocketFactory.getDefault
				socket = socketFactory.createSocket(this.uri.getHost, this.uri.getPort)
			} else {
				throw new StompException("Library is not support this scheme")
			}
			readerThread = new Thread(new Runnable() {

				def run() {
					reader()
				}
			})
			readerThread.start()
			val connectFrame = new StompFrame(StompCommand.fromString("connect"))
			if (uri.getUserInfo != null) {
				val credentials = uri.getUserInfo.split(":")
				if (credentials.length == 2) {
					connectFrame.header.put("login", credentials(0))
					connectFrame.header.put("passcode", credentials(1))
				}
			}
			send(connectFrame)
			synchronized {
				wait(5000)
			}
		} catch {
			case e: Exception => {
				val ex = new StompException("some problem with connection")
				ex.initCause(e)
				throw ex
			}
		}
	}

	def disconnect() {
		if (socket.isConnected) {
			try {
				val frame = new StompFrame(StompCommand.fromString("disconnect"))
				frame.header.put("session", sessionId)
				send(frame)
				running = false
				socket.close()
			} catch {
				case e: Exception =>
			}
		}
	}

	private def reader() {
		try {
			val in = this.socket.getInputStream
			val sb = new StringBuilder()
			while (running) {
				try {
					sb.setLength(0)
					var ch: Int = 0
					do {
						ch = in.read()
						if (ch < 0) {
							onCriticalError(new IOException("stome server disconnected!"))
							return
						}
					} while (ch < 'A' || ch > 'Z');
					do {
						sb.append(ch.toChar)
						ch = in.read()
					} while (ch != 0);
					val frame = StompFrame.parse(sb.toString)
					frame.command match {
						case CONNECTED =>
							synchronized {
								notify()
							}
							sessionId = frame.header.get("session")
							onConnected(sessionId)

						case DISCONNECTED => onDisconnected()
						case RECEIPT =>
							var receiptId = frame.header.get("receipt-id")
							onReceipt(receiptId)

						case MESSAGE =>
							var messageId = frame.header.get("message-id")
							onMessage(messageId, frame.body)

						case ERROR =>
							var message = frame.header.get("message")
							onError(message, frame.body)

						case _ => //break
					}
				} catch {
					case e: IOException => {
						onCriticalError(e)
						return
					}
				}
			}
		} catch {
			case e: IOException => {
				onCriticalError(e)
				return
			}
		}
	}

	def begin(transaction: String) {
		val frame = new StompFrame(StompCommand.fromString("begin"))
		frame.header.put("transaction", transaction)
		send(frame)
	}

	def commit(transaction: String) {
		val frame = new StompFrame(StompCommand.fromString("commit"))
		frame.header.put("transaction", transaction)
		send(frame)
	}

	def abort(transaction: String) {
		val frame = new StompFrame(StompCommand.fromString("abort"))
		frame.header.put("transaction", transaction)
		send(frame)
	}

	def send(destination: String, message: String) {
		val frame = new StompFrame(StompCommand.fromString("send"))
		frame.header.put("destination", destination)
		frame.header.put("session", sessionId)
		frame.body = message
		send(frame)
	}

	def send(destination: String, header: Map[String, String], message: String) {
		val frame = new StompFrame(StompCommand.fromString("send"))
		frame.header.put("destination", destination)
		frame.header.put("session", sessionId)
		for (key <- header.keySet) {
			frame.header.put(key, header.get(key))
		}
		frame.body = message
		send(frame)
	}

	def subscribe(destination: String) {
		subscribe(destination, Ack.fromString("auto"))
	}

	def subscribe(destination: String, ack: Ack) {
		val frame = new StompFrame(StompCommand.fromString("subscribe"))
		frame.header.put("destination", destination)
		frame.header.put("session", sessionId)
		frame.header.put("ack", ack.toString)
		send(frame)
	}

	def unsubscribe(destination: String) {
		val frame = new StompFrame(StompCommand.fromString("unsubscribe"))
		frame.header.put("destination", destination)
		frame.header.put("session", sessionId)
		send(frame)
	}

	def ack(messageId: String) {
		val frame = new StompFrame(StompCommand.fromString("ack"))
		frame.header.put("message-id", messageId)
		send(frame)
	}

	def ack(messageId: String, transaction: String) {
		val frame = new StompFrame(StompCommand.fromString("ack"))
		frame.header.put("message-id", messageId)
		frame.header.put("transaction", transaction)
		send(frame)
	}

	private def send(frame: StompFrame) {
		synchronized {
			try {
				println("writing MEssage = "+ frame.body)
				socket.getOutputStream.write(frame.getBytes)
			} catch {
				case e: IOException => {
					val ex = new StompException("Problem with sending frame")
					ex.initCause(e)
					throw ex
				}
			}
		}
	}
}
