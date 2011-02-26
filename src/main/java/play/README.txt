ReceiveRpc & SendRpc
	Run ReceiveRpc and then SendRpc. The two outputs look something like:
	
	SendRpc:
	 [x] SendRpc: Sent 'Hello World !!!!'  with CORR ID = bb2e52d8-dc71-4775-b54e-ff5f7242a75c
	      replyTo = amq.gen-93cPbp2kTrBN3mr1ICV6SA==
	 [*] Waiting for responses. To exit press CTRL+C
	 [x] Received RESPONSE 'HELLO WORLD !!!! -- ## corr_id=bb2e52d8-dc71-4775-b54e-ff5f7242a75c ##'
	     corr_id=bb2e52d8-dc71-4775-b54e-ff5f7242a75c
	     
	ReceiveRpc:
	 [*] ReceiveRpc: Waiting for messages. To exit press CTRL+C
	 [x] Received 'Hello World !!!!'
	 [x]   with replyTo= 'amq.gen-93cPbp2kTrBN3mr1ICV6SA==' corr_id='bb2e52d8-dc71-4775-b54e-ff5f7242a75c'
	 [x]     contentType= 'text/plain'
	 [x] Sent 'HELLO WORLD !!!! -- ## corr_id=bb2e52d8-dc71-4775-b54e-ff5f7242a75c ##'

	