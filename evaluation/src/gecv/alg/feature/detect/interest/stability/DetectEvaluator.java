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

package gecv.alg.feature.detect.interest.stability;

import gecv.abst.feature.detect.interest.InterestPointDetector;
import gecv.alg.feature.benchmark.StabilityAlgorithm;
import gecv.alg.feature.benchmark.StabilityEvaluator;
import gecv.struct.image.ImageBase;
import jgrl.geometry.UtilPoint2D_I32;
import jgrl.struct.affine.Affine2D_F32;
import jgrl.struct.point.Point2D_F32;
import jgrl.struct.point.Point2D_I32;
import jgrl.transform.affine.AffinePointOps;

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
		return new String[]{"Found","Percent"};
	}

	@Override
	public void extractInitial(StabilityAlgorithm alg, T image)
	{
		InterestPointDetector<T> detector = alg.getAlgorithm();
		detector.detect(image);
		original = createDetectionList(detector);
	}

	@Override
	public double[] evaluateImage(StabilityAlgorithm alg, T image, Affine2D_F32 initToImage)
	{
		// move the found points to the new coordinate system or just use the
		List<Point2D_I32> original = initToImage == null ? this.original : transformOriginal(image,initToImage);

		InterestPointDetector<T> detector = alg.getAlgorithm();
		detector.detect(image);
		List<Point2D_I32> found = createDetectionList(detector);

		int numMissed = 0;

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
			}
		}

		double[] results = new double[3];

		// number found
		results[0] = original.size();
		// fraction matched
		results[1] = 100.0*(original.size()-numMissed)/(double)original.size();

		return results;
	}

	private List<Point2D_I32> transformOriginal( T image , Affine2D_F32 initToImage )
	{
		List<Point2D_I32> ret = new ArrayList<Point2D_I32>();

		Point2D_F32 f = new Point2D_F32();
		Point2D_F32 t = new Point2D_F32();

		for( Point2D_I32 p : original ) {
			f.x = p.x;
			f.y = p.y;
			AffinePointOps.transform(initToImage,f,t);
			if( image.isInBounds((int)t.x,(int)t.y) ) {
				ret.add( new Point2D_I32((int)t.x,(int)t.y));
			}
		}
		return ret;
	}

	public List<Point2D_I32> createDetectionList( InterestPointDetector det ) {
		List<Point2D_I32> list = new ArrayList<Point2D_I32>();
		for( int i = 0; i < det.getNumberOfFeatures(); i++ ) {
			list.add(det.getLocation(i).copy());
		}
		return list;
	}
}
