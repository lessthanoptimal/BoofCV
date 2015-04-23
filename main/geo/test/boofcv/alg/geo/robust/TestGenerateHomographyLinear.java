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

package boofcv.alg.geo.robust;

import boofcv.struct.geo.AssociatedPair;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.junit.Test;

import java.util.List;
import java.util.Random;


/**
 * @author Peter Abeles
 */
public class TestGenerateHomographyLinear implements ModelTestingInterface<Homography2D_F64,AssociatedPair>
{
	Random rand = new Random(234);

	@Test
	public void fitModel() {
		StandardModelFitterTests<Homography2D_F64,AssociatedPair> alg =
				new StandardModelFitterTests<Homography2D_F64,AssociatedPair>(this,4) {
					@Override
					public ModelFitter<Homography2D_F64,AssociatedPair> createAlg() {
						return new GenerateHomographyLinear(true);
					}
				};

		alg.allTest();
	}

	@Test
	public void modelGenerator() {
		StandardModelGeneratorTests<Homography2D_F64,AssociatedPair> alg =
				new StandardModelGeneratorTests<Homography2D_F64,AssociatedPair>(this,4) {
					@Override
					public ModelGenerator<Homography2D_F64,AssociatedPair> createAlg() {
						return new GenerateHomographyLinear(true);
					}

					@Override
					public Homography2D_F64 createModelInstance() {
						return new Homography2D_F64();
					}
				};

		alg.allTest();
	}

	@Override
	public Homography2D_F64 createRandomModel() {
		Homography2D_F64 model = new Homography2D_F64();
		model.a11 = rand.nextDouble();
		model.a12 = rand.nextDouble();
		model.a13 = rand.nextDouble();
		model.a21 = rand.nextDouble();
		model.a22 = rand.nextDouble();
		model.a23 = rand.nextDouble();
		model.a31 = rand.nextDouble();
		model.a32 = rand.nextDouble();
		model.a33 = rand.nextDouble();

		return model;
	}

	@Override
	public AssociatedPair createRandomPointFromModel(Homography2D_F64 transform) {
		AssociatedPair ret = new AssociatedPair();
		ret.p1.x = rand.nextDouble()*10;
		ret.p1.y = rand.nextDouble()*10;

		HomographyPointOps_F64.transform(transform, ret.p1, ret.p2);

		return ret;
	}

	@Override
	public boolean doPointsFitModel(Homography2D_F64 transform, List<AssociatedPair> dataSet) {
		Point2D_F64 expected = new Point2D_F64();

		for( AssociatedPair p : dataSet ) {
			HomographyPointOps_F64.transform(transform, p.p1, expected);

			if( expected.distance(p.p2) > 0.01 )
				return false;
		}

		return true;
	}
}
