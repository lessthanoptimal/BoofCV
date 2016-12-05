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

package boofcv.abst.distort;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.affine.Affine2D_F64;

/**
 * <p>High level interface for rendering a distorted image into another one.  Uses a flow style interface to remove
 * much of the drugery.</p>
 *
 * <p>If you are changing the input images and avoiding declaring new memory then you need to be careful
 * how this class is used.  For example, call {@link #setRefs(ImageBase, ImageBase)} instead of
 * {@link #init(ImageBase, ImageBase)}.  Init() will discard the previous settings while with setRefs() it's possible
 * to update only what has changed.  Make sure you follow the instructions in setRefs() and browsing the code in this
 * class might help you understand what's going on.</p>
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
	PixelTransform2_F32 outputToInput;

	// type of border being used
	BorderType borderType;

	// if the transform should be cached or not.
	boolean cached = false;

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
	 * Specifies the input and output image and sets interpolation to BILINEAR, black image border, cache is off.
	 */
	public FDistort init(ImageBase input, ImageBase output) {
		this.input = input;
		this.output = output;

		inputType = input.getImageType();
		interp(InterpolationType.BILINEAR);
		border(0);

		cached = false;
		distorter = null;
		outputToInput = null;

		return this;
	}

	/**
	 * All this does is set the references to the images.  Nothing else is changed and its up to the
	 * user to correctly update everything else.
	 *
	 * If called the first time you need to do the following
	 * <pre>
	 * 1) specify the interpolation method
	 * 2) specify the transform
	 * 3) specify the border
	 * </pre>
	 *
	 * If called again and the image shape has changed you need to do the following:
	 * <pre>
	 * 1) Update the transform
	 * </pre>
	 */
	public FDistort setRefs( ImageBase input, ImageBase output ) {
		this.input = input;
		this.output = output;

		inputType = input.getImageType();
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
		interp.setBorder(border);
		return this;
	}

	/**
	 * Sets the border by type.
	 */
	public FDistort border( BorderType type ) {
		if( borderType == type )
			return this;
		borderType = type;
		return border(FactoryImageBorder.generic(type, inputType));
	}

	/**
	 * Sets the border to a fixed gray-scale value
	 */
	public FDistort border( double value ) {
		// to recycle here the value also needs to be saved
//		if( borderType == BorderType.VALUE )
//			return this;
		borderType = BorderType.ZERO;
		return border(FactoryImageBorder.genericValue(value, inputType));
	}

	/**
	 * <p>Sets the border to EXTEND.</p>
	 */
	public FDistort borderExt() {
		return border(BorderType.EXTENDED);
	}


	/**
	 * used to provide a custom interpolation algorithm
	 *
	 * <p>
	 * NOTE: This will force the distorter to be declared again, even if nothing has changed.  This only matters if
	 * you are being very careful about your memory management.
	 * </p>
	 */
	public FDistort interp(InterpolatePixelS interpolation) {
		distorter = null;
		this.interp = interpolation;
		return this;
	}

	/**
	 * Specifies the interpolation used by type.
	 */
	public FDistort interp(InterpolationType type) {
		distorter = null;
		this.interp = FactoryInterpolation.createPixel(0, 255, type, BorderType.EXTENDED, inputType);

		return this;
	}

	/**
	 * Sets interpolation to use nearest-neighbor
	 */
	public FDistort interpNN() {
		return interp(InterpolationType.NEAREST_NEIGHBOR);
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
	public FDistort transform( PixelTransform2_F32 outputToInput ) {
		this.outputToInput = outputToInput;
		return this;
	}

	/**
	 * Used to manually specify a transform.  From output to input
	 */
	public FDistort transform( Point2Transform2_F32 outputToInput ) {
		return transform( new PointToPixelTransform_F32(outputToInput));
	}

	/**
	 * Affine transform from input to output
	 */
	public FDistort affine(double a11, double a12, double a21, double a22,
						   double dx, double dy) {

		PixelTransformAffine_F32 transform;

		if( outputToInput != null && outputToInput instanceof PixelTransformAffine_F32 ) {
			transform = (PixelTransformAffine_F32)outputToInput;
		} else {
			transform = new PixelTransformAffine_F32();
		}

		Affine2D_F32 m = new Affine2D_F32();

		m.a11 = (float)a11;
		m.a12 = (float)a12;
		m.a21 = (float)a21;
		m.a22 = (float)a22;
		m.tx = (float)dx;
		m.ty = (float)dy;

		m.invert(transform.getModel());

		return transform(transform);
	}

	public FDistort affine( Affine2D_F64 affine ) {
		return affine(affine.a11,affine.a12,affine.a21,affine.a22,affine.tx,affine.ty);
	}

	/**
	 * <p>Applies a distortion which will rescale the input image into the output image.  You
	 * might want to consider using {@link #scaleExt()} instead since it sets the border behavior
	 * to extended, which is probably what you want to do.</p>
	 *
	 * NOTE: Checks to see if it can recycle the previous transform and update it with a new affine model
	 * to avoid declaring new memory.
	 */
	public FDistort scale() {
		if( outputToInput != null && outputToInput instanceof PixelTransformAffine_F32 ) {
			PixelTransformAffine_F32 affine = (PixelTransformAffine_F32)outputToInput;
			DistortSupport.transformScale(output, input, affine);
			return this;
		} else {
			return transform(DistortSupport.transformScale(output, input, null));
		}
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
		PixelTransform2_F32 outputToInput = DistortSupport.transformRotate(input.width/2,input.height/2,
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
				case GRAY:
					distorter = FactoryDistort.distortSB(cached, (InterpolatePixelS)interp, typeOut);
					break;

				case PLANAR:
					distorter = FactoryDistort.distortPL(cached, (InterpolatePixelS)interp, typeOut);
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
