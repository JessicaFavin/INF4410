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
	}

	

	private void get(String nom){
		File f;
		try{
			f = new File("./files/"+nom);
			String checksum = "-1";
			if(f.exists()){
				checksum = getMD5(nom);
				System.out.println("Client : "+checksum);
			}

			File f2 = server.get(nom, checksum); 
			System.out.println("File : "+(f2!=null));
			if(f2!=null){
				
				BufferedReader br = new BufferedReader(new FileReader(f2));
				System.out.println("f2 "+br.readLine());
				br.close();
				Client.copyFileUsingStream(f2, f);
				System.out.println(nom+" synchronisé.");
			} else {
				System.out.println("Le fichier est déjà à jour.");
			}
		} catch(Exception e) {
			e.printStackTrace();
		} 
	}

	private String getMD5(String nom){
		String res = "";
		try{
			InputStream fis =  new FileInputStream("./files/"+nom);

			byte[] buffer = new byte[1024];
			MessageDigest complete = MessageDigest.getInstance("MD5");
			int numRead;

			do {
				numRead = fis.read(buffer);
				if (numRead > 0) {
				   complete.update(buffer, 0, numRead);
				}
			} while (numRead != -1);

			fis.close();
			byte[] digest = complete.digest();
			res = digest.toString();
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
			if(idLock.equals("-1")){
				System.out.println(nom +" verrouillé");
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
			File contenu = new File("./files/"+nom);
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

	private static void copyFileUsingStream(File source, File dest) throws IOException {
	    InputStream is = null;
	    OutputStream os = null;
	    try {
	        is = new FileInputStream(source);
	        os = new FileOutputStream(dest);
	        byte[] buffer = new byte[1024];
	        int length;
	        while ((length = is.read(buffer)) > 0) {
	            os.write(buffer, 0, length);
	        }
	    } finally {
	        is.close();
	        os.close();
	    }
	}


}
