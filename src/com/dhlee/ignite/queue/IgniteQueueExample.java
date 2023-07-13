package com.dhlee.ignite.queue;

import java.util.UUID;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteQueue;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CollectionConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;

import static org.apache.ignite.cache.CacheMode.PARTITIONED;

/**
 * Ignite cache distributed queue example. This example demonstrates
 * {@code FIFO} unbounded cache queue.
 * <p>
 * Remote nodes should always be started with special configuration file which
 * enables P2P class loading: {@code 'ignite.{sh|bat}
 * examples/config/example-ignite.xml'}.
 * <p>
 * Alternatively you can run {@link ExampleNodeStartup} in another JVM which
 * will start node with {@code examples/config/example-ignite.xml}
 * configuration.
 */
public class IgniteQueueExample {
	/** Number of retries */
	private static final int RETRIES = 20;

	/** Queue instance. */
	private static IgniteQueue<String> queue;

	private static String HOST = "127.0.0.1:47500..47509"; //

	private static String MULTICAST_GROUP = "228.10.10.157";

	/**
	 * Executes example.
	 *
	 * @param args Command line arguments, none required.
	 * @throws Exception If example execution failed.
	 */
	public static void main(String[] args) throws Exception {
//        try (Ignite ignite = Ignition.start("examples/config/example-ignite.xml")) {
		TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
		ipFinder.setMulticastGroup(MULTICAST_GROUP);
		TcpDiscoverySpi discoSpi = new TcpDiscoverySpi();
		discoSpi.setIpFinder(ipFinder);

		IgniteConfiguration cfg = new IgniteConfiguration();
		cfg.setDiscoverySpi(discoSpi);
//		 cfg.setGridName("test");

		Ignite ignite = Ignition.start(cfg);

		System.out.println();
		System.out.println(">>> Ignite queue example started.");

		// Make queue name.
		String queueName = UUID.randomUUID().toString();

		queue = initializeQueue(ignite, queueName);

		readFromQueue(ignite);

		writeToQueue(ignite);

		clearAndRemoveQueue();
//        }

		System.out.println("Cache queue example finished.");

		// IMPORTANT - call when Application
		ignite.close();
	}

	/**
	 * Initialize queue.
	 *
	 * @param ignite    Ignite.
	 * @param queueName Name of queue.
	 * @return Queue.
	 * @throws IgniteException If execution failed.
	 */
	private static IgniteQueue<String> initializeQueue(Ignite ignite, String queueName) throws IgniteException {
		CollectionConfiguration colCfg = new CollectionConfiguration();

		colCfg.setCacheMode(PARTITIONED);

		// Initialize new FIFO queue.
		IgniteQueue<String> queue = ignite.queue(queueName, 0, colCfg);

		// Initialize queue items.
		// We will be use blocking operation and queue size must be appropriated.
		for (int i = 0; i < ignite.cluster().nodes().size() * RETRIES * 2; i++)
			queue.put(Integer.toString(i));

		System.out.println("Queue size after initializing: " + queue.size());

		return queue;
	}

	/**
	 * Read items from head and tail of queue.
	 *
	 * @param ignite Ignite.
	 * @throws IgniteException If failed.
	 */
	private static void readFromQueue(Ignite ignite) throws IgniteException {
		final String queueName = queue.name();

		// Read queue items on each node.
		ignite.compute().broadcast(new QueueClosure(queueName, false));

		System.out.println("Queue size after reading [expected=0, actual=" + queue.size() + ']');
	}

	/**
	 * Write items into queue.
	 *
	 * @param ignite Ignite.
	 * @throws IgniteException If failed.
	 */
	private static void writeToQueue(Ignite ignite) throws IgniteException {
		final String queueName = queue.name();

		// Write queue items on each node.
		ignite.compute().broadcast(new QueueClosure(queueName, true));

		System.out.println("Queue size after writing [expected=" + ignite.cluster().nodes().size() * RETRIES
				+ ", actual=" + queue.size() + ']');

		System.out.println("Iterate over queue.");

		// Iterate over queue.
		for (String item : queue)
			System.out.println("Queue item: " + item);
	}

	/**
	 * Clear and remove queue.
	 *
	 * @throws IgniteException If execution failed.
	 */
	private static void clearAndRemoveQueue() throws IgniteException {
		System.out.println("Queue size before clearing: " + queue.size());

		// Clear queue.
		queue.clear();

		System.out.println("Queue size after clearing: " + queue.size());

		// Remove queue.
		queue.close();

		// Try to work with removed queue.
		try {
			queue.poll();
		} catch (IllegalStateException expected) {
			System.out.println("Expected exception - " + expected.getMessage());
		}
	}

	/**
	 * Closure to populate or poll the queue.
	 */
	private static class QueueClosure implements IgniteRunnable {
		/** Queue name. */
		private final String queueName;

		/** Flag indicating whether to put or poll. */
		private final boolean put;

		/**
		 * @param queueName Queue name.
		 * @param put       Flag indicating whether to put or poll.
		 */
		QueueClosure(String queueName, boolean put) {
			this.queueName = queueName;
			this.put = put;
		}

		/** {@inheritDoc} */
		@Override
		public void run() {
			IgniteQueue<String> queue = Ignition.ignite().queue(queueName, 0, null);

			if (put) {
				UUID locId = Ignition.ignite().cluster().localNode().id();

				for (int i = 0; i < RETRIES; i++) {
					String item = locId + "_" + Integer.toString(i);

					queue.put(item);

					System.out.println("Queue item has been added: " + item);
				}
			} else {
				// Take items from queue head.
				for (int i = 0; i < RETRIES; i++)
					System.out.println("Queue item has been read from queue head - take: " + queue.take());

				// Take items from queue head once again.
				for (int i = 0; i < RETRIES; i++)
					System.out.println("Queue item has been read from queue head - poll: " + queue.poll());
			}
		}
	}
}
