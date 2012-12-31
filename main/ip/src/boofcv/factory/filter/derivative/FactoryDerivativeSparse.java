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

package boofcv.factory.filter.derivative;

import boofcv.abst.filter.ImageFunctionSparse;
import boofcv.abst.filter.convolve.ImageConvolveSparse;
import boofcv.alg.filter.derivative.LaplacianEdge;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.*;
import boofcv.factory.filter.convolve.FactoryConvolveSparse;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageInteger;
import boofcv.struct.image.ImageSingleBand;

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
	 * @param imageType The type of image which is to be processed.
	 * @param border How the border should be handled.  If null {@link BorderType#EXTENDED} will be used.
	 * @return Filter for performing a sparse laplacian.
	 */
	public static <T extends ImageSingleBand> ImageFunctionSparse<T> createLaplacian( Class<T> imageType , ImageBorder<T> border )
	{
		if( border == null ) {
			border = FactoryImageBorder.general(imageType,BorderType.EXTENDED);
		}

		if( GeneralizedImageOps.isFloatingPoint(imageType)) {
			ImageConvolveSparse<ImageFloat32, Kernel2D_F32> r = FactoryConvolveSparse.create(ImageFloat32.class,LaplacianEdge.kernel_F32);

			r.setImageBorder((ImageBorder_F32)border);

			return (ImageFunctionSparse<T>)r;
		} else {
			ImageConvolveSparse<ImageInteger, Kernel2D_I32> r = FactoryConvolveSparse.create(ImageInteger.class,LaplacianEdge.kernel_I32);

			r.setImageBorder((ImageBorder_I32)border);

			return (ImageFunctionSparse<T>)r;
		}
	}
}
