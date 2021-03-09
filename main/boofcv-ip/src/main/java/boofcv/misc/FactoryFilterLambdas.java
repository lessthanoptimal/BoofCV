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

package boofcv.misc;

import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Random filters for lambas. Not sure where else to put these
 *
 * @author Peter Abeles
 */
public class FactoryFilterLambdas {
	/**
	 * Creates a filter which seeks to scale an image down to the specified pixel count. If
	 * the image is larger then the original image is returned. Uses {@link AverageDownSampleOps} to scale the image.
	 *
	 * @param maxImagePixels If the image has more pixels than this it will be scaled down. Set to a value &le; 0
	 * to disable this function;
	 */
	public static<T extends ImageBase<T>> BoofLambdas.Transform<T>
	createDownSampleFilter(int maxImagePixels, ImageType<T> imageType) {
		// if disabled return a function that does nothing
		if (maxImagePixels<=0)
			return (img)->img;

		// Scale down and use this workspace to scale in
		T scaled = imageType.createImage(1,1);
		return (full)->{
			double scale = Math.sqrt(maxImagePixels)/Math.sqrt(full.width*full.height);
			if (scale < 1.0) {
				scaled.reshape((int)(scale*full.width), (int)(scale*full.height));
				AverageDownSampleOps.down(full, scaled);
				return scaled;
			}
			return full;
		};
	}
}
