/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.benchmark.feature.detect;

import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.benchmark.feature.BenchmarkAlgorithm;
import boofcv.benchmark.feature.distort.DistortParam;
import boofcv.benchmark.feature.distort.StabilityEvaluator;
import boofcv.benchmark.feature.distort.StabilityEvaluatorPoint;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.affine.AffinePointOps_F64;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Peter Abeles
 */
public class DetectEvaluator<T extends ImageSingleBand> implements StabilityEvaluator<T> {

	// two points are considered to be the same if they are within this tolerance
	double matchTolerance = 3;

	// location where features were originally detected
	List<Point2D_F64> original;

	@Override
	public String[] getMetricNames() {
		return new String[]{"Found","Percent"};
	}

	@Override
	public void extractInitial(BenchmarkAlgorithm alg, T image)
	{
		InterestPointDetector<T> detector = alg.getAlgorithm();
		detector.detect(image);
		original = createDetectionList(detector);
	}

	@Override
	public double[] evaluateImage(BenchmarkAlgorithm alg, T image, DistortParam param )
	{
		Affine2D_F64 initToImage = StabilityEvaluatorPoint.createTransform(param.scale,param.rotation,image.width,image.height);

		// move the found points to the new coordinate system or just use the
		List<Point2D_F64> original = transformOriginal(image,initToImage);

		InterestPointDetector<T> detector = alg.getAlgorithm();
		detector.detect(image);
		List<Point2D_F64> found = createDetectionList(detector);

		int numMissed = 0;

		for( Point2D_F64 origPt : original ) {
			double bestDist = Double.MAX_VALUE;
			for( Point2D_F64 p : found ) {
				double d = origPt.distance(p);

				if( d < bestDist ) {
					bestDist = d;
				}
			}

			if( bestDist > matchTolerance ) {
				numMissed++;
			}
		}

		double[] results = new double[3];

		// number found
		results[0] = original.size();
		// fraction matched
		results[1] = 100.0*(original.size()-numMissed)/(double)original.size();

		return results;
	}

	private List<Point2D_F64> transformOriginal( T image , Affine2D_F64 initToImage )
	{
		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();

		Point2D_F64 t = new Point2D_F64();

		for( Point2D_F64 f : original ) {
			AffinePointOps_F64.transform(initToImage, f, t);
			if( image.isInBounds((int)t.x,(int)t.y) ) {
				ret.add( new Point2D_F64(t.x,t.y));
			}
		}
		return ret;
	}

	public List<Point2D_F64> createDetectionList( InterestPointDetector det ) {
		List<Point2D_F64> list = new ArrayList<Point2D_F64>();
		for( int i = 0; i < det.getNumberOfFeatures(); i++ ) {
			list.add(det.getLocation(i).copy());
		}
		return list;
	}
}
