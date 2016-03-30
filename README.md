# StompClientScala
This is a simple stomp client written in scala. To use it you must have some server running that supports stomp protocol. ActiveMQ is a simple server that supprts stomp.

#Stomp Client Example Code
step 01: Create a class that extends StompClient abstract class and override required methods
Then use following code to listen for messages from a queue
#Receive Messages
  val client = new StompClientWrapper("host", "port", "user", "password")
		client.connect
		client.subscribe("queue")
		client.disconnect
		
#Send Messages
	client.connect
	client.send("queue","message")
	
