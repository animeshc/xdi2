package xdi2.core.features.linkcontracts;

import java.io.Serializable;

import xdi2.core.ContextNode;
import xdi2.core.Literal;
import xdi2.core.Relation;
import xdi2.core.constants.XDILinkContractConstants;
import xdi2.core.features.linkcontracts.util.XDIPolicyExpression;

/**
 * An XDI policy belonging to an XDI link contract, represented as a context
 * node.
 * 
 * @author markus
 */
public final class Policy implements Serializable, Comparable<Policy> {

	private static final long serialVersionUID = -9212794041490417047L;

	private LinkContract linkContract;
	private ContextNode contextNode;

	private XDIPolicyExpression policyType = XDIPolicyExpression.LC_POL_EMPTY;

	protected Policy(LinkContract linkContract, ContextNode contextNode) {

		if (linkContract == null || contextNode == null)
			throw new NullPointerException();

		this.linkContract = linkContract;
		this.contextNode = contextNode;
	}

	public XDIPolicyExpression getPolicyType() {

		return policyType;
	}

	public void setPolicyType(XDIPolicyExpression polType) {

		policyType = polType;
	}

	/*
	 * Static methods
	 */

	/**
	 * Checks if a context node is a valid XDI policy.
	 * 
	 * @param contextNode
	 *            The context node to check.
	 * @return True if the context node is a valid XDI policy.
	 */
	public static boolean isValid(ContextNode contextNode) {

		return XDILinkContractConstants.XRI_SS_IF.equals(contextNode
				.getArcXri());
	}

	/**
	 * Factory method that creates an XDI policy bound to a given context node.
	 * 
	 * @param linkContract
	 *            The XDI link contract to which this XDI policy belongs.
	 * @param contextNode
	 *            The context node that is an XDI policy.
	 * @return The XDI policy.
	 */
	public static Policy fromLinkContractAndContextNode(
			LinkContract linkContract, ContextNode contextNode) {

		if (!isValid(contextNode))
			return null;

		return new Policy(linkContract, contextNode);
	}

	/*
	 * Instance methods
	 */

	/**
	 * Returns the link contract to which this policy belongs.
	 * 
	 * @return A message envelope.
	 */
	public LinkContract getLinkContract() {

		return this.linkContract;
	}

	/**
	 * Returns the underlying context node to which this policy is bound.
	 * 
	 * @return A context node that represents the policy.
	 */
	protected ContextNode getContextNode() {

		return this.contextNode;
	}

	// ...
	// here go methods for working with the policy ...
	// ...

	/**
	 * Create an AND node under $if. There can only be one AND node under $if.
	 * 
	 * @return newly created context node
	 */

	public AndExpression getAndNode(boolean create) {
		AndExpression andNode = null;
		ContextNode nodeExists = this.getContextNode().getContextNode(
				XDILinkContractConstants.XRI_SS_AND);
		if (null != nodeExists) {
			andNode = AndExpression.fromContextNode(nodeExists);
			return andNode;
		}

		if (policyType == XDIPolicyExpression.LC_POL_EMPTY
				|| policyType == XDIPolicyExpression.LC_POL_AND) {
			if (!this.getContextNode().containsContextNode(
					XDILinkContractConstants.XRI_SS_AND)) {
				andNode = AndExpression
						.fromContextNode(this.getContextNode()
								.createContextNode(
										XDILinkContractConstants.XRI_SS_AND));
				policyType = XDIPolicyExpression.LC_POL_AND;
			}
		}

		return andNode;
	}

	public OrExpression getOrNode(boolean create) {

		OrExpression orNode = null;
		ContextNode nodeExists = this.getContextNode().getContextNode(
				XDILinkContractConstants.XRI_SS_OR);
		if (null != nodeExists) {
			orNode = OrExpression.fromContextNode(nodeExists);
			return orNode;
		}

		if (policyType == XDIPolicyExpression.LC_POL_EMPTY
				|| policyType == XDIPolicyExpression.LC_POL_OR) {
			if (!this.getContextNode().containsContextNode(
					XDILinkContractConstants.XRI_SS_OR)) {
				orNode = OrExpression.fromContextNode(this.getContextNode()
						.createContextNode(XDILinkContractConstants.XRI_SS_OR));
				policyType = XDIPolicyExpression.LC_POL_OR;
			}
		}

		return orNode;
	}

	public NotExpression getNotNode(boolean create) {

		NotExpression notNode = null;
		ContextNode nodeExists = this.getContextNode().getContextNode(
				XDILinkContractConstants.XRI_SS_NOT);
		if (null != nodeExists) {
			notNode = NotExpression.fromContextNode(nodeExists);
			return notNode;
		}

		if (policyType == XDIPolicyExpression.LC_POL_EMPTY
				|| policyType == XDIPolicyExpression.LC_POL_NOT) {
			if (!this.getContextNode().containsContextNode(
					XDILinkContractConstants.XRI_SS_NOT)) {
				notNode = NotExpression
						.fromContextNode(this.getContextNode()
								.createContextNode(
										XDILinkContractConstants.XRI_SS_NOT));
				policyType = XDIPolicyExpression.LC_POL_NOT;
			}
		}

		return notNode;
	}

	public void setLiteralExpression(String literalData) {
		if (policyType == XDIPolicyExpression.LC_POL_EMPTY
				|| policyType == XDIPolicyExpression.LC_POL_SINGLETON) {
			Literal literal = this.getContextNode().getLiteral();
			if (literal == null) {
				literal = this.getContextNode().createLiteral(literalData);
			} else {
				literal.setLiteralData(literalData);
			}
			policyType = XDIPolicyExpression.LC_POL_SINGLETON;
		}
	}

	public String getLiteralExpression() {
		String str = "";
		Literal literal = this.getContextNode().getLiteral();
		if (literal != null) {
			str = literal.getLiteralData();
		}
		return str;
	}

	public Relation getRuleReference(ContextNode target) {
		Relation relation = this.getContextNode().getRelation(
				XDILinkContractConstants.XRI_S_VARIABLE_REF);
		if (relation != null) {
			return relation;
		}

		relation = this.getContextNode().createRelation(
				XDILinkContractConstants.XRI_S_VARIABLE_REF, target);

		return relation;
	}

	/*
	 * Object methods
	 */

	@Override
	public String toString() {

		return this.getContextNode().toString();
	}

	@Override
	public boolean equals(Object object) {

		if (object == null || !(object instanceof Policy))
			return false;
		if (object == this)
			return true;

		Policy other = (Policy) object;

		return this.getContextNode().equals(other.getContextNode());
	}

	@Override
	public int hashCode() {

		int hashCode = 1;

		hashCode = (hashCode * 31) + this.getContextNode().hashCode();

		return hashCode;
	}

	public int compareTo(Policy other) {

		if (other == this || other == null)
			return 0;

		return this.getContextNode().compareTo(other.getContextNode());
	}
}
