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

package boofcv.alg.sfm.d2;

import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.distort.PixelTransformHomography_F32;
import boofcv.struct.distort.PixelTransform;
import georegression.struct.ConvertFloatType;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.homography.Homography2D_F32;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F32;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for creating {@link StitchingTransform} of different motion models.
 *
 * @author Peter Abeles
 */
public class FactoryStitchingTransform {

	public static StitchingTransform<Affine2D_F64> createAffine_F64() {
		return new StitchingTransform<>() {
			final Affine2D_F32 input_F32 = new Affine2D_F32();

			@Override
			public PixelTransform<Point2D_F32> convertPixel( Affine2D_F64 input,
															 @Nullable PixelTransform<Point2D_F32> output ) {

				ConvertFloatType.convert(input, input_F32);

				if (output != null) {
					((PixelTransformAffine_F32)output).setTo(input_F32);
				} else {
					PixelTransformAffine_F32 a = new PixelTransformAffine_F32();
					a.setTo(input_F32);
					output = a;
				}

				return output;
			}

			@Override
			public Homography2D_F64 convertH( Affine2D_F64 input,
											  @Nullable Homography2D_F64 output ) {
				if (output == null)
					output = new Homography2D_F64();
				output.setTo(
						input.a11, input.a12, input.tx,
						input.a21, input.a22, input.ty,
						0, 0, 1);
				return output;
			}
		};
	}

	public static StitchingTransform<Affine2D_F32> createAffine_F32() {
		return new StitchingTransform<>() {
			@Override
			public PixelTransform<Point2D_F32> convertPixel( Affine2D_F32 input,
															 @Nullable PixelTransform<Point2D_F32> output ) {
				if (output != null) {
					((PixelTransformAffine_F32)output).setTo(input);
				} else {
					output = new PixelTransformAffine_F32(input);
				}

				return output;
			}

			@Override
			public Homography2D_F64 convertH( Affine2D_F32 input, @Nullable Homography2D_F64 output ) {
				if (output == null)
					output = new Homography2D_F64();
				output.setTo(
						input.a11, input.a12, input.tx,
						input.a21, input.a22, input.ty,
						0, 0, 1);
				return output;
			}
		};
	}

	public static StitchingTransform<Homography2D_F32> createHomography_F32() {
		return new StitchingTransform<>() {
			@Override
			public PixelTransform<Point2D_F32> convertPixel( Homography2D_F32 input,
															 @Nullable PixelTransform<Point2D_F32> output ) {
				if (output != null) {
					((PixelTransformHomography_F32)output).setTo(input);
				} else {
					output = new PixelTransformHomography_F32(input);
				}

				return output;
			}

			@Override
			public Homography2D_F64 convertH( Homography2D_F32 input, @Nullable Homography2D_F64 output ) {
				if (output == null)
					output = new Homography2D_F64();
				output.setTo(
						input.a11, input.a12, input.a13,
						input.a21, input.a22, input.a23,
						input.a31, input.a32, input.a33);
				return output;
			}
		};
	}

	public static StitchingTransform<Homography2D_F64> createHomography_F64() {
		return new StitchingTransform<>() {
			@Override
			public PixelTransform<Point2D_F32> convertPixel( Homography2D_F64 input, @Nullable PixelTransform<Point2D_F32> output ) {
				if (output != null) {
					((PixelTransformHomography_F32)output).setTo(input);
				} else {
					output = new PixelTransformHomography_F32(input);
				}

				return output;
			}

			@Override
			public Homography2D_F64 convertH( Homography2D_F64 input, @Nullable Homography2D_F64 output ) {
				if (output == null)
					output = new Homography2D_F64();
				output.setTo(input);
				return output;
			}
		};
	}
}
