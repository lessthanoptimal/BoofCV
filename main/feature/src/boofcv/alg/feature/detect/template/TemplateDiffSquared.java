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

package boofcv.alg.feature.detect.template;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;

/**
 * <p>
 * Scores the difference between the template and the image using difference squared error.
 * The error is multiplied by -1 to ensure that the best fits are peaks and not minimums.
 * </p>
 *
 * <p> error = -1*Sum<sub>(o,u)</sub> [I(x,y) - T(x-o,y-u)]^2 </p>
 *
 * @author Peter Abeles
 */
public abstract class TemplateDiffSquared<T extends ImageBase>
		extends BaseTemplateIntensity<T> {
	// IF MORE IMAGE TYPES ARE ADDED CREATE A GENERATOR FOR THIS CLASS

	public static class F32 extends TemplateDiffSquared<GrayF32> {
		@Override
		protected float evaluate(int tl_x, int tl_y) {

			float total = 0;

			for (int y = 0; y < template.height; y++) {
				int imageIndex = image.startIndex + (tl_y + y) * image.stride + tl_x;
				int templateIndex = template.startIndex + y * template.stride;

				for (int x = 0; x < template.width; x++) {
					float error = image.data[imageIndex++] - template.data[templateIndex++];
					total += error * error;
				}
			}

			return -total;
		}

		@Override
		protected float evaluateMask(int tl_x, int tl_y) {
			float total = 0;

			for (int y = 0; y < template.height; y++) {
				int imageIndex = image.startIndex + (tl_y + y) * image.stride + tl_x;
				int templateIndex = template.startIndex + y * template.stride;
				int maskIndex = mask.startIndex + y * mask.stride;

				for (int x = 0; x < template.width; x++) {
					float error = image.data[imageIndex++] - template.data[templateIndex++];
					total += mask.data[maskIndex++] * error * error;
				}
			}

			return -total;
		}
	}

	public static class U8 extends TemplateDiffSquared<GrayU8> {
		@Override
		protected float evaluate(int tl_x, int tl_y) {

			float total = 0;

			// Reduce change of numerical overflow and delay conversion to float
			float div = 255.0f*255.0f;

			for (int y = 0; y < template.height; y++) {
				int imageIndex = image.startIndex + (tl_y + y) * image.stride + tl_x;
				int templateIndex = template.startIndex + y * template.stride;

				int rowTotal = 0;
				for (int x = 0; x < template.width; x++) {
					int error = (image.data[imageIndex++] & 0xFF) - (template.data[templateIndex++] & 0xFF);
					rowTotal += error * error;
				}

				total += rowTotal / div;
			}

			return -total;
		}

		@Override
		protected float evaluateMask(int tl_x, int tl_y) {

			float total = 0;

			// Reduce change of numerical overflow and delay conversion to float
			float div = 255.0f*255.0f*255.0f;

			for (int y = 0; y < template.height; y++) {
				int imageIndex = image.startIndex + (tl_y + y) * image.stride + tl_x;
				int templateIndex = template.startIndex + y * template.stride;
				int maskIndex = mask.startIndex + y * mask.stride;

				int rowTotal = 0;
				for (int x = 0; x < template.width; x++) {
					int m = mask.data[maskIndex++] & 0xFF;
					int error = (image.data[imageIndex++] & 0xFF) - (template.data[templateIndex++] & 0xFF);
					rowTotal += m*error * error;
				}

				total += rowTotal / div;
			}

			return -total;
		}
	}

	@Override
	public boolean isBorderProcessed() {
		return false;
	}
}
