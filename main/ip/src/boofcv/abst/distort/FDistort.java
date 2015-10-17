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
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.affine.Affine2D_F64;

/**
 * High level interface for rendering a distorted image into another one.  Uses a flow style interface to remove
 * much of the drugery.
 *
 * @author Peter Abeles
 */
public class FDistort
{
	// type of input image
	ImageType inputType;

	// input and output images
	ImageBase input,output;
	// specifies how the borders are handled
	ImageDistort distorter;
	InterpolatePixel interp;
	PixelTransform_F32 outputToInput;

	// if the transform should be cached or not.
	boolean cached;

	/**
	 * Constructor in which input and output images are specified.  Equivalent to calling
	 * {@link #init(ImageBase, ImageBase)}
	 *
	 * @param input Input image
	 * @param output output image
	 */
	public FDistort(ImageBase input, ImageBase output) {
		init(input, output);
	}

	/**
	 * Constructor
	 * @param inputType Type of input image
	 */
	public FDistort(ImageType inputType) {
		this.inputType = inputType;
	}

	public FDistort() {
	}

	/**
	 * Specifies the input and output image.
	 */
	public FDistort init(ImageBase input, ImageBase output) {
		this.input = input;
		this.output = output;

		inputType = input.getImageType();
		interp(TypeInterpolate.BILINEAR);
		border(0);

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
		}
		this.input = input;
		inputType = input.getImageType();
		return this;
	}

	/**
	 * Changes the output image.  The previous distortion is thrown away only if the output
	 * image has a different shape
	 */
	public FDistort output( ImageBase output ) {
		if( this.output == null || this.output.width != output.width || this.output.height != output.height ) {
			distorter = null;
		}
		this.output = output;
		return this;
	}

	/**
	 * Sets how the interpolation handles borders.
	 */
	public FDistort border( ImageBorder border ) {
		distorter = null;
		interp.setBorder(border);
		return this;
	}

	/**
	 * Sets the border by type.
	 */
	public FDistort border( BorderType type ) {
		return border(FactoryImageBorder.generic(type, inputType));
	}

	/**
	 * Sets the border to a fixed gray-scale value
	 */
	public FDistort border( double value ) {
		return border(FactoryImageBorder.genericValue(value, inputType));
	}

	/**
	 * Sets the border to EXTEND
	 */
	public FDistort borderExt() {
		return border(BorderType.EXTENDED);
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
		this.interp = FactoryInterpolation.createPixel(0, 255, type, BorderType.EXTENDED, inputType);
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

	public FDistort affine( Affine2D_F64 affine ) {
		return affine(affine.a11,affine.a12,affine.a21,affine.a22,affine.tx,affine.ty);
	}

	/**
	 * Applies a distortion which will rescale the input image into the output image.  You
	 * might want to consider using {@link #scaleExt()} instead since it sets the border behavior to extended, which
	 * is probably what you want to do.
	 */
	public FDistort scale() {
		return transform(DistortSupport.transformScale(output, input));
	}

	/**
	 * Scales the image and sets the border to {@link BorderType#EXTENDED}. This is normally what you want
	 * to do when scaling an image.  If you don't use an extended border when you hit the right and bottom
	 * boundaries it will go outside the image bounds and if a fixed value of 0 is used it will average towards
	 * zero.
	 */
	public FDistort scaleExt() {
		return scale().borderExt();
	}

	/**
	 * Applies a distortion which will rotate the input image by the specified amount.
	 */
	public FDistort rotate( double angleInputToOutput ) {
		PixelTransform_F32 outputToInput = DistortSupport.transformRotate(input.width/2,input.height/2,
				output.width/2,output.height/2,(float)angleInputToOutput);

		return transform(outputToInput);
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
					distorter = FactoryDistort.distortSB(cached, (InterpolatePixelS)interp, typeOut);
					break;

				case MULTI_SPECTRAL:
					distorter = FactoryDistort.distortMS(cached, (InterpolatePixelS)interp, typeOut);
					break;

				case INTERLEAVED:
					distorter = FactoryDistort.distortIL(cached, (InterpolatePixelMB) interp, output.getImageType());
					break;

				default:
					throw new IllegalArgumentException("Unsupported image type");
			}
		}
		distorter.setModel(outputToInput);

		distorter.apply(input,output);
	}
}
