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

package boofcv.alg.feature.describe.stability;

import boofcv.abst.feature.describe.ExtractFeatureDescription;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.benchmark.BenchmarkAlgorithm;
import boofcv.alg.feature.benchmark.distort.StabilityEvaluatorPoint;
import boofcv.alg.feature.orientation.OrientationNoGradient;
import boofcv.evaluation.ErrorStatistics;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_I32;

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

	// estimates feature orientation
	OrientationNoGradient<T> orientationAlg;
	// saved feature orientation in first frame
	double theta[];

	public DescribeEvaluator(int borderSize,
							 InterestPointDetector<T> detector,
							 OrientationNoGradient<T> orientationAlg ) {
		super(borderSize, detector);
		this.orientationAlg = orientationAlg;
	}

	@Override
	public void extractInitial(BenchmarkAlgorithm alg, T image, List<Point2D_I32> points)
	{
		initial.clear();

		ExtractFeatureDescription<T> extract = alg.getAlgorithm();
		extract.setImage(image);

		if( theta == null || theta.length < points.size() )
			theta = new double[ points.size() ];

		// extract initial feature description and angle
		orientationAlg.setImage(image);
		orientationAlg.setScale(1);
		for( int i = 0; i < points.size(); i++ ) {
			Point2D_I32 p = points.get(i);
			theta[i] = orientationAlg.compute(p.x,p.y);
			TupleDesc_F64 f = extract.process(p.x,p.y,0,1.0,null);
			if( f == null )
				initial.add(f);
			else
				initial.add(f.copy());
		}
	}

	@Override
	public double[] evaluateImage(BenchmarkAlgorithm alg, T image,
							   double scale , double theta ,
							   List<Point2D_I32> points, List<Integer> indexes)
	{
		ExtractFeatureDescription<T> extract = alg.getAlgorithm();
		extract.setImage(image);

		// compute the median error
		errors.reset();
		for( int i = 0; i < points.size(); i++ ) {
			Point2D_I32 p = points.get(i);
			int index =  indexes.get(i);

			// the feature's orientation is the orientation in the initial frame + the rotation
			TupleDesc_F64 f = extract.process(p.x,p.y,this.theta[index]+theta,scale,null);
			TupleDesc_F64 e = initial.get(index);

			if( f != null && e != null ) {
				// normalize the error based on the magnitude of the descriptor in the first frame
				// this adjusts the error for descriptor length and magnitude
				double errorNorm = errorNorm(f,e);
				double initNorm = norm(e);
				errors.add( errorNorm/initNorm );
			}
		}

		double p50 = errors.getFraction(0.5);
		double p90 = errors.getFraction(0.9);

		return new double[]{p50*10,p90*10};
	}

	private double norm( TupleDesc_F64 desc ) {
		double total = 0;
		for( int i = 0; i < desc.value.length; i++ ) {
			total += desc.value[i]*desc.value[i];
		}
		return Math.sqrt(total);
	}

	private double errorNorm( TupleDesc_F64 a , TupleDesc_F64 b  ) {
		double total = 0;
		for( int i = 0; i < a.value.length; i++ ) {
			double error = a.value[i] - b.value[i];
			total += error*error;
		}
		return Math.sqrt(total);
	}

	@Override
	public String[] getMetricNames() {
		return new String[]{"50% * 10","90% * 10"};
	}
}
