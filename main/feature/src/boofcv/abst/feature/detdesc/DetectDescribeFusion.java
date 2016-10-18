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

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;

/**
 * Wrapper class around independent feature detectors, region orientation, and descriptors, that allow
 * them to be used as a single integrated unit. Providing an algorithm for estimating orientation is
 * optional.  If one is provided, any orientation estimate provided by the detector is ignored.
 *
 * @see InterestPointDetector
 * @see OrientationImage
 * @see DescribeRegionPoint
 *
 * @author Peter Abeles
 */
public class DetectDescribeFusion<T extends ImageGray, TD extends TupleDesc>
	implements DetectDescribePoint<T, TD>
{
	// detects interest points
	private InterestPointDetector<T> detector;
	// optional override for orientation
	private OrientationImage<T> orientation;
	// describes each feature found
	private DescribeRegionPoint<T, TD> describe;

	// list of extracted feature descriptors
	private FastQueue<TD> descs;

	// storage for found orientations
	private GrowQueue_F64 featureRadiuses = new GrowQueue_F64(10);
	private GrowQueue_F64 featureAngles = new GrowQueue_F64(10);
	private FastQueue<Point2D_F64> location = new FastQueue<>(10, Point2D_F64.class, false);

	/**
	 * Configures the algorithm.
	 *
	 * @param detector Feature detector
	 * @param orientation (Optional) orientation estimation algorithm
	 * @param describe Describes features
	 */
	public DetectDescribeFusion(InterestPointDetector<T> detector,
								OrientationImage<T> orientation,
								DescribeRegionPoint<T, TD> describe)
	{
		this.describe = describe;
		this.orientation = orientation;
		this.detector = detector;

		final DescribeRegionPoint<T, TD> locaDescribe = describe;

		descs = new FastQueue<TD>(100,describe.getDescriptionType(),true) {
			protected TD createInstance() {
				return locaDescribe.createDescription();
			}
		};
	}

	@Override
	public TD createDescription() {
		return describe.createDescription();
	}

	@Override
	public TD getDescription(int index) {
		return descs.get(index);
	}

	@Override
	public Class<TD> getDescriptionType() {
		return describe.getDescriptionType();
	}

	@Override
	public void detect(T input) {
		descs.reset();
		featureRadiuses.reset();
		featureAngles.reset();
		location.reset();

		if( orientation != null ) {
			orientation.setImage(input);
		}
		describe.setImage(input);

		detector.detect(input);

		int N = detector.getNumberOfFeatures();

		for( int i = 0; i < N; i++ ) {
			Point2D_F64 p = detector.getLocation(i);
			double radius = detector.getRadius(i);
			double yaw = detector.getOrientation(i);

			if( orientation != null ) {
				orientation.setObjectRadius(radius);
				yaw = orientation.compute(p.x,p.y);
			}

			if( describe.process(p.x,p.y,yaw,radius,descs.grow()) ) {
				featureRadiuses.push(radius);
				featureAngles.push(yaw);
				location.add(p);
			} else {
				descs.removeTail();
			}
		}
	}

	@Override
	public int getNumberOfFeatures() {
		return location.size();
	}

	@Override
	public Point2D_F64 getLocation(int featureIndex) {
		return location.get(featureIndex);
	}

	@Override
	public double getRadius(int featureIndex) {
		return featureRadiuses.get(featureIndex);
	}

	@Override
	public double getOrientation(int featureIndex) {
		return featureAngles.get(featureIndex);
	}

	@Override
	public boolean hasScale() {
		return detector.hasScale();
	}

	@Override
	public boolean hasOrientation() {
		if( orientation == null )
			return detector.hasOrientation();
		return true;
	}
}
