ReceiveRpc & SendRpc
	Run ReceiveRpc and then SendRpc. The two outputs should look like so:
	
	SendRpc:
	 [*] Waiting for responses. To exit press CTRL+C
	 [x] Sent 'Hello World !!!!'  with CORR ID = 02c471d7-e96e-4375-9cb7-bbcc406c0bca
	 [x] Received RESPONSE '##corr_id=02c471d7-e96e-4375-9cb7-bbcc406c0bca## Hello World !!!!'
	     corr_id=02c471d7-e96e-4375-9cb7-bbcc406c0bca

	ReceiveRpc:
	 [*] Waiting for messages. To exit press CTRL+C
	 [x] Received 'Hello World !!!!'
	 [x]     replyTo 'amq.gen-VSHSoKw/Czh+9Es0a0rLmA=='    corr_id=02c471d7-e96e-4375-9cb7-bbcc406c0bca
	 [x]     contentType 'MyContentType'
	 [x] Sent '##corr_id=02c471d7-e96e-4375-9cb7-bbcc406c0bca## Hello World !!!!'

	