/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package boofcv.alg.geo.d2;

import boofcv.alg.geo.AssociatedPair;
import jgrl.struct.affine.Affine2D_F64;
import jgrl.struct.point.Point2D_F64;
import jgrl.transform.affine.AffinePointOps;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestModelFitterAffine2D {

	Random rand = new Random(234);


	@Test
	public void checkMinPoints() {
		ModelFitterAffine2D fitter = new ModelFitterAffine2D();
		assertEquals(3,fitter.getMinimumPoints());
	}

	/**
	 * Give it points which have been transform by the true affine model.  See
	 * if the transform is correctly estimated
	 */
	@Test
	public void simpleTest() {

		Affine2D_F64 model = new Affine2D_F64(1,2,3,4,5,6);

		List<AssociatedPair> dataSet = new ArrayList<AssociatedPair>();

		// give it perfect observations
		for( int i = 0; i < 10; i++ ) {
			AssociatedPair p = new AssociatedPair();
			p.keyLoc.set(rand.nextDouble(),rand.nextDouble());

			AffinePointOps.transform(model,p.keyLoc,p.currLoc);
			dataSet.add(p);
		}

		ModelFitterAffine2D fitter = new ModelFitterAffine2D();

		Affine2D_F64 found = new Affine2D_F64();
		fitter.fitModel(dataSet,null,found);

		// test the found transform by seeing if it recomputes the current points
		Point2D_F64 a = new Point2D_F64();
		for( int i = 0; i < 10; i++ ) {
			AssociatedPair p = dataSet.get(i);

			AffinePointOps.transform(found,p.keyLoc,a);

			assertTrue(a.isIdentical(p.currLoc,1e-4f));
		}
	}
}
