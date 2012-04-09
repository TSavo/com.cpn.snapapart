package com.cpn.snapapart;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import com.cpn.execute.LogDevice;
import com.cpn.execute.SystemExecutor;
import com.cpn.execute.SystemExecutorException;

public class SnapApartServer implements Partitioner {

	String server = SnapApartServerConfiguration.get("SnapApartServer.Server"); //$NON-NLS-1$
	String device = SnapApartServerConfiguration.get("SnapApartServer.Device"); //$NON-NLS-1$
	
	public SnapApartServer() {
		super();
	}

	public synchronized int partitionVolume(String aDevice) throws RemoteException {
		LogDevice out = new LogDevice() {

			@Override
			public LogDevice log(String aString) {
				System.out.println(aString);
				return this;
			}
		};
		LogDevice err = new LogDevice() {

			@Override
			public LogDevice log(String aString) {
				System.out.println(aString);
				return this;
			}
		};

		System.out.println("Partitioning " + aDevice); //$NON-NLS-1$
		SystemExecutor exec = new SystemExecutor().setWorkingDirectory("/root").setOutputLogDevice(out).setErrorLogDevice(err); //$NON-NLS-1$
		try {
			exec.runCommand("iscsiadm -m discovery -t sendtargets -p " + server); //$NON-NLS-1$

			exec.runCommand("iscsiadm -m node -T iqn.2010-10.org.openstack:" + aDevice.replaceAll("vol-", "volume-") + " -p " + server + ":3260 --login"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-5$

			try {
				exec.runCommand("/root/partDisk.sh"); //$NON-NLS-1$
				exec.runCommand("mkfs.ext3 " + device + "1"); //$NON-NLS-1$ //$NON-NLS-2$
				exec.runCommand("mkfs.ext3 " + device + "2"); //$NON-NLS-1$ //$NON-NLS-2$
				exec.runCommand("mkfs.ext3 " + device + "3"); //$NON-NLS-1$ //$NON-NLS-2$
				exec.runCommand("mount " + device + "1 /mnt/rom"); //$NON-NLS-1$ //$NON-NLS-2$
				exec.runCommand("mount " + device + "2 /mnt/flash"); //$NON-NLS-1$ //$NON-NLS-2$
				exec.runCommand("mount " + device + "3 /mnt/kav_base"); //$NON-NLS-1$ //$NON-NLS-2$
				try {
					exec.runCommand("echo cpn > /mnt/rom/company"); //$NON-NLS-1$
					exec.runCommand("rmdir /mnt/rom/lost+found"); //$NON-NLS-1$
					exec.runCommand("rmdir /mnt/flash/lost+found"); //$NON-NLS-1$
					exec.runCommand("rmdir /mnt/kav_base/lost+found"); //$NON-NLS-1$
					exec.runCommand("mkdir -p /mnt/flash/persist/etc"); //$NON-NLS-1$
					exec.runCommand("rsync -a /var/www/download/kav/i386_itw/ /mnt/kav_base/"); //$NON-NLS-1$

				} finally {
					exec.runCommand("umount /mnt/rom"); //$NON-NLS-1$
					exec.runCommand("umount /mnt/flash"); //$NON-NLS-1$
					exec.runCommand("umount /mnt/kav_base"); //$NON-NLS-1$
				}
			} finally {
				exec.runCommand("iscsiadm -m node -T iqn.2010-10.org.openstack:" + aDevice.replaceAll("vol-", "volume-") + " -p " + server + ":3260 --logout"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				exec.runCommand("iscsiadm -m session"); //$NON-NLS-1$
			}
			return 0;
		} catch (SystemExecutorException | IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static void main(String[] args) {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		try {
			String name = "SnapApartServer"; //$NON-NLS-1$
			SnapApartServer engine = new SnapApartServer();
			Remote stub = UnicastRemoteObject.exportObject(engine, 0);
			
			Registry registry = LocateRegistry.getRegistry();
			registry.rebind(name, stub);
			System.out.println("SnapApartServer listening..."); //$NON-NLS-1$
			while(true){
				Thread.sleep(100000);
			}
		} catch (Exception e) {
			System.err.println("SnapApartServer exception:"); //$NON-NLS-1$
			e.printStackTrace();
		}
	}
}
