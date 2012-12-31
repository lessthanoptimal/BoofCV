/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.struct.geo;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.NormOps;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTrifocalTensor {

	@Test
	public void getT() {
		TrifocalTensor t = new TrifocalTensor();
		assertTrue(t.T1 == t.getT(0));
		assertTrue(t.T2 == t.getT(1));
		assertTrue(t.T3 == t.getT(2));
	}

	@Test
	public void set() {
		TrifocalTensor t = new TrifocalTensor();
		t.T1.set(0,0,1);
		t.T2.set(0,0,2);
		t.T3.set(0,0,3);

		TrifocalTensor a = new TrifocalTensor();
		a.set(t);

		assertTrue(1 == a.T1.get(0,0));
		assertTrue(2 == a.T2.get(0,0));
		assertTrue(3 == a.T3.get(0,0));
	}

	@Test
	public void convertFrom() {
		DenseMatrix64F A = new DenseMatrix64F(27,1);
		for( int i = 0; i < 27; i++ )
			A.set(i,i);

		TrifocalTensor t = new TrifocalTensor();
		t.convertFrom(A);

		for( int i = 0; i < 27; i++ )
			assertTrue(i == t.getT( i/9 ).get(i%9));
	}

	@Test
	public void convertTo() {
		TrifocalTensor t = new TrifocalTensor();

		for( int i = 0; i < 27; i++ )
			t.getT( i/9 ).set(i % 9, i);

		DenseMatrix64F A = new DenseMatrix64F(27,1);
		t.convertTo(A);
		for( int i = 0; i < 27; i++ )
			assertTrue(i==A.get(i));

	}

	@Test
	public void normalizeScale() {
		TrifocalTensor t = new TrifocalTensor();

		for( int i = 0; i < 27; i++ )
			t.getT( i/9 ).set(i%9,i);

		DenseMatrix64F A = new DenseMatrix64F(27,1);
		t.convertTo(A);

		double N = NormOps.normF(A);

		t.normalizeScale();
		for( int i = 0; i < 27; i++ )
			assertEquals(A.get(i)/N,t.getT( i/9 ).get(i%9),1e-8);
	}
}
