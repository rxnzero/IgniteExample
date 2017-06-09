package com.dhlee.ignite.topic;

import java.util.Collections;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;

public class ClusterRemote {
	
	private static String HOST = "127.0.0.1:47500..47509"; //
    
	private static String MULTICAST_GROUP = "228.10.10.157";
	
	public static void main(String[] args) throws InterruptedException {
		TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
		 ipFinder.setMulticastGroup(MULTICAST_GROUP);

//		TcpDiscoveryVmIpFinder  ipFinder = new TcpDiscoveryVmIpFinder();
//		 ipFinder.setAddresses(Collections.singletonList(HOST));

		 TcpDiscoverySpi discoSpi = new TcpDiscoverySpi();
		 discoSpi.setIpFinder(ipFinder);

		 IgniteConfiguration cfg = new IgniteConfiguration();
		 cfg.setDiscoverySpi(discoSpi);
//		 cfg.setGridName("test");
		     
		 Ignite ignite = Ignition.start(cfg);
		
		
		System.out.println();
        System.out.println(">>> ClusterRemote started.");
	}
}
