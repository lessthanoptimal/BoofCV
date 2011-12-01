package boofcv.alg.feature.associate;

import boofcv.struct.feature.TupleDesc_F32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestScoreAssociateSad_F32 {

	@Test
	public void compareToExpected() {
		ScoreAssociateSad_F32 scorer = new ScoreAssociateSad_F32();

		TupleDesc_F32 a = new TupleDesc_F32(5);
		TupleDesc_F32 b = new TupleDesc_F32(5);

		a.value=new float[]{1,2,3,4,5};
		b.value=new float[]{-1,2,6,3,6};

		assertEquals(7,scorer.score(a,b),1e-2);
	}

	@Test
	public void checkZeroMinimum() {
		ScoreAssociateSad_F32 scorer = new ScoreAssociateSad_F32();
		assertTrue(scorer.isZeroMinimum());
	}
}
