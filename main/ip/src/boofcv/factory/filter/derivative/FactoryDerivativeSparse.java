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

package boofcv.factory.filter.derivative;

import boofcv.abst.filter.ImageFunctionSparse;
import boofcv.abst.filter.convolve.ImageConvolveSparse;
import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.alg.filter.derivative.LaplacianEdge;
import boofcv.alg.filter.derivative.impl.*;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.*;
import boofcv.factory.filter.convolve.FactoryConvolveSparse;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.sparse.GradientValue;
import boofcv.struct.sparse.SparseImageGradient;

/**
 * Creates filters for performing sparse derivative calculations.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryDerivativeSparse {

	/**
	 * Creates a sparse Laplacian filter.
	 *
	 * @see boofcv.alg.filter.derivative.LaplacianEdge
	 *
	 * @param imageType The type of image which is to be processed.
	 * @param border How the border should be handled.  If null {@link BorderType#EXTENDED} will be used.
	 * @return Filter for performing a sparse laplacian.
	 */
	public static <T extends ImageGray>
	ImageFunctionSparse<T> createLaplacian( Class<T> imageType , ImageBorder<T> border )
	{
		if( border == null ) {
			border = FactoryImageBorder.single(imageType, BorderType.EXTENDED);
		}

		if( GeneralizedImageOps.isFloatingPoint(imageType)) {
			ImageConvolveSparse<GrayF32, Kernel2D_F32> r = FactoryConvolveSparse.convolve2D(GrayF32.class, LaplacianEdge.kernel_F32);

			r.setImageBorder((ImageBorder_F32)border);

			return (ImageFunctionSparse<T>)r;
		} else {
			ImageConvolveSparse<GrayI, Kernel2D_I32> r = FactoryConvolveSparse.convolve2D(GrayI.class, LaplacianEdge.kernel_I32);

			r.setImageBorder((ImageBorder_S32)border);

			return (ImageFunctionSparse<T>)r;
		}
	}

	/**
	 * Creates a sparse sobel gradient operator.
	 *
	 * @see GradientSobel
	 *
	 * @param imageType The type of image which is to be processed.
	 * @param border How the border should be handled.  If null then the borders can't be processed.
	 * @return Sparse gradient
	 */
	public static <T extends ImageGray, G extends GradientValue>
	SparseImageGradient<T,G> createSobel( Class<T> imageType , ImageBorder<T> border )
	{
		if( imageType == GrayF32.class) {
			return (SparseImageGradient)new GradientSparseSobel_F32((ImageBorder_F32)border);
		} else if( imageType == GrayU8.class ){
			return (SparseImageGradient)new GradientSparseSobel_U8((ImageBorder_S32)border);
		} else {
			throw new IllegalArgumentException("Unsupported image type "+imageType.getSimpleName());
		}
	}

	/**
	 * Creates a sparse prewitt gradient operator.
	 *
	 * @see boofcv.alg.filter.derivative.GradientPrewitt
	 *
	 * @param imageType The type of image which is to be processed.
	 * @param border How the border should be handled.  If null then the borders can't be processed.
	 * @return Sparse gradient.
	 */
	public static <T extends ImageGray, G extends GradientValue>
	SparseImageGradient<T,G> createPrewitt( Class<T> imageType , ImageBorder<T> border )
	{
		if( imageType == GrayF32.class) {
			return (SparseImageGradient)new GradientSparsePrewitt_F32((ImageBorder_F32)border);
		} else if( imageType == GrayU8.class ){
			return (SparseImageGradient)new GradientSparsePrewitt_U8((ImageBorder_S32)border);
		} else {
			throw new IllegalArgumentException("Unsupported image type "+imageType.getSimpleName());
		}
	}

	/**
	 * Creates a sparse three gradient operator.
	 *
	 * @see boofcv.alg.filter.derivative.GradientThree
	 *
	 * @param imageType The type of image which is to be processed.
	 * @param border How the border should be handled.  If null then the borders can't be processed.
	 * @return Sparse gradient.
	 */
	public static <T extends ImageGray, G extends GradientValue>
	SparseImageGradient<T,G> createThree( Class<T> imageType , ImageBorder<T> border )
	{
		if( imageType == GrayF32.class) {
			return (SparseImageGradient)new GradientSparseThree_F32((ImageBorder_F32)border);
		} else if( imageType == GrayU8.class ){
			return (SparseImageGradient)new GradientSparseThree_U8((ImageBorder_S32)border);
		} else {
			throw new IllegalArgumentException("Unsupported image type "+imageType.getSimpleName());
		}
	}

	/**
	 * Creates a sparse two-0 gradient operator.
	 *
	 * @see boofcv.alg.filter.derivative.GradientTwo0
	 *
	 * @param imageType The type of image which is to be processed.
	 * @param border How the border should be handled.  If null then the borders can't be processed.
	 * @return Sparse gradient.
	 */
	public static <T extends ImageGray, G extends GradientValue>
	SparseImageGradient<T,G> createTwo0( Class<T> imageType , ImageBorder<T> border )
	{
		if( imageType == GrayF32.class) {
			return (SparseImageGradient)new GradientSparseTwo0_F32((ImageBorder_F32)border);
		} else if( imageType == GrayU8.class ){
			return (SparseImageGradient)new GradientSparseTwo0_U8((ImageBorder_S32)border);
		} else {
			throw new IllegalArgumentException("Unsupported image type "+imageType.getSimpleName());
		}
	}

	/**
	 * Creates a sparse two-1 gradient operator.
	 *
	 * @see boofcv.alg.filter.derivative.GradientTwo1
	 *
	 * @param imageType The type of image which is to be processed.
	 * @param border How the border should be handled.  If null then the borders can't be processed.
	 * @return Sparse gradient.
	 */
	public static <T extends ImageGray, G extends GradientValue>
	SparseImageGradient<T,G> createTwo1( Class<T> imageType , ImageBorder<T> border )
	{
		if( imageType == GrayF32.class) {
			return (SparseImageGradient)new GradientSparseTwo1_F32((ImageBorder_F32)border);
		} else if( imageType == GrayU8.class ){
			return (SparseImageGradient)new GradientSparseTwo1_U8((ImageBorder_S32)border);
		} else {
			throw new IllegalArgumentException("Unsupported image type "+imageType.getSimpleName());
		}
	}
}
