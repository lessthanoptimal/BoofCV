/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detdesc;

import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.describe.DescribePointSurfPlanar;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;

import java.util.List;

/**
 * Computes a color SURF descriptor from a {@link Planar} image. Features are detected,
 * orientation estimated, and laplacian sign computed using a gray scale image. The gray scale image is found by
 * computing the average across all bands for each pixel. A descriptor is computed inside band individually
 * and stored in a descriptor which is N*length long. N = number of bands and length = number of
 * elements in normal descriptor.
 *
 * @param <II> Type of integral image
 * @author Peter Abeles
 * @see boofcv.alg.feature.describe.DescribePointSurfPlanar
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectDescribeSurfPlanar<II extends ImageGray<II>> {
	// SURF algorithms
	protected FastHessianFeatureDetector<II> detector;
	protected OrientationIntegral<II> orientation;
	protected DescribePointSurfPlanar<II> describe;

	// storage for computed features
	protected DogArray<TupleDesc_F64> descriptions;
	// detected scale points
	protected List<ScalePoint> foundPoints;
	// orientation of features
	protected DogArray_F64 featureAngles = new DogArray_F64(10);

	public DetectDescribeSurfPlanar( FastHessianFeatureDetector<II> detector,
									 OrientationIntegral<II> orientation,
									 DescribePointSurfPlanar<II> describe ) {
		this.detector = detector;
		this.orientation = orientation;
		this.describe = describe;

		descriptions = new DogArray<>(() -> new TupleDesc_F64(describe.getDescriptorLength()));
	}

	public TupleDesc_F64 createDescription() {
		return describe.createDescription();
	}

	public TupleDesc_F64 getDescription( int index ) {
		return descriptions.get(index);
	}

	public boolean isWhite( int index ) {
		return detector.getFoundFeatures().get(index).white;
	}

	/**
	 * Detects and describes features inside provide images. All images are integral images.
	 *
	 * @param grayII Gray-scale integral image
	 * @param colorII Color integral image
	 */
	public void detect( II grayII, Planar<II> colorII ) {

		descriptions.reset();
		featureAngles.reset();

		// detect features
		detector.detect(grayII);

		// describe the found interest points
		foundPoints = detector.getFoundFeatures();

		descriptions.resize(foundPoints.size());
		featureAngles.resize(foundPoints.size());

		describe(grayII, colorII);
	}

	protected void describe( II grayII, Planar<II> colorII ) {
		orientation.setImage(grayII);
		describe.setImage(grayII, colorII);
		for (int i = 0; i < foundPoints.size(); i++) {
			ScalePoint p = foundPoints.get(i);
			orientation.setObjectRadius(p.scale);
			double angle = orientation.compute(p.pixel.x, p.pixel.y);

			describe.describe(p.pixel.x, p.pixel.y, angle, p.scale, descriptions.get(i));

			featureAngles.set(i, angle);
		}
	}

	public DescribePointSurfPlanar<II> getDescribe() {
		return describe;
	}

	public int getNumberOfFeatures() {
		return foundPoints.size();
	}

	public Point2D_F64 getLocation( int featureIndex ) {
		return foundPoints.get(featureIndex).pixel;
	}

	public double getRadius( int featureIndex ) {
		return foundPoints.get(featureIndex).scale;
	}

	public double getOrientation( int featureIndex ) {
		return featureAngles.get(featureIndex);
	}
}
