package com.vaimee.sepa.apps.chat;

public interface IMessageHandler {
	public void onMessageReceived(String userUri,String fromUri,String messageUri,String user,String message,String time);
	public void onMessageRemoved(String userUri, String toUri,String messageUri, String user, String message,String time);
	public void onMessageSent(String userUri, String toUri,String messageUri,String time);	
}
