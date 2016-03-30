package iot.protocols.stom.client

import java.net.URI

class StompClientWrapper(private val uri: URI) extends StompClient(uri) {

	def this(host: String, port: String,user:String,password:String) {
		this(new URI(s"tcp://$user:$password@$host:$port"))
	}
	def onConnected(sessionId: String) {
		println("connected:")
	}

	def onDisconnected() {
		println("disconnected")
	}

	def onMessage(messageId: String, body: String) {
		println(body)
	}

	def onReceipt(receiptId: String) {
		println("receipt")
	}

	def onError(message: String, description: String) {
		println("error: message = " + message + " description = " + description)
	}

	def onCriticalError(e: Exception) {
		e.printStackTrace()
	}
}