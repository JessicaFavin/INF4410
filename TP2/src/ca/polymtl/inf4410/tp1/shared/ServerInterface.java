package ca.polymtl.inf4410.tp1.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;

public interface ServerInterface extends Remote {
	public int calcul(String[] instructions) throws RemoteException;
	public boolean demandeCalcul(int instSoumise) throws RemoteException;
}