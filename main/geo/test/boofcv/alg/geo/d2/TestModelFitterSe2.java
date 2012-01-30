/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.d2;

import boofcv.alg.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestModelFitterSe2 {

	Random rand = new Random(234);

	/**
	 * Provide a simple scenario where its off a bit and see if it converges
	 */
	@Test
	public void basicTest() {
		int N = 10;
		Se2_F64 model = new Se2_F64(1,2,-0.3);

		List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
		for( int i = 0; i < N; i++ ) {
			Point2D_F64 a = new Point2D_F64(rand.nextDouble()*4,rand.nextDouble()*4);
			Point2D_F64 b = SePointOps_F64.transform(model,a,null);

			pairs.add( new AssociatedPair(b,a,false));
		}

		ModelFitterSe2 alg = new ModelFitterSe2();

		Se2_F64 noisyParam = new Se2_F64(model.getX()-0.1,model.getY()+0.1,model.getYaw()+0.01);
		Se2_F64 found = new Se2_F64();

		assertTrue(alg.fitModel(pairs,noisyParam,found));

		assertEquals(model.getX(),found.getX(),0.0001);
		assertEquals(model.getY(),found.getY(),0.0001);
		assertEquals(model.getYaw(),found.getYaw(),0.01);
	}

}
