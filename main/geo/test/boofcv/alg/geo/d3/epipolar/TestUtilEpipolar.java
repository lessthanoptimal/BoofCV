/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.geo.d3.epipolar;

import boofcv.alg.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestUtilEpipolar {

	Random rand = new Random(234234);

	@Test
	public void computeNormalization() {

		List<AssociatedPair> list = new ArrayList<AssociatedPair>();
		for( int i = 0; i < 12; i++ ) {
			AssociatedPair p = new AssociatedPair();

			p.currLoc.set(rand.nextDouble()*5,rand.nextDouble()*5);
			p.keyLoc.set(rand.nextDouble() * 5, rand.nextDouble() * 5);

			list.add(p);
		}

		// compute statistics
		double meanX0 = 0;
		double meanY0 = 0;
		double meanX1 = 0;
		double meanY1 = 0;

		for( AssociatedPair p : list ) {
			meanX0 += p.keyLoc.x;
			meanY0 += p.keyLoc.y;
			meanX1 += p.currLoc.x;
			meanY1 += p.currLoc.y;
		}

		meanX0 /= list.size();
		meanY0 /= list.size();
		meanX1 /= list.size();
		meanY1 /= list.size();

		double sigmaX0 = 0;
		double sigmaY0 = 0;
		double sigmaX1 = 0;
		double sigmaY1 = 0;

		for( AssociatedPair p : list ) {
			sigmaX0 += Math.pow(p.keyLoc.x-meanX0,2);
			sigmaY0 += Math.pow(p.keyLoc.y-meanY0,2);
			sigmaX1 += Math.pow(p.currLoc.x-meanX1,2);
			sigmaY1 += Math.pow(p.currLoc.y-meanY1,2);
		}

		sigmaX0 = Math.sqrt(sigmaX0/list.size());
		sigmaY0 = Math.sqrt(sigmaY0/list.size());
		sigmaX1 = Math.sqrt(sigmaX1/list.size());
		sigmaY1 = Math.sqrt(sigmaY1/list.size());

		// test the output
		DenseMatrix64F N1 = new DenseMatrix64F(3,3);
		DenseMatrix64F N2 = new DenseMatrix64F(3,3);

		UtilEpipolar.computeNormalization(N1,N2,list);

		assertEquals(1/sigmaX0, N1.get(0,0),1e-8);
		assertEquals(1/sigmaY0, N1.get(1,1),1e-8);
		assertEquals(-meanX0/sigmaX0, N1.get(0,2),1e-8);
		assertEquals(-meanY0/sigmaY0, N1.get(1,2),1e-8);
		assertEquals(1, N1.get(2,2),1e-8);

		assertEquals(1/sigmaX1, N2.get(0,0),1e-8);
		assertEquals(1/sigmaY1, N2.get(1,1),1e-8);
		assertEquals(-meanX1/sigmaX1, N2.get(0,2),1e-8);
		assertEquals(-meanY1/sigmaY1, N2.get(1,2),1e-8);
		assertEquals(1, N2.get(2,2),1e-8);
	}

	/**
	 * Test it against a simple test case
	 */
	@Test
	public void normalize() {
		DenseMatrix64F N = new DenseMatrix64F(3,3,true,1,2,3,4,5,6,7,8,9);

		Point2D_F64 a = new Point2D_F64(3,4);
		Point2D_F64 found = new Point2D_F64(3,4);
		Point2D_F64 expected = new Point2D_F64(3,4);

		expected.x = a.x * N.get(0,0) + N.get(0,2);
		expected.y = a.y * N.get(1,1) + N.get(1,2);


		UtilEpipolar.pixelToNormalized(a, found, N);

		assertEquals(found.x,expected.x,1e-8);
		assertEquals(found.y,expected.y,1e-8);
	}
}
