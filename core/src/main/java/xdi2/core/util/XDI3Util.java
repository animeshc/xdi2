package xdi2.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.core.constants.XDIConstants;
import xdi2.core.features.nodetypes.XdiInnerRoot;
import xdi2.core.xri3.XDI3Segment;
import xdi2.core.xri3.XDI3SubSegment;

/**
 * Various utility methods for working with XRI 3.0 syntax.
 * 
 * @author markus
 */
public final class XDI3Util {

	private static final Logger log = LoggerFactory.getLogger(XDI3Util.class);

	private XDI3Util() { }

	/**
	 * Checks if an XRI starts with a certain other XRI.
	 */
	public static XDI3Segment startsWith(final XDI3Segment xri, final XDI3Segment startXri, final boolean variablesInXri, final boolean variablesInStart) {

		if (xri == null) throw new NullPointerException();
		if (startXri == null) throw new NullPointerException();

		XDI3Segment result = null;

		try {

			if (startXri.equals(XDIConstants.XRI_S_ROOT)) { result = XDIConstants.XRI_S_ROOT; return result; }
			if (xri.equals(XDIConstants.XRI_S_ROOT)) { result = null; return result; }

			int xriIndex = 0, startIndex = 0;

			while (true) {

				if (startIndex == startXri.getNumSubSegments()) { result = XDI3Util.parentXri(xri, xriIndex); return result; }
				if (xriIndex == xri.getNumSubSegments()) { result = null; return result; }

				// check variables

				if (variablesInXri && VariableUtil.isVariable(xri.getSubSegment(xriIndex))) {

					if (VariableUtil.matches(xri.getSubSegment(xriIndex), startXri.getSubSegment(startIndex))) {

						startIndex++;

						if (VariableUtil.isMultiple(xri.getSubSegment(xriIndex))) {

							while (startIndex < startXri.getNumSubSegments() && 
									VariableUtil.matches(xri.getSubSegment(xriIndex), startXri.getSubSegment(startIndex))) startIndex++;
						}

						xriIndex++;

						continue;
					} else {

						{ result = null; return result; }
					}
				}

				if (variablesInStart && VariableUtil.isVariable(startXri.getSubSegment(startIndex))) {

					if (VariableUtil.matches(startXri.getSubSegment(startIndex), xri.getSubSegment(xriIndex))) {

						xriIndex++;

						if (VariableUtil.isMultiple(startXri.getSubSegment(startIndex))) {

							while (xriIndex < xri.getNumSubSegments() && 
									VariableUtil.matches(startXri.getSubSegment(startIndex), xri.getSubSegment(xriIndex))) xriIndex++;
						}

						startIndex++;

						continue;
					} else {

						{ result = null; return result; }
					}
				}

				// no variables? just match the subsegment

				if (! (xri.getSubSegment(xriIndex).equals(startXri.getSubSegment(startIndex)))) { result = null; return result; }

				xriIndex++;
				startIndex++;
			}
		} finally {

			if (log.isTraceEnabled()) log.trace("startsWith(" + xri + "," + startXri + "," + variablesInXri + "," + variablesInStart + ") --> " + result);
		}
	}

	/**
	 * Checks if an XRI starts with a certain other XRI.
	 */
	public static XDI3Segment startsWith(XDI3Segment xri, XDI3Segment startXri) {

		return startsWith(xri, startXri, false, false);
	}

	/**
	 * Checks if an XRI ends with a certain other XRI.
	 */
	public static XDI3Segment endsWith(final XDI3Segment xri, final XDI3Segment endXri, final boolean variablesInXri, final boolean variablesInEnd) {

		if (xri == null) throw new NullPointerException();
		if (endXri == null) throw new NullPointerException();

		XDI3Segment result = null;

		try {

			if (endXri.equals(XDIConstants.XRI_S_ROOT)) { result = XDIConstants.XRI_S_ROOT; return result; }
			if (xri.equals(XDIConstants.XRI_S_ROOT)) { result = null; return result; }

			int xriIndex = xri.getNumSubSegments() - 1, endIndex = endXri.getNumSubSegments() - 1;

			while (true) {

				if (endIndex == -1) { result = XDI3Util.localXri(xri, - xriIndex - 1); return result; }
				if (xriIndex == -1) { result = null; return result; }

				// check variables

				if (variablesInXri && VariableUtil.isVariable(xri.getSubSegment(xriIndex))) {

					if (VariableUtil.matches(xri.getSubSegment(xriIndex), endXri.getSubSegment(endIndex))) {

						endIndex--;

						if (VariableUtil.isMultiple(xri.getSubSegment(xriIndex))) {

							while (endIndex > -1 && 
									VariableUtil.matches(xri.getSubSegment(xriIndex), endXri.getSubSegment(endIndex))) endIndex--;
						}

						xriIndex--;

						continue;
					} else {

						{ result = null; return result; }
					}
				}

				if (variablesInEnd && VariableUtil.isVariable(endXri.getSubSegment(endIndex))) {

					if (VariableUtil.matches(endXri.getSubSegment(endIndex), xri.getSubSegment(xriIndex))) {

						xriIndex--;

						if (VariableUtil.isMultiple(endXri.getSubSegment(endIndex))) {

							while (xriIndex > -1 && 
									VariableUtil.matches(endXri.getSubSegment(endIndex), xri.getSubSegment(xriIndex))) xriIndex--;
						}

						endIndex--;

						continue;
					} else {

						{ result = null; return result; }
					}
				}

				// no variables? just match the subsegment

				if (! (xri.getSubSegment(xriIndex).equals(endXri.getSubSegment(endIndex)))) { result = null; return result; }

				xriIndex--;
				endIndex--;
			}
		} finally {

			if (log.isTraceEnabled()) log.trace("endsWith(" + xri + "," + endXri + "," + variablesInXri + "," + variablesInEnd + ") --> " + result);
		}
	}

	/**
	 * Checks if an XRI ends with a certain other XRI.
	 */
	public static XDI3Segment endsWith(final XDI3Segment xri, final XDI3Segment endXri) {

		return endsWith(xri, endXri, false, false);
	}

	/**
	 * Extracts an XRI's parent subsegment(s), counting either from the start or the end.
	 * For =a*b*c*d and 1, this returns =a
	 * For =a*b*c*d and -1, this returns =a*b*c
	 */
	public static XDI3Segment parentXri(final XDI3Segment xri, final int numSubSegments) {

		if (xri == null) throw new NullPointerException();

		XDI3Segment result = null;

		try {

			StringBuilder buffer = new StringBuilder();

			if (numSubSegments > 0) {

				for (int i = 0; i < numSubSegments; i++) buffer.append(xri.getSubSegment(i).toString());
			} else if (numSubSegments < 0) {

				for (int i = 0; i < xri.getNumSubSegments() - (- numSubSegments); i++) buffer.append(xri.getSubSegment(i).toString());
			} else {

				{ result = xri; return result; }
			}

			if (buffer.length() == 0) { result = null; return result; }

			{ result = XDI3Segment.create(buffer.toString()); return result; }
		} finally {

			if (log.isTraceEnabled()) log.trace("parentXri(" + xri + "," + numSubSegments + ") --> " + result);
		}
	}

	/**
	 * Extracts an XRI's local subsegment(s).
	 * For =a*b*c*d and 1, this returns *d
	 * For =a*b*c*d and -1, this returns *b*c*d
	 */
	public static XDI3Segment localXri(final XDI3Segment xri, final int numSubSegments) {

		if (xri == null) throw new NullPointerException();

		XDI3Segment result = null;

		try {

			StringBuilder buffer = new StringBuilder();

			if (numSubSegments > 0) {

				for (int i = xri.getNumSubSegments() - numSubSegments; i < xri.getNumSubSegments(); i++) buffer.append(xri.getSubSegment(i).toString());
			} else if (numSubSegments < 0) {

				for (int i = (- numSubSegments); i < xri.getNumSubSegments(); i++) buffer.append(xri.getSubSegment(i).toString());
			} else {

				{ result = xri; return xri; }
			}

			if (buffer.length() == 0) { result = null; return result; }

			{ result = XDI3Segment.create(buffer.toString()); return result; }
		} finally {

			if (log.isTraceEnabled()) log.trace("localXri(" + xri + "," + numSubSegments + ") --> " + result);
		}
	}

	/**
	 * Removes a start XRI from an XRI.
	 * E.g. for =a*b*c*d and =a*b, this returns *c*d
	 * E.g. for =a*b*c*d and (), this returns =a*b*c*d
	 * E.g. for =a*b*c*d and =a*b*c*d, this returns ()
	 * E.g. for =a*b*c*d and =x, this returns null
	 */
	public static XDI3Segment removeStartXri(final XDI3Segment xri, final XDI3Segment start, final boolean variablesInXri, final boolean variablesInStart) {

		if (xri == null) throw new NullPointerException();
		if (start == null) throw new NullPointerException();

		XDI3Segment result = null;

		try {

			if (start.equals(XDIConstants.XRI_S_ROOT)) { result = xri; return result; }
			if (xri.equals(XDIConstants.XRI_S_ROOT)) { result = null; return result; }

			XDI3Segment startXri = XDI3Util.startsWith(xri, start, variablesInXri, variablesInStart);
			if (startXri == null) { result = null; return result; }

			if (xri.equals(startXri)) { result = XDIConstants.XRI_S_ROOT; return result; }

			{ result = XDI3Util.localXri(xri, - startXri.getNumSubSegments()); return result; }
		} finally {

			if (log.isTraceEnabled()) log.trace("removeStartXri(" + xri + "," + start + "," + variablesInXri + "," + variablesInStart + ") --> " + result);
		}
	}

	/**
	 * Removes a start XRI from an XRI.
	 * E.g. for =a*b*c*d and =a*b, this returns *c*d
	 * E.g. for =a*b*c*d and (), this returns =a*b*c*d
	 * E.g. for =a*b*c*d and =a*b*c*d, this returns ()
	 * E.g. for =a*b*c*d and =x, this returns null
	 */
	public static XDI3Segment removeStartXri(final XDI3Segment xri, final XDI3Segment start) {

		return removeStartXri(xri, start, false, false);
	}

	/**
	 * Removes an end XRI from an XRI.
	 * E.g. for =a*b*c*d and *c*d, this returns =a*b
	 * E.g. for =a*b*c*d and (), this returns =a*b*c*d
	 * E.g. for =a*b*c*d and =a*b*c*d, this returns ()
	 * E.g. for =a*b*c*d and *y, this returns null
	 */
	public static XDI3Segment removeEndXri(XDI3Segment xri, XDI3Segment end, boolean variablesInXri, boolean variablesInEnd) {

		if (xri == null) throw new NullPointerException();
		if (end == null) throw new NullPointerException();

		XDI3Segment result = null;

		try {

			if (end.equals(XDIConstants.XRI_S_ROOT)) { result = xri; return result; }
			if (xri.equals(XDIConstants.XRI_S_ROOT)) { result = null; return result; }

			XDI3Segment endXri = XDI3Util.endsWith(xri, end, variablesInXri, variablesInEnd);
			if (endXri == null) { result = null; return result; }

			if (xri.equals(endXri)) { result = XDIConstants.XRI_S_ROOT; return result; }

			{ result = XDI3Util.parentXri(xri, - endXri.getNumSubSegments()); return result; }
		} finally {

			if (log.isTraceEnabled()) log.trace("removeEndXri(" + xri + "," + end + "," + variablesInXri + "," + variablesInEnd + ") --> " + result);
		}
	}

	/**
	 * Removes an end XRI from an XRI.
	 * E.g. for =a*b*c*d and *c*d, this returns =a*b
	 * E.g. for =a*b*c*d and (), this returns =a*b*c*d
	 * E.g. for =a*b*c*d and =a*b*c*d, this returns ()
	 * E.g. for =a*b*c*d and *y, this returns null
	 */
	public static XDI3Segment removeEndXri(final XDI3Segment xri, final XDI3Segment end) {

		return removeEndXri(xri, end, false, false);
	}

	/**
	 * Replaces all occurences of a subsegment with a segment.
	 */
	public static XDI3Segment replaceXri(final XDI3Segment xri, final XDI3SubSegment oldXri, final XDI3Segment newXri, final boolean replaceInPartialSubjectAndPredicate) {

		if (xri == null) throw new NullPointerException();
		if (oldXri == null) throw new NullPointerException();
		if (newXri == null) throw new NullPointerException();

		XDI3Segment result = null;

		try {

			List<XDI3SubSegment> subSegments = new ArrayList<XDI3SubSegment> ();

			for (XDI3SubSegment subSegment : xri.getSubSegments()) {

				if (XdiInnerRoot.isInnerRootArcXri(subSegment)) {

					XDI3Segment subject = XdiInnerRoot.getSubjectOfInnerRootXri(subSegment);
					XDI3Segment predicate = XdiInnerRoot.getPredicateOfInnerRootXri(subSegment);

					subject = replaceXri(subject, oldXri, newXri, replaceInPartialSubjectAndPredicate);

					subSegments.add(XdiInnerRoot.createInnerRootArcXri(subject, predicate));
				} else {

					if (subSegment.equals(oldXri)) {

						subSegments.addAll(newXri.getSubSegments());
					} else {

						subSegments.add(subSegment);
					}
				}
			}

			{ result = XDI3Segment.fromComponents(subSegments); return result; }
		} finally {

			if (log.isTraceEnabled()) log.trace("replaceXri(" + xri + "," + oldXri + "," + newXri + "," + replaceInPartialSubjectAndPredicate + ") --> " + result);
		}
	}

	/**
	 * Concats all XRIs into a new XRI.
	 */
	public static XDI3Segment concatXris(final XDI3Segment[] xris) {

		if (xris == null) throw new NullPointerException();

		XDI3Segment result = null;

		try {

			StringBuffer buffer = new StringBuffer();

			for (XDI3Segment xri : xris) {

				if (xri != null && ! XDIConstants.XRI_S_ROOT.equals(xri)) buffer.append(xri.toString());
			}

			if (buffer.length() == 0) buffer.append("()");

			{ result = XDI3Segment.create(buffer.toString()); return result; }
		} finally {

			if (log.isTraceEnabled()) log.trace("concatXris(" + Arrays.asList(xris) + ") --> " + result);
		}
	}

	/**
	 * Concats two XRIs into a new XRI.
	 */
	public static XDI3Segment concatXris(XDI3Segment xri1, XDI3Segment xri2) {

		if (xri1 == null) throw new NullPointerException();
		if (xri2 == null) throw new NullPointerException();

		XDI3Segment result = null;

		try {

			StringBuffer buffer = new StringBuffer();
			if (xri1 != null && ! XDIConstants.XRI_S_ROOT.equals(xri1)) buffer.append(xri1.toString()); 
			if (xri2 != null && ! XDIConstants.XRI_S_ROOT.equals(xri2)) buffer.append(xri2.toString()); 

			if (buffer.length() == 0) buffer.append("()");

			{ result = XDI3Segment.create(buffer.toString()); return result; }
		} finally {

			if (log.isTraceEnabled()) log.trace("concatXris(" + xri1 + "," + xri2 + ") --> " + result);
		}
	}

	/**
	 * Concats two XRIs into a new XRI.
	 */
	public static XDI3Segment concatXris(final XDI3Segment xri1, final XDI3SubSegment xri2) {

		return concatXris(xri1, xri2 == null ? null : XDI3Segment.fromComponent(xri2));
	}

	/**
	 * Concats two XRIs into a new XRI.
	 */
	public static XDI3Segment concatXris(final XDI3SubSegment xri1, final XDI3Segment xri2) {

		return concatXris(xri1 == null ? null : XDI3Segment.fromComponent(xri1), xri2);
	}

	/**
	 * Concats two XRIs into a new XRI.
	 */
	public static XDI3Segment concatXris(final XDI3SubSegment xri1, final XDI3SubSegment xri2) {

		return concatXris(xri1 == null ? null : XDI3Segment.fromComponent(xri1), xri2 == null ? null : XDI3Segment.fromComponent(xri2));
	}

	/**
	 * Checks if an XRI is a valid Cloud Number.
	 */
	public static boolean isCloudNumber(final XDI3Segment xri) {

		if (xri == null) throw new NullPointerException();

		Boolean result = null;

		try {

			if (xri.getNumSubSegments() < 2) { result = Boolean.FALSE; return result.booleanValue(); }

			for (int i=0; i< xri.getNumSubSegments(); i+=2) {

				if (! xri.getSubSegment(i).isClassXs()) { result = Boolean.FALSE; return result.booleanValue(); }
				if (! XDIConstants.CS_EQUALS.equals(xri.getSubSegment(i).getCs()) && ! XDIConstants.CS_AT.equals(xri.getSubSegment(i).getCs())) { result = Boolean.FALSE; return result.booleanValue(); }

				if (! XDIConstants.CS_BANG.equals(xri.getSubSegment(i + 1).getCs())) { result = Boolean.FALSE; return result.booleanValue(); }
			}

			{ result = Boolean.TRUE; return result.booleanValue(); }
		} finally {

			if (log.isTraceEnabled()) log.trace("isCloudNumber(" + xri + ") --> " + result);
		}
	}

	/*
	 * Helper classes
	 */

	public static final Comparator<? super XDI3Segment> XDI3Segment_ASCENDING_COMPARATOR = new Comparator<XDI3Segment>() {

		@Override
		public int compare(XDI3Segment o1, XDI3Segment o2) {

			if (o1.getNumSubSegments() < o2.getNumSubSegments()) return -1;
			if (o1.getNumSubSegments() > o2.getNumSubSegments()) return 1;

			return o1.compareTo(o2);
		}
	};

	public static final Comparator<? super XDI3Segment> XDI3Segment_DESCENDING_COMPARATOR = new Comparator<XDI3Segment>() {

		@Override
		public int compare(XDI3Segment o1, XDI3Segment o2) {

			if (o1.getNumSubSegments() > o2.getNumSubSegments()) return -1;
			if (o1.getNumSubSegments() < o2.getNumSubSegments()) return 1;

			return o1.compareTo(o2);
		}
	};
}
