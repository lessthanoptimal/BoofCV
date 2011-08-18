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

package gecv.alg.detect.interest.benchmark;

import gecv.abst.detect.interest.InterestPointDetector;
import gecv.alg.feature.StabilityAlgorithm;
import gecv.alg.feature.StabilityEvaluator;
import gecv.struct.image.ImageBase;
import jgrl.geometry.UtilPoint2D_I32;
import jgrl.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Peter Abeles
 */
public class DetectEvaluator<T extends ImageBase> implements StabilityEvaluator<T> {

	// two points are considered to be the same if they are within this tolerance
	double matchTolerance = 3;

	// location where features were originally detected
	List<Point2D_I32> original;

	@Override
	public String[] getMetricNames() {
		return new String[]{"Matched","Missed","Error"};
	}

	@Override
	public void extractInitial(StabilityAlgorithm alg, T image)
	{
		InterestPointDetector<T> detector = alg.getAlgorithm();
		detector.detect(image);
		original = createDetectionList(detector);
	}

	@Override
	public double[] evaluateImage(StabilityAlgorithm alg, T image)
	{
		InterestPointDetector<T> detector = alg.getAlgorithm();
		detector.detect(image);
		List<Point2D_I32> found = createDetectionList(detector);

		int numMissed = 0;
		double total = 0;

		for( Point2D_I32 origPt : original ) {
			double bestDist = Double.MAX_VALUE;
			for( Point2D_I32 p : found ) {
				double d = UtilPoint2D_I32.distance(origPt,p);

				if( d < bestDist ) {
					bestDist = d;
				}
			}

			if( bestDist > matchTolerance ) {
				numMissed++;
			} else {
				total += bestDist;
			}
		}

		double[] results = new double[3];

		// number that matched
		results[0] = original.size()-numMissed;
		// number that missed
		results[1] = numMissed;
		// error
		results[2] = total/(original.size()-numMissed);


		return results;
	}

	public List<Point2D_I32> createDetectionList( InterestPointDetector det ) {
		List<Point2D_I32> list = new ArrayList<Point2D_I32>();
		for( int i = 0; i < det.getNumberOfFeatures(); i++ ) {
			list.add(det.getLocation(i).copy());
		}
		return list;
	}
}
