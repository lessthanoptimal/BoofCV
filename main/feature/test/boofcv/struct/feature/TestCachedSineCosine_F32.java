package boofcv.struct.feature;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestCachedSineCosine_F32 {

	/**
	 * Compare solution against a hand generated one
	 */
	@Test
	public void knowCase() {
		CachedSineCosine_F32 alg = new CachedSineCosine_F32(-2,1,5);

		assertEquals(-2,alg.minAngle,1e-4);
		assertEquals(1,alg.maxAngle,1e-4);
		assertEquals(0.75,alg.delta,1e-4);

		assertEquals(-0.41615,alg.c[0],0.2);
		assertEquals(-0.90930,alg.s[0],0.2);

		assertEquals(0.87758,alg.c[2],0.2);
		assertEquals(-0.47943,alg.s[2],0.2);

		assertEquals(0.54030,alg.c[4],0.2);
		assertEquals(0.84147,alg.s[4],0.2);

		assertEquals(0,alg.computeIndex(-2));
		assertEquals(2,alg.computeIndex(0));
		assertEquals(4,alg.computeIndex(1));
	}
}
