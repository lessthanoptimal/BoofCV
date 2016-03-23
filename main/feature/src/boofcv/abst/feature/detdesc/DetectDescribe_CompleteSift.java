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

import boofcv.alg.feature.detdesc.CompleteSift;
import boofcv.core.image.GConvertImage;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;

/**
 * Wrapper around {@link CompleteSift} for {@link DetectDescribePoint}.
 *
 * @author Peter Abeles
 */
public class DetectDescribe_CompleteSift<In extends ImageGray>
		implements DetectDescribePoint<In,BrightFeature> {

	CompleteSift alg;
	GrayF32 imageFloat = new GrayF32(1,1);

	public DetectDescribe_CompleteSift(CompleteSift alg) {
		this.alg = alg;
	}

	@Override
	public BrightFeature createDescription() {
		return new BrightFeature(alg.getDescriptorLength());
	}

	@Override
	public BrightFeature getDescription(int index) {
		return alg.getDescriptions().data[index];
	}

	@Override
	public Class<BrightFeature> getDescriptionType() {
		return BrightFeature.class;
	}

	@Override
	public void detect(In input) {
		if( input instanceof GrayF32)
			alg.process((GrayF32)input);
		else {
			imageFloat.reshape(input.width,input.height);
			GConvertImage.convert(input,imageFloat);
			alg.process(imageFloat);
		}
	}

	@Override
	public int getNumberOfFeatures() {
		return alg.getDescriptions().size;
	}

	@Override
	public Point2D_F64 getLocation(int featureIndex) {
		return alg.getLocations().get(featureIndex);
	}

	@Override
	public double getRadius(int featureIndex) {
		return alg.getLocations().get(featureIndex).scale;
	}

	@Override
	public double getOrientation(int featureIndex) {
		return alg.getOrientations().get(featureIndex);
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
