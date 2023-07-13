package com.dhlee.ignite.topic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.ConnectorConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.EventType;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

public class ClusterRemote {

	private static int discoverPort = 10210; //40000;
	private static String HOST = "127.0.0.1:" + discoverPort; //"127.0.0.1:47500..47509"; //

	private static String MULTICAST_GROUP = "228.10.10.157";
	private static boolean useMulticast = false;
	
	public static void main(String[] args) throws InterruptedException {
		
		// create ignite working dir
		String igniteWorkingDir = "d:/ignite_work";
		Path storagePath = Paths.get(igniteWorkingDir);
		try {
			if(Files.notExists(storagePath)) {
				Files.createDirectories(storagePath);
				System.out.println("igniteWorkingDir create new : " + igniteWorkingDir);
			}
			else {
				System.out.println("igniteWorkingDir already exist : " + igniteWorkingDir);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		IgniteConfiguration cfg = new IgniteConfiguration();
		cfg.setWorkDirectory(igniteWorkingDir);
		 
		TcpCommunicationSpi communicationSpi = new TcpCommunicationSpi();
		communicationSpi.setLocalPort(discoverPort - 100);
		communicationSpi.setIdleConnectionTimeout(15000);
		communicationSpi.setConnectTimeout(1000);
		communicationSpi.setReconnectCount(3);
		communicationSpi.setMaxConnectTimeout(30000);
		communicationSpi.setConnectionsPerNode(2);
		communicationSpi.setSlowClientQueueLimit(1000);
		cfg.setCommunicationSpi(communicationSpi);
		
		TcpDiscoverySpi discoSpi = new TcpDiscoverySpi();
		discoSpi.setLocalPort(discoverPort);
		discoSpi.setClientReconnectDisabled(false);
		discoSpi.setSocketTimeout(5000);
		discoSpi.setNetworkTimeout(5000);
		discoSpi.setAckTimeout(5000);
		discoSpi.setMaxAckTimeout(15000);
		discoSpi.setReconnectCount(3);
		
		if(useMulticast) {
			TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
			ipFinder.setMulticastGroup(MULTICAST_GROUP);
			discoSpi.setIpFinder(ipFinder);
		}
		else {
			TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
			
//			ipFinder.setAddresses(Collections.singletonList(HOST));
			
			List<String> lists = new ArrayList<>();
			lists.add(HOST);
			ipFinder.setAddresses(lists);
			
			discoSpi.setIpFinder(ipFinder);
		}
		cfg.setDiscoverySpi(discoSpi);
		
		ClientConnectorConfiguration cliConnCfg = new ClientConnectorConfiguration();
		cliConnCfg.setPort(discoverPort + 1);
		cfg.setClientConnectorConfiguration(cliConnCfg);

		ConnectorConfiguration connectorCfg = new ConnectorConfiguration();
		connectorCfg.setPort(discoverPort + 2);
		cfg.setConnectorConfiguration(connectorCfg);
		
		// Paging Store Setting
		DataStorageConfiguration dataStorageCfg = new DataStorageConfiguration();		
        dataStorageCfg.setStoragePath("ignite-persistence");
        
		DataRegionConfiguration dataRegionCfg = new DataRegionConfiguration();
		dataRegionCfg.setName("TestDataRegion");
		dataRegionCfg.setMaxSize(10 * 1024 * 1024); // DataRegion must have size more than 10MB 
		dataRegionCfg.setPersistenceEnabled(true);
		dataStorageCfg.setDataRegionConfigurations(dataRegionCfg);
		cfg.setDataStorageConfiguration(dataStorageCfg);
		
		cfg.setClientMode(false);
		cfg.setIncludeEventTypes(EventType.EVT_CACHE_OBJECT_PUT, EventType.EVT_CACHE_OBJECT_REMOVED,
				EventType.EVT_CACHE_OBJECT_EXPIRED);
				
		cfg.setGridName("test-grid");
//		cfg.setIgniteInstanceName("test-inst-01");
		cfg.setConsistentId("test-inst-01");

		Ignite ignite = Ignition.start(cfg);

		System.out.println();
		System.out.println(">>> ClusterRemote started.");
	}
}
