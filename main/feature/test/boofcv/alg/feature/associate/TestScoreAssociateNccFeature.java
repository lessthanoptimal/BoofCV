package boofcv.alg.feature.associate;

import boofcv.struct.feature.NccFeature;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Peter Abeles
 */
public class TestScoreAssociateNccFeature {

	@Test
	public void compareToExpected() {
		ScoreAssociateNccFeature scorer = new ScoreAssociateNccFeature();

		NccFeature a = new NccFeature(5);
		NccFeature b = new NccFeature(5);

		a.variance=12;
		b.variance=7;
		a.value=new double[]{1,2,3,4,5};
		b.value=new double[]{2,-1,7,-8,10};

		assertEquals(-0.46429,scorer.score(a,b),1e-2);
	}

	@Test
	public void checkZeroMinimum() {
		ScoreAssociateNccFeature scorer = new ScoreAssociateNccFeature();
		assertFalse(scorer.isZeroMinimum());
	}
}
