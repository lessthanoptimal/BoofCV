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

package boofcv.alg.filter.binary;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.binary.impl.ImplThresholdImageOps;
import boofcv.alg.filter.binary.impl.ImplThresholdImageOps_MT;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

/**
 * <p>
 * Operations for thresholding images and converting them into a binary image.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class ThresholdImageOps {

	/**
	 * Applies a global threshold across the whole image. If 'down' is true, then pixels with values <=
	 * to 'threshold' are set to 1 and the others set to 0. If 'down' is false, then pixels with values >
	 * to 'threshold' are set to 1 and the others set to 0.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Binary output image. If null a new image will be declared. Modified.
	 * @param threshold threshold value.
	 * @param down If true then the inequality <= is used, otherwise if false then &gt; is used.
	 * @return Output image.
	 */
	public static GrayU8 threshold( GrayF32 input, @Nullable GrayU8 output,
									float threshold, boolean down ) {
		output = InputSanityCheck.declareOrReshape(input, output, GrayU8.class);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplThresholdImageOps_MT.threshold(input, output, threshold, down);
		} else {
			ImplThresholdImageOps.threshold(input, output, threshold, down);
		}

		return output;
	}

	/**
	 * Applies a global threshold across the whole image. If 'down' is true, then pixels with values <=
	 * to 'threshold' are set to 1 and the others set to 0. If 'down' is false, then pixels with values >
	 * to 'threshold' are set to 1 and the others set to 0.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Binary output image. If null a new image will be declared. Modified.
	 * @param threshold threshold value.
	 * @param down If true then the inequality <= is used, otherwise if false then &gt; is used.
	 * @return Output image.
	 */
	public static GrayU8 threshold( GrayF64 input, @Nullable GrayU8 output,
									double threshold, boolean down ) {
		output = InputSanityCheck.declareOrReshape(input, output, GrayU8.class);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplThresholdImageOps_MT.threshold(input, output, threshold, down);
		} else {
			ImplThresholdImageOps.threshold(input, output, threshold, down);
		}

		return output;
	}

	/**
	 * Applies a global threshold across the whole image. If 'down' is true, then pixels with values <=
	 * to 'threshold' are set to 1 and the others set to 0. If 'down' is false, then pixels with values >
	 * to 'threshold' are set to 1 and the others set to 0.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Binary output image. If null a new image will be declared. Modified.
	 * @param threshold threshold value.
	 * @param down If true then the inequality <= is used, otherwise if false then &gt; is used.
	 * @return Output image.
	 */
	public static GrayU8 threshold( GrayU8 input, @Nullable GrayU8 output,
									int threshold, boolean down ) {
		output = InputSanityCheck.declareOrReshape(input, output, GrayU8.class);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplThresholdImageOps_MT.threshold(input, output, threshold, down);
		} else {
			ImplThresholdImageOps.threshold(input, output, threshold, down);
		}

		return output;
	}

	/**
	 * Applies a global threshold across the whole image. If 'down' is true, then pixels with values <=
	 * to 'threshold' are set to 1 and the others set to 0. If 'down' is false, then pixels with values >
	 * to 'threshold' are set to 1 and the others set to 0.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Binary output image. If null a new image will be declared. Modified.
	 * @param threshold threshold value.
	 * @param down If true then the inequality <= is used, otherwise if false then &gt; is used.
	 * @return Output image.
	 */
	public static GrayU8 threshold( GrayS16 input, @Nullable GrayU8 output,
									int threshold, boolean down ) {
		output = InputSanityCheck.declareOrReshape(input, output, GrayU8.class);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplThresholdImageOps_MT.threshold(input, output, threshold, down);
		} else {
			ImplThresholdImageOps.threshold(input, output, threshold, down);
		}

		return output;
	}

	/**
	 * Applies a global threshold across the whole image. If 'down' is true, then pixels with values <=
	 * to 'threshold' are set to 1 and the others set to 0. If 'down' is false, then pixels with values >
	 * to 'threshold' are set to 1 and the others set to 0.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Binary output image. If null a new image will be declared. Modified.
	 * @param threshold threshold value.
	 * @param down If true then the inequality <= is used, otherwise if false then &gt; is used.
	 * @return Output image.
	 */
	public static GrayU8 threshold( GrayU16 input, @Nullable GrayU8 output,
									int threshold, boolean down ) {
		output = InputSanityCheck.declareOrReshape(input, output, GrayU8.class);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplThresholdImageOps_MT.threshold(input, output, threshold, down);
		} else {
			ImplThresholdImageOps.threshold(input, output, threshold, down);
		}

		return output;
	}

	/**
	 * Applies a global threshold across the whole image. If 'down' is true, then pixels with values <=
	 * to 'threshold' are set to 1 and the others set to 0. If 'down' is false, then pixels with values >
	 * to 'threshold' are set to 1 and the others set to 0.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Binary output image. If null a new image will be declared. Modified.
	 * @param threshold threshold value.
	 * @param down If true then the inequality <= is used, otherwise if false then &gt; is used.
	 * @return Output image.
	 */
	public static GrayU8 threshold( GrayS32 input, @Nullable GrayU8 output,
									int threshold, boolean down ) {
		output = InputSanityCheck.declareOrReshape(input, output, GrayU8.class);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplThresholdImageOps_MT.threshold(input, output, threshold, down);
		} else {
			ImplThresholdImageOps.threshold(input, output, threshold, down);
		}

		return output;
	}

	/**
	 * Thresholds the image using a locally adaptive threshold that is computed using a local square region centered
	 * on each pixel. The threshold is equal to the average value of the surrounding pixels times the scale.
	 * If down is true then b(x,y) = I(x,y) &le; T(x,y) * scale ? 1 : 0. Otherwise
	 * b(x,y) = I(x,y) * scale &gt; T(x,y) ? 0 : 1
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image. If null it will be declared internally.
	 * @param width Width of square region.
	 * @param scale Scale factor used to adjust threshold. Try 0.95
	 * @param down Should it threshold up or down.
	 * @param storage1 (Optional) Storage for intermediate step. If null will be declared internally.
	 * @param storage2 (Optional) Storage for intermediate step. If null will be declared internally.
	 * @return Thresholded image.
	 */
	public static GrayU8 localMean( GrayU8 input, @Nullable GrayU8 output,
									ConfigLength width, float scale, boolean down,
									@Nullable GrayU8 storage1, @Nullable GrayU8 storage2,
									@Nullable GrowArray<DogArray_I32> storage3 ) {

		output = InputSanityCheck.declareOrReshape(input, output, GrayU8.class);
		storage1 = InputSanityCheck.declareOrReshape(input, storage1, GrayU8.class);
		storage2 = InputSanityCheck.declareOrReshape(input, storage2, GrayU8.class);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplThresholdImageOps_MT.localMean(input, output, width, scale, down, storage1, storage2, storage3);
		} else {
			ImplThresholdImageOps.localMean(input, output, width, scale, down, storage1, storage2, storage3);
		}

		return output;
	}

	/**
	 * Thresholds the image using a locally adaptive threshold that is computed using a local square region centered
	 * on each pixel. The threshold is equal to the gaussian weighted sum of the surrounding pixels times the scale.
	 * If down is true then b(x,y) = I(x,y) &le; T(x,y) * scale ? 1 : 0. Otherwise
	 * b(x,y) = I(x,y) * scale &gt; T(x,y) ? 0 : 1
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image. If null it will be declared internally.
	 * @param width Width of square region.
	 * @param scale Scale factor used to adjust threshold. Try 0.95
	 * @param down Should it threshold up or down.
	 * @param storage1 (Optional) Storage for intermediate step. If null will be declared internally.
	 * @param storage2 (Optional) Storage for intermediate step. If null will be declared internally.
	 * @return Thresholded image.
	 */
	public static GrayU8 localGaussian( GrayU8 input, @Nullable GrayU8 output,
										ConfigLength width, float scale, boolean down,
										@Nullable GrayU8 storage1, @Nullable GrayU8 storage2 ) {

		output = InputSanityCheck.declareOrReshape(input, output, GrayU8.class);
		storage1 = InputSanityCheck.declareOrReshape(input, storage1, GrayU8.class);
		storage2 = InputSanityCheck.declareOrReshape(input, storage2, GrayU8.class);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplThresholdImageOps_MT.localGaussian(input, output, width, scale, down, storage1, storage2);
		} else {
			ImplThresholdImageOps.localGaussian(input, output, width, scale, down, storage1, storage2);
		}

		return output;
	}

	/**
	 * Thresholds the image using a locally adaptive threshold that is computed using a local square region centered
	 * on each pixel. The threshold is equal to the average value of the surrounding pixels times the scale.
	 * If down is true then b(x,y) = I(x,y) &le; T(x,y) * scale ? 1 : 0. Otherwise
	 * b(x,y) = I(x,y) * scale &gt; T(x,y) ? 0 : 1
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image. If null it will be declared internally.
	 * @param width Width of square region.
	 * @param scale Scale factor used to adjust threshold. Try 0.95
	 * @param down Should it threshold up or down.
	 * @param storage1 (Optional) Storage for intermediate step. If null will be declared internally.
	 * @param storage2 (Optional) Storage for intermediate step. If null will be declared internally.
	 * @return Thresholded image.
	 */
	public static GrayU8 localMean( GrayU16 input, @Nullable GrayU8 output,
									ConfigLength width, float scale, boolean down,
									@Nullable GrayU16 storage1, @Nullable GrayU16 storage2,
									@Nullable GrowArray<DogArray_I32> storage3 ) {

		output = InputSanityCheck.declareOrReshape(input, output, GrayU8.class);
		storage1 = InputSanityCheck.declareOrReshape(input, storage1, GrayU16.class);
		storage2 = InputSanityCheck.declareOrReshape(input, storage2, GrayU16.class);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplThresholdImageOps_MT.localMean(input, output, width, scale, down, storage1, storage2, storage3);
		} else {
			ImplThresholdImageOps.localMean(input, output, width, scale, down, storage1, storage2, storage3);
		}

		return output;
	}

	/**
	 * Thresholds the image using a locally adaptive threshold that is computed using a local square region centered
	 * on each pixel. The threshold is equal to the gaussian weighted sum of the surrounding pixels times the scale.
	 * If down is true then b(x,y) = I(x,y) &le; T(x,y) * scale ? 1 : 0. Otherwise
	 * b(x,y) = I(x,y) * scale &gt; T(x,y) ? 0 : 1
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image. If null it will be declared internally.
	 * @param width Width of square region.
	 * @param scale Scale factor used to adjust threshold. Try 0.95
	 * @param down Should it threshold up or down.
	 * @param storage1 (Optional) Storage for intermediate step. If null will be declared internally.
	 * @param storage2 (Optional) Storage for intermediate step. If null will be declared internally.
	 * @return Thresholded image.
	 */
	public static GrayU8 localGaussian( GrayU16 input, @Nullable GrayU8 output,
										ConfigLength width, float scale, boolean down,
										@Nullable GrayU16 storage1, @Nullable GrayU16 storage2 ) {

		output = InputSanityCheck.declareOrReshape(input, output, GrayU8.class);
		storage1 = InputSanityCheck.declareOrReshape(input, storage1, GrayU16.class);
		storage2 = InputSanityCheck.declareOrReshape(input, storage2, GrayU16.class);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplThresholdImageOps_MT.localGaussian(input, output, width, scale, down, storage1, storage2);
		} else {
			ImplThresholdImageOps.localGaussian(input, output, width, scale, down, storage1, storage2);
		}

		return output;
	}

	/**
	 * Thresholds the image using a locally adaptive threshold that is computed using a local square region centered
	 * on each pixel. The threshold is equal to the average value of the surrounding pixels times the scale.
	 * If down is true then b(x,y) = I(x,y) &le; T(x,y) * scale ? 1 : 0. Otherwise
	 * b(x,y) = I(x,y) * scale &gt; T(x,y) ? 0 : 1
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image. If null it will be declared internally.
	 * @param width Width of square region.
	 * @param scale Scale factor used to adjust threshold. Try 0.95
	 * @param down Should it threshold up or down.
	 * @param storage1 (Optional) Storage for intermediate step. If null will be declared internally.
	 * @param storage2 (Optional) Storage for intermediate step. If null will be declared internally.
	 * @return Thresholded image.
	 */
	public static GrayU8 localMean( GrayF32 input, @Nullable GrayU8 output,
									ConfigLength width, float scale, boolean down,
									@Nullable GrayF32 storage1, @Nullable GrayF32 storage2,
									@Nullable GrowArray<DogArray_F32> storage3 ) {

		output = InputSanityCheck.declareOrReshape(input, output, GrayU8.class);
		storage1 = InputSanityCheck.declareOrReshape(input, storage1, GrayF32.class);
		storage2 = InputSanityCheck.declareOrReshape(input, storage2, GrayF32.class);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplThresholdImageOps_MT.localMean(input, output, width, scale, down, storage1, storage2, storage3);
		} else {
			ImplThresholdImageOps.localMean(input, output, width, scale, down, storage1, storage2, storage3);
		}

		return output;
	}

	/**
	 * Thresholds the image using a locally adaptive threshold that is computed using a local square region centered
	 * on each pixel. The threshold is equal to the gaussian weighted sum of the surrounding pixels times the scale.
	 * If down is true then b(x,y) = I(x,y) &le; T(x,y) * scale ? 1 : 0. Otherwise
	 * b(x,y) = I(x,y) * scale &gt; T(x,y) ? 0 : 1
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image. If null it will be declared internally.
	 * @param width Width of square region.
	 * @param scale Scale factor used to adjust threshold. Try 0.95
	 * @param down Should it threshold up or down.
	 * @param storage1 (Optional) Storage for intermediate step. If null will be declared internally.
	 * @param storage2 (Optional) Storage for intermediate step. If null will be declared internally.
	 * @return Thresholded image.
	 */
	public static GrayU8 localGaussian( GrayF32 input, @Nullable GrayU8 output,
										ConfigLength width, float scale, boolean down,
										@Nullable GrayF32 storage1, @Nullable GrayF32 storage2 ) {

		output = InputSanityCheck.declareOrReshape(input, output, GrayU8.class);
		storage1 = InputSanityCheck.declareOrReshape(input, storage1, GrayF32.class);
		storage2 = InputSanityCheck.declareOrReshape(input, storage2, GrayF32.class);

		if (BoofConcurrency.USE_CONCURRENT) {
			ImplThresholdImageOps_MT.localGaussian(input, output, width, scale, down, storage1, storage2);
		} else {
			ImplThresholdImageOps.localGaussian(input, output, width, scale, down, storage1, storage2);
		}

		return output;
	}
}
