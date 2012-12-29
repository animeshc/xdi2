package xdi2.core.features.linkcontracts.condition;

import xdi2.core.ContextNode;
import xdi2.core.util.locator.ContextNodeLocator;
import xdi2.core.xri3.impl.XDI3Segment;
import xdi2.core.xri3.impl.XDI3Statement;

/**
 * An XDI generic condition, represented as a statement.
 * 
 * @author markus
 */
public class GenericCondition extends Condition {

	private static final long serialVersionUID = 3812888725775095575L;

	protected GenericCondition(XDI3Statement statement) {

		super(statement);
	}

	/*
	 * Static methods
	 */

	/**
	 * Checks if a statement is a valid XDI generic condition.
	 * @param relation The relation to check.
	 * @return True if the relation is a valid XDI generic condition.
	 */
	public static boolean isValid(XDI3Statement statement) {

		return true;
	}

	/**
	 * Factory method that creates an XDI generic condition bound to a given statement.
	 * @param statement The statement that is an XDI generic condition.
	 * @return The XDI generic condition.
	 */
	public static GenericCondition fromStatement(XDI3Statement statement) {

		if (! isValid(statement)) return null;

		return new GenericCondition(statement);
	}

	/*
	 * Instance methods
	 */

	@Override
	public boolean evaluateInternal(ContextNodeLocator contextNodeLocator) {

		if (this.getStatement().isContextNodeStatement()) {

			ContextNode subject = contextNodeLocator.locateContextNode(this.getStatement().getContextNodeXri());

			return subject != null;
		}

		if (this.getStatement().isRelationStatement()) {

			ContextNode subject = contextNodeLocator.locateContextNode(this.getStatement().getContextNodeXri());
			XDI3Segment arcXri = this.getStatement().getArcXri();
			XDI3Segment targetContextNodeXri = contextNodeLocator.getContextNodeXri(this.getStatement().getTargetContextNodeXri());

			return subject.containsRelation(arcXri, targetContextNodeXri);
		}

		if (this.getStatement().isLiteralStatement()) {

			ContextNode subject = contextNodeLocator.locateContextNode(this.getStatement().getContextNodeXri());
			String literalData = this.getStatement().getLiteralData();

			return subject.containsLiteral(literalData);
		}

		return false;
	}
}
