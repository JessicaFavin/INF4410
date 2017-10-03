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
import java.io.FileOutputStream;


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
	public byte[] get(String nom, byte[] checksum) throws RemoteException
	{
		File f = new File("./files/"+nom);
		byte[] res = null;
		if(f.exists()){
			byte[] md5 = getMD5(nom);
			if(checksum==null || !MessageDigest.isEqual(md5, checksum)){
				try{
					res= Files.readAllBytes(Paths.get("./files/"+nom));
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}

		return res;
		
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

	@Override
	public String generateclientid() throws RemoteException
	{
		return Integer.toString(count++);	
	}

	@Override
	public String lock(String nom, String clientid) throws RemoteException{
		String idLock = "";
		if(!map.containsValue(clientid)){
			idLock = map.get(nom);
			if(idLock!=null && idLock.equals("-1")){
				map.put(nom, clientid);
			}
		} else {
			idLock = "-2";
		}
		return idLock;
	}

	@Override
	public boolean push(String nom, byte[] contenu, String clientid) throws RemoteException {

		boolean b = false;
		try{
			String idLock = map.get(nom);
			if(idLock.equals(clientid)){
				FileOutputStream fos = new FileOutputStream("./files/"+nom);
				fos.write(contenu);
				fos.close();
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
