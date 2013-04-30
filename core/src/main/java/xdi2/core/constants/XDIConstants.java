package xdi2.core.constants;

import xdi2.core.xri3.XDI3Constants;
import xdi2.core.xri3.XDI3Segment;
import xdi2.core.xri3.XDI3SubSegment;

/**
 * General XDI constants.
 * 
 * @author markus
 */
public class XDIConstants {

	public static final XDI3Segment XRI_S_ROOT = XDI3Segment.create(XDI3Constants.XS_ROOT);
	public static final XDI3Segment XRI_S_CONTEXT = XDI3Segment.create(XDI3Constants.XS_ROOT);
	public static final XDI3Segment XRI_S_LITERAL = XDI3Segment.create(XDI3Constants.CS_VALUE.toString());

	public static final XDI3Segment XRI_S_TRUE = XDI3Segment.create("$true");
	public static final XDI3Segment XRI_S_FALSE = XDI3Segment.create("$false");

	public static final XDI3SubSegment XRI_SS_TRUE = XDI3SubSegment.create("$true");
	public static final XDI3SubSegment XRI_SS_FALSE = XDI3SubSegment.create("$false");

	public static final XDI3SubSegment XRI_SS_ROOT = XDI3SubSegment.create(XDI3Constants.XS_ROOT);
	public static final XDI3SubSegment XRI_SS_CONTEXT = XDI3SubSegment.create(XDI3Constants.XS_ROOT);
	public static final XDI3SubSegment XRI_SS_LITERAL = XDI3SubSegment.create(XDI3Constants.CS_VALUE.toString());

	private XDIConstants() { }
}
