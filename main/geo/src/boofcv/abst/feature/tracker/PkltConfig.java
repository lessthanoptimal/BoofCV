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

package boofcv.abst.feature.tracker;

import boofcv.alg.tracker.klt.KltConfig;
import boofcv.struct.image.ImageSingleBand;


/**
 * Configuration class for {@link PointTrackerKltPyramid}.
 *
 * @author Peter Abeles
 */
public class PkltConfig<I extends ImageSingleBand, D extends ImageSingleBand> {
	/** configuration for low level KLT tracker */
	public KltConfig config;
	/** The radius of a feature descriptor in layer. 3 is a reasonable number. */
	public int templateRadius;

	/** Scale factor for each layer in the pyramid */
	public int pyramidScaling[];

	/** Type of input image */
	public Class<I> typeInput;
	/** Type of image derivative */
	public Class<D> typeDeriv;

	/**
	 * If you have no idea what these parameters should be use this function to
	 * create a reasonable default.
	 * @return
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	PkltConfig<I,D> createDefault(Class<I> typeInput, Class<D> typeDeriv) {
		PkltConfig<I,D> ret = new PkltConfig<I,D>();

		ret.typeInput = typeInput;
		ret.typeDeriv = typeDeriv;

		ret.config = KltConfig.createDefault();
		ret.templateRadius = 2;
		ret.pyramidScaling = new int[]{1,2,4};

		return ret;
	}

	/**
	 * Returns the scale factor for the top most layer in the pyramid.
	 * @return Scale factor.
	 */
	public int computeScalingTop() {
		return pyramidScaling[ pyramidScaling.length-1];
	}
}
