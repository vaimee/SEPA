package it.unibo.arces.wot.sepa.engine.protocol.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import it.unibo.arces.wot.sepa.engine.protocol.http.handler.SPARQL11Handler;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class ServerPrincipaleQuery extends Thread{
	
	SPARQL11Handler handler;
	Scheduler scheduler;

	public ServerPrincipaleQuery(Scheduler scheduler) throws IllegalArgumentException {
		this.scheduler = scheduler;
	}
	
	

	public void run()  {
		int port = 1080;
	
		ServerSocket serverSocket = null;
		Socket clientSocket = null;

		try {
			serverSocket = new ServerSocket(port);
			serverSocket.setReuseAddress(true);
			System.out.println("ServerPrincipale: started ");
			System.out.println("Server: socket created: " + serverSocket);
		} catch (Exception e) {
			System.err
			.println("Server: problems about server socket's creation: "+ e.getMessage());
			e.printStackTrace();
			try {
				serverSocket.close();
			} catch (IOException e1) {
				System.out.println("Errore nella chiusura della socketServer");
				e1.printStackTrace();
			}
			System.exit(1); //idem
		}

		try {
			while (true) {
				System.out.println("Server: waiting for query...\n");

				try {
					clientSocket = serverSocket.accept(); //bloccante!!!
					System.out.println("Server: connection accepted: " + clientSocket);
				} catch (Exception e) {
					System.err
					.println("Server: impossible to establish a connection: "
							+ e.getMessage());
					e.printStackTrace();
					continue;
				}
				
				try {
					new ServerSecondarioQuery(clientSocket, scheduler).start();
				} catch (Exception e) {
					System.err.println("Server: problemi nel server thread: "
							+ e.getMessage());
					e.printStackTrace();
					continue;
				}
			}// while true
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Server: termino...");
			System.exit(2);
		}
	}
}

