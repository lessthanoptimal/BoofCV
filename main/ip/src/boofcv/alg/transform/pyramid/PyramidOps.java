/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.transform.pyramid;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.pyramid.ImagePyramid;

import java.util.Random;


/**
 * Various operations related to image pyramids.
 *
 * @author Peter Abeles
 */
public class PyramidOps {

	/**
	 * Randomizes the image in each layers using a uniform distribution.
	 *
	 * @param pyramid Image pyramid.
	 * @param rand Random number generator.
	 * @param min min value.
	 * @param max maximum value.
	 */
	public static <I extends ImageBase>
	void randomize( ImagePyramid<I> pyramid , Random rand , int min , int max ) {

		for( int i = 0; i < pyramid.getNumLayers(); i++ ) {
			I imageIn = pyramid.getLayer(i);
			GeneralizedImageOps.randomize(imageIn,rand,min,max);
		}
	}

	/**
	 * <p>
	 * Runs an image filter through each layer in the pyramid.
	 * </p>
	 *
	 * <p>
	 * It is assumed that the output has the same scales as the input.  If not
	 * initialized then it will be initialized.  If already initialized it is
	 * assumed to be setup for the same input image size.
	 * </p>
	 *
	 * @param input Input pyramid.
	 * @param filter Filter being applied to the pyramid.
	 * @param output Output pyramid where filter results are saved.
	 */
	public static <I extends ImageBase, O extends ImageBase>
	void filter(ImagePyramid<I> input, FilterImageInterface<I, O> filter, ImagePyramid<O> output
	)
	{
		if( !output.isInitialized() )
			output.initialize(input.getInputWidth(),input.getInputHeight());

		for( int i = 0; i < output.getNumLayers(); i++ ) {
			I imageIn = input.getLayer(i);
			O imageOut = output.getLayer(i);

			filter.process(imageIn,imageOut);
		}
	}

	/**
	 * <p>
	 * Computes the gradient for each image the pyramid.
	 * </p>
	 *
	 * <p>
	 * It is assumed that the gradient has the same scales as the input.  If not
	 * initialized then it will be initialized.  If already initialized it is
	 * assumed to be setup for the same input image size.
	 * </p>
	 * 
	 * @param input Input pyramid.
	 * @param gradient Computes image gradient
	 * @param derivX Pyramid where x-derivative is stored.
	 * @param derivY Pyramid where y-derivative is stored.
	 */
	public static <I extends ImageBase, O extends ImageBase>
	void gradient(ImagePyramid<I> input,
				  ImageGradient<I, O> gradient, ImagePyramid<O> derivX,
				  ImagePyramid<O> derivY
	)
	{
		if( !derivX.isInitialized() )
			derivX.initialize(input.getInputWidth(),input.getInputHeight());
		else if( derivX.getInputWidth() != input.getInputWidth() ||
				derivX.getInputWidth() != input.getInputWidth() ) {
			derivX.initialize(input.getInputWidth(),input.getInputHeight());
		}

		if( !derivY.isInitialized() )
			derivY.initialize(input.getInputWidth(),input.getInputHeight());
		else if( derivY.getInputWidth() != input.getInputWidth() ||
				derivY.getInputWidth() != input.getInputWidth() ) {
			derivY.initialize(input.getInputWidth(),input.getInputHeight());
		}

		for( int i = 0; i < input.getNumLayers(); i++ ) {
			I imageIn = input.getLayer(i);
			O x = derivX.getLayer(i);
			O y = derivY.getLayer(i);

			gradient.process(imageIn,x,y);
		}
	}
}
