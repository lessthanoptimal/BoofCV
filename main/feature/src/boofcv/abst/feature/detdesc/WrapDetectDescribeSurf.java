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

package boofcv.abst.feature.detdesc;

import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.struct.BoofDefaults;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.SurfFeatureQueue;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.GrowQueue_F64;

import java.util.List;

/**
 * Wrapper around SURF algorithms for {@link DetectDescribePoint}.
 *
 * @link FastHessianFeatureDetector
 * @link OrientationIntegral
 * @link DescribePointSurf
 *
 * @param <T> Input image type
 * @param <II> Integral image type
 *
 * @author Peter Abeles
 */
public class WrapDetectDescribeSurf
		<T extends ImageGray, II extends ImageGray>
	implements DetectDescribePoint<T,BrightFeature>
{
	// SURF algorithms
	private FastHessianFeatureDetector<II> detector;
	private OrientationIntegral<II> orientation;
	private DescribePointSurf<II> describe;

	// storage for integral image
	private II ii;

	// storage for computed features
	private SurfFeatureQueue features;
	// detected scale points
	private List<ScalePoint> foundPoints;
	// orientation of features
	private GrowQueue_F64 featureAngles = new GrowQueue_F64(10);

	public WrapDetectDescribeSurf(FastHessianFeatureDetector<II> detector,
								  OrientationIntegral<II> orientation,
								  DescribePointSurf<II> describe)
	{
		this.detector = detector;
		this.orientation = orientation;
		this.describe = describe;

		features = new SurfFeatureQueue(describe.getDescriptionLength());
	}

	@Override
	public BrightFeature createDescription() {
		return describe.createDescription();
	}

	@Override
	public BrightFeature getDescription(int index) {
		return features.get(index);
	}

	@Override
	public Class<BrightFeature> getDescriptionType() {
		return BrightFeature.class;
	}

	@Override
	public void detect(T input) {
		if( ii != null ) {
			ii.reshape(input.width,input.height);
		}

		// compute integral image
		ii = GIntegralImageOps.transform(input, ii);
		orientation.setImage(ii);
		describe.setImage(ii);
		features.reset();
		featureAngles.reset();

		// detect features
		detector.detect(ii);

		// describe the found interest points
		foundPoints = detector.getFoundPoints();

		for( int i = 0; i < foundPoints.size(); i++ ) {
			ScalePoint p = foundPoints.get(i);
			double radius = p.scale* BoofDefaults.SURF_SCALE_TO_RADIUS;

			orientation.setObjectRadius(radius);
			double angle = orientation.compute(p.x,p.y);
			describe.describe(p.x,p.y, angle, p.scale, features.grow());
			featureAngles.push(angle);
		}
	}

	@Override
	public int getNumberOfFeatures() {
		return foundPoints.size();
	}

	@Override
	public Point2D_F64 getLocation(int featureIndex) {
		return foundPoints.get(featureIndex);
	}

	@Override
	public double getRadius(int featureIndex) {
		return foundPoints.get(featureIndex).scale* BoofDefaults.SURF_SCALE_TO_RADIUS;
	}

	@Override
	public double getOrientation(int featureIndex) {
		return featureAngles.get(featureIndex);
	}

	@Override
	public boolean hasScale() {
		return true;
	}

	@Override
	public boolean hasOrientation() {
		return true;
	}
}
