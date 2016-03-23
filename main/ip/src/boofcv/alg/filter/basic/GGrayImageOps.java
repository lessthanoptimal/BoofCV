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

package boofcv.alg.filter.basic;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;


/**
 * Weakly typed version of {@link GrayImageOps}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class GGrayImageOps {

	/**
	 * <p>
	 * Stretches the image's intensity:<br>
	 * O<sub>x,y</sub> = I<sub>x,y</sub>&gamma; + beta<br>
	 * </p>
	 * <p>
	 * The image's intensity is clamped at 0 and max;
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static <T extends ImageGray> T stretch(T input, double gamma, double beta, double max , T output) {
		if( input instanceof GrayF32) {
			return (T)GrayImageOps.stretch((GrayF32)input,gamma,(float)beta,(float)max,(GrayF32)output);
		} else if( input instanceof GrayU8) {
			return (T)GrayImageOps.stretch((GrayU8)input,gamma,(int)beta,(int)max,(GrayU8)output);
		} else if( input instanceof GrayS16) {
			return (T)GrayImageOps.stretch((GrayS16)input,gamma,(int)beta,(int)max,(GrayS16)output);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getSimpleName());
		}
	}

		/**
	 * <p>
	 * Brightens the image's intensity:<br>
	 * O<sub>x,y</sub> = I<sub>x,y</sub> + beta<br>
	 * </p>
	 * <p>
	 * The image's intensity is clamped at 0 and max;
	 * </p>
	 *
	 * @param input  Input image. Not modified.
	 * @param beta   How much the image is brightened by.
	 * @param output If not null, the output image.  If null a new image is declared and returned.  Modified.
	 * @return Output image.
	 */
	public static <T extends ImageGray> T brighten(T input , double beta, double max , T output )
	{
		if( input instanceof GrayF32) {
			return (T)GrayImageOps.brighten((GrayF32) input, (float) beta, (float) max, (GrayF32) output);
		} else if( input instanceof GrayU8) {
			return (T)GrayImageOps.brighten((GrayU8)input,(int)beta,(int)max,(GrayU8)output);
		} else if( input instanceof GrayS16) {
			return (T)GrayImageOps.brighten((GrayS16) input, (int) beta, (int) max, (GrayS16) output);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getSimpleName());
		}
	}
}
