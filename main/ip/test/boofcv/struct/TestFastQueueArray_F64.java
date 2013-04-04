package boofcv.struct;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestFastQueueArray_F64 {

	@Test
	public void stuff() {
		FastQueueArray_F64 alg = new FastQueueArray_F64(3);

		double found[] = alg.grow();
		assertEquals(3,found.length);
		assertEquals(1,alg.size());
		assertEquals(3,alg.get(0).length);
	}

}
