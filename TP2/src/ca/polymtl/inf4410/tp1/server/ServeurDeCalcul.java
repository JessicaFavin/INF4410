package ca.polymtl.inf4410.tp1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

import ca.polymtl.inf4410.tp1.shared.ServerInterface;

public class ServeurDeCalcul implements ServerInterface{

	private int instMax;
	private int opPossible;
	private Double repErronee;

	public ServeurDeCalcul(int q) {
		super();
		instMax = q;
		opPossible = instMax;
		repErronee = 0.0;
	}

	public ServeurDeCalcul(int q, Double repErronee) {
		super();
		instMax = q;
		opPossible = instMax;
		this.repErronee = repErronee;
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject
					.exportObject(this, 5037);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err
					.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}

	@Override
	public int calcul(String[] instructions) throws RemoteException
	{
		int instSoumise = instructions.length;
		if(instructions.length>opPossible){
			int tauxRefus = ((instSoumise-instMax)/(5*instMax))*100;
			Random randomGenerator = new Random();
			//génère un chiffre au hasard entre 0 et 100 inclus.
      		int randomInt = randomGenerator.nextInt(101);
      		if(randomInt < tauxRefus){
				return -1;
      		}
		}

		opPossible -= instSoumise;
		int res = 0;
		for(String ins : instructions) {
			String[] part = ins.split(" ");
			switch(part[0]){
				case "prime" :
					res += Operations.prime(Integer.parseInt(part[1]))%4000;
				break;
				case "pell" :
					res += Operations.pell(Integer.parseInt(part[1]))%4000;
				break;
			}
			opPossible++;
		}

		return res;
	}


	public static void main(String[] args) {
		ServeurDeCalcul server = new ServeurDeCalcul(Integer.parseInt(args[0]), Double.parseDouble(args[1]));
		server.run();
	}

}