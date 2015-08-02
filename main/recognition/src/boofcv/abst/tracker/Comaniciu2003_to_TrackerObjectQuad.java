/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.tracker.meanshift.TrackerMeanShiftComaniciu2003;
import boofcv.struct.RectangleRotate_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.shapes.Quadrilateral_F64;

/**
 * Wrapper around {@link TrackerMeanShiftComaniciu2003} for {@link TrackerObjectQuad}
 *
 * @author Peter Abeles
 */
public class Comaniciu2003_to_TrackerObjectQuad<T extends ImageBase>
		implements TrackerObjectQuad<T>
{
	TrackerMeanShiftComaniciu2003<T> alg;

	RectangleRotate_F32 rectangle = new RectangleRotate_F32();

	ImageType<T> type;

	public Comaniciu2003_to_TrackerObjectQuad(TrackerMeanShiftComaniciu2003<T> alg, ImageType<T> type) {
		this.alg = alg;
		this.type = type;
	}

	@Override
	public boolean initialize(T image, Quadrilateral_F64 location) {

		Sfot_to_TrackObjectQuad.quadToRectRot(location,rectangle);

		alg.initialize(image,rectangle);

		return true;
	}

	@Override
	public boolean process(T image, Quadrilateral_F64 location) {

		alg.track(image);

		Sfot_to_TrackObjectQuad.rectRotToQuad(alg.getRegion(),location);

		return true;
	}

	@Override
	public ImageType<T> getImageType() {
		return type;
	}
}
