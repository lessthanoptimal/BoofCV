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

package gecv.alg.geo.d2;

import gecv.alg.geo.AssociatedPair;
import gecv.numerics.fitting.modelset.ModelFitter;
import jgrl.fitting.affine.MotionAffinePoint2D_F32;
import jgrl.struct.affine.Affine2D_F32;
import jgrl.struct.point.Point2D_F32;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Peter Abeles
 */
// todo comment
public class ModelFitterAffine2D implements ModelFitter<Affine2D_F32,AssociatedPair> {

	// model affine fitter
	MotionAffinePoint2D_F32 fitter = new MotionAffinePoint2D_F32();

	// key frame points
	List<Point2D_F32> from = new ArrayList<Point2D_F32>();
	// current frame points
	List<Point2D_F32> to = new ArrayList<Point2D_F32>();

	@Override
	public Affine2D_F32 declareModel() {
		return new Affine2D_F32();
	}

	@Override
	public boolean fitModel(List<AssociatedPair> dataSet,
						 Affine2D_F32 initParam , Affine2D_F32 foundParam) {
		from.clear();
		to.clear();

		for( int i = 0; i < dataSet.size(); i++ ) {
			AssociatedPair p = dataSet.get(i);
			from.add(p.keyLoc);
			to.add(p.currLoc);
		}

		if( !fitter.process(from,to) )
			return false;

		foundParam.set(fitter.getMotion());

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return fitter.getMinimumPoints();
	}
}
