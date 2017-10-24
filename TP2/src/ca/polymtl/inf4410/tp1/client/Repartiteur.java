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

import ca.polymtl.inf4410.tp1.shared.ServerInterface;


public class Repartiteur {
	private Stack<String> listeOperations;
	private boolean estSecurise;
	private int nServeur;
	private ServerInterface[] listeServeur;
	private int[] capaciteServeur;


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
				for(int i = 0; i<nServeur; i ++){
					if(estSecurise){
						String[] part = br.readLine().split(" ");
						capaciteServeur[i] = Integer.parseInt(part[0]);
						listeServeur[i] = loadServerStub(part[1]);
					}	
					else{
						String[] part = br.readLine().split(" ");
						capaciteServeur[i] = Integer.parseInt(part[0]);
						//Double t = Double.parseDouble(part[2]);
						listeServeur[i] = loadServerStub(part[1]);
					}
					if (System.getSecurityManager() == null) {
						System.setSecurityManager(new SecurityManager());
					}

					
						
				}
				br.close();
				lancer();
			}
		} catch(Exception e){
			e.printStackTrace();
		}

	}

	public void lancer(){
		try{
			int res = 0;
			ExecutorService executor = Executors.newFixedThreadPool(nServeur);
			Future<Integer>[] future = new Future[nServeur];
			while(!listeOperations.empty()){
				
				for(int i=0; i<listeServeur.length; i++){
					
					if(future[i] != null && future[i].isDone())
					{
						int q = capaciteServeur[i];
						int dataSize = Math.min(q, listeOperations.size());

						//demande de calcul
						if(listeServeur[i].demandeCalcul(dataSize)){

							// création des sous instructions
							String[] data = new String[dataSize];
							for(int j=0; j<dataSize; j++){
								data[j] = listeOperations.pop();
							}

							//lancement du thread de calcul
							Calcul thread = new Calcul(listeServeur[i], data);
							future[i] = executor.submit(thread);
						}
						//else diminuer la datasize

						

						//recupération du résultat
						if(future[i] != null){
							res = (res+(Integer)future[i].get())%4000;
						}
					}

					
				}
			}
			System.out.println("Resultat: "+res);
		} catch(Exception e) {
			e.printStackTrace();
		}
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

