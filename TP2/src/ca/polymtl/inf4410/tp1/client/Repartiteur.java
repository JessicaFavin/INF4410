package ca.polymtl.inf4410.tp1.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import ca.polymtl.inf4410.tp1.shared.ServerInterface;


public class Repartiteur {
	private Stack<String> listeOperations;
	private boolean estSecurise;
	private int nServeur;
	private ServerInterface[] listeServeur;
	private int[] capaciteServeur;
	private ArrayList<String>[] dataEnvoye;
	private boolean[] serveurCrashe;
	private int[] resultMalicious;
	private boolean[] isAdd;
	private boolean[] isSaved;
	private Stack<Integer> idDataLibre;


	public Repartiteur(String nom, boolean secure, String serveurFichier){
		super();
		try{

			estSecurise = secure;
			listeOperations = new Stack<String>();
			File op = new File(nom);
			if(op.exists())
			{
				BufferedReader br = new BufferedReader(new FileReader(nom));
				String inst = br.readLine();
				while(inst!=null){
					listeOperations.push(inst);
					inst = br.readLine();
				}
			}
			File f = new File(serveurFichier);
			if(f.exists())
			{
				BufferedReader br = new BufferedReader(new FileReader(serveurFichier));
				nServeur = Integer.parseInt(br.readLine());
				listeServeur = new ServerInterface[nServeur];
				capaciteServeur = new int[nServeur];
				dataEnvoye = new ArrayList[nServeur];
				resultMalicious = new int[nServeur];
				isAdd = new boolean[nServeur];
				isSaved = new boolean[nServeur];
				for(int i=0; i<nServeur; i++){
					dataEnvoye[i] = new ArrayList<String>();
					resultMalicious[i] = -1;
					isAdd[i] = true;
					isSaved[i] = true;
				}

				serveurCrashe = new boolean[nServeur];
				for(int i = 0; i<nServeur; i ++){
					if(estSecurise){
						String[] part = br.readLine().split(" ");
						capaciteServeur[i] = Integer.parseInt(part[0]);
						listeServeur[i] = loadServerStub(part[1]);
					}
					else{
						String[] part = br.readLine().split(" ");
						capaciteServeur[i] = Integer.parseInt(part[0]);
						listeServeur[i] = loadServerStub(part[1]);
					}
					if (System.getSecurityManager() == null) {
						System.setSecurityManager(new SecurityManager());
					}



				}
				br.close();
				if(estSecurise)
					lancerSecure();
				else
					lancerPasSecure();
			}
		} catch(Exception e){
			e.printStackTrace();
		}

	}

	public void lancerSecure(){
		int res = 0;
		ExecutorService executor = Executors.newFixedThreadPool(nServeur);
		Future<Integer>[] future = new Future[nServeur];
		long start = System.nanoTime();
		try{
			while(!listeOperations.empty()){
				for(int i=0; i<listeServeur.length; i++){
					int dataSize = Math.min(capaciteServeur[i], listeOperations.size());
					if(!serveurCrashe[i]){
						try {
							//si le serveur a fini ses calculs et le résultat a été sauvegardé
							if(isAdd[i]){
								//demande de calcul
								if(listeServeur[i].demandeCalcul(dataSize)){
									//System.out.println("Calcul serveur "+i);
									// création des sous instructions
									isAdd[i] = false;
									String[] data = new String[dataSize];
									dataEnvoye[i].clear();
									for(int j=0; j<dataSize; j++){
										data[j] = listeOperations.pop();
										//sauvegarde de la data en cas de panne
										dataEnvoye[i].add(data[j]);
									}
									//lancement du thread de calcul
									Calcul thread = new Calcul(listeServeur[i], data);
									future[i] = executor.submit(thread);
									capaciteServeur[i]++;
								} else {
									//else diminuer la datasize
									capaciteServeur[i]--;
								}

							}
							//recupération du résultat
							if(future[i].isDone() && !isAdd[i]){
								res = (res+(Integer)future[i].get())%4000;
								isAdd[i] = true;
							}

						} catch (Exception e) {
							//e.printStackTrace();
							//on rempile les datas qui tournaient lors du crash
							if(!dataEnvoye[i].isEmpty()){
								for(int j=0; j<dataEnvoye[i].size(); j++){
									listeOperations.push(dataEnvoye[i].get(j));
								}
							}
							serveurCrashe[i] = true;
						}
					}

				}
			}

			for(int i=0; i<listeServeur.length; i++){
				if(!serveurCrashe[i])
					res = (res+(Integer)future[i].get())%4000;
			}
			long end = System.nanoTime();
			System.out.println("Temps écoulé appel normal: " + (end - start)
				+ " ns");
			System.out.println("Resultat: "+res);
			System.exit(1);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void lancerPasSecure(){
		try{
			int id = 0;
			int res = 0;
			Stack<Integer> idDataToSend = new Stack<Integer>();
			Stack<Integer> idServerAsking = new Stack<Integer>();
			ExecutorService executor = Executors.newFixedThreadPool(nServeur);
			Future<Integer>[] future = new Future[nServeur];
			long start = System.nanoTime();
			int[] dataID = new int[nServeur];
			idDataLibre = new Stack<Integer>();
			for(int i=0;i<nServeur; i++){
				idDataLibre.push(i);
			}
			while(!listeOperations.empty()){
				
				for(int i=0; i<listeServeur.length; i++){
					try{

						int dataSize = Math.min(capaciteServeur[i], listeOperations.size());
						if(!serveurCrashe[i]){
							//si le serveur a fini ses calculs et le résultat a été sauvegardé
							if(isSaved[i]){
								//demande de calcul
								if(listeServeur[i].demandeCalcul(dataSize)){
									//s'il n'y a pas de double de data à envoyer
									if(idDataToSend.empty()){
										if(!idDataLibre.isEmpty()){
											//on renvoi le prochain id libre
											id =  (int) idDataLibre.pop();
										} else {
											System.out.println("Erreur! plus de place dans la table de data");
											//passe son tour en espérant une libération de place prochaine
											//continue;
										}
										//System.out.println("New data");
										// création des sous instructions
										isSaved[i] = false;
										isAdd[id] = false;
										String[] data = new String[dataSize];
										for(int j=0; j<dataSize; j++){
											data[j] = listeOperations.pop();
										}
										dataEnvoye[id].clear();

										//sauvegarde des datas envoyées à l'id libre trouvé
										for(int j=0; j<dataSize; j++){
											dataEnvoye[id].add(data[j]);
										}
										//sauvegarde l'id pour qu'il soit envoyé à un autre serveur
										idDataToSend.push(id);
										idServerAsking.push(i);
										dataID[i] = id;
										//lancement du thread de calcul
										Calcul thread = new Calcul(listeServeur[i], data);
										future[i] = executor.submit(thread);
									} else {
										int serverAsking = idServerAsking.pop();
										//on verifie qu'on ne renvoi pas au même serveur les donnees
										if(i!=serverAsking){
											id = idDataToSend.pop();
											dataID[i] = id;
											//send the data found in dataEnvoye[id]
											isSaved[i] = false;
											String[] data = new String[dataEnvoye[id].size()];
											data = dataEnvoye[id].toArray(data);
											//lancement du thread de calcul
											Calcul thread = new Calcul(listeServeur[i], data);
											future[i] = executor.submit(thread);
										} else {
											idServerAsking.push(serverAsking);
										}
										
									}
									capaciteServeur[i]++;
								} else {
									//else diminuer la datasize
									capaciteServeur[i]--;
								}
							}
							//recupération du résultat
							if(future[i].isDone() && !isSaved[i]){
								
								Integer resTmp = (Integer)future[i].get()%4000;
								//on récupère l'idData associée au serveur
								id = dataID[i];
								//on compare le résultat à celui ou ceux de la table resultMalicious
								if(resultMalicious[id]!=-1){
									//System.out.println(id+" "+resultMalicious[id]+" "+resTmp+" server "+i);
									if(resultMalicious[id]==resTmp){
										isAdd[id] = true;
										res = (res+resTmp)%4000;
										//System.out.println(resTmp);
										// si le resultat est bon on libère les tables
										resultMalicious[id] = -1;
										//et on ajoute l'id a la stack
										idDataLibre.push(id);
									} else {
										//on sauvegarde le resultat pour le prochain serveur
										resultMalicious[id] = resTmp;										
										//on relance la data
										idDataToSend.push(id);
										idServerAsking.push(i);
									}
								} else {
									//on sauvegarde le resultat pour le prochain serveur
									resultMalicious[id] = resTmp;
									
								}
								isSaved[i] = true;

							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						//on relance les datas du serveur down
						idDataToSend.push(id);
						idServerAsking.push(-1);
						//isSaved[i] = true;
						//on sauvegarde que le serveur est down
						serveurCrashe[i] = true;
					}
				}
			}

			//System.out.println("Fin premiere boucle");
			//sauvegarde des résultats finaux
			boolean allAdded = false;
			while(!allAdded){
				for(int i=0; i<listeServeur.length; i++){
					try {
						if(!serveurCrashe[i]){
							//si le serveur a fini ses calculs et le résultat a été sauvegardé
							if(isSaved[i]){
								//on ne fait que renvoyer les mauvais résultats
								//demande de calcul
								
								if(!idDataToSend.empty()){
									int serverAsking = idServerAsking.pop();
									if(i!=serverAsking){
										if(listeServeur[i].demandeCalcul(dataEnvoye[id].size())){
											id = idDataToSend.pop();
											dataID[i] = id;
											isAdd[id] = false;
											isSaved[i] = false;
											//send the data found in dataEnvoye[id]
											String[] data = new String[dataEnvoye[id].size()];
											data = dataEnvoye[id].toArray(data);
											//lancement du thread de calcul
											Calcul thread = new Calcul(listeServeur[i], data);
											future[i] = executor.submit(thread);
											capaciteServeur[i]++;
										} else {
											//else diminuer la datasize
											capaciteServeur[i]--;
											idServerAsking.push(serverAsking);
										}
									} else {
										idServerAsking.push(serverAsking);
									}
								}
							}
							

							
							
							
							//recupération du résultat
							if(!isSaved[i] && future[i].isDone()){
								//System.out.println(" Serveur : " + i);
								Integer resTmp = (Integer)future[i].get()%4000;
								//on récupère l'idData associée au serveur
								id = dataID[i];
								//on compare le résultat à celui ou ceux de la table resultMalicious
								if(resultMalicious[id]!=-1){
									//System.out.println(isAdd[0]+" "+isAdd[1]+" "+isAdd[2]+" "+isAdd[3]);
									//System.out.println(id+" "+resultMalicious[id]+" "+resTmp+" server "+i);
									if(resultMalicious[id]==resTmp){
										//System.out.println(resTmp);
										res = (res+resTmp)%4000;
										isAdd[id] = true;
										// si le resultat est bon on libère les tables
										resultMalicious[id] = -1;
										//et on ajoute l'id a la stack
										idDataLibre.push(id);
										//System.out.println("Id "+id+" libéré");
									} else {
										//on sauvegarde le resultat pour le prochain serveur
										resultMalicious[id] = resTmp;
										//on relance la data
										idDataToSend.push(id);
										idServerAsking.push(i);
										//System.out.println("Renvoi id "+id);
									}
								} else {
									//on sauvegarde le resultat pour le prochain serveur
									resultMalicious[id] = resTmp;
								}
								isSaved[i] = true;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						//on relance les datas du serveur down
						idDataToSend.push(id);
						idServerAsking.push(-1);
						//isSaved[i] = true;
						//on sauvegarde que le serveur est down
						serveurCrashe[i] = true;
					}
					
				} 
				allAdded = true;
				for(int i=0; i<nServeur; i++){
					if(!serveurCrashe[i] && !isAdd[dataID[i]])
						allAdded = false;
				}
			}

			long end = System.nanoTime();
			System.out.println("Temps écoulé appel normal: " + (end - start)+ " ns");
			System.out.println("Resultat: "+res);
			System.exit(1);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private boolean allAdded() {
		for(int i=0; i<nServeur; i++){
			if(!isAdd[i])
				return false;
		}
		//System.out.println("");
		return true;
	}


	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname, 5005);
			stub = (ServerInterface) registry.lookup("server");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
			e.printStackTrace();
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
			e.printStackTrace();
		}

		return stub;
	}


	public static void main(String[] args){

		if(args.length == 3){
			Repartiteur repartiteur = new Repartiteur(args[0], Boolean.parseBoolean(args[1]), args[2]);
		} else {
			System.out.println("./Repartiteur nomDuFichier modeSecure (true ou false) serveurFichier");
		}



	}

	public class Calcul implements Callable<Integer> {

		private ServerInterface server;
		private String[] data;

		public Calcul(ServerInterface s, String[] d){
			server = s;
			data = d;
		}

		public Integer call() throws Exception{
			Integer res = -1;
			try {
				res = (Integer)server.calcul(data);
			} catch(Exception e) {
				//e.printStackTrace();
				throw new Exception();
			}
			return res;
		}
	}

}
