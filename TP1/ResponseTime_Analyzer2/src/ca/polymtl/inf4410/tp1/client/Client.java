package ca.polymtl.inf4410.tp1.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.File;
import java.security.MessageDigest;
import java.security.DigestInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.StandardCopyOption;

import ca.polymtl.inf4410.tp1.shared.ServerInterface;

public class Client {
	public static void main(String[] args) {
		try{
			String func = "";
			if (args.length > 0) {
				func = args[0];
			}

			Client client = new Client();
			switch(func){
				case "list":
						client.list();
					break;
				case "create":
					if(args[1]!=null)
						client.server.create(args[1]);
					break;
				case "get":
					if(args[1]!=null)
						client.get(args[1]);
					break;
				case "push":
					if(args[1]!=null)
							client.push(args[1]);
					break;
				case "lock":
						if(args[1]!=null)
							client.lock(args[1]);
					break;
				case "syncLocalDir":
					client.syncLocalDir();
					break;
				case "lire":
					if(args[1]!=null)
						client.server.lire(args[1]);
					break;
				default:
					break;
			}
		} catch(RemoteException re) {
			re.printStackTrace();
		}

	}

	private ServerInterface server = null;

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

	public Client() {
		super();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		server = loadServerStub("132.207.12.251");
		//server = loadServerStub("127.0.0.1");
	}

	

	private void get(String nom){
		File f;
		try{
			f = new File("./files/"+nom);
			byte[] checksum = null;


			if(f.exists()){
				checksum = getMD5(nom);
			}

			
			byte[] res = server.get(nom,checksum);
			if(res!=null){
				FileOutputStream fos = new FileOutputStream("./files/"+nom);
				fos.write(res);
				fos.close();
				System.out.println(nom+" synchronisé.");
			} else {
				System.out.println("Le fichier est déjà à jour ou n'existe pas.");
			}
			
	
		}catch(Exception e) {
			e.printStackTrace();
		} 
	}

	private byte[] getMD5(String nom){
		byte[] res = null;
		try{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(Files.readAllBytes(Paths.get("./files/"+nom)));
			res = md.digest();
		} catch(Exception e){
			e.printStackTrace();
		}
		
		return res;
	}

	private void list(){
		try{
			HashMap<String, String> map = this.server.list();
			for (Map.Entry<String, String> entry : map.entrySet()) {
				if(entry.getValue().equals("-1")){
					System.out.println(entry.getKey() + " non verrouillé");
				} else{
					System.out.println(entry.getKey() + " verrouillé par client "+entry.getValue());
				}
			}
			
		} catch(RemoteException re) {
			re.printStackTrace();
		}
	}

	private String generateclientid()
	{
		File f = new File("id.txt");
		String id = "";
		try{
			if(f.exists())
			{
				BufferedReader br = new BufferedReader(new FileReader("id.txt"));
				id = br.readLine();
				br.close();
			}
			else
			{
				id = this.server.generateclientid();
				PrintWriter writer = new PrintWriter("id.txt", "UTF-8");
    			writer.println(id);
				writer.close();
			}	

		} catch (Exception e) {
			e.printStackTrace();
		}
		return id;
	}

	private void lock(String nom){

		try{
			get(nom);
			String idLock = server.lock(nom, generateclientid());
			if(idLock==null){
				System.out.println("Le fichier n'existe pas.");
			} else if(idLock.equals("-1")){
				System.out.println(nom +" verrouillé");
			} else if(idLock.equals("-2")){
				System.out.println("Vous avez déjà verrouillé un fichier.");
			} else if(idLock.equals(generateclientid())) {
				System.out.println(nom + " déjà verrouillé par vous");
			} else {
				System.out.println(nom + " est déjà verrouillé par client " + idLock); 
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void push(String nom){
		try{
			byte[] contenu = Files.readAllBytes(Paths.get("./files/"+nom));
			boolean res = server.push(nom, contenu, generateclientid());
			if(res) {
				System.out.println(nom+" a été envoyé au serveur");
			} else {
				System.out.println("Opération refusée : vous devez verrouiller d'abord verrouiller le fichier.");
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void syncLocalDir() {
		try{
			File[] listFile = server.syncLocalDir();
			for(File file : listFile){
				get(file.getName());
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
