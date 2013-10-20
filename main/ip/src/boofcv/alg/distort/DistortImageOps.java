/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort;

import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.ImageRectangle_F32;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.shapes.Rectangle2D_F32;
import georegression.struct.shapes.Rectangle2D_I32;


/**
 * <p>
 * Provides common function for distorting images.
 * </p>
 *
 * @author Peter Abeles
 */
public class DistortImageOps {

	/**
	 * <p>
	 * Applies an affine transformation from the input image to the output image.
	 * </p>
	 *
	 * <p>
	 * Input coordinates (x,y) to output coordinate (x',y')<br>
	 * x' = a11*x + a12*y + dx<br>
	 * y' = a21*x + a22*y + dy
	 * </p>
	 *
	 * @param input Which which is being rotated.
	 * @param output The image in which the output is written to.
	 * @param interpType Which type of interpolation will be used.
	 */
	public static <T extends ImageBase>
	void affine( T input , T output , TypeInterpolate interpType ,
				 double a11 , double a12, double a21 , double a22 ,
				 double dx, double dy )
	{
		Affine2D_F32 m = new Affine2D_F32();
		m.a11 = (float)a11;
		m.a12 = (float)a12;
		m.a21 = (float)a21;
		m.a22 = (float)a22;
		m.tx = (float)dx;
		m.ty = (float)dy;

		m = m.invert(null);

		PixelTransformAffine_F32 model = new PixelTransformAffine_F32(m);
		
		if( input instanceof ImageSingleBand ) {
			distortSingle((ImageSingleBand) input, (ImageSingleBand) output, model, false, interpType);
		} else if( input instanceof MultiSpectral ) {
			distortMS((MultiSpectral) input, (MultiSpectral) output, model, false, interpType);
		}
	}

	/**
	 * Applies a pixel transform to a single band image.  Easier to use function.
	 *
	 * @param input Input (source) image.
	 * @param output Where the result of transforming the image image is written to.
	 * @param transform The transform that is being applied to the image
	 * @param skipOutsidePixels Should pixels that go outside the source image be skipped over
	 *                          or set to the value of zero.
	 * @param interpType Which type of pixel interpolation should be used. BILINEAR is in general recommended
	 */
	public static <T extends ImageSingleBand>
	void distortSingle(T input, T output,
					   PixelTransform_F32 transform,
					   boolean skipOutsidePixels, TypeInterpolate interpType)
	{
		Class<T> inputType = (Class<T>)input.getClass();
		InterpolatePixelS<T> interp = FactoryInterpolation.createPixelS(0, 255, interpType, inputType);

		ImageBorder<T> border;
		if( skipOutsidePixels ) {
			border = null;
		} else {
			border = FactoryImageBorder.value(inputType,0);
		}

		ImageDistort<T> distorter = FactoryDistort.distort(interp, border, inputType);
		distorter.setModel(transform);
		distorter.apply(input,output);
	}

	/**
	 * Applies a pixel transform to a single band image.  More flexible but order to use function.
	 *
	 * @param input Input (source) image.
	 * @param output Where the result of transforming the image image is written to.
	 * @param transform The transform that is being applied to the image
	 * @param border How border pixels are handled.
	 * @param interp Interpolation algorithm.
	 */
	public static <T extends ImageSingleBand>
	void distortSingle(T input, T output,
					   PixelTransform_F32 transform,
					   ImageBorder<T> border,
					   InterpolatePixelS<T> interp )
	{
		Class<T> inputType = (Class<T>)input.getClass();
		ImageDistort<T> distorter = FactoryDistort.distort(interp, border, inputType);
		distorter.setModel(transform);
		distorter.apply(input,output);
	}

	/**
	 * Applies a pixel transform to a {@link MultiSpectral} image.
	 *
	 * @param input Input (source) image.
	 * @param output Where the result of transforming the image image is written to.
	 * @param transform The transform that is being applied to the image
	 * @param skipOutsidePixels Should pixels that go outside the source image be skipped over
	 *                          or set to the value of zero.
	 * @param interpType Which type of pixel interpolation should be used.
	 */
	public static <T extends ImageSingleBand,M extends MultiSpectral<T>>
	void distortMS(M input, M output,
				   PixelTransform_F32 transform, boolean skipOutsidePixels,
				   TypeInterpolate interpType)
	{
		Class<T> bandType = input.getType();
		InterpolatePixelS<T> interp = FactoryInterpolation.createPixelS(0, 255, interpType, bandType);

		ImageBorder<T> border;
		if( skipOutsidePixels ) {
			border = null;
		} else {
			border = FactoryImageBorder.value(bandType,0);
		}

		ImageDistort<T> distorter = FactoryDistort.distort(interp, border, bandType);
		distorter.setModel(transform);

		distortMS(input,output,distorter);
	}

	/**
	 * Easy way to create {@link ImageDistort} given {@link PixelTransform_F32}.  To improve
	 * performance the distortion is automatically cached.
	 *
	 * @see FactoryDistort
	 * @see FactoryInterpolation
	 *
	 * @param transform Image transform.
	 * @param interpType Which interpolation. Try bilinear.
	 * @param imageType Image of single band image it will process.
	 * @return The {@link ImageDistort}
	 */
	public static <T extends ImageSingleBand>
	ImageDistort<T> createImageDistort( PointTransform_F32 transform ,
										TypeInterpolate interpType,
										Class<T> imageType ) {
		InterpolatePixelS<T> interp = FactoryInterpolation.createPixelS(0, 255, interpType, imageType);
		ImageDistort<T> distorter = FactoryDistort.distortCached(interp, FactoryImageBorder.value(imageType, 0), imageType);
		distorter.setModel(new PointToPixelTransform_F32(transform));

		return distorter;
	}

	/**
	 * Rescales the input image and writes the results into the output image.  The scale
	 * factor is determined independently of the width and height.
	 *
	 * @param input Input image. Not modified.
	 * @param output Rescaled input image. Modified.
	 * @param interpType Which interpolation algorithm should be used.
	 */
	public static <T extends ImageBase>
	void scale( T input , T output , TypeInterpolate interpType ) {

		PixelTransformAffine_F32 model = DistortSupport.transformScale(output, input);

		if( input instanceof ImageSingleBand ) {
			distortSingle((ImageSingleBand) input, (ImageSingleBand) output, model, false, interpType);
		} else if( input instanceof MultiSpectral ) {
			distortMS((MultiSpectral) input, (MultiSpectral) output, model, false, interpType);
		}
	}

	/**
	 * <p>
	 * Rotates the image using the specified interpolation type.  The rotation is performed
	 * around the specified center of rotation in the input image.
	 * </p>
	 *
	 * <p>
	 * Input coordinates (x,y) to output coordinate (x',y')<br>
	 * x' = x_c + c*(x-x_c) - s(y - y_c)<br>
	 * y' = y_c + s*(x-x_c) + c(y - y_c)
	 * </p>
	 *
	 * @param input Which which is being rotated.
	 * @param output The image in which the output is written to.
	 * @param interpType Which type of interpolation will be used.
	 * @param angleInputToOutput Angle of rotation in radians. From input to output, CCW rotation.
	 */
	public static <T extends ImageBase>
	void rotate( T input , T output , TypeInterpolate interpType , float angleInputToOutput ) {

		float offX = 0;//(output.width+1)%2;
		float offY = 0;//(output.height+1)%2;

		PixelTransform_F32 model = DistortSupport.transformRotate(input.width/2,input.height/2,
				output.width/2-offX,output.height/2-offY,angleInputToOutput);

		if( input instanceof ImageSingleBand ) {
			distortSingle((ImageSingleBand) input, (ImageSingleBand) output, model, false, interpType);
		} else if( input instanceof MultiSpectral ) {
			distortMS((MultiSpectral) input, (MultiSpectral) output, model, false, interpType);
		}
	}

	/**
	 * Applies a distortion to a {@link MultiSpectral} image.
	 *
	 * @param input Image being distorted. Not modified.
	 * @param output Output image. modified.
	 * @param distortion The distortion model
	 * @param <T> Band type.
	 */
	public static <T extends ImageSingleBand> void distortMS( MultiSpectral<T> input , MultiSpectral<T> output ,
															  ImageDistort<T> distortion )
	{
		for( int band = 0; band < input.getNumBands(); band++ )
			distortion.apply(input.getBand(band),output.getBand(band));
	}

	/**
	 * Finds an axis-aligned bounding box which would contain a image after it has been transformed.
	 * A sanity check is done to made sure it is contained inside the destination image's bounds.
	 * If it is totally outside then a rectangle with negative width or height is returned.
	 *
	 * @param srcWidth Width of the source image
	 * @param srcHeight Height of the source image
	 * @param dstWidth Width of the destination image
	 * @param dstHeight Height of the destination image
	 * @param transform Transform being applied to the image
	 * @return Bounding box
	 */
	public static Rectangle2D_I32 boundBox( int srcWidth , int srcHeight ,
											int dstWidth , int dstHeight ,
											PixelTransform_F32 transform )
	{
		Rectangle2D_I32 ret = boundBox(srcWidth,srcHeight,transform);

		int x0 = ret.tl_x;
		int y0 = ret.tl_y;
		int x1 = ret.tl_x + ret.width;
		int y1 = ret.tl_y + ret.height;

		if( x0 < 0 ) x0 = 0;
		if( x1 > dstWidth) x1 = dstWidth;
		if( y0 < 0 ) y0 = 0;
		if( y1 > dstHeight) y1 = dstHeight;

		return new Rectangle2D_I32(x0,y0,x1-x0,y1-y0);
	}

	/**
	 * Finds an axis-aligned bounding box which would contain a image after it has been transformed.
	 * The returned bounding box can be larger then the original image.
	 *
	 * @param srcWidth Width of the source image
	 * @param srcHeight Height of the source image
	 * @param transform Transform being applied to the image
	 * @return Bounding box
	 */
	public static Rectangle2D_I32 boundBox( int srcWidth , int srcHeight ,
											PixelTransform_F32 transform )
	{
		int x0,y0,x1,y1;

		transform.compute(0,0);
		x0=x1=(int)transform.distX;
		y0=y1=(int)transform.distY;

		for( int i = 1; i < 4; i++ ) {
			if( i == 1 )
				transform.compute(srcWidth,0);
			else if( i == 2 )
				transform.compute(0,srcHeight);
			else if( i == 3 )
				transform.compute(srcWidth-1,srcHeight);

			if( transform.distX < x0 )
				x0 = (int)transform.distX;
			else if( transform.distX > x1 )
				x1 = (int)transform.distX;
			if( transform.distY < y0 )
				y0 = (int)transform.distY;
			else if( transform.distY > y1 )
				y1 = (int)transform.distY;
		}

		return new Rectangle2D_I32(x0,y0,x1-x0,y1-y0);
	}

	/**
	 * Finds an axis-aligned bounding box which would contain a image after it has been transformed.
	 * The returned bounding box can be larger then the original image.
	 *
	 * @param srcWidth Width of the source image
	 * @param srcHeight Height of the source image
	 * @param transform Transform being applied to the image
	 * @return Bounding box
	 */
	public static Rectangle2D_F32 boundBox_F32( int srcWidth , int srcHeight ,
												PixelTransform_F32 transform )
	{
		ImageRectangle_F32 r=new ImageRectangle_F32();

		r.x0=r.y0=Float.MAX_VALUE;
		r.x1=r.y1=-Float.MAX_VALUE;

		for( int y = 0; y < srcHeight; y++ ) {
			transform.compute(0, y);
			updateBoundBox(transform, r);
			transform.compute(srcWidth, y);
			updateBoundBox(transform, r);
		}

		for( int x = 0; x < srcWidth; x++ ) {
			transform.compute(x, 0);
			updateBoundBox(transform, r);
			transform.compute(x, srcHeight);
			updateBoundBox(transform, r);
		}

		return new Rectangle2D_F32(r.x0,r.y0,r.x1-r.x0,r.y1-r.y0);
	}

	private static void updateBoundBox(PixelTransform_F32 transform, ImageRectangle_F32 r) {
		if( transform.distX < r.x0 )
			r.x0 = transform.distX;
		else if( transform.distX > r.x1 )
			r.x1 = transform.distX;
		if( transform.distY < r.y0 )
			r.y0 = transform.distY;
		else if( transform.distY > r.y1 )
			r.y1 = transform.distY;
	}
}
