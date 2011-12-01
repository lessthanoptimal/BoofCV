package boofcv.alg.feature.associate;

import boofcv.struct.feature.TupleDesc_U8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestScoreAssociateSad_U8 {

	@Test
	public void compareToExpected() {
		ScoreAssociateSad_U8 scorer = new ScoreAssociateSad_U8();

		TupleDesc_U8 a = new TupleDesc_U8(5);
		TupleDesc_U8 b = new TupleDesc_U8(5);

		a.value=new byte[]{1,2,3,4,5};
		b.value=new byte[]{6,2,6,3,6};

		assertEquals(10,scorer.score(a,b),1e-2);
	}

	@Test
	public void checkZeroMinimum() {
		ScoreAssociateSad_U8 scorer = new ScoreAssociateSad_U8();
		assertTrue(scorer.isZeroMinimum());
	}
}
