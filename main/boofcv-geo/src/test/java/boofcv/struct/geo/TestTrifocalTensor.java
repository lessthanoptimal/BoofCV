/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

import boofcv.testing.BoofStandardJUnit;
import org.ejml.MapPrintFormat;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.NormOps_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TestTrifocalTensor extends BoofStandardJUnit {

	@Test void getT() {
		var t = new TrifocalTensor();
		assertSame(t.T1, t.getT(0));
		assertSame(t.T2, t.getT(1));
		assertSame(t.T3, t.getT(2));
	}

	@Test void set() {
		var t = new TrifocalTensor();
		t.T1.set(0,0,1);
		t.T2.set(0,0,2);
		t.T3.set(0,0,3);

		var a = new TrifocalTensor();
		a.setTo(t);

		assertEquals(a.T1.get(0, 0), 1);
		assertEquals(a.T2.get(0, 0), 2);
		assertEquals(a.T3.get(0, 0), 3);
	}

	@Test void convertFrom() {
		var A = new DMatrixRMaj(27,1);
		for( int i = 0; i < 27; i++ )
			A.set(i,i);

		var t = new TrifocalTensor();
		t.convertFrom(A);

		for( int i = 0; i < 27; i++ )
			assertEquals(t.getT(i/9).get(i%9), i);
	}

	@Test void convertTo() {
		var t = new TrifocalTensor();

		for( int i = 0; i < 27; i++ )
			t.getT( i/9 ).set(i % 9, i);

		var A = new DMatrixRMaj(27,1);
		t.convertTo(A);
		for( int i = 0; i < 27; i++ )
			assertEquals(A.get(i), i);

	}

	@Test void normalizeScale() {
		var t = new TrifocalTensor();

		for( int i = 0; i < 27; i++ )
			t.getT( i/9 ).set(i%9,i);

		var A = new DMatrixRMaj(27,1);
		t.convertTo(A);

		double N = NormOps_DDRM.normF(A);

		t.normalizeScale();
		for( int i = 0; i < 27; i++ )
			assertEquals(A.get(i)/N,t.getT( i/9 ).get(i%9),1e-8);
	}

	@Test void formatMap() {
		var t = new TrifocalTensor();

		for( int i = 0; i < 27; i++ )
			t.getT( i/9 ).set(i%9,i);

		String found = t.formatMap(new MapPrintFormat().withPrecision(2));
		assertEquals("{T1: [{0, 1, 2},\n" +
				"{3, 4, 5},\n" +
				"{6, 7, 8}], T2: [{9, 10, 11},\n" +
				"{12, 13, 14},\n" +
				"{15, 16, 17}], T3: [{18, 19, 20},\n" +
				"{21, 22, 23},\n" +
				"{24, 25, 26}]}", found);
	}
}
