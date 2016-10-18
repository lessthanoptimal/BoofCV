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
import boofcv.abst.feature.detect.interest.DetectorInterestPointMulti;
import boofcv.abst.feature.detect.interest.FoundPointSO;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

/**
 * Wrapper class around independent multi feature detectors, region orientation, and descriptors, that allow
 * them to be used as a single integrated unit. Providing an algorithm for estimating orientation is
 * optional.  If one is provided, any orientation estimate provided by the detector is ignored.
 *
 * @see boofcv.abst.feature.detect.interest.InterestPointDetector
 * @see OrientationImage
 * @see DescribeRegionPoint
 *
 * @author Peter Abeles
 */
public class DetectDescribeMultiFusion<T extends ImageGray, TD extends TupleDesc>
		implements DetectDescribeMulti<T,TD> {

	// feature detector
	private DetectorInterestPointMulti<T> detector;
	// optional override for orientation
	private OrientationImage<T> orientation;
	// describes each feature found
	private DescribeRegionPoint<T,TD> describe;

	// storage for each set
	private SetInfo<TD> info[];

	public DetectDescribeMultiFusion(DetectorInterestPointMulti<T> detector,
									 OrientationImage<T> orientation,
									 DescribeRegionPoint<T, TD> describe) {
		this.detector = detector;
		this.orientation = orientation;
		this.describe = describe;

		info = new SetInfo[ detector.getNumberOfSets() ];
		for( int i = 0; i < info.length; i++ ) {
			info[i] = new SetInfo<>();
			info[i].descriptors = UtilFeature.createQueue(describe,10);
		}
	}

	@Override
	public void process(T image) {
		// detect features and setup describe and orientation
		detector.detect(image);

		describe.setImage(image);
		if( orientation != null )
			orientation.setImage(image);

		// go through each set of features
		for( int i = 0; i < info.length; i++ ) {
			FoundPointSO points = detector.getFeatureSet(i);
			SetInfo<TD> setInfo = info[i];
			setInfo.reset();

			// describe each detected feature
			for( int j = 0; j < points.getNumberOfFeatures(); j++ ) {
				Point2D_F64 p = points.getLocation(j);
				double radius = points.getRadius(j);
				double ori = points.getOrientation(j);

				if( orientation != null ) {
					orientation.setObjectRadius(radius);
					ori = orientation.compute(p.x,p.y);
				}

				TD d = setInfo.descriptors.grow();

				if( describe.process(p.x,p.y,ori,radius,d)) {
					setInfo.location.grow().set(p);
				} else {
					setInfo.descriptors.removeTail();
				}
			}
		}
	}

	@Override
	public int getNumberOfSets() {
		return info.length;
	}

	@Override
	public PointDescSet<TD> getFeatureSet(int set) {
		return info[set];
	}

	@Override
	public TD createDescription() {
		return describe.createDescription();
	}

	@Override
	public Class<TD> getDescriptionType() {
		return describe.getDescriptionType();
	}

	/**
	 * Storage for a set
	 */
	private class SetInfo<TD extends TupleDesc> implements PointDescSet<TD>
	{
		FastQueue<Point2D_F64> location = new FastQueue<>(10, Point2D_F64.class, true);
		FastQueue<TD> descriptors;

		public void reset() {
			location.reset();
			descriptors.reset();
		}

		@Override
		public int getNumberOfFeatures() {
			return location.size;
		}

		@Override
		public Point2D_F64 getLocation(int featureIndex) {
			return location.get(featureIndex);
		}

		@Override
		public TD getDescription(int index) {
			return descriptors.get(index);
		}
	}
}
