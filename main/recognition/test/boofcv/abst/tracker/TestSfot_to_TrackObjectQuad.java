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

package boofcv.abst.tracker;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.alg.tracker.sfot.SparseFlowObjectTracker;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
public class TestSfot_to_TrackObjectQuad extends TextureGrayTrackerObjectRectangleTests {

	@Override
	public TrackerObjectQuad<GrayU8> create(ImageType<GrayU8> imageType) {

		Class ct = ImageDataType.typeToSingleClass(imageType.getDataType());
		Class dt = GImageDerivativeOps.getDerivativeType(ct);

		ImageGradient gradient = FactoryDerivative.sobel(ct, dt);
		SfotConfig config = new SfotConfig();

		SparseFlowObjectTracker tracker = new SparseFlowObjectTracker(config,ct,dt,gradient);

		return new Sfot_to_TrackObjectQuad(tracker,ct);
	}

}
