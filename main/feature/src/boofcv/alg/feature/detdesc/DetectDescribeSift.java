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

package boofcv.alg.feature.detdesc;

import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.detect.interest.SiftDetector;
import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.SurfFeatureQueue;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;

/**
 * SIFT where detection, orientation estimation, and describing are done all at once.  This
 * allows the image pyramid to only be computed once.
 *
 * @see OrientationHistogramSift
 * @see SiftDetector
 * @see DescribePointSift
 * @see SiftImageScaleSpace
 *
 * @author Peter Abeles
 */
public class DetectDescribeSift {

	// computes the image's scale-space
	protected SiftImageScaleSpace ss;

	// core SIFT algorithms
	protected SiftDetector detector;
	protected OrientationHistogramSift orientation;
	protected DescribePointSift describe;

	// storage for features and their attributes
	protected SurfFeatureQueue features;
	protected GrowQueue_F64 featureScales;
	protected GrowQueue_F64 featureAngles;
	protected FastQueue<Point2D_F64> location;

	public DetectDescribeSift(SiftImageScaleSpace ss,
							  SiftDetector detector,
							  OrientationHistogramSift orientation,
							  DescribePointSift describe)
	{
		this.ss = ss;
		this.detector = detector;
		this.orientation = orientation;
		this.describe = describe;

		features = new SurfFeatureQueue(describe.getDescriptorLength());
		featureScales = new GrowQueue_F64(100);
		featureAngles = new GrowQueue_F64(100);
		location = new FastQueue<Point2D_F64>(100,Point2D_F64.class,true);
	}

	/**
	 * Processes the image and extracts SIFT features
	 *
	 * @param input input image
	 */
	public void process( ImageFloat32 input ) {

		features.reset();
		featureScales.reset();
		featureAngles.reset();
		location.reset();

		ss.constructPyramid(input);
		ss.computeFeatureIntensity();
		ss.computeDerivatives();

		detector.process(ss);
		orientation.setScaleSpace(ss);
		describe.setScaleSpace(ss);

		FastQueue<ScalePoint> found = detector.getFoundPoints();

		for( int i = 0; i < found.size; i++ ) {
			ScalePoint sp = found.data[i];
			orientation.process(sp.x,sp.y,sp.scale);

			GrowQueue_F64 angles = orientation.getOrientations();

			int imageIndex = orientation.getImageIndex();
			double pixelScale = orientation.getPixelScale();

			for( int j = 0; j < angles.size; j++ ) {
				SurfFeature desc = features.grow();

				double yaw = angles.data[j];

				describe.process(sp.x,sp.y,sp.scale,yaw,imageIndex,pixelScale,desc);

				desc.laplacianPositive = sp.white;
				featureScales.push(sp.scale);
				featureAngles.push(yaw);
				location.grow().set(sp.x,sp.y);
			}
		}
	}

	public int getDescriptorLength() {
		return describe.getDescriptorLength();
	}

	public SurfFeatureQueue getFeatures() {
		return features;
	}

	public GrowQueue_F64 getFeatureScales() {
		return featureScales;
	}

	public GrowQueue_F64 getFeatureAngles() {
		return featureAngles;
	}

	public FastQueue<Point2D_F64> getLocation() {
		return location;
	}
}
