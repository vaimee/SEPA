package it.unibo.arces.wot.sepa.engine.protocol.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import it.unibo.arces.wot.sepa.engine.protocol.http.handler.SPARQL11Handler;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class ServerPrincipaleSubscribe extends Thread{
	
	SPARQL11Handler handler;
	Scheduler scheduler;

	public ServerPrincipaleSubscribe(Scheduler scheduler) throws IllegalArgumentException {
		this.scheduler = scheduler;
	}
	
	

	public void run()  {
		int port = 1081;
		
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
			/*
			 * E' necessario che il collegamento TCP venga mantenuto sempre aperto, in quanto
			 * ogni volta che avviene una modifica, SEPA deve rispondere al client che ha fatto
			 * la subscribe
			 */
				System.out.println("Server: waiting for subscribe...\n");

				try {
					clientSocket = serverSocket.accept(); //bloccante!!!
					System.out.println("Server: connection accepted: " + clientSocket);
				} catch (Exception e) {
					System.err
					.println("Server: impossible to establish a connection: "
							+ e.getMessage());
					e.printStackTrace();
					//continue;
				}
				
				try {
					new ServerSecondarioSubscribe(clientSocket, scheduler).start();
				} catch (Exception e) {
					System.err.println("Server: problemi nel server thread: "
							+ e.getMessage());
					e.printStackTrace();
					//continue;
				}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Server: termino...");
			System.exit(2);
		}
	}
}

