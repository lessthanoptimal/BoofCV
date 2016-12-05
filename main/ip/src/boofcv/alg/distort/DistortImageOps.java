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

package boofcv.alg.distort;

import boofcv.abst.distort.FDistort;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.ImageRectangle_F32;
import boofcv.struct.ImageRectangle_F64;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.distort.PixelTransform2_F64;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.shapes.RectangleLength2D_F32;
import georegression.struct.shapes.RectangleLength2D_F64;
import georegression.struct.shapes.RectangleLength2D_I32;


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
	 * @param input Which which is being rotated.
	 * @param output The image in which the output is written to.
	 * @param borderType Describes how pixels outside the image border should be handled.
	 * @param interpType Which type of interpolation will be used.
	 *
	 * @deprecated As of v0.19.  Use {@link FDistort} instead
	 */
	@Deprecated
	public static <T extends ImageBase>
	void affine(T input, T output, BorderType borderType, InterpolationType interpType,
				double a11, double a12, double a21, double a22,
				double dx, double dy)
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

		if( input instanceof ImageGray) {
			distortSingle((ImageGray)input, (ImageGray)output, model, interpType, borderType);
		} else if( input instanceof Planar) {
			distortPL((Planar) input, (Planar) output, model, borderType, interpType);
		}
	}

	/**
	 * Applies a pixel transform to a single band image.  Easier to use function.
	 *
	 * @deprecated As of v0.19.  Use {@link FDistort} instead
	 *
	 * @param input Input (source) image.
	 * @param output Where the result of transforming the image image is written to.
	 * @param transform The transform that is being applied to the image
	 * @param interpType Which type of pixel interpolation should be used. BILINEAR is in general recommended
	 * @param borderType Specifies how to handle image borders.
	 */
	public static <Input extends ImageGray,Output extends ImageGray>
	void distortSingle(Input input, Output output,
					   PixelTransform2_F32 transform,
					   InterpolationType interpType, BorderType borderType)
	{
		boolean skip = borderType == BorderType.SKIP;
		if( skip )
			borderType = BorderType.EXTENDED;

		Class<Input> inputType = (Class<Input>)input.getClass();
		Class<Output> outputType = (Class<Output>)input.getClass();
		InterpolatePixelS<Input> interp = FactoryInterpolation.createPixelS(0, 255, interpType, borderType, inputType);

		ImageDistort<Input,Output> distorter = FactoryDistort.distortSB(false, interp, outputType);
		distorter.setRenderAll(!skip);
		distorter.setModel(transform);
		distorter.apply(input,output);
	}

	/**
	 * Applies a pixel transform to a single band image.  More flexible but order to use function.
	 *
	 * @deprecated As of v0.19.  Use {@link FDistort} instead
	 *
	 * @param input Input (source) image.
	 * @param output Where the result of transforming the image image is written to.
	 * @param renderAll true it renders all pixels, even ones outside the input image.
	 * @param transform The transform that is being applied to the image
	 * @param interp Interpolation algorithm.
	 */
	public static <Input extends ImageGray,Output extends ImageGray>
	void distortSingle(Input input, Output output,
					   boolean renderAll, PixelTransform2_F32 transform,
					   InterpolatePixelS<Input> interp)
	{
		Class<Output> inputType = (Class<Output>)input.getClass();
		ImageDistort<Input,Output> distorter = FactoryDistort.distortSB(false, interp, inputType);
		distorter.setRenderAll(renderAll);
		distorter.setModel(transform);
		distorter.apply(input,output);
	}

	/**
	 * Applies a pixel transform to a {@link Planar} image.
	 *
	 * @deprecated As of v0.19.  Use {@link FDistort} instead
	 *
	 * @param input Input (source) image.
	 * @param output Where the result of transforming the image image is written to.
	 * @param transform The transform that is being applied to the image
	 * @param borderType Describes how pixels outside the image border should be handled.
	 * @param interpType Which type of pixel interpolation should be used.
	 */
	public static <Input extends ImageGray,Output extends ImageGray,
			M extends Planar<Input>,N extends Planar<Output>>
	void distortPL(M input, N output,
				   PixelTransform2_F32 transform,
				   BorderType borderType, InterpolationType interpType)
	{
		Class<Input> inputBandType = input.getBandType();
		Class<Output> outputBandType = output.getBandType();
		InterpolatePixelS<Input> interp = FactoryInterpolation.createPixelS(0, 255, interpType, borderType, inputBandType);

		ImageDistort<Input,Output> distorter = FactoryDistort.distortSB(false, interp, outputBandType);
		distorter.setModel(transform);

		distortPL(input,output,distorter);
	}

	/**
	 * Easy way to create {@link ImageDistort} given {@link PixelTransform2_F32}.  To improve
	 * performance the distortion is automatically cached.
	 *
	 * @see FactoryDistort
	 * @see FactoryInterpolation
	 *
	 * @deprecated As of v0.19.  Use {@link FDistort} instead
	 *
	 * @param transform Image transform.
	 * @param interpType Which interpolation. Try bilinear.
	 * @param inputType Image of single band image it will process.
	 * @return The {@link ImageDistort}
	 */
	public static <Input extends ImageGray,Output extends ImageGray>
	ImageDistort<Input,Output> createImageDistort(Point2Transform2_F32 transform,
												  InterpolationType interpType, BorderType borderType,
												  Class<Input> inputType, Class<Output> outputType)
	{
		InterpolatePixelS<Input> interp = FactoryInterpolation.createPixelS(0, 255, interpType,borderType, inputType);
		ImageDistort<Input,Output> distorter =
				FactoryDistort.distortSB(true, interp, outputType);
		distorter.setModel(new PointToPixelTransform_F32(transform));

		return distorter;
	}

	/**
	 * Rescales the input image and writes the results into the output image.  The scale
	 * factor is determined independently of the width and height.
	 * @param input Input image. Not modified.
	 * @param output Rescaled input image. Modified.
	 * @param borderType Describes how pixels outside the image border should be handled.
	 * @param interpType Which interpolation algorithm should be used.
	 */
	@Deprecated
	public static <T extends ImageBase>
	void scale(T input, T output, BorderType borderType, InterpolationType interpType) {

		PixelTransformAffine_F32 model = DistortSupport.transformScale(output, input, null);

		if( input instanceof ImageGray) {
			distortSingle((ImageGray) input, (ImageGray) output, model, interpType, borderType);
		} else if( input instanceof Planar) {
			distortPL((Planar) input, (Planar) output, model,  borderType, interpType);
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
	 * @deprecated As of v0.19.  Use {@link FDistort} instead
	 *
	 * @param input Which which is being rotated.
	 * @param output The image in which the output is written to.
	 * @param borderType Describes how pixels outside the image border should be handled.
	 * @param interpType Which type of interpolation will be used.
	 * @param angleInputToOutput Angle of rotation in radians. From input to output, CCW rotation.
	 */
	@Deprecated
	public static <T extends ImageBase>
	void rotate(T input, T output, BorderType borderType, InterpolationType interpType, float angleInputToOutput) {

		float offX = 0;//(output.width+1)%2;
		float offY = 0;//(output.height+1)%2;

		PixelTransform2_F32 model = DistortSupport.transformRotate(input.width / 2, input.height / 2,
				output.width / 2 - offX, output.height / 2 - offY, angleInputToOutput);

		if( input instanceof ImageGray) {
			distortSingle((ImageGray) input, (ImageGray) output, model, interpType, borderType);
		} else if( input instanceof Planar) {
			distortPL((Planar) input, (Planar) output, model,borderType, interpType);
		}
	}

	/**
	 * Applies a distortion to a {@link Planar} image.
	 *
	 * @deprecated As of v0.19.  Use {@link FDistort} instead
	 *
	 * @param input Image being distorted. Not modified.
	 * @param output Output image. modified.
	 * @param distortion The distortion model
	 * @param <Input> Band type.
	 */
	public static <Input extends ImageGray,Output extends ImageGray>
	void distortPL(Planar<Input> input , Planar<Output> output ,
				   ImageDistort<Input,Output> distortion )
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
	public static RectangleLength2D_I32 boundBox( int srcWidth , int srcHeight ,
												  int dstWidth , int dstHeight ,
												  PixelTransform2_F32 transform )
	{
		RectangleLength2D_I32 ret = boundBox(srcWidth,srcHeight,transform);

		int x0 = ret.x0;
		int y0 = ret.y0;
		int x1 = ret.x0 + ret.width;
		int y1 = ret.y0 + ret.height;

		if( x0 < 0 ) x0 = 0;
		if( x1 > dstWidth) x1 = dstWidth;
		if( y0 < 0 ) y0 = 0;
		if( y1 > dstHeight) y1 = dstHeight;

		return new RectangleLength2D_I32(x0,y0,x1-x0,y1-y0);
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
	public static RectangleLength2D_I32 boundBox( int srcWidth , int srcHeight ,
												  PixelTransform2_F32 transform )
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

		return new RectangleLength2D_I32(x0,y0,x1-x0,y1-y0);
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
	public static RectangleLength2D_F32 boundBox_F32( int srcWidth , int srcHeight ,
													  PixelTransform2_F32 transform )
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

		return new RectangleLength2D_F32(r.x0,r.y0,r.x1-r.x0,r.y1-r.y0);
	}

	private static void updateBoundBox(PixelTransform2_F32 transform, ImageRectangle_F32 r) {
		if( transform.distX < r.x0 )
			r.x0 = transform.distX;
		else if( transform.distX > r.x1 )
			r.x1 = transform.distX;
		if( transform.distY < r.y0 )
			r.y0 = transform.distY;
		else if( transform.distY > r.y1 )
			r.y1 = transform.distY;
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
	public static RectangleLength2D_F64 boundBox_F64( int srcWidth , int srcHeight ,
													  PixelTransform2_F64 transform )
	{
		ImageRectangle_F64 r = new ImageRectangle_F64();

		r.x0=r.y0=Double.MAX_VALUE;
		r.x1=r.y1=-Double.MAX_VALUE;

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

		return new RectangleLength2D_F64(r.x0,r.y0,r.x1-r.x0,r.y1-r.y0);
	}

	private static void updateBoundBox(PixelTransform2_F64 transform, ImageRectangle_F64 r) {
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
