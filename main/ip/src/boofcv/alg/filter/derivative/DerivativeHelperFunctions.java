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

package boofcv.alg.filter.derivative;

import boofcv.alg.filter.convolve.ConvolveImageNoBorder;
import boofcv.alg.filter.convolve.border.ConvolveJustBorder_General;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.core.image.border.ImageBorder_S32;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;


/**
 * @author Peter Abeles
 */
public class DerivativeHelperFunctions {

	public static void processBorderHorizontal(GrayU8 orig , GrayS16 deriv ,
											   Kernel1D_I32 kernel , ImageBorder_S32 borderType )
	{
		borderType.setImage(orig);
		ConvolveJustBorder_General.horizontal(kernel, borderType,deriv);

		GrayU8 origSub;
		GrayS16 derivSub;

		origSub = orig.subimage(0,0,orig.width,2, null);
		derivSub = deriv.subimage(0,0,orig.width,2, null);
		ConvolveImageNoBorder.horizontal(kernel,origSub,derivSub);
		origSub = orig.subimage(0,orig.height-2,orig.width,orig.height, null);
		derivSub = deriv.subimage(0,orig.height-2,orig.width,orig.height, null);
		ConvolveImageNoBorder.horizontal(kernel,origSub,derivSub);
	}

	public static void processBorderHorizontal(GrayU8 orig , GrayS32 deriv ,
											   Kernel1D_I32 kernel , ImageBorder_S32 borderType )
	{
		borderType.setImage(orig);
		ConvolveJustBorder_General.horizontal(kernel, borderType,deriv);

		GrayU8 origSub;
		GrayS32 derivSub;

		origSub = orig.subimage(0,0,orig.width,2, null);
		derivSub = deriv.subimage(0,0,orig.width,2, null);
		ConvolveImageNoBorder.horizontal(kernel,origSub,derivSub);
		origSub = orig.subimage(0,orig.height-2,orig.width,orig.height, null);
		derivSub = deriv.subimage(0,orig.height-2,orig.width,orig.height, null);
		ConvolveImageNoBorder.horizontal(kernel,origSub,derivSub);
	}

	public static void processBorderHorizontal(GrayS16 orig , GrayS16 deriv ,
											   Kernel1D_I32 kernel , ImageBorder_S32 borderType )
	{
		borderType.setImage(orig);
		ConvolveJustBorder_General.horizontal(kernel, borderType,deriv);

		GrayS16 origSub;
		GrayS16 derivSub;

		origSub = orig.subimage(0,0,orig.width,2, null);
		derivSub = deriv.subimage(0,0,orig.width,2, null);
		ConvolveImageNoBorder.horizontal(kernel,origSub,derivSub);
		origSub = orig.subimage(0,orig.height-2,orig.width,orig.height, null);
		derivSub = deriv.subimage(0,orig.height-2,orig.width,orig.height, null);
		ConvolveImageNoBorder.horizontal(kernel,origSub,derivSub);
	}

	public static void processBorderVertical(GrayU8 orig , GrayS16 deriv ,
											 Kernel1D_I32 kernel , ImageBorder_S32 borderType)
	{
		borderType.setImage(orig);
		ConvolveJustBorder_General.vertical(kernel,borderType,deriv);

		GrayU8 origSub;
		GrayS16 derivSub;

		origSub = orig.subimage(0,0,2,orig.height, null);
		derivSub = deriv.subimage(0,0,2,orig.height, null);
		ConvolveImageNoBorder.vertical(kernel,origSub,derivSub);
		origSub = orig.subimage(orig.width-2,0,orig.width,orig.height, null);
		derivSub = deriv.subimage(orig.width-2,0,orig.width,orig.height, null);
		ConvolveImageNoBorder.vertical(kernel,origSub,derivSub);
	}

	public static void processBorderVertical(GrayU8 orig , GrayS32 deriv ,
											 Kernel1D_I32 kernel , ImageBorder_S32 borderType)
	{
		borderType.setImage(orig);
		ConvolveJustBorder_General.vertical(kernel,borderType,deriv);

		GrayU8 origSub;
		GrayS32 derivSub;

		origSub = orig.subimage(0,0,2,orig.height, null);
		derivSub = deriv.subimage(0,0,2,orig.height, null);
		ConvolveImageNoBorder.vertical(kernel,origSub,derivSub);
		origSub = orig.subimage(orig.width-2,0,orig.width,orig.height, null);
		derivSub = deriv.subimage(orig.width-2,0,orig.width,orig.height, null);
		ConvolveImageNoBorder.vertical(kernel,origSub,derivSub);
	}

	public static void processBorderVertical(GrayS16 orig , GrayS16 deriv ,
											 Kernel1D_I32 kernel , ImageBorder_S32 borderType)
	{
		borderType.setImage(orig);
		ConvolveJustBorder_General.vertical(kernel, borderType ,deriv);

		GrayS16 origSub;
		GrayS16 derivSub;

		origSub = orig.subimage(0,0,2,orig.height, null);
		derivSub = deriv.subimage(0,0,2,orig.height, null);
		ConvolveImageNoBorder.vertical(kernel,origSub,derivSub);
		origSub = orig.subimage(orig.width-2,0,orig.width,orig.height, null);
		derivSub = deriv.subimage(orig.width-2,0,orig.width,orig.height, null);
		ConvolveImageNoBorder.vertical(kernel,origSub,derivSub);
	}

	public static void processBorderHorizontal(GrayF32 orig , GrayF32 deriv ,
											   Kernel1D_F32 kernel , ImageBorder_F32 borderType )
	{
		borderType.setImage(orig);
		ConvolveJustBorder_General.horizontal(kernel, borderType , deriv );

		GrayF32 origSub;
		GrayF32 derivSub;

		origSub = orig.subimage(0,0,orig.width,2, null);
		derivSub = deriv.subimage(0,0,orig.width,2, null);
		ConvolveImageNoBorder.horizontal(kernel,origSub,derivSub);
		origSub = orig.subimage(0,orig.height-2,orig.width,orig.height, null);
		derivSub = deriv.subimage(0,orig.height-2,orig.width,orig.height, null);
		ConvolveImageNoBorder.horizontal(kernel,origSub,derivSub);
	}

	public static void processBorderVertical(GrayF32 orig , GrayF32 deriv ,
											 Kernel1D_F32 kernel , ImageBorder_F32 borderType)
	{
		borderType.setImage(orig);
		ConvolveJustBorder_General.vertical(kernel, borderType ,deriv );

		GrayF32 origSub;
		GrayF32 derivSub;

		origSub = orig.subimage(0,0,2,orig.height, null);
		derivSub = deriv.subimage(0,0,2,orig.height, null);
		ConvolveImageNoBorder.vertical(kernel,origSub,derivSub);
		origSub = orig.subimage(orig.width-2,0,orig.width,orig.height, null);
		derivSub = deriv.subimage(orig.width-2,0,orig.width,orig.height, null);
		ConvolveImageNoBorder.vertical(kernel,origSub,derivSub);
	}

}
