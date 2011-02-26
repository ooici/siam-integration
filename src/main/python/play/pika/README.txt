
gpb_send_rpc
------------
	Run SiamCiServerGpb.java (currently via SiamCiMain) and then gpb_send_rpc.py
	The outputs look like the following:
	
	gpb_send_rpc:
		Sending command WITH CORR ID: 0c4afdd0-f2ed-45ee-855d-82f4903ed29e
		Command sent:
		    <class 'net.ooici.play.instr.instrument_defs_pb2.Command'>
		    | command: "hiCmd"
		    | args {
		    |   channel: "ch1"
		    |   parameter: "pr1"
		    | }
		    | args {
		    |   channel: "ch2"
		    |   parameter: "pr2"
		    | }
		    | 
		correlation_id --> None
		Command received:
		    <class 'net.ooici.play.instr.instrument_defs_pb2.Command'>
		    | command: "hiCmd"
		    | args {
		    |   channel: "ch1"
		    |   parameter: "pr1"
		    | }
		    | args {
		    |   channel: "ch2"
		    |   parameter: "pr2"
		    | }
		    | 

	mvn exec:java -Dsiam-ci 
		===========Waiting for call============
		  [x] Received body len 31
		  [x]   with replyTo= 'amq.gen-td2SPsaBYdLlv4/bboZJKg==' corr_id='0c4afdd0-f2ed-45ee-855d-82f4903ed29e'
		  [x]     contentType= 'application/octet-stream'
		  [x] Command received:
		    class net.ooici.play.instr.InstrumentDefs$Command
		    | command: "hiCmd"
		    | args {
		    |   channel: "ch1"
		    |   parameter: "pr1"
		    | }
		    | args {
		    |   channel: "ch2"
		    |   parameter: "pr2"
		    | }
		    | 
		  [x] Command replied.
		===========Waiting for call============



demo_receive_rpc & demo_send_rpc
--------------------------------
	Run demo_receive_rpc and then demo_send_rpc. The two outputs should look like so:
	
	demo_send_rpc:
		demo_send: Connected to RabbitMQ
		demo_send: Received our Channel
		demo_send: Receiver Queue Declared: demo_receive_queue
		demo_send: Reply Queue Declared: amq.gen-YrB2pXNlV4dIEwiHjFKPeA==
		Sending: Hello World #0: 1298753977.15681410  WITH CORR ID: 15b8b2b3-5173-487d-9981-10e1bd8e2297
		
	demo_receive_rpc:
		demo_receive: Connected to RabbitMQ
		demo_receive: Received our Channel
		demo_receive: Receiver Queue Declared: demo_receive_queue
		Received message: 'Hello World #0: 1298753977.15681410'
		!!!!  header_frame.reply_to is None!  Not replying.

	The sender sends the message and appropriate replyTo and correlationId properties. However,
	the properties are not received/reported by the pika library, so, in particular, there is
	not routingKey (replyTo value) for the receiver to reply properly!  (pika bug?)
	