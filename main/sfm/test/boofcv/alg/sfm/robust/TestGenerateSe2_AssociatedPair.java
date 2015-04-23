/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.robust;

import boofcv.alg.geo.robust.GenerateSe2_AssociatedPair;
import boofcv.alg.geo.robust.ModelTestingInterface;
import boofcv.alg.geo.robust.StandardModelGeneratorTests;
import boofcv.struct.geo.AssociatedPair;
import georegression.fitting.MotionTransformPoint;
import georegression.fitting.se.MotionSe2PointSVD_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.junit.Test;

import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestGenerateSe2_AssociatedPair implements ModelTestingInterface<Se2_F64,AssociatedPair>
{
	Random rand = new Random(234);

	@Test
	public void modelGenerator() {
		StandardModelGeneratorTests<Se2_F64,AssociatedPair> alg =
				new StandardModelGeneratorTests<Se2_F64,AssociatedPair>(this,3) {
					@Override
					public ModelGenerator<Se2_F64,AssociatedPair> createAlg() {
						MotionTransformPoint<Se2_F64, Point2D_F64> alg = new MotionSe2PointSVD_F64();
						return new GenerateSe2_AssociatedPair(alg);
					}

					@Override
					public Se2_F64 createModelInstance() {
						return new Se2_F64();
					}
				};

		alg.checkMinPoints();
		alg.simpleTest();
	}

	@Override
	public Se2_F64 createRandomModel() {

		double x = rand.nextDouble()*5;
		double y = rand.nextDouble()*5;
		double yaw = 2*rand.nextDouble()*Math.PI;

		return new Se2_F64(x,y,yaw);
	}

	@Override
	public AssociatedPair createRandomPointFromModel(Se2_F64 motion) {
		Point2D_F64 location = new Point2D_F64(rand.nextGaussian(),rand.nextGaussian());
		Point2D_F64 observation = new Point2D_F64();

		SePointOps_F64.transform(motion, location, observation);

		return new AssociatedPair(location,observation);
	}

	@Override
	public boolean doPointsFitModel(Se2_F64 motion, List<AssociatedPair> dataSet) {

		Point2D_F64 expected = new Point2D_F64();

		for( AssociatedPair p : dataSet ) {
			SePointOps_F64.transform(motion, p.p1, expected);

			if( expected.distance(p.p2) > 0.01 )
				return false;
		}

		return true;
	}

}
