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
 * Template matching which uses normalized cross correlation (NCC).
 *
 * @author Peter Abeles
 */
public abstract class TemplateNCC <T extends ImageBase>
		extends BaseTemplateIntensity<T>
{
	@Override
	public void process(T template) {
		setupTemplate(template);
		super.process(template);
	}

	/**
	 * Precompute statistical information on the template
	 */
	public abstract void setupTemplate( T template );

	public static class F32 extends TemplateNCC<GrayF32> {

		float area;
		float templateMean;
		float templateSigma;

		@Override
		protected float evaluate(int tl_x, int tl_y) {

			float top = 0;
			float imageMean = 0;
			float imageSigma = 0;

			for (int y = 0; y < template.height; y++) {
				int imageIndex = image.startIndex + (tl_y + y) * image.stride + tl_x;

				for (int x = 0; x < template.width; x++) {
					imageMean += image.data[imageIndex++];
				}
			}

			imageMean /= area;

			for (int y = 0; y < template.height; y++) {
				int imageIndex = image.startIndex + (tl_y + y) * image.stride + tl_x;
				int templateIndex = template.startIndex + y * template.stride;

				for (int x = 0; x < template.width; x++) {
					float templateVal = template.data[templateIndex++];

					float diff = image.data[imageIndex++] - imageMean;
					imageSigma += diff*diff;

					top += diff*(templateVal-templateMean);
				}
			}
			imageSigma = (float)Math.sqrt(imageSigma/area);

			// technically top should be divided by area, but that won't change the solution
			return top/(imageSigma*templateSigma);
		}

		@Override
		protected float evaluateMask(int tl_x, int tl_y) {

			float top = 0;
			float imageMean = 0;
			float imageSigma = 0;

			for (int y = 0; y < template.height; y++) {
				int imageIndex = image.startIndex + (tl_y + y) * image.stride + tl_x;

				for (int x = 0; x < template.width; x++) {
					imageMean += image.data[imageIndex++];
				}
			}

			imageMean /= area;

			for (int y = 0; y < template.height; y++) {
				int imageIndex = image.startIndex + (tl_y + y) * image.stride + tl_x;
				int templateIndex = template.startIndex + y * template.stride;
				int maskIndex = mask.startIndex + y * mask.stride;

				for (int x = 0; x < template.width; x++) {
					float templateVal = template.data[templateIndex++];

					float diff = image.data[imageIndex++] - imageMean;
					imageSigma += diff*diff;

					top += mask.data[maskIndex++]*diff*(templateVal-templateMean);
				}
			}
			imageSigma = (float)Math.sqrt(imageSigma/area);

			// technically top should be divided by area, but that won't change the solution
			return top/(imageSigma*templateSigma);
		}

		@Override
		public void setupTemplate(GrayF32 template) {
			area = template.width*template.height;

			templateMean = 0;

			for (int y = 0; y < template.height; y++) {
				int templateIndex = template.startIndex + y * template.stride;

				for (int x = 0; x < template.width; x++) {
					templateMean += template.data[templateIndex++];
				}
			}

			templateMean /= area;

			templateSigma = 0;

			for (int y = 0; y < template.height; y++) {
				int templateIndex = template.startIndex + y * template.stride;

				for (int x = 0; x < template.width; x++) {
					float diff = template.data[templateIndex++] - templateMean;
					templateSigma += diff*diff;
				}
			}

			templateSigma = (float)Math.sqrt(templateSigma/area);
		}
	}

	public static class U8 extends TemplateNCC<GrayU8> {

		float area;
		float templateMean;
		float templateSigma;

		@Override
		protected float evaluate(int tl_x, int tl_y) {

			float top = 0;
			int imageSum = 0;
			float imageMean = 0;
			float imageSigma = 0;

			for (int y = 0; y < template.height; y++) {
				int imageIndex = image.startIndex + (tl_y + y) * image.stride + tl_x;

				for (int x = 0; x < template.width; x++) {
					imageSum += image.data[imageIndex++] & 0xFF;
				}
			}

			imageMean = imageSum/area;

			for (int y = 0; y < template.height; y++) {
				int imageIndex = image.startIndex + (tl_y + y) * image.stride + tl_x;
				int templateIndex = template.startIndex + y * template.stride;

				for (int x = 0; x < template.width; x++) {
					int templateVal = template.data[templateIndex++] & 0xFF;

					float diff = (image.data[imageIndex++] & 0xFF) - imageMean;
					imageSigma += diff*diff;

					top += diff*(templateVal-templateMean);
				}
			}
			imageSigma = (float)Math.sqrt(imageSigma/area);

			// technically top should be divided by area, but that won't change the solution
			return top/(imageSigma*templateSigma);
		}

		@Override
		protected float evaluateMask(int tl_x, int tl_y) {

			float top = 0;
			int imageSum = 0;
			float imageMean = 0;
			float imageSigma = 0;

			for (int y = 0; y < template.height; y++) {
				int imageIndex = image.startIndex + (tl_y + y) * image.stride + tl_x;

				for (int x = 0; x < template.width; x++) {
					imageSum += image.data[imageIndex++] & 0xFF;
				}
			}

			imageMean = imageSum/area;

			for (int y = 0; y < template.height; y++) {
				int imageIndex = image.startIndex + (tl_y + y) * image.stride + tl_x;
				int templateIndex = template.startIndex + y * template.stride;
				int maskIndex = mask.startIndex + y * mask.stride;

				for (int x = 0; x < template.width; x++) {
					int templateVal = template.data[templateIndex++] & 0xFF;
					int m = mask.data[maskIndex++] & 0xFF;

					float diff = (image.data[imageIndex++] & 0xFF) - imageMean;
					imageSigma += diff*diff;

					top += m*diff*(templateVal-templateMean);
				}
			}
			imageSigma = (float)Math.sqrt(imageSigma/area);

			// technically top should be divided by area, but that won't change the solution
			return top/(imageSigma*templateSigma);
		}

		@Override
		public void setupTemplate(GrayU8 template) {
			area = template.width*template.height;

			templateMean = 0;

			for (int y = 0; y < template.height; y++) {
				int templateIndex = template.startIndex + y * template.stride;

				for (int x = 0; x < template.width; x++) {
					templateMean += template.data[templateIndex++] & 0xFF;
				}
			}

			templateMean /= area;

			templateSigma = 0;

			for (int y = 0; y < template.height; y++) {
				int templateIndex = template.startIndex + y * template.stride;

				for (int x = 0; x < template.width; x++) {
					float diff = (template.data[templateIndex++] & 0xFF) - templateMean;
					templateSigma += diff*diff;
				}
			}

			templateSigma = (float)Math.sqrt(templateSigma/area);
		}
	}

	@Override
	public boolean isBorderProcessed() {
		return false;
	}
}
