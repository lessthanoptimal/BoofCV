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

package boofcv.alg.geo.robust;

import boofcv.struct.geo.AssociatedPair;
import georegression.fitting.affine.MotionAffinePoint2D_F64;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelGenerator;

import java.util.ArrayList;
import java.util.List;


/**
 * Fits an {@link Affine2D_F64} motion model to a list of {@link AssociatedPair}.
 *
 * @author Peter Abeles
 */
public class GenerateAffine2D implements
		ModelGenerator<Affine2D_F64,AssociatedPair>,
		ModelFitter<Affine2D_F64,AssociatedPair>
{

	// model affine fitter
	MotionAffinePoint2D_F64 fitter = new MotionAffinePoint2D_F64();

	// key frame points
	List<Point2D_F64> from = new ArrayList<>();
	// current frame points
	List<Point2D_F64> to = new ArrayList<>();

	@Override
	public boolean fitModel(List<AssociatedPair> dataSet, Affine2D_F64 initial, Affine2D_F64 found) {
		from.clear();
		to.clear();

		for( int i = 0; i < dataSet.size(); i++ ) {
			AssociatedPair p = dataSet.get(i);
			from.add(p.p1);
			to.add(p.p2);
		}

		if( !fitter.process(from,to) )
			return false;

		found.set(fitter.getTransformSrcToDst());
		return true;
	}

	@Override
	public boolean generate(List<AssociatedPair> dataSet, Affine2D_F64 model ) {

		from.clear();
		to.clear();

		for( int i = 0; i < dataSet.size(); i++ ) {
			AssociatedPair p = dataSet.get(i);
			from.add(p.p1);
			to.add(p.p2);
		}

		if( !fitter.process(from,to) )
			return false;

		model.set(fitter.getTransformSrcToDst());

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return fitter.getMinimumPoints();
	}
}
