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

package boofcv.abst.feature.detdesc;

import boofcv.alg.feature.detdesc.DetectDescribeSift;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

/**
 * Wrapper around {@link DetectDescribeSift} for {@link DetectDescribePoint}.
 *
 * @author Peter Abeles
 */
public class WrapDetectDescribeSift implements DetectDescribePoint<ImageFloat32,SurfFeature> {

	DetectDescribeSift alg;

	public WrapDetectDescribeSift(DetectDescribeSift alg) {
		this.alg = alg;
	}

	@Override
	public SurfFeature createDescription() {
		return new SurfFeature(alg.getDescriptorLength());
	}

	@Override
	public SurfFeature getDescription(int index) {
		return alg.getFeatures().data[index];
	}

	@Override
	public Class<SurfFeature> getDescriptionType() {
		return SurfFeature.class;
	}

	@Override
	public void detect(ImageFloat32 input) {
		alg.process(input);
	}

	@Override
	public int getNumberOfFeatures() {
		return alg.getFeatures().size;
	}

	@Override
	public Point2D_F64 getLocation(int featureIndex) {
		return alg.getLocation().get(featureIndex);
	}

	@Override
	public double getScale(int featureIndex) {
		return alg.getFeatureScales().get(featureIndex);
	}

	@Override
	public double getOrientation(int featureIndex) {
		return alg.getFeatureAngles().get(featureIndex);
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
