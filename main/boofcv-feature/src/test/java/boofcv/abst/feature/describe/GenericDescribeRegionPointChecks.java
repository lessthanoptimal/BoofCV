/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.describe;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
abstract class GenericDescribeRegionPointChecks {

	protected Random rand = new Random(234);
	protected abstract DescribeRegionPoint<GrayF32,TupleDesc_F64> createAlg();

	/**
	 * A course check to see if it respects the radius size parameter. just checks to see if the descriptor changes
	 * when it hits a certain size
	 */
	@Test
	void radiusSize() {
		GrayF32 image = new GrayF32(100,120);

		int cx = 45;
		int cy = 48;
		int radius = 20;
		int aradius = radius+3;

		ImageMiscOps.fill(image,255);
		ImageMiscOps.fill(image.subimage(cx-aradius,cy-aradius,cx+aradius+1,cy+aradius+1),50);
//		image.set(cx,cy,100); // give it some texture so it doesn't amplify noise

		DescribeRegionPoint<GrayF32,TupleDesc_F64> describe = createAlg();
		describe.setImage(image);

		TupleDesc_F64 t1 = (TupleDesc_F64)describe.createDescription();
		describe.process(cx,cy,0,radius,t1);
		TupleDesc_F64 t2 = (TupleDesc_F64)describe.createDescription();

		// Moving by 1 pixels should make no difference
		describe.process(cx+1,cy,0,radius,t2);
		checkEquals(t1,t2);

		// Move it so that the feature's edge goes past the edge
		describe.process(cx+4,cy,0,radius,t2);
		checkNotEquals(t1,t2);
	}

	void checkEquals( TupleDesc_F64 a , TupleDesc_F64 b ) {
		assertEquals(a.size(),b.size());

		int totalMissMatches = 0;
		for (int i = 0; i < a.size(); i++) {
			if( Math.abs(a.getDouble(i)-b.getDouble(i)) > 0.1 ) {
				totalMissMatches++;
			}
		}
		assertTrue(totalMissMatches < a.size()/8);
	}

	void checkNotEquals( TupleDesc_F64 a , TupleDesc_F64 b ) {
		assertEquals(a.size(),b.size());
		int totalMissMatches = 0;
		for (int i = 0; i < a.size(); i++) {
			if( Math.abs(a.getDouble(i)-b.getDouble(i)) > UtilEjml.EPS ) {
				totalMissMatches++;
			}
		}
		assertTrue(totalMissMatches > a.size()/8);
	}
}