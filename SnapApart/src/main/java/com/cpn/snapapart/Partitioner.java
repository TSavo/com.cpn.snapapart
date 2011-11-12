package com.cpn.snapapart;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Partitioner extends Remote {
  int partitionVolume(String aDevice) throws RemoteException;
}