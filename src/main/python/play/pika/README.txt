
gpb_send_rpc
------------
	Run SiamCiServerGpb.java (currently via SiamCiMain) and then gpb_send_rpc.py
	The outputs look like the following:
	
	gpb_send_rpc:
		Sending command WITH CORR ID: b602c2e9-3683-4568-8e85-b4f23bbf1a8c
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

	mvn exec:java -Dsiam-ci 
		===========Waiting for call============
		net.ooici.siamci.impl.gpb.SiamCiServerGpb._showCommand(){106} -  [x] Command: command=hiCmd
		net.ooici.siamci.impl.gpb.SiamCiServerGpb._showCommand(){108} -  [x]          arg: channel=ch1  parameter=pr1
		net.ooici.siamci.impl.gpb.SiamCiServerGpb._showCommand(){108} -  [x]          arg: channel=ch2  parameter=pr2
		net.ooici.siamci.impl.gpb.SiamCiServerGpb._run(){88} -  [x] Received body len 31
		net.ooici.siamci.impl.gpb.SiamCiServerGpb._run(){89} -  [x]   with replyTo= 'amq.gen-8OeH1CGKYhT185/uH5eWWA==' corr_id='b602c2e9-3683-4568-8e85-b4f23bbf1a8c'
		net.ooici.siamci.impl.gpb.SiamCiServerGpb._run(){90} -  [x]     contentType= 'application/octet-stream'
		net.ooici.siamci.impl.gpb.SiamCiServerGpb._run(){101} -  [x] Sent '[B@a51064e'
		net.ooici.siamci.impl.gpb.SiamCiServerGpb._run(){76} - 
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
	