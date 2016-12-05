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

package boofcv.factory.distort;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.impl.*;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.*;

/**
 * @author Peter Abeles
 */
public class FactoryDistort {

	/**
	 * Creates a {@link boofcv.alg.distort.ImageDistort} for the specified image type, transformation
	 * and interpolation instance.  Min and max pixel values are assumed to be 0 and 255, respectively.
	 *
	 * @param cached If true the distortion is only computed one.  False for recomputed each time, but less memory.
	 * @param interpolationType  Which interpolation method it should use
	 * @param borderType How pixels outside the image border are handled
	 * @param inputType Type of input image
	 * @param outputType Type of output image
	 * @return ImageDistort
	 */
	public static <Input extends ImageBase, Output extends ImageBase>
	ImageDistort<Input, Output> distort(boolean cached, InterpolationType interpolationType, BorderType borderType,
										ImageType<Input> inputType, ImageType<Output> outputType) {
		InterpolatePixel<Input> interp =
				FactoryInterpolation.createPixel(0,255, interpolationType,borderType,inputType);

		return distort(cached, interp,outputType);
	}

	/**
	 * Creates a {@link boofcv.alg.distort.ImageDistort} for the specified image type, transformation
	 * and interpolation instance.
	 *
	 * @param cached If true the distortion is only computed one.  False for recomputed each time, but less memory.
	 * @param interp Which interpolation algorithm should be used.
	 * @param outputType Type of output image.
	 * @return ImageDistort
	 */
	public static <Input extends ImageBase, Output extends ImageBase>
	ImageDistort<Input, Output> distort(boolean cached, InterpolatePixel<Input> interp, ImageType<Output> outputType) {
		switch( outputType.getFamily() ) {
			case GRAY:
				return distortSB(cached,(InterpolatePixelS)interp,outputType.getImageClass());
			case PLANAR:
				return distortPL(cached,(InterpolatePixelS)interp,outputType.getImageClass());
			case INTERLEAVED:
				if( interp instanceof InterpolatePixelS )
					throw new IllegalArgumentException("Interpolation function for single band images was" +
							" passed in for an interleaved image");
				return distortIL(cached,(InterpolatePixelMB) interp, (ImageType)outputType);
			default:
				throw new IllegalArgumentException("Unknown image family "+outputType.getFamily());
		}
	}

	/**
	 * Creates a {@link boofcv.alg.distort.ImageDistort} for the specified image type, transformation
	 * and interpolation instance.
	 *
	 * @param cached If true the distortion is only computed one.  False for recomputed each time, but less memory.
	 * @param interp Which interpolation algorithm should be used.
	 * @param outputType Type of output image.
	 */
	public static <Input extends ImageGray, Output extends ImageGray>
	ImageDistort<Input, Output> distortSB(boolean cached, InterpolatePixelS<Input> interp, Class<Output> outputType)
	{
		if( cached ) {
			if( outputType == GrayF32.class ) {
				return (ImageDistort<Input,Output>)new ImplImageDistortCache_F32(interp);
			} else if( GrayS32.class.isAssignableFrom(outputType) ) {
				return (ImageDistort<Input,Output>)new ImplImageDistortCache_S32(interp);
			} else if( GrayI16.class.isAssignableFrom(outputType) ) {
				return (ImageDistort<Input,Output>)new ImplImageDistortCache_I16(interp);
			} else if( GrayI8.class.isAssignableFrom(outputType) ) {
				return (ImageDistort<Input,Output>)new ImplImageDistortCache_I8(interp);
			} else {
				throw new IllegalArgumentException("Output type not supported: "+outputType.getSimpleName());
			}
		} else {
			if (outputType == GrayF32.class) {
				return (ImageDistort<Input, Output>) new ImplImageDistort_F32(interp);
			} else if (GrayS32.class.isAssignableFrom(outputType)) {
				return (ImageDistort<Input, Output>) new ImplImageDistort_S32(interp);
			} else if (GrayI16.class.isAssignableFrom(outputType)) {
				return (ImageDistort<Input, Output>) new ImplImageDistort_I16(interp);
			} else if (GrayI8.class.isAssignableFrom(outputType)) {
				return (ImageDistort<Input, Output>) new ImplImageDistort_I8(interp);
			} else {
				throw new IllegalArgumentException("Output type not supported: " + outputType.getSimpleName());
			}
		}
	}

	/**
	 * Creates a {@link boofcv.alg.distort.ImageDistort} for the planar images, transformation
	 * and interpolation instance.
	 *
	 * @param cached If true the distortion is only computed one.  False for recomputed each time, but less memory.
	 * @param interp Which interpolation algorithm should be used.
	 * @param outputType Type of output image.
	 */
	public static <Input extends ImageGray,Output extends ImageGray>
	ImageDistort<Planar<Input>,Planar<Output>>
	distortPL(boolean cached , InterpolatePixelS<Input> interp, Class<Output> outputType)
	{
		ImageDistort<Input, Output> distortSingle = distortSB(cached, interp, outputType);
		return new ImplImageDistort_PL<>(distortSingle);
	}

	public static <Input extends ImageInterleaved, Output extends ImageInterleaved>
	ImageDistort<Input, Output>
	distortIL(boolean cached, InterpolatePixelMB<Input> interp, ImageType<Output> outputType)
	{
		if( cached ) {
			throw new IllegalArgumentException("Cached not supported yet");
		} else {
			switch( outputType.getDataType() ) {
				case F32:
					return (ImageDistort<Input, Output>) new ImplImageDistort_IL_F32((InterpolatePixelMB)interp);

				case U8:
					return (ImageDistort<Input, Output>) new ImplImageDistort_IL_U8((InterpolatePixelMB)interp);

				default:
					throw new IllegalArgumentException("Not supported yet");
			}
		}
	}
}
