/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.FastQueue;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;

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
public class DetectDescribeFusion<T extends ImageSingleBand, D extends TupleDesc>
	implements DetectDescribePoint<T,D>
{
	// detects interest points
	private InterestPointDetector<T> detector;
	// optional override for orientation
	private OrientationImage<T> orientation;
	// describes each feature found
	private DescribeRegionPoint<T,D> describe;

	// list of extracted feature descriptors
	private FastQueue<D> descs;

	/**
	 * Configures the algorithm.
	 *
	 * @param detector Feature detector
	 * @param orientation (Optional) orientation estimation algorithm
	 * @param describe Describes features
	 */
	public DetectDescribeFusion(InterestPointDetector<T> detector,
								OrientationImage<T> orientation,
								DescribeRegionPoint<T, D> describe)
	{
		this.describe = describe;
		this.orientation = orientation;
		this.detector = detector;

		final DescribeRegionPoint<T,D> locaDescribe = describe;

		descs = new FastQueue<D>(100,describe.getDescriptorType(),true) {
			protected D createInstance() {
				return locaDescribe.createDescription();
			}
		};
	}

	@Override
	public D createDescription() {
		return describe.createDescription();
	}

	@Override
	public D getDescriptor(int index) {
		return descs.get(index);
	}

	@Override
	public Class<D> getDescriptorType() {
		return describe.getDescriptorType();
	}

	@Override
	public void detect(T input) {
		descs.reset();

		if( orientation != null ) {
			orientation.setImage(input);
		}
		describe.setImage(input);

		detector.detect(input);

		int N = detector.getNumberOfFeatures();

		for( int i = 0; i < N; i++ ) {
			Point2D_F64 p = detector.getLocation(i);
			double scale = detector.getScale(i);
			double yaw = detector.getOrientation(i);

			if( orientation != null ) {
				orientation.setScale(scale);
				yaw = orientation.compute(p.x,p.y);
			}

			if( describe.isInBounds(p.x,p.y,yaw,scale) ) {
				D d = descs.grow();
				describe.process(p.x,p.y,yaw,scale,d);
			}
		}
	}

	@Override
	public int getNumberOfFeatures() {
		return detector.getNumberOfFeatures();
	}

	@Override
	public Point2D_F64 getLocation(int featureIndex) {
		return detector.getLocation(featureIndex);
	}

	@Override
	public double getScale(int featureIndex) {
		return detector.getScale(featureIndex);
	}

	@Override
	public double getOrientation(int featureIndex) {
		return detector.getOrientation(featureIndex);
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
