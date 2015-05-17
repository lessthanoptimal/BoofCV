/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.distort;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.affine.Affine2D_F32;

/**
 * High level interface for rendering a distorted image into another one.  Uses a flow style interface to remove
 * much of the drugery.
 *
 * @author Peter Abeles
 */
// TODO rename.  Too similar to ImageDistort.
	// Distort , FDistort
public class FDistort
{
	// input and output images
	ImageBase input,output;
	// specifies how the borders are handled
	ImageBorder border;
	ImageDistort distorter;
	InterpolatePixelS interp;
	PixelTransform_F32 outputToInput;

	// if the transform should be cached or not.
	boolean cached;

	public FDistort(ImageBase input, ImageBase output) {
		init(input, output);
	}

	public FDistort() {
	}

	public FDistort init(ImageBase input, ImageBase output) {
		this.input = input;
		this.output = output;

		ImageType inputType = input.getImageType();
		interp = FactoryInterpolation.createPixelS(0, 255, TypeInterpolate.BILINEAR, inputType.getImageClass());

		switch( inputType.getFamily() ) {
			case SINGLE_BAND:
				border = FactoryImageBorder.value(inputType.getImageClass(), 0);
				break;

			case MULTI_SPECTRAL:
				border = FactoryImageBorder.value(inputType.getImageClass(), 0);
				break;

			default:
				throw new IllegalArgumentException("Unsupported image type");
		}
		cached = false;
		distorter = null;
		outputToInput = null;

		return this;
	}

	/**
	 * Changes the input image.  The previous distortion is thrown away only if the input
	 * image has a different shape
	 */
	public FDistort input( ImageBase input ) {
		if( this.input == null || this.input.width != input.width || this.input.height != input.height ) {
			distorter = null;
			outputToInput = null;
		}
		this.input = input;
		return this;
	}

	/**
	 * Changes the output image.  The previous distortion is thrown away only if the output
	 * image has a different shape
	 */
	public FDistort output( ImageBase output ) {
		if( this.output == null || this.output.width != output.width || this.output.height != output.height ) {
			distorter = null;
			outputToInput = null;
		}
		this.output = output;
		return this;
	}

	/**
	 * Sets the border.  null means those pixels are skipped
	 */
	public FDistort border( ImageBorder border ) {
		distorter = null;
		this.border = border;
		return this;
	}

	/**
	 * used to provide a custom interpolation algorithm
	 */
	public FDistort interp(InterpolatePixelS interpolation) {
		distorter = null;
		this.interp = interpolation;
		return this;
	}

	/**
	 * Specifies the interpolation used by type.
	 */
	public FDistort interp(TypeInterpolate type) {
		distorter = null;
		this.interp = FactoryInterpolation.createPixelS(0, 255, type,
				input.getImageType().getImageClass());
		;
		return this;
	}

	/**
	 * Sets interpolation to use nearest-neighbor
	 */
	public FDistort interpNN() {
		return interp(TypeInterpolate.NEAREST_NEIGHBOR);
	}

	/**
	 * used to turn on and off caching of the distortion
	 */
	public FDistort cached( boolean cached ) {
		distorter = null;
		this.cached = cached;
		return this;
	}

	/**
	 * Used to manually specify a transform.  From output to input
	 */
	public FDistort transform( PixelTransform_F32 outputToInput ) {
		this.outputToInput = outputToInput;
		return this;
	}

	/**
	 * Used to manually specify a transform.  From output to input
	 */
	public FDistort transform( PointTransform_F32 outputToInput ) {
		return transform( new PointToPixelTransform_F32(outputToInput));
	}

	/**
	 * Affine transform from input to output
	 */
	public FDistort affine(double a11, double a12, double a21, double a22,
						  double dx, double dy) {

		Affine2D_F32 m = new Affine2D_F32();
		m.a11 = (float)a11;
		m.a12 = (float)a12;
		m.a21 = (float)a21;
		m.a22 = (float)a22;
		m.tx = (float)dx;
		m.ty = (float)dy;

		m = m.invert(null);

		return transform(new PixelTransformAffine_F32(m));
	}

	/**
	 * Applies a distortion which will rescale the input image into the output image
	 */
	public FDistort scale() {
		return transform(DistortSupport.transformScale(output, input));
	}

	/**
	 * Applies a distortion which will rotate the input image by the specified amount.
	 */
	public FDistort rotate( double angleInputToOutput ) {
		PixelTransform_F32 model = DistortSupport.transformRotate(input.width/2,input.height/2,
				output.width/2,output.height/2,(float)angleInputToOutput);

		return transform(model);
	}

	/**
	 * Applies the distortion.
	 */
	public void apply() {
		// see if the distortion class needs to be created again
		if( distorter == null ) {
			Class typeOut = output.getImageType().getImageClass();
			switch( input.getImageType().getFamily() ) {
				case SINGLE_BAND:
					distorter = FactoryDistort.distort(cached, interp, border, typeOut);
					break;

				case MULTI_SPECTRAL:
					distorter = FactoryDistort.distortMS(cached, interp, border, typeOut);
					break;

				default:
					throw new IllegalArgumentException("Unsupported image type");
			}
		}
		distorter.setModel(outputToInput);

		distorter.apply(input,output);
	}
}
