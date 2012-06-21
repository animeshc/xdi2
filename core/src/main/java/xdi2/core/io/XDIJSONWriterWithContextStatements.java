package xdi2.core.io;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import xdi2.core.Graph;

public class XDIJSONWriterWithContextStatements extends XDIJSONWriter {

	private static final long serialVersionUID = 1831976206235969200L;

	public static final String FORMAT_NAME = "XDI/JSON_WITH_CONTEXT_STATEMENTS";
	public static final String MIME_TYPE = "application/xdi+json";
	public static final String DEFAULT_FILE_EXTENSION = "json";

	@Override
	public Writer write(Graph graph, Writer writer, Properties parameters) throws IOException {

		if (parameters == null) parameters = new Properties();

		parameters.put(XDIJSONWriter.PARAMETER_WRITE_CONTEXT_STATEMENTS, Boolean.TRUE.toString());

		return super.write(graph, writer, parameters);
	}
}