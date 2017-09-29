package ca.polymtl.inf4410.tp1.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.File;
import java.security.MessageDigest;
import java.security.DigestInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.HashMap;

public interface ServerInterface extends Remote {
	public void create(String nom) throws RemoteException;
	public HashMap<String, String> list() throws RemoteException;
	public File[] syncLocalDir() throws RemoteException;
	public byte[] get(String nom, byte[] checksum) throws RemoteException;
	public String generateclientid() throws RemoteException;
	public String lock(String nom, String clientid) throws RemoteException;
	public boolean push(String nom, byte[] contenu, String clientid) throws RemoteException;
	public void lire(String name) throws RemoteException;
}
