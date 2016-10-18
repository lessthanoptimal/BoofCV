/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.pose;

import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPositionFromPairLinear2 extends ChecksMotionNPoint {

	PositionFromPairLinear2 alg = new PositionFromPairLinear2();

	@Test
	public void minimalObservationTest() {
		standardTest(2);
		planarTest(2);
	}

	@Test
	public void overdetermined() {
		standardTest(6);
		planarTest(6);
	}

	@Override
	public Se3_F64 compute(List<AssociatedPair> obs, List<Point3D_F64> locations) {
		List<Point2D_F64> l = new ArrayList<>();
		
		for( AssociatedPair p : obs )
			l.add(p.p2);
		
		
		assertTrue(alg.process(motion.getR(), locations, l));
		
		Se3_F64 found = new Se3_F64();
		found.R.set(motion.R);
		found.T.set(alg.getT());

		return found;
	}
}
