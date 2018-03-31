package boofcv.abst.geo.pose;

import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;

/**
 * @author Peter Abeles
 */
public class TestIPPE_to_EstimatePnP extends CheckEstimate1ofPnP {
	public TestIPPE_to_EstimatePnP() {
		super(FactoryMultiView.computePnP_1(EnumPNP.IPPE,-1,-1), false);

		worldPointsZZero = true;
	}
}
