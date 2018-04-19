package it.unibo.arces.wot.sepa.apps.chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class ExtremeChat {
	public static int N_CLIENTS = 2;
	public static int BASE = 0;

	private static Users users;
	private static List<PingPongClient> clients = new ArrayList<PingPongClient>();
	
	class MessageListener implements ChatListener {

		@Override
		public void onMessageReceived(Message message) {
			System.out.println(message.getTime()+" [" +users.getUserName(message.getFrom())+ "] --> ["+users.getUserName(message.getTo())+"] Message: "+message.getText()); 
		}

		@Override
		public void onBrokenConnection() {
			// TODO Auto-generated method stub
			
		}
		
	}
	public static void main(String[] args)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, IOException {

		if (args.length != 2) {
			System.out.println("Running with default parameters (you can run with two arguments: <BASE> <N_CLIENTS>");
		}
		else
		{
			BASE = Integer.parseInt(args[0]);	
			N_CLIENTS = Integer.parseInt(args[1]);
		}
		
		// Register chat BOTS
		UserRegistration registration = new UserRegistration();
		for (int i = BASE; i < BASE+N_CLIENTS; i++) {
			registration.register("ChatBot" + i);
		}
		
		users = new Users();
		if(!users.joinChat()) System.exit(-1);		
		
		MessageListener listener = new ExtremeChat().new MessageListener();
		
		for (String user : users.getUsers()) {
			PingPongClient client = new PingPongClient(user,users,listener);
			clients.add(client);	
			new Thread(client).start();
		}
		
		System.out.println("Press a key to exit...");
		System.in.read();
		System.exit(0);
	}
}
