package xdi2.tests.core.impl.keyvalue;

import java.io.IOException;

import xdi2.core.Graph;
import xdi2.core.impl.keyvalue.properties.PropertiesKeyValueGraphFactory;
import xdi2.core.impl.keyvalue.properties.PropertiesKeyValueStore;
import xdi2.tests.core.impl.AbstractGraphTest;

public class PropertiesKeyValueGraphTest extends AbstractGraphTest {

	private static PropertiesKeyValueGraphFactory graphFactory = new PropertiesKeyValueGraphFactory();

	@Override
	protected void setUp() throws Exception {

		super.setUp();

		PropertiesKeyValueStore.cleanup();
	}

	@Override
	protected void tearDown() throws Exception {

		super.tearDown();

		PropertiesKeyValueStore.cleanup();
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
