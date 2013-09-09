package xdi2.tests.core.impl.keyvalue;

import java.io.IOException;

import xdi2.core.Graph;
import xdi2.core.impl.keyvalue.bdb.BDBKeyValueGraphFactory;
import xdi2.core.impl.keyvalue.bdb.BDBKeyValueStore;
import xdi2.tests.core.impl.AbstractGraphTest;

public class BDBKeyValueGraphTest extends AbstractGraphTest {

	private static BDBKeyValueGraphFactory graphFactory = new BDBKeyValueGraphFactory();

	public static final String DATABASE_PATH = "./xdi2-bdb/";

	static {
		
		graphFactory.setDatabasePath(DATABASE_PATH);
	}

	@Override
	protected void setUp() throws Exception {

		super.setUp();

		BDBKeyValueStore.cleanup(DATABASE_PATH);
	}

	@Override
	protected void tearDown() throws Exception {

		super.tearDown();

		BDBKeyValueStore.cleanup(DATABASE_PATH);
	}

	@Override
	protected Graph openNewGraph(String identifier) throws IOException {

		return graphFactory.openGraph(identifier);
	}

	@Override
	protected Graph reopenGraph(Graph graph, String identifier) throws IOException {

		graph.close();

		return graphFactory.openGraph(identifier);
	}
}
