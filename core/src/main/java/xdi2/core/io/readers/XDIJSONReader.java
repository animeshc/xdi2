package xdi2.core.io.readers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.core.ContextNode;
import xdi2.core.Graph;
import xdi2.core.Literal;
import xdi2.core.Relation;
import xdi2.core.exceptions.Xdi2GraphException;
import xdi2.core.exceptions.Xdi2ParseException;
import xdi2.core.impl.memory.MemoryGraphFactory;
import xdi2.core.io.AbstractXDIReader;
import xdi2.core.io.MimeType;
import xdi2.core.xri3.impl.XRI3Segment;
import xdi2.core.xri3.impl.XRI3SubSegment;
import xdi2.core.xri3.impl.parser.ParserException;

public class XDIJSONReader extends AbstractXDIReader {

	private static final long serialVersionUID = 1450041480967749122L;

	private static final Logger log = LoggerFactory.getLogger(XDIJSONReader.class);

	public static final String FORMAT_NAME = "XDI/JSON";
	public static final String FILE_EXTENSION = "json";
	public static final MimeType MIME_TYPE = new MimeType("application/xdi+json");
	
	public XDIJSONReader(Properties parameters) {

		super(parameters);
	}

	@Override
	protected void init() {

	}

	public void read(Graph graph, JSONObject graphObject, State state) throws IOException, Xdi2ParseException, JSONException {

		//		this.readContextNode(graph.getRootContextNode(), graphObject);

		for (Iterator<?> keys = graphObject.keys(); keys.hasNext(); ) {

			String key = (String) keys.next();
			JSONArray value = graphObject.getJSONArray(key);

			String[] strings = key.split("/");
			if (strings.length != 2) throw new Xdi2ParseException("Invalid key: " + key);

			String subject = strings[0];
			String predicate = strings[1];
			ContextNode contextNode = graph.findContextNode(makeXRI3Segment(subject, state), true);

			if (predicate.equals("()")) {

				// add context nodes

				for (int i=0; i<value.length(); i++) {

					XRI3SubSegment arcXri = makeXRI3SubSegment(value.getString(i), state);

					// ignore implied context nodes

					if (contextNode.containsContextNode(arcXri)) {

						continue;
					} else {

						ContextNode innerContextNode = contextNode.createContextNode(arcXri);
						if (log.isDebugEnabled()) log.debug("Under " + contextNode.getXri() + ": Created context node " + innerContextNode.getArcXri() + " --> " + innerContextNode.getXri());
					}
				}
			} else if (predicate.equals("!")) {

				// add literal

				if (value.length() != 1) throw new Xdi2ParseException("JSON array for key " + key + " must have exactly one item");

				String literalData = value.getString(0);

				Literal literal = contextNode.createLiteral(literalData);
				if (log.isDebugEnabled()) log.debug("Under " + contextNode.getXri() + ": Created literal --> " + literal.getLiteralData());
			} else {

				// add relations

				XRI3Segment arcXri = makeXRI3Segment(predicate, state);

				for (int i=0; i<value.length(); i++) {
					
					String valueString = value.getString(i);
					
					JSONObject jsonObject = null;
					
					try {
						
						//try parsing the valueString as a possible cross-reference
						jsonObject = new JSONObject(valueString);
					} catch (JSONException e) {
						//ignore if no cross-reference was found
					}
					
					Relation relation = null;
					
					if (jsonObject != null) {
						
						// if a cross-reference exists, recursively parse each nested JSON and add it as a relation
						for (Iterator<?> innerKeys = jsonObject.keys(); innerKeys.hasNext(); ) {
							String innerKey = (String) innerKeys.next();
							JSONArray innerValue = jsonObject.getJSONArray(innerKey);

							JSONObject innerJSONObject = new JSONObject();
							innerJSONObject.put(innerKey, innerValue);
							
							Graph tempGraph = MemoryGraphFactory.getInstance().openGraph();
							read(tempGraph, innerJSONObject, state);
							
							String innerValueString = ("(" + tempGraph.toString(new MimeType("text/xdi")) + ")").replaceAll("[ \n]", "");
							XRI3Segment innerTargetContextNodeXri = makeXRI3Segment(innerValueString, state);
							relation = contextNode.createRelation(arcXri, innerTargetContextNodeXri);
							if (log.isDebugEnabled()) log.debug("Under " + contextNode.getXri() + ": Created relation " + relation.getArcXri() + " --> " + relation.getTargetContextNodeXri());
						}
					} else {
						
						XRI3Segment targetContextNodeXri = makeXRI3Segment(valueString, state);
						relation = contextNode.createRelation(arcXri, targetContextNodeXri);
						if (log.isDebugEnabled()) log.debug("Under " + contextNode.getXri() + ": Created relation " + relation.getArcXri() + " --> " + relation.getTargetContextNodeXri());
					}
				}
			}
		}
	}

	private void read(Graph graph, BufferedReader bufferedReader, State state) throws IOException, Xdi2ParseException, JSONException {

		String line;
		StringBuilder graphString = new StringBuilder();

		while ((line = bufferedReader.readLine()) != null) {

			graphString.append(line + "\n");
		}

		this.read(graph, new JSONObject(graphString.toString()), state);
	}

	@Override
	public Reader read(Graph graph, Reader reader) throws IOException, Xdi2ParseException {

		State state = new State();

		try {

			this.read(graph, new BufferedReader(reader), state);
		} catch (JSONException ex) {

			throw new Xdi2ParseException("JSON parse error: " + ex.getMessage(), ex);
		} catch (Xdi2GraphException ex) {

			throw new Xdi2ParseException("Graph problem: " + ex.getMessage(), ex);
		} catch (ParserException ex) {

			throw new Xdi2ParseException("Cannot parse XRI " + state.lastXriString + ": " + ex.getMessage(), ex);
		}

		return reader;
	}

	private static class State {

		private String lastXriString;
	}

	private static XRI3Segment makeXRI3Segment(String xriString, State state) {

		state.lastXriString = xriString;
		return new XRI3Segment(xriString);
	}

	private static XRI3SubSegment makeXRI3SubSegment(String xriString, State state) {

		state.lastXriString = xriString;
		return new XRI3SubSegment(xriString);
	}
}
