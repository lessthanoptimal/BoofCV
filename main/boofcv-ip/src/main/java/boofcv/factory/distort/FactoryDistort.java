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

package boofcv.factory.distort;

import boofcv.abst.distort.ConfigDeformPointMLS;
import boofcv.abst.distort.PointDeformKeyPoints;
import boofcv.abst.distort.PointDeform_MLS;
import boofcv.alg.distort.*;
import boofcv.alg.distort.impl.ImplImageDistort_PL;
import boofcv.alg.distort.mls.ImageDeformPointMLS_F32;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.*;

/**
 * Factory for operations which distort the image.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class FactoryDistort {

	/**
	 * Creates a {@link boofcv.alg.distort.ImageDistort} for the specified image type, transformation
	 * and interpolation instance. Min and max pixel values are assumed to be 0 and 255, respectively.
	 *
	 * @param cached If true the distortion is only computed one. False for recomputed each time, but less memory.
	 * @param interpolationType Which interpolation method it should use
	 * @param borderType How pixels outside the image border are handled
	 * @param inputType Type of input image
	 * @param outputType Type of output image
	 * @return ImageDistort
	 */
	public static <Input extends ImageBase<Input>, Output extends ImageBase<Output>>
	ImageDistort<Input, Output> distort( boolean cached, InterpolationType interpolationType, BorderType borderType,
										 ImageType<Input> inputType, ImageType<Output> outputType ) {
		InterpolatePixel<Input> interp =
				FactoryInterpolation.createPixel(0, 255, interpolationType, borderType, inputType);

		return distort(cached, interp, outputType);
	}

	/**
	 * Creates a {@link boofcv.alg.distort.ImageDistort} for the specified image type, transformation
	 * and interpolation instance.
	 *
	 * @param cached If true the distortion is only computed one. False for recomputed each time, but less memory.
	 * @param interp Which interpolation algorithm should be used.
	 * @param outputType Type of output image.
	 * @return ImageDistort
	 */
	public static <Input extends ImageBase<Input>, Output extends ImageBase<Output>>
	ImageDistort<Input, Output> distort( boolean cached, InterpolatePixel<Input> interp, ImageType<Output> outputType ) {
		return switch (outputType.getFamily()) {
			case GRAY -> distortSB(cached, (InterpolatePixelS)interp, outputType.getImageClass());
			case PLANAR -> distortPL(cached, (InterpolatePixelS)interp, outputType.getImageClass());
			case INTERLEAVED -> {
				if (interp instanceof InterpolatePixelS)
					throw new IllegalArgumentException("Interpolation function for single band images was" +
							" passed in for an interleaved image");
				yield distortIL(cached, (InterpolatePixelMB)interp, (ImageType)outputType);
			}
		};
	}

	/**
	 * Creates a {@link boofcv.alg.distort.ImageDistort} for the specified image type, transformation
	 * and interpolation instance.
	 *
	 * @param cached If true the distortion is only computed one. False for recomputed each time, but less memory.
	 * @param interp Which interpolation algorithm should be used.
	 * @param outputType Type of output image.
	 */
	public static <Input extends ImageGray<Input>, Output extends ImageGray<Output>>
	ImageDistort<Input, Output> distortSB( boolean cached, InterpolatePixelS<Input> interp, Class<Output> outputType ) {
		AssignPixelValue_SB<Output> assigner;
		if (outputType == GrayF32.class) {
			assigner = (AssignPixelValue_SB)new AssignPixelValue_SB.F32();
		} else if (GrayS32.class.isAssignableFrom(outputType)) {
			assigner = (AssignPixelValue_SB)new AssignPixelValue_SB.S32();
		} else if (GrayI16.class.isAssignableFrom(outputType)) {
			assigner = (AssignPixelValue_SB)new AssignPixelValue_SB.I16();
		} else if (GrayI8.class.isAssignableFrom(outputType)) {
			assigner = (AssignPixelValue_SB)new AssignPixelValue_SB.I8();
		} else {
			throw new IllegalArgumentException("Output type not supported: " + outputType.getSimpleName());
		}

		if (BoofConcurrency.USE_CONCURRENT) {
			if (cached) {
				return new ImageDistortCache_SB_MT<>(assigner, interp);
			} else {
				return new ImageDistortBasic_SB_MT<>(assigner, interp);
			}
		} else if (cached) {
			return new ImageDistortCache_SB<>(assigner, interp);
		} else {
			return new ImageDistortBasic_SB<>(assigner, interp);
		}
	}

	/**
	 * Creates a {@link boofcv.alg.distort.ImageDistort} for the planar images, transformation
	 * and interpolation instance.
	 *
	 * @param cached If true the distortion is only computed one. False for recomputed each time, but less memory.
	 * @param interp Which interpolation algorithm should be used.
	 * @param outputType Type of output image.
	 */
	public static <Input extends ImageGray<Input>, Output extends ImageGray<Output>>
	ImageDistort<Planar<Input>, Planar<Output>>
	distortPL( boolean cached, InterpolatePixelS<Input> interp, Class<Output> outputType ) {
		ImageDistort<Input, Output> distortSingle = distortSB(cached, interp, outputType);
		return new ImplImageDistort_PL<>(distortSingle);
	}

	public static <Input extends ImageInterleaved<Input>, Output extends ImageInterleaved<Output>>
	ImageDistort<Input, Output>
	distortIL( boolean cached, InterpolatePixelMB<Input> interp, ImageType<Output> outputType ) {
		if (cached) {
			throw new IllegalArgumentException("Cached not supported yet");
		} else {
			AssignPixelValue_MB<Output> assigner = switch (outputType.getDataType()) {
				case F32 -> (AssignPixelValue_MB)new AssignPixelValue_MB.F32();
				case S32 -> (AssignPixelValue_MB)new AssignPixelValue_MB.S32();
				case U16, S16, I16 -> (AssignPixelValue_MB)new AssignPixelValue_MB.I16();
				case U8, S8, I8 -> (AssignPixelValue_MB)new AssignPixelValue_MB.I8();
				default -> throw new RuntimeException("Not yet supported " + outputType);
			};

			if (BoofConcurrency.USE_CONCURRENT) {
				return new ImageDistortBasic_IL_MT<>(assigner, interp);
			} else {
				return new ImageDistortBasic_IL<>(assigner, interp);
			}
		}
	}

	public static PointDeformKeyPoints deformMls( ConfigDeformPointMLS config ) {
		if (config == null)
			config = new ConfigDeformPointMLS();

		ImageDeformPointMLS_F32 alg = new ImageDeformPointMLS_F32(config.type);
		alg.setAlpha(config.alpha);

		return new PointDeform_MLS(alg, config.rows, config.cols);
	}
}
