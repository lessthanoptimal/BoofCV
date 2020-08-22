/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.pyramid;

import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.struct.pyramid.PyramidDiscrete;
import org.jetbrains.annotations.Nullable;

/**
 * Creates an image pyramid by down sampling square regions using {@link AverageDownSampleOps}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class PyramidDiscreteAverage<T extends ImageBase<T>> extends PyramidDiscrete<T> {

	/**
	 *
	 * @param imageType Type of image processed
	 * @param saveOriginalReference If a reference to the full resolution image should be saved instead of copied.
	 *                              Set to false if you don't know what you are doing.
	 * @param configLayers Specifies how the levels are computed
	 */
	public PyramidDiscreteAverage(ImageType<T> imageType,
								  boolean saveOriginalReference,
								  @Nullable ConfigDiscreteLevels configLayers)
	{
		super(imageType,saveOriginalReference,configLayers);
	}

	protected PyramidDiscreteAverage( PyramidDiscreteAverage<T> orig ) {
		super(orig);
	}

	@Override
	public void process(T input) {
		super.initialize(input.width,input.height);

		if (levelScales[0] == 1) {
			if (isSaveOriginalReference()) {
				setFirstLayer(input);
			} else {
				getLayer(0).setTo(input);
			}
		} else {
			AverageDownSampleOps.down(input, levelScales[0], getLayer(0));
		}

		for (int index = 1; index < getNumLayers(); index++) {
			int width = levelScales[index]/ levelScales[index-1];
			AverageDownSampleOps.down(getLayer(index-1),width,getLayer(index));
		}
	}

	/**
	 * The center of the sampling kernel is 1/2 the square region's width
	 *
	 * @param layer Layer in the pyramid
	 * @return offset
	 */
	@Override
	public double getSampleOffset(int layer) {
		return (levelScales[layer]-1)/2.0;
	}

	@Override
	public double getSigma(int layer) {
		return 0;
	}

	@Override
	public ImagePyramid<T> copyStructure() {
		return new PyramidDiscreteAverage<>(this);
	}
}
