package xdi2.tests.core.util;

import junit.framework.TestCase;
import xdi2.core.Graph;
import xdi2.core.impl.memory.MemoryGraphFactory;
import xdi2.core.util.CopyUtil;
import xdi2.core.xri3.impl.XDI3Statement;

public class CopyUtilTest extends TestCase {

	public void testCopyUtil() throws Exception {

		Graph graph = MemoryGraphFactory.getInstance().openGraph();
		graph.createStatement(new XDI3Statement("=markus+email/!/(data:,markus.sabadello@gmail.com)"));
		graph.createStatement(new XDI3Statement("=markus/+friend/=neustar*animesh"));
		graph.createStatement(new XDI3Statement("=neustar*animesh+email/!/(data:,animesh@gmail.com)"));
		graph.createStatement(new XDI3Statement("=neustar*animesh/+friend/=markus"));

		Graph graph2 = MemoryGraphFactory.getInstance().openGraph();
		CopyUtil.copyGraph(graph, graph2, null);

		Graph graph3 = MemoryGraphFactory.getInstance().openGraph();
		CopyUtil.copyGraph(graph, graph3, null);
		CopyUtil.copyGraph(graph2, graph3, null);

		assertEquals(graph, graph2);
		assertEquals(graph2, graph3);
		assertEquals(graph3, graph);
	}
}
