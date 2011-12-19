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

package boofcv.alg.geo.d2;

import boofcv.alg.geo.AssociatedPair;
import boofcv.numerics.fitting.modelset.ModelFitter;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.affine.AffinePointOps;

import java.util.List;
import java.util.Random;


/**
 * @author Peter Abeles
 */
public class TestModelFitterAffine2D extends StandardModelFitterTests<Affine2D_F64,AssociatedPair> {

	Random rand = new Random(234);

	public TestModelFitterAffine2D() {
		super(3);
	}

	@Override
	public ModelFitter<Affine2D_F64, AssociatedPair> createAlg() {
		return new ModelFitterAffine2D();
	}

	@Override
	public Affine2D_F64 createRandomModel() {
		Affine2D_F64 model = new Affine2D_F64();
		model.a11 = rand.nextDouble();
		model.a12 = rand.nextDouble();
		model.a21 = rand.nextDouble();
		model.a22 = rand.nextDouble();
		model.tx = rand.nextDouble();
		model.ty = rand.nextDouble();

		return model;
	}

	@Override
	public AssociatedPair createRandomPointFromModel(Affine2D_F64 affine) {
		AssociatedPair ret = new AssociatedPair();
		ret.keyLoc.x = rand.nextDouble()*10;
		ret.keyLoc.y = rand.nextDouble()*10;

		AffinePointOps.transform(affine,ret.keyLoc,ret.currLoc);

		return ret;
	}

	@Override
	public boolean doPointsFitModel(Affine2D_F64 affine, List<AssociatedPair> dataSet) {

		Point2D_F64 expected = new Point2D_F64();

		for( AssociatedPair p : dataSet ) {
			AffinePointOps.transform(affine,p.keyLoc,expected);

			if( expected.distance(p.currLoc) > 0.01 )
				return false;
		}

		return true;
	}
}
