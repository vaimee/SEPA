package it.unibo.arces.wot.sepa.engine.protocol.tcp;

//Alessio Pazzani 0000766862
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;



/*
*
* Varie: controllo campi inviati dal client
* 
* 
* sistemo il server quando ne ho tempo
*
*/


public class ServerSecondarioQuery extends Thread {
	private Socket clientSocket = null;
	private Scheduler scheduler = null;

	public ServerSecondarioQuery(Socket clientSocket, Scheduler scheduler) {
		this.clientSocket = clientSocket;
		this.scheduler = scheduler;
	}

	public void run() {
		System.out.println("Attivazione figlio: "+ Thread.currentThread().getName());

		DataInputStream inSock;
		DataOutputStream outSock;

		try {
			inSock = new DataInputStream(clientSocket.getInputStream());
			outSock = new DataOutputStream(clientSocket.getOutputStream());
		} catch (IOException ioe) {
			System.out.println("Problemi nella creazione degli stream di input/output su socket: ");
			ioe.printStackTrace();
			return;
		}

		try {
			try {
				//INIZIALIZZAZIONE CAMPI MANDATI DAL CLIENT
				String richiestaClient; 	
				while ((richiestaClient = inSock.readUTF()) != null) {

					System.out.println("Codice ricevuto dal cliente: " + richiestaClient + "\n");
					
					StringTokenizer token = new StringTokenizer(richiestaClient); //parse della stringa per capirne il tipo
					
					String requestType = token.nextToken(" ");
					
					System.out.println("Tipo di richiesta: " + requestType + "\n");
					
					QueryRequest req = new QueryRequest(richiestaClient);
					UpdateRequest up = new UpdateRequest(richiestaClient);
					//System.out.println(req.getSPARQL());//stampa insert {<ciao> <ciao> <ciao>} where ....
					
					if(requestType.equals("select")){
						scheduler.schedule(req, new ResponseTCPHandler(outSock));
						System.out.println("Query analizzata correttamente\n\n");
						System.out.println("In attesa di una query\n\n");
						
					} else if(requestType.equals("delete") || requestType.equals("insert")){
						
						scheduler.schedule(up, new ResponseTCPHandler(outSock));
						System.out.println("Update analizzata correttamente\n\n");
						System.out.println("In attesa di una query\n\n");
						
					} else{ 
						System.out.println("Tipo di query non riconosciuta\n\n");
					}
					
					
					//per la subscribe devo fare un nuovo server che accetta le richieste sulla porta
					// 1081 (ad esempio, diversa da 1080, molto simile al precedente)
					/* quando accetto la connessione guardo se è subscribe -> vado su serverSecondario
					 * faccio lo schedule di una SubscribeRequest(...) 
					 * responseTCPHandler rimarrà la stessa
					 * 
					 * delete {<ciao> <ciao> ?a} insert { <ciao> <ciao> <ciao> } where { <ciao> <ciao> ?a }
					 * select ?a where { <ciao> <ciao> ?a } per la sottoscrizione
					 */

					
				} // while
			}catch (EOFException eof) {
				System.out.println("Raggiunta la fine delle ricezioni, chiudo...");
				clientSocket.close();
				System.out.println("PutFileServer: termino...");
				//System.exit(0);
			} catch (SocketTimeoutException ste) {
				System.out.println("Timeout scattato: ");
				ste.printStackTrace();
				clientSocket.close();
				//System.exit(1);
			} catch (Exception e) {
				System.out.println("Problemi, i seguenti : ");
				e.printStackTrace();
				System.out.println("Chiudo ed esco...");
				clientSocket.close();
				//System.exit(2);
			}

		}//try esterno
		catch (IOException ioe) {
			System.out.println("Problemi nella chiusura della socket: ");
			ioe.printStackTrace();
			System.out.println("Chiudo ed esco...");
			//System.exit(3);
		}
	}//run
}//class