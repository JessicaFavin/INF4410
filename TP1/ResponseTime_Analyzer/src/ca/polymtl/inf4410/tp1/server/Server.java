package ca.polymtl.inf4410.tp1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.File;
import java.security.MessageDigest;
import java.security.DigestInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.BufferedReader;


import ca.polymtl.inf4410.tp1.shared.ServerInterface;

public class Server implements ServerInterface {

	private int count = 0;
	private HashMap<String, String> map;

	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}

	public Server() {
		super();
		initMap();

	}

	private void initMap() {
		File f = null;
      	File[] paths = null;
      	map = new HashMap();
      	try {  
      
         	// create new file
         	f = new File("./files/");
         
         	// returns pathnames for files and directory
         	paths = f.listFiles();
         	if(paths!=null){
         		for(File fi: paths){
					map.put(fi.getName(), "-1");
				}
         	}
         	
         	
         
      	} catch(Exception e) {
         
         	// if any error occurs
         	e.printStackTrace();
      	}
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject
					.exportObject(this, 0);

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
	public void create(String nom) throws RemoteException{
		File f = new File("./files/"+nom);
		try{
			f.createNewFile();
			map.put(nom, "-1");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public HashMap<String, String> list() throws RemoteException{
      	return map;
	}

	@Override
	public File[] syncLocalDir() throws RemoteException
	{
		File f = null;
      	File[] paths = null;
      
      	try {  
      
         	// create new file
         	f = new File("./files/");
         
         	// returns pathnames for files and directory
         	paths = f.listFiles();
         
         	
         }
         catch(Exception e) {
         
         	// if any error occurs
         	e.printStackTrace();
      	}
      	return paths;
	}

	@Override
	public File get(String nom, String checksum) throws RemoteException
	{
		File f = new File("./files/"+nom);
		if(f.exists()){
			String md5 = getMD5(nom);
			System.out.println("Server : "+md5);
			if(checksum.equals("-1") || !md5.equals(checksum)){
				try{
					BufferedReader br = new BufferedReader(new FileReader(f));
					System.out.println(br.readLine());
					br.close();
				} catch(Exception e) {
					e.printStackTrace();
				}
				return f;
			}
		}

		return null;
		
	}


	private String getMD5(String nom) throws RemoteException{
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

	@Override
	public String generateclientid() throws RemoteException
	{
		return Integer.toString(count++);	
	}

	@Override
	public String lock(String nom, String clientid) throws RemoteException{
		String idLock = map.get(nom);
		if(idLock.equals("-1")){
			map.put(nom, clientid);
		}
		return idLock;
	}

	@Override
	public boolean push(String nom, File contenu, String clientid) throws RemoteException {

		boolean b = false;
		try{
			String idLock = map.get(nom);
			if(idLock.equals(clientid)){
				contenu.createNewFile();
				b = true;
				map.put(nom,"-1");
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return b;
	}

	@Override
	public void lire(String name) throws RemoteException {
		try{
			BufferedReader br = new BufferedReader(new FileReader("./files/"+name));
			System.out.println(br.readLine());
			br.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}


}
