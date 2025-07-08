package com.vaimee.sepa.apps.chat.client;

import java.io.IOException;

import com.vaimee.sepa.apps.chat.ChatMonitor;
import com.vaimee.sepa.apps.chat.Users;
import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;

public class PingPongClient extends BasicClient {
	private int index = 0;
	
	public PingPongClient(String userURI, Users users,ChatMonitor monitor)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException, IOException, InterruptedException {
		super(userURI, users,1,monitor);
	}
	
	@Override
	public void onMessageReceived(String userUri, String fromUri,String messageUri, String user, String message,String time) {
		super.onMessageReceived(userUri,fromUri,messageUri,user,message,time);
		sendMessage(fromUri, "Reply #" + index++);
	}

}
