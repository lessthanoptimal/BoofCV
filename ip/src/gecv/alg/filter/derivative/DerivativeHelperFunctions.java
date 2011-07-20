/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.derivative;

import gecv.alg.filter.convolve.ConvolveImageNoBorder;
import gecv.alg.filter.convolve.border.ConvolveJustBorder_General;
import gecv.core.image.border.ImageBorder_F32;
import gecv.core.image.border.ImageBorder_I32;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;


/**
 * @author Peter Abeles
 */
public class DerivativeHelperFunctions {

	public static void processBorderHorizontal( ImageUInt8 orig , ImageSInt16 deriv ,
												Kernel1D_I32 kernel , int border , ImageBorder_I32 borderType )
	{
		borderType.setImage(orig);
		ConvolveJustBorder_General.horizontal(kernel, borderType,deriv,border);

		ImageUInt8 origSub;
		ImageSInt16 derivSub;

		origSub = orig.subimage(0,0,orig.width,2);
		derivSub = deriv.subimage(0,0,orig.width,2);
		ConvolveImageNoBorder.horizontal(kernel,origSub,derivSub,true);
		origSub = orig.subimage(0,orig.height-2,orig.width,orig.height);
		derivSub = deriv.subimage(0,orig.height-2,orig.width,orig.height);
		ConvolveImageNoBorder.horizontal(kernel,origSub,derivSub,true);
	}

	public static void processBorderHorizontal( ImageSInt16 orig , ImageSInt16 deriv ,
												Kernel1D_I32 kernel , int border , ImageBorder_I32 borderType )
	{
		borderType.setImage(orig);
		ConvolveJustBorder_General.horizontal(kernel, borderType,deriv,border);

		ImageSInt16 origSub;
		ImageSInt16 derivSub;

		origSub = orig.subimage(0,0,orig.width,2);
		derivSub = deriv.subimage(0,0,orig.width,2);
		ConvolveImageNoBorder.horizontal(kernel,origSub,derivSub,true);
		origSub = orig.subimage(0,orig.height-2,orig.width,orig.height);
		derivSub = deriv.subimage(0,orig.height-2,orig.width,orig.height);
		ConvolveImageNoBorder.horizontal(kernel,origSub,derivSub,true);
	}

	public static void processBorderVertical( ImageUInt8 orig , ImageSInt16 deriv ,
											  Kernel1D_I32 kernel , int border , ImageBorder_I32 borderType)
	{
		borderType.setImage(orig);
		ConvolveJustBorder_General.vertical(kernel,borderType,deriv,border);

		ImageUInt8 origSub;
		ImageSInt16 derivSub;

		origSub = orig.subimage(0,0,2,orig.height);
		derivSub = deriv.subimage(0,0,2,orig.height);
		ConvolveImageNoBorder.vertical(kernel,origSub,derivSub,true);
		origSub = orig.subimage(orig.width-2,0,orig.width,orig.height);
		derivSub = deriv.subimage(orig.width-2,0,orig.width,orig.height);
		ConvolveImageNoBorder.vertical(kernel,origSub,derivSub,true);
	}

	public static void processBorderVertical( ImageSInt16 orig , ImageSInt16 deriv ,
											  Kernel1D_I32 kernel , int border , ImageBorder_I32 borderType)
	{
		borderType.setImage(orig);
		ConvolveJustBorder_General.vertical(kernel, borderType ,deriv,border);

		ImageSInt16 origSub;
		ImageSInt16 derivSub;

		origSub = orig.subimage(0,0,2,orig.height);
		derivSub = deriv.subimage(0,0,2,orig.height);
		ConvolveImageNoBorder.vertical(kernel,origSub,derivSub,true);
		origSub = orig.subimage(orig.width-2,0,orig.width,orig.height);
		derivSub = deriv.subimage(orig.width-2,0,orig.width,orig.height);
		ConvolveImageNoBorder.vertical(kernel,origSub,derivSub,true);
	}

	public static void processBorderHorizontal( ImageFloat32 orig , ImageFloat32 deriv ,
												Kernel1D_F32 kernel , int border , ImageBorder_F32 borderType )
	{
		borderType.setImage(orig);
		ConvolveJustBorder_General.horizontal(kernel, borderType , deriv , border);

		ImageFloat32 origSub;
		ImageFloat32 derivSub;

		origSub = orig.subimage(0,0,orig.width,2);
		derivSub = deriv.subimage(0,0,orig.width,2);
		ConvolveImageNoBorder.horizontal(kernel,origSub,derivSub,true);
		origSub = orig.subimage(0,orig.height-2,orig.width,orig.height);
		derivSub = deriv.subimage(0,orig.height-2,orig.width,orig.height);
		ConvolveImageNoBorder.horizontal(kernel,origSub,derivSub,true);
	}

	public static void processBorderVertical( ImageFloat32 orig , ImageFloat32 deriv ,
											  Kernel1D_F32 kernel , int border , ImageBorder_F32 borderType)
	{
		borderType.setImage(orig);
		ConvolveJustBorder_General.vertical(kernel, borderType ,deriv,border);

		ImageFloat32 origSub;
		ImageFloat32 derivSub;

		origSub = orig.subimage(0,0,2,orig.height);
		derivSub = deriv.subimage(0,0,2,orig.height);
		ConvolveImageNoBorder.vertical(kernel,origSub,derivSub,true);
		origSub = orig.subimage(orig.width-2,0,orig.width,orig.height);
		derivSub = deriv.subimage(orig.width-2,0,orig.width,orig.height);
		ConvolveImageNoBorder.vertical(kernel,origSub,derivSub,true);
	}

}
