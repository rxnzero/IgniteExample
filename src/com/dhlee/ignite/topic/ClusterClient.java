package com.dhlee.ignite.topic;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteMessaging;
import org.apache.ignite.Ignition;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;

public class ClusterClient {
	
	private static String HOST = "127.0.0.1:47500..47509"; //..47509
    
	// Local Network Control Block : 224.0.0.0 - 224.0.0.255 
	// Use 224.0.1.0 ~ 238.255.255.255 
	private static String MULTICAST_GROUP = "228.10.10.157";
	private static long timeout = 2000;
	
	/** Number of test count. */
    private static final int TEST_COUNT = 10;
    
	public static void main(String[] args) {
		String localTopicName = args[0];
		String remoteTopicName = args[1];
		ConcurrentHashMap<String, String> resultMap = new ConcurrentHashMap<String, String>();
		
		// http://apacheignite.readme.io/docs/cluster-config
		// case1. Multicast Base Discovery
//		TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
//		ipFinder.setMulticastGroup(MULTICAST_GROUP); 
		
		// case2. Static IP Base Discovery
		TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
		ipFinder.setAddresses(Collections.singletonList(HOST));
		
		TcpDiscoverySpi discoSpi = new TcpDiscoverySpi();
		 discoSpi.setIpFinder(ipFinder);

		 IgniteConfiguration cfg = new IgniteConfiguration();
		 cfg.setDiscoverySpi(discoSpi);
		     
		 Ignite ignite = Ignition.start(cfg);
		
		 System.out.println();
        System.out.println(">>> ClusterClient started. TopicName - " + localTopicName);

        // Group for remote nodes.
        // IMPORT : when use default server started, single server env will not execute!
//        ClusterGroup rmts = ignite.cluster().forRemotes();
        
        // Group for all nodes.
        ClusterGroup rmts = ignite.cluster();
        
        // Listen for messages from remote nodes to make sure that they received all the messages.
        int msgCnt = rmts.nodes().size() * TEST_COUNT;
        System.out.println(">>> Messaging node size = " + rmts.nodes().size() );
        
        IgniteBiPredicate<UUID, String> predicate = new IgniteBiPredicate<UUID, String>() {
			private static final long serialVersionUID = 1L;

			@Override public boolean apply(UUID nodeId, String msg) {
            	try {
            		synchronized(resultMap) {
            			System.out.println("## Received Topic : Message = " + localTopicName + " : " + msg);
                    	resultMap.put(localTopicName, msg);
            			resultMap.notifyAll();
            		}
            	}
            	catch(Exception ex) {
            		ex.printStackTrace();
            	}
            	// stop listen
            	return false;
            }
        };
        
        IgniteMessaging message = ignite.message(ignite.cluster().forLocal());
        
        int retry = 0;
        for (int i = 0; i < TEST_COUNT; i++) {
        	message.localListen(localTopicName, predicate);
        	retry = 0;
        	System.out.println("\n>>> send request to "+ i +" : " +remoteTopicName);
            ignite.message(rmts).sendOrdered(remoteTopicName, ("INDEX-"+ i +" : hello "+ remoteTopicName), timeout * 10);
            String result = null;
            while (true) {
            	if( (result = resultMap.remove(localTopicName)) != null) {
            		retry = 0;
            		break;
            	}
            	else {
            		if(retry > 2) {
            			System.out.println("* retry timeout (ms)  -> " + (retry * timeout));
            			break;
            		}
            		synchronized(resultMap) {
            			System.out.println("* Waiting");
            			try {
							resultMap.wait(timeout);
						} catch (InterruptedException e) {
							; // nothing to do
						}
            			retry++;
            		}
            	}
            }
            
            message.stopLocalListen(localTopicName, predicate);
            
        	System.out.println(">>> result : " +  result);
        }
        System.out.println("$$$ END $$$");
        
        // IMPORTANT - call when Application
        ignite.close();
	}
}
