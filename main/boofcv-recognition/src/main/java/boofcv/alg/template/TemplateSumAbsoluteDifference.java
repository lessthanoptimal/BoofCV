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

package boofcv.alg.template;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;

import java.util.Objects;

/**
 * <p>
 * Scores the difference between the template and the image using sum of absolute difference (SAD) error.
 * The error is multiplied by -1 to ensure that the best fits are peaks and not minimums.
 * </p>
 *
 * <p> error = -1*Sum<sub>(o,u)</sub> |I(x,y) - T(x-o,y-u)| </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings("NullAway.Init")
public abstract class TemplateSumAbsoluteDifference<T extends ImageBase<T>>
		implements TemplateIntensityImage.EvaluatorMethod<T> {
	TemplateIntensityImage<T> o;

	@Override
	public void initialize( TemplateIntensityImage<T> owner ) {
		this.o = owner;
	}
	// IF MORE IMAGE TYPES ARE ADDED CREATE A GENERATOR FOR THIS CLASS

	public static class F32 extends TemplateSumAbsoluteDifference<GrayF32> {
		@Override
		public float evaluate( int tl_x, int tl_y ) {

			float total = 0;

			for (int y = 0; y < o.template.height; y++) {
				int imageIndex = o.image.startIndex + (tl_y + y)*o.image.stride + tl_x;
				int templateIndex = o.template.startIndex + y*o.template.stride;

				float rowTotal = 0.0f;
				for (int x = 0; x < o.template.width; x++) {
					rowTotal += Math.abs(o.image.data[imageIndex++] - o.template.data[templateIndex++]);
				}
				total += rowTotal;
			}

			return total;
		}

		@Override
		public float evaluateMask( int tl_x, int tl_y ) {
			Objects.requireNonNull(o.mask);
			float total = 0;

			for (int y = 0; y < o.template.height; y++) {
				int imageIndex = o.image.startIndex + (tl_y + y)*o.image.stride + tl_x;
				int templateIndex = o.template.startIndex + y*o.template.stride;
				int maskIndex = o.mask.startIndex + y*o.mask.stride;

				float rowTotal = 0.0f;
				for (int x = 0; x < o.template.width; x++) {
					rowTotal += o.mask.data[maskIndex++]*Math.abs(o.image.data[imageIndex++] - o.template.data[templateIndex++]);
				}
				total += rowTotal;
			}

			return total;
		}
	}

	public static class U8 extends TemplateSumAbsoluteDifference<GrayU8> {
		@Override
		public float evaluate( int tl_x, int tl_y ) {

			float total = 0;

			for (int y = 0; y < o.template.height; y++) {
				int imageIndex = o.image.startIndex + (tl_y + y)*o.image.stride + tl_x;
				int templateIndex = o.template.startIndex + y*o.template.stride;

				int rowTotal = 0;
				for (int x = 0; x < o.template.width; x++) {
					rowTotal += Math.abs((o.image.data[imageIndex++] & 0xFF) - (o.template.data[templateIndex++] & 0xFF));
				}

				total += rowTotal;
			}

			return total;
		}

		@Override
		public float evaluateMask( int tl_x, int tl_y ) {
			Objects.requireNonNull(o.mask);
			float total = 0;

			for (int y = 0; y < o.template.height; y++) {
				int imageIndex = o.image.startIndex + (tl_y + y)*o.image.stride + tl_x;
				int templateIndex = o.template.startIndex + y*o.template.stride;
				int maskIndex = o.mask.startIndex + y*o.mask.stride;

				int rowTotal = 0;
				for (int x = 0; x < o.template.width; x++) {
					int m = o.mask.data[maskIndex++] & 0xFF;
					rowTotal += m*Math.abs((o.image.data[imageIndex++] & 0xFF) - (o.template.data[templateIndex++] & 0xFF));
				}

				total += rowTotal;
			}

			return total;
		}
	}

	@Override
	public boolean isBorderProcessed() {
		return false;
	}

	@Override
	public boolean isMaximize() {
		return false;
	}
}
