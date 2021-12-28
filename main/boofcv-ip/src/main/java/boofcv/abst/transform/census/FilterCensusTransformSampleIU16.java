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

package boofcv.abst.transform.census;

import boofcv.alg.transform.census.GCensusTransform;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.InterleavedU16;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Census transform which saves output in a {@link InterleavedU16}.
 *
 * @author Peter Abeles
 */
public class FilterCensusTransformSampleIU16<In extends ImageGray<In>>
		extends FilterCensusTransform<In, InterleavedU16> {
	FastAccess<Point2D_I32> samples;
	DogArray_I32 workSpace = new DogArray_I32();

	public FilterCensusTransformSampleIU16( FastAccess<Point2D_I32> samples, @Nullable ImageBorder<In> border, Class<In> imageType ) {
		super(computeRadius(samples), border, imageType, ImageType.IL_U16);
		this.samples = samples;
	}

	@Override
	public void process( In in, InterleavedU16 out ) {
		GCensusTransform.sample_IU16(in, samples, out, border, workSpace);
	}
}
