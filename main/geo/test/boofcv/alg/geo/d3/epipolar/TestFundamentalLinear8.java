package boofcv.alg.geo.d3.epipolar;

import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestFundamentalLinear8 extends CommonFundamentalChecks {

	@Test
	public void perfectFundamental() {
		checkEpipolarMatrix(8,true,new FundamentalLinear8(true));
		checkEpipolarMatrix(15,true,new FundamentalLinear8(true));
	}

	@Test
	public void perfectEssential() {
		checkEpipolarMatrix(8,false,new FundamentalLinear8(false));
		checkEpipolarMatrix(15,false,new FundamentalLinear8(false));
	}
}
