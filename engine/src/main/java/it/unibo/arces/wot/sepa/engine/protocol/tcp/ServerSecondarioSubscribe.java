package it.unibo.arces.wot.sepa.engine.protocol.tcp;

//Alessio Pazzani 0000766862
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;



/*
*
* Varie: controllo campi inviati dal client
* 
* 
* sistemo il server quando ne ho tempo
*
*/


public class ServerSecondarioSubscribe extends Thread {
	private Socket clientSocket = null;
	private Scheduler scheduler = null;
	private int numSubscribe = 0;

	public ServerSecondarioSubscribe(Socket clientSocket, Scheduler scheduler) {
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
					String spuid = token.nextToken(" ");
					
					System.out.println("Tipo di richiesta: " + requestType + "\n");
					
					//requestType.equalsIgnoreCase("select")
					SubscribeRequest req = new SubscribeRequest(richiestaClient);
					UnsubscribeRequest unreq = new UnsubscribeRequest(spuid);
					
					if(requestType.equals("select")){
						scheduler.schedule(req, new ResponseTCPHandler(outSock));
						System.out.println("Inviato aggiornamento al client con successo\n\n");
						numSubscribe++;
						
					} if(requestType.equals("unsubscribe")){
						scheduler.schedule(unreq, new ResponseTCPHandlerUnsubscribe(outSock, numSubscribe));
						System.out.println("Unsubscribe relativo a\n" + spuid + "\neseguita con successo");
						numSubscribe--;
						if(numSubscribe==0){
							System.out.println("Terminate tutte le subscribe");
							System.out.println("Connessione chiusa in quanto terminate le sottoscrizioni");
							
						} else if(numSubscribe<0){
							System.out.println("spuid non esistente\n\n");
						}
					}else{ 
						System.out.println("Tipo di subscribe/unsubuscribe non riconosciuta\n");
					}
					
					
					//per la subscribe devo fare un nuovo server che accetta le richieste sulla porta
					// 1081 (ad esempio, diversa da 1080, molto simile al precedente)
					/* quando accetto la connessione guardo se è subscribe -> vado su serverSecondario
					 * faccio lo schedule di una SubscribeRequest(...) 
					 * responseTCPHandler rimarrà la stessa
					 * 
					 * delete {<ciao> <ciao> ?a} insert { <ciao> <ciao> <ciao> } where { <ciao> <ciao> ?a }
					 * select ?a where { <ciao> <ciao> ?a } per la sottoscrizione
					 * select * where { ?a ?b ?c } per la sottoscrizione a tutto
					 * insert data { <ciao> <ciao> <ciao> } per l'update (N.B cambio ogni volta almeno un <ciao>)
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
			}catch(SocketException ex){
				System.out.println("Sono state chiuse tutte le socket, addio\n");
				clientSocket.close();
			}catch (Exception e) {
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