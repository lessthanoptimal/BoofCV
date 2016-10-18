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

package boofcv.alg.flow;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.transform.pyramid.PyramidFloatGaussianScale;
import boofcv.alg.transform.pyramid.PyramidFloatScale;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.PyramidFloat;

/**
 * Useful functions when computing dense optical flow
 *
 * @author Peter Abeles
 */
public class UtilDenseOpticalFlow {

	/**
	 * <p>
	 * Create a standard image pyramid used by dense optical flow parameters.  The first layer is the size
	 * of the input image and the last layer is &ge; the minSize.  The sigma for each layer is computed
	 * using the following formula:<br>
	 * <br>
	 * sigmaLayer = sigma*sqrt( scale^-2 - 1 )
	 * </p>
	 *
	 * <p>
	 * If the scale is 1 then a single layer pyramid will be created.  If the scale is 0 then the scale will
	 * be determined by the maxLayers parameter.
	 * </p>
	 *
	 * @param width Width of input image.
	 * @param height Height of input image.
	 * @param scale Scale between layers.  0 &le; scale &le; 1.  Try 0.7
	 * @param sigma Adjusts the amount of blur applied to each layer.  If sigma &le; 0 then no blur is applied.
	 * @param minSize The minimum desired image size in the pyramid
	 * @param maxLayers The maximum number of layers in the pyramid.
	 * @param imageType Type of image for each layer
	 * @param <T> Image type
	 * @return The image pyramid.
	 */
	public static <T extends ImageGray>
	PyramidFloat<T> standardPyramid( int width , int height ,
									 double scale, double sigma ,
									 int minSize, int maxLayers , Class<T> imageType ) {
		if( scale > 1.0 || scale < 0 )
			throw new IllegalArgumentException("Scale must be 0 <= scale <= 1");

		int numScales;

		if( scale == 1 || maxLayers == 1 ) {
			numScales = 1;
		} else if ( scale == 0 ) {
			numScales = maxLayers;

			double desiredReduction = minSize/(double)Math.min(width,height);
			scale = Math.pow(desiredReduction,1.0/(numScales-1));

		} else {
			// this is how much the input image needs to be shrunk
			double desiredReduction = minSize/(double)Math.min(width,height);

			// number the number of frames needed and round to the nearest integer
			numScales = (int)(Math.log(desiredReduction)/Math.log(scale) + 0.5);

			if( numScales > maxLayers )
				numScales = maxLayers;

			// compute a new scale factor using this number of scales
			scale = Math.pow(desiredReduction,1.0/numScales);

			// add one since the first scale is going to be the original image
			numScales++;
		}

		InterpolatePixelS<T> interp = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);

		if( sigma > 0 ) {
			double layerSigma = sigma*Math.sqrt(Math.pow(scale,-2)-1);

			double scaleFactors[] = new double[ numScales ];
			double scaleSigmas[] = new double[ numScales ];

			scaleFactors[0] = 1;
			scaleSigmas[0] = layerSigma;
			for( int i = 1; i < numScales; i++ ) {
				scaleFactors[i] = scaleFactors[i-1]/scale;
				scaleSigmas[i] = layerSigma;
			}

			return new PyramidFloatGaussianScale<>(interp, scaleFactors, scaleSigmas, imageType);
		} else {
			double scaleFactors[] = new double[ numScales ];

			scaleFactors[0] = 1;
			for( int i = 1; i < numScales; i++ ) {
				scaleFactors[i] = scaleFactors[i-1]/scale;
			}

			return new PyramidFloatScale<>(interp, scaleFactors, imageType);
		}
	}
}
