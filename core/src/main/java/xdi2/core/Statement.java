package xdi2.core;

import java.io.Serializable;

import xdi2.core.xri3.impl.XDI3Segment;
import xdi2.core.xri3.impl.XDI3Statement;

/**
 * This interface represents a statement in an XDI graph.
 * A statement consists of a subject, predicate and object.
 * 
 * @author markus
 */
public interface Statement extends Serializable, Comparable<Statement> {

	/*
	 * General methods
	 */

	/**
	 * Gets the subject of this statement.
	 * @return A subject.
	 */
	public XDI3Segment getSubject();

	/**
	 * Gets the predicate of this statement.
	 * @return A predicate.
	 */
	public XDI3Segment getPredicate();

	/**
	 * Gets the object of this statement.
	 * @return An object.
	 */
	public XDI3Segment getObject();

	/**
	 * Returns the graph that contains this statement.
	 * @return The graph that contains this statement.
	 */
	public Graph getGraph();

	/**
	 * Returns the context node XRI of this statement.
	 */
	public XDI3Segment getContextNodeXri();

	/**
	 * Delete the statement.
	 */
	public void delete();

	/**
	 * Expresses the statement as an XDI statement in the form subject/predicate/object
	 * @return An XDI statement.
	 */
	public XDI3Statement getXdiStatement();

	/*
	 * Sub-interfaces
	 */

	public interface ContextNodeStatement extends Statement {

		public ContextNode getContextNode();
	}

	public interface RelationStatement extends Statement {

		public Relation getRelation();
	}

	public interface LiteralStatement extends Statement {

		public Literal getLiteral();
	}
}
