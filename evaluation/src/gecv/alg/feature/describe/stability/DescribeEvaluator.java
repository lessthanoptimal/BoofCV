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

package gecv.alg.feature.describe.stability;

import gecv.abst.feature.describe.ExtractFeatureDescription;
import gecv.abst.feature.detect.interest.InterestPointDetector;
import gecv.alg.feature.benchmark.StabilityAlgorithm;
import gecv.alg.feature.benchmark.StabilityEvaluatorPoint;
import gecv.evaluation.ErrorStatistics;
import gecv.struct.feature.TupleDesc_F64;
import gecv.struct.image.ImageBase;
import jgrl.struct.affine.Affine2D_F32;
import jgrl.struct.point.Point2D_I32;
import jgrl.struct.point.Vector2D_F32;
import jgrl.transform.affine.AffinePointOps;

import java.util.ArrayList;
import java.util.List;


/**
 * Computes error metrics for feature descriptions.  These errors can not be used to compare the
 * quality of one description against another but should be viewed as a way to see if a
 * descriptor is invariant against different types of distortion and its noise sensitivity.
 *
 * @author Peter Abeles
 */
public class DescribeEvaluator <T extends ImageBase>
	extends StabilityEvaluatorPoint<T>
{
	List<TupleDesc_F64> initial = new ArrayList<TupleDesc_F64>();
	ErrorStatistics errors = new ErrorStatistics(500);

	public DescribeEvaluator(int borderSize, InterestPointDetector<T> detector) {
		super(borderSize, detector);
	}

	@Override
	public void extractInitial(StabilityAlgorithm alg, T image, List<Point2D_I32> points)
	{
		initial.clear();

		ExtractFeatureDescription<T> extract = alg.getAlgorithm();
		extract.setImage(image);

		for( Point2D_I32 p : points ) {
			TupleDesc_F64 f = extract.process(p.x,p.y,1.0,null);
			if( f == null )
				initial.add(f);
			else
				initial.add(f.copy());
		}
	}

	@Override
	public double[] evaluateImage(StabilityAlgorithm alg, T image,
							   Affine2D_F32 initToImage,
							   List<Point2D_I32> points, List<Integer> indexes)
	{
		ExtractFeatureDescription<T> extract = alg.getAlgorithm();
		extract.setImage(image);

		double scale = 1;
		if( initToImage != null ) {
			Vector2D_F32 v1 = new Vector2D_F32(1,1);
			Vector2D_F32 v2 = new Vector2D_F32();

			AffinePointOps.transform(initToImage,v1,v2);
			scale = v2.norm()/v1.norm();
		}


		// compute the median error
		errors.reset();
		for( int i = 0; i < points.size(); i++ ) {
			Point2D_I32 p = points.get(i);
			int index = indexes != null ? indexes.get(i) : i;

			TupleDesc_F64 f = extract.process(p.x,p.y,scale,null);
			TupleDesc_F64 e = initial.get(index);
			if( f != null && e != null ) {
				double error = 0;
				for( int j = 0; j < f.value.length; j++ ) {
					error += Math.abs(f.value[j] - e.value[j]);
				}
				errors.add(error/f.value.length);
			}
		}

		double p50 = errors.getFraction(0.5);
		double p90 = errors.getFraction(0.9);

		return new double[]{p50*30,p90*30};
	}

	@Override
	public String[] getMetricNames() {
		return new String[]{"50% * 30","90% *30"};
	}
}
