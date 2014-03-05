/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.feature;

import boofcv.misc.BoofMiscOps;
import org.ejml.data.DenseMatrix64F;
import org.junit.After;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * Tests which check to see if specific objects can be serialized or not
 *
 * @author Peter Abeles
 */
public class TestObjectSerialization {

	@After
	public void cleanup() {
		File f = new File("temp.txt");

		if( f.exists() )
			if( !f.delete() )
				throw new RuntimeException("Can't clean up temp.txt");
	}

	@Test
	public void testTupleDesc_F64() {
		TupleDesc_F64 orig = new TupleDesc_F64(20);
		for( int i = 0; i < orig.value.length; i++ ) {
			orig.value[i] = i;
		}

		BoofMiscOps.saveXML(orig,"temp.txt");

		TupleDesc_F64 found = BoofMiscOps.loadXML("temp.txt");

		for( int i = 0; i < orig.value.length; i++ ) {
			assertEquals(orig.value[i],found.value[i],1e-8);
		}
	}

	@Test
	public void testSurfFeature() {
		SurfFeature orig = new SurfFeature(20);
		orig.laplacianPositive = true;
		for( int i = 0; i < orig.value.length; i++ ) {
			orig.value[i] = i;
		}

		BoofMiscOps.saveXML(orig,"temp.txt");

		SurfFeature found = BoofMiscOps.loadXML("temp.txt");

		assertEquals(orig.laplacianPositive,found.laplacianPositive);
		for( int i = 0; i < orig.value.length; i++ ) {
			assertEquals(orig.value[i],found.value[i],1e-8);
		}
	}

	@Test
	public void testNccFeature() {
		NccFeature orig = new NccFeature(20);
		orig.mean = 1.2;
		orig.sigma = 3.4;
		for( int i = 0; i < orig.value.length; i++ ) {
			orig.value[i] = i;
		}

		BoofMiscOps.saveXML(orig,"temp.txt");

		NccFeature found = BoofMiscOps.loadXML("temp.txt");

		assertEquals(orig.mean,found.mean,1e-8);
		assertEquals(orig.sigma,found.sigma,1e-8);
		for( int i = 0; i < orig.value.length; i++ ) {
			assertEquals(orig.value[i],found.value[i],1e-8);
		}
	}

	@Test
	public void testDenseMatrix64F() {
		DenseMatrix64F orig = new DenseMatrix64F(2,3,true,new double[]{1,2,3,4,5,6});

		BoofMiscOps.saveXML(orig,"temp.txt");

		DenseMatrix64F found = BoofMiscOps.loadXML("temp.txt");

		assertEquals(orig.numRows,found.numRows);
		assertEquals(orig.numCols,found.numCols);

		for( int i = 0; i < orig.data.length; i++ ) {
			assertEquals(orig.data[i],found.data[i],1e-8);
		}

	}
}
