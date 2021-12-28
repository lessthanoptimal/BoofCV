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
import org.ejml.UtilEjml;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Template matching which uses squared difference normed
 *
 * <p> error = -1*Sum<sub>(o,u)</sub> (I(x,y) - T(x-o,y-u)**2/sqrt(sum I(x,y)**2 * sum T(x-o,y-u)**2) </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings("NullAway.Init")
public abstract class TemplateSqDiffNormed<T extends ImageBase<T>>
		implements TemplateIntensityImage.EvaluatorMethod<T> {
	// used to avoid divide by zero
	float EPS = UtilEjml.F_EPS;
	TemplateIntensityImage<T> o;

	@Override
	public void initialize( TemplateIntensityImage<T> owner ) {
		this.o = owner;
		setupTemplate(o.template, o.mask);
	}

	@Override
	public boolean isMaximize() {
		return true;
	}

	/**
	 * Precompres template statistics here
	 */
	public abstract void setupTemplate( T template, @Nullable T mask );

	public static class F32 extends TemplateSqDiffNormed<GrayF32> {
		float area;
		float templateSumSq;

		@Override
		public float evaluate( int tl_x, int tl_y ) {
			float imageSumSq = 0;

			for (int y = 0; y < o.template.height; y++) {
				int imageIndex = o.image.startIndex + (tl_y + y)*o.image.stride + tl_x;

				for (int x = 0; x < o.template.width; x++) {
					float v = o.image.data[imageIndex++];
					imageSumSq += v*v;
				}
			}
			imageSumSq /= area;


			float errorSumSq = 0;
			for (int y = 0; y < o.template.height; y++) {
				int imageIndex = o.image.startIndex + (tl_y + y)*o.image.stride + tl_x;
				int templateIndex = o.template.startIndex + y*o.template.stride;

				for (int x = 0; x < o.template.width; x++) {
					float error = (o.image.data[imageIndex++] - o.template.data[templateIndex++]);
					errorSumSq += error*error;
				}
			}
			errorSumSq /= area;

			return (float)(-errorSumSq/Math.sqrt(EPS + templateSumSq*imageSumSq));
		}

		@Override
		public float evaluateMask( int tl_x, int tl_y ) {
			Objects.requireNonNull(o.mask);
			float imageSumSq = 0;

			for (int y = 0; y < o.template.height; y++) {
				int imageIndex = o.image.startIndex + (tl_y + y)*o.image.stride + tl_x;
				int maskIndex = o.mask.startIndex + y*o.mask.stride;

				for (int x = 0; x < o.template.width; x++) {
					float v = o.image.data[imageIndex++]*o.mask.data[maskIndex++];
					imageSumSq += v*v;
				}
			}
			imageSumSq /= area;

			float errorSumSq = 0.0f;
			for (int y = 0; y < o.template.height; y++) {
				int imageIndex = o.image.startIndex + (tl_y + y)*o.image.stride + tl_x;
				int templateIndex = o.template.startIndex + y*o.template.stride;
				int maskIndex = o.mask.startIndex + y*o.mask.stride;

				for (int x = 0; x < o.template.width; x++) {
					float mask = o.mask.data[maskIndex++];
					float error = (o.image.data[imageIndex++] - o.template.data[templateIndex++])*mask;
					errorSumSq += error*error;
				}
			}
			errorSumSq /= area;

			return (float)(-errorSumSq/Math.sqrt(EPS + templateSumSq*imageSumSq));
		}

		@Override
		public void setupTemplate( GrayF32 template, @Nullable GrayF32 mask ) {
			area = 0.0f;

			templateSumSq = 0;

			for (int y = 0; y < o.template.height; y++) {
				int templateIndex = o.template.startIndex + y*o.template.stride;

				for (int x = 0; x < o.template.width; x++) {
					float m = mask != null ? mask.unsafe_get(x, y) : 1.0f;
					float v = o.template.data[templateIndex++]*m;
					templateSumSq += v*v;
					area += m;
				}
			}
			area *= area;

			// divide by the area to keep the numbers reasonable sized. This won't change the solution
			templateSumSq /= area;
		}
	}

	public static class U8 extends TemplateSqDiffNormed<GrayU8> {
		double area;
		double templateSumSq;

		@Override
		public float evaluate( int tl_x, int tl_y ) {
			double imageSumSq = 0;

			for (int y = 0; y < o.template.height; y++) {
				int imageIndex = o.image.startIndex + (tl_y + y)*o.image.stride + tl_x;

				for (int x = 0; x < o.template.width; x++) {
					double v = o.image.data[imageIndex++] & 0xFF;
					imageSumSq += v*v;
				}
			}
			imageSumSq /= area;


			double errorSumSq = 0;
			for (int y = 0; y < o.template.height; y++) {
				int imageIndex = o.image.startIndex + (tl_y + y)*o.image.stride + tl_x;
				int templateIndex = o.template.startIndex + y*o.template.stride;

				for (int x = 0; x < o.template.width; x++) {
					double error = (o.image.data[imageIndex++] & 0xFF) - (o.template.data[templateIndex++] & 0xFF);
					errorSumSq += error*error;
				}
			}
			errorSumSq /= area;

			return (float)(-errorSumSq/Math.sqrt(EPS + templateSumSq*imageSumSq));
		}

		@Override
		public float evaluateMask( int tl_x, int tl_y ) {
			Objects.requireNonNull(o.mask);
			double imageSumSq = 0;

			for (int y = 0; y < o.template.height; y++) {
				int imageIndex = o.image.startIndex + (tl_y + y)*o.image.stride + tl_x;
				int maskIndex = o.mask.startIndex + y*o.mask.stride;

				for (int x = 0; x < o.template.width; x++) {
					double v = (o.image.data[imageIndex++] & 0xFF)*(o.mask.data[maskIndex++] & 0xFF);
					imageSumSq += v*v;
				}
			}
			imageSumSq /= area;

			double errorSumSq = 0.0f;
			for (int y = 0; y < o.template.height; y++) {
				int imageIndex = o.image.startIndex + (tl_y + y)*o.image.stride + tl_x;
				int templateIndex = o.template.startIndex + y*o.template.stride;
				int maskIndex = o.mask.startIndex + y*o.mask.stride;

				for (int x = 0; x < o.template.width; x++) {
					double mask = o.mask.data[maskIndex++] & 0xFF;
					double error = ((o.image.data[imageIndex++] & 0xFF) - (o.template.data[templateIndex++] & 0xFF))*mask;
					errorSumSq += error*error;
				}
			}
			errorSumSq /= area;

			return (float)(-errorSumSq/Math.sqrt(EPS + templateSumSq*imageSumSq));
		}

		@Override
		public void setupTemplate( GrayU8 template, @Nullable GrayU8 mask ) {
			area = 0.0;
			templateSumSq = 0.0;

			for (int y = 0; y < o.template.height; y++) {
				int templateIndex = o.template.startIndex + y*o.template.stride;

				for (int x = 0; x < o.template.width; x++) {
					double m = mask != null ? mask.unsafe_get(x, y) : 1.0;
					double v = (o.template.data[templateIndex++] & 0xFF)*m;
					templateSumSq += v*v;
					area += m;
				}
			}

			area *= area;

			// divide by the area to keep the numbers reasonable sized. This won't change the solution
			templateSumSq /= area;
		}
	}

	@Override
	public boolean isBorderProcessed() {
		return false;
	}
}
