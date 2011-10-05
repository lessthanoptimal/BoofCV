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

package boofcv.abst.feature.tracker;

import boofcv.alg.feature.associate.AssociateSurfBasic;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.feature.orientation.OrientationIntegral;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.SurfFeatureQueue;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_I32;

import java.util.List;


/**
 * Wrapper around SURF features for {@link PointSequentialTracker}.
 *
 * @author Peter Abeles
 */
public class PstWrapperSurf<I extends ImageBase,II extends ImageBase>
	extends DetectAssociateTracker<I,SurfFeature>
{
	II integralImage;

	private FastHessianFeatureDetector<II> detector;
	private OrientationIntegral<II> orientation;
	private DescribePointSurf<II> describe;
	private AssociateSurfBasic assoc;

	public PstWrapperSurf(FastHessianFeatureDetector<II> detector,
						  OrientationIntegral<II> orientation,
						  DescribePointSurf<II> describe,
						  AssociateSurfBasic assoc ,
						  Class<II> integralType ) {
		this.detector = detector;
		this.orientation = orientation;
		this.describe = describe;
		this.assoc = assoc;
		this.integralImage = GeneralizedImageOps.createImage(integralType,1,1);
	}

	@Override
	public void setInputImage(I input) {
		integralImage.reshape(input.width,input.height);
		GIntegralImageOps.transform(input,integralImage);
	}

	@Override
	public FastQueue<SurfFeature> createFeatureDescQueue() {
		return new SurfFeatureQueue(64);
	}

	@Override
	public void detectFeatures(FastQueue<Point2D_I32> location, FastQueue<SurfFeature> description) {
		// detect interest points
		detector.detect(integralImage);
		List<ScalePoint> points = detector.getFoundPoints();

		// extract feature descriptions
		orientation.setImage(integralImage);
		describe.setImage(integralImage);
		SurfFeature tmpFeat = description.pop();
		for( ScalePoint p : points ) {
			orientation.setScale(p.scale);
			double angle = orientation.compute(p.x,p.y);

			SurfFeature feat = describe.describe(p.x,p.y,p.scale,angle,tmpFeat);
			if( feat != null ) {
				location.pop().set(p.x,p.y);
				tmpFeat = description.pop();
			}
		}
		description.removeTail();
	}

	@Override
	public FastQueue<AssociatedIndex> associate(FastQueue<SurfFeature> featSrc, FastQueue<SurfFeature> featDst) {
		assoc.setSrc(featSrc);
		assoc.setDst(featDst);

		assoc.associate();
		return assoc.getMatches();
	}
}
