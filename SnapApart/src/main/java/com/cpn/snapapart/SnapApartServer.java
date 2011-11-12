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

public class SnapApartServer implements Remote, Partitioner {

	public SnapApartServer() {
		super();
	}

	@Override
	public int partitionVolume(String aDevice) throws RemoteException {
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
				System.err.println(aString);
				return this;
			}
		};

		System.out.println("Partitioning " + aDevice);
		SystemExecutor exec = new SystemExecutor().setWorkingDirectory("/root").setOutputLogDevice(out).setErrorLogDevice(err);
		try {
			exec.runCommand("iscsiadm -m discovery -t sendtargets -p splaims01");

			exec.runCommand("iscsiadm -m node -T iqn.2010-10.org.openstack:" + aDevice.replaceAll("vol-", "volume-") + " -p 172.16.3.1:3260 --login");

			try {
				exec.runCommand("/root/partDisk.sh");
				exec.runCommand("mkfs.ext3 /dev/sdc1");
				exec.runCommand("mkfs.ext3 /dev/sdc2");
				exec.runCommand("mkfs.ext3 /dev/sdc3");
				exec.runCommand("mount /dev/sdc1 /mnt/rom");
				exec.runCommand("mount /dev/sdc2 /mnt/flash");
				exec.runCommand("mount /dev/sdc3 /mnt/kav_base");
				try {
					exec.runCommand("echo cpn > /mnt/rom/company");
					exec.runCommand("rmdir /mnt/rom/lost+found");
					exec.runCommand("rmdir /mnt/flash/lost+found");
					exec.runCommand("rmdir /mnt/kav_base/lost+found");
					exec.runCommand("mkdir -p /mnt/flash/persist/etc");
					exec.runCommand("rsync -a /var/www/download/kav/i386_itw/ /mnt/kav_base/");

				} finally {
					exec.runCommand("umount /mnt/rom");
					exec.runCommand("umount /mnt/flash");
					exec.runCommand("umount /mnt/kav_base");
				}
			} finally {
				exec.runCommand("iscsiadm -m node -T iqn.2010-10.org.openstack:" + aDevice.replaceAll("vol-", "volume-") + " -p 172.16.3.1:3260 --logout");
				exec.runCommand("iscsiadm -m session");
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
			String name = "SnapApartServer";
			SnapApartServer engine = new SnapApartServer();
			Partitioner stub = (Partitioner) UnicastRemoteObject.exportObject(engine, 0);
			Registry registry = LocateRegistry.getRegistry();
			registry.rebind(name, stub);
			System.out.println("SnapApartServer bound");
		} catch (Exception e) {
			System.err.println("SnapApartServer exception:");
			e.printStackTrace();
		}
	}
}
