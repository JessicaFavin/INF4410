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

import ca.polymtl.inf4410.tp1.shared.ServerInterface;


public class Repartiteur {
	private Stack<String> listeOperations;
	private boolean estSecurise;
	private int nServeur;
	private ServerInterface[] listeServeur;
	private int[] capaciteServeur;
	private ArrayList<String>[] dataEnvoye;
	private boolean[] serveurCrashe;
	private int[][] resultMalicious;
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
				for(int i=0; i<nServeur; i++){
					dataEnvoye[i] = new ArrayList<String>();
					resultMalicious[i] = new int[2];
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
		try{
			int res = 0;
			ExecutorService executor = Executors.newFixedThreadPool(nServeur);
			Future<Integer>[] future = new Future[nServeur];
			long start = System.nanoTime();
			while(!listeOperations.empty()){
				for(int i=0; i<listeServeur.length; i++){
					int dataSize = Math.min(capaciteServeur[i], listeOperations.size());
					if(!serveurCrashe[i] && (future[i] == null || future[i].isDone()))
					{
						try {
							//demande de calcul
							if(listeServeur[i].demandeCalcul(dataSize)){
								capaciteServeur[i]++;
								// création des sous instructions
								String[] data = new String[dataSize];
								for(int j=0; j<dataSize; j++){
									data[j] = listeOperations.pop();
								}
								//lancement du thread de calcul
								Calcul thread = new Calcul(listeServeur[i], data);
								future[i] = executor.submit(thread);
								for(int j=0; j<dataEnvoye[i].size(); j++){
									dataEnvoye[i].add(data[j]);
								}
							} else {
								//else diminuer la datasize
								capaciteServeur[i]--;
							}

							//recupération du résultat
							if(future[i] != null){
								res = (res+(Integer)future[i].get())%4000;
								future[i] = null;
							}
						} catch (Exception e) {
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
			int idDataToSend = -1;
			ExecutorService executor = Executors.newFixedThreadPool(nServeur);
			Future<Integer>[] future = new Future[nServeur];
			long start = System.nanoTime();
			int[] dataID = new int[nServeur];
			idDataLibre = new Stack<Integer>();
			while(!listeOperations.empty()){


				for(int i=0; i<listeServeur.length; i++){
					int dataSize = Math.min(capaciteServeur[i], listeOperations.size());

					if(!serveurCrashe[i] && (future[i] == null || future[i].isDone())){
						try{
							//demande de calcul
							if(listeServeur[i].demandeCalcul(dataSize)){
								capaciteServeur[i]++;

								//s'il n'y a pas de double de data à envoyer
								if(idDataToSend==-1){
									// création des sous instructions
									String[] data = new String[dataSize];
									for(int j=0; j<dataSize; j++){
										data[j] = listeOperations.pop();
									}
									//lancement du thread de calcul
									Calcul thread = new Calcul(listeServeur[i], data);
									future[i] = executor.submit(thread);
									//find id for data
									id = findFreeDataId();
									dataEnvoye[id] = new ArrayList<String>();
									//sauvegarde des datas envoyées à l'id libre trouvé
									for(int j=0; j<capaciteServeur[i]; j++){
										dataEnvoye[id].add(data[j]);
									}
									//sauvegarde l'id pour qu'il soit envoyé à un autre serveur
									idDataToSend = id;
									dataID[i] = id;
								} else {
									dataID[i] = idDataToSend;
									//send the data found in dataEnvoye[idDataToSend]
									String[] data = new String[dataEnvoye[idDataToSend].size()];
									data = dataEnvoye[idDataToSend].toArray(data);
									//lancement du thread de calcul
									Calcul thread = new Calcul(listeServeur[i], data);
									future[i] = executor.submit(thread);
									//ne pas renvoyer la data
									idDataToSend = -1;
								}
							} else {
								//else diminuer la datasize
								capaciteServeur[i]--;
							}
							//recupération du résultat
							if(future[i] != null){
								resTmp = (Integer)future[i].get()

								future[i] = null;
								//on récupère l'idData associée au serveur
								id = dataID[i];
								//on compare le résultat à celui ou ceux de la table resultMalicious
								if(resultMalicious[id][0]!=-1){
									if(resultMalicious[id][0]==res){
										res = (res+resTmp)%4000;
										// si le resultat est bon on libère les tables
										//et on ajoute l'id a la stack
									} else {
										if(resultMalicious[id][1]!=-1){
											if(resultMalicious[id][1]==res){
												res = (res+resTmp)%4000;
												// si le resultat est bon on libère les tables
												//et on ajoute l'id a la stack
											} else {
												//aucune valeur n'est égale, on en remplace une au hasard
												Random random = new Random();
												boolean out = random.nextBoolean();
												if(out)
													resultMalicious[id][1] = res;
												else
													resultMalicious[id][0] = res;
											}
										} else {
											//on sauvegarde le resultat pour le prochain serveur
											resultMalicious[id][1] = res;
										}
									}
								} else {
									//on sauvegarde le resultat pour le prochain serveur
									resultMalicious[id][0] = res;
								}



							}
						} catch (Exception e) {
							//on relance les datas du serveur down



							//on sauvegarde que le serveur est down
							serveurCrashe[i] = true;
						}
					}


				}
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

	private int findFreeDataId() {
		if(!idDataLibre.isEmpty()){
			//on renvoi le prochain id libre
			return (int) idDataLibre.pop();
		}
		return dataEnvoye.size();
	}

	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (ServerInterface) registry.lookup("server");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
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

		public Integer call(){
			Integer res = -1;
			try {
				res = (Integer)server.calcul(data);
			} catch(Exception e) {
				e.printStackTrace();
			}
			return res;
		}
	}

}
