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

package gecv.alg.filter.blur;

import gecv.alg.InputSanityCheck;
import gecv.alg.filter.convolve.edge.ConvolveNormalized;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
// median: two implementations.  bubble sort and what's in the book
// well three: naive to double check
// mean: naive and highly optimized
public class BlurImageOps {

	public static void kernel(ImageUInt8 input, ImageUInt8 output, Kernel1D_I32 kernel ,
								   ImageUInt8 storage ) {
		InputSanityCheck.checkSameShape(input,output,storage);

		ConvolveNormalized.horizontal(kernel,input,storage);
		ConvolveNormalized.vertical(kernel,storage,output);
	}

	public static void kernel(ImageSInt16 input, ImageSInt16 output, Kernel1D_I32 kernel ,
								   ImageSInt16 storage ) {
		InputSanityCheck.checkSameShape(input,output,storage);

		ConvolveNormalized.horizontal(kernel,input,storage);
		ConvolveNormalized.vertical(kernel,storage,output);
	}

	public static void kernel(ImageFloat32 input, ImageFloat32 output, Kernel1D_F32 kernel ,
							  ImageFloat32 storage ) {
		InputSanityCheck.checkSameShape(input,output,storage);

		ConvolveNormalized.horizontal(kernel,input,storage);
		ConvolveNormalized.vertical(kernel,storage,output);
	}

	public static ImageUInt8 mean(ImageUInt8 input, ImageUInt8 output, int radius) {
		return null;
	}

	public static ImageUInt8 median(ImageUInt8 input, ImageUInt8 output, int radius) {
		return null;
	}

	public static ImageUInt8 gaussian(ImageUInt8 input, ImageUInt8 output, int radius) {
		return null;
	}

	public static ImageFloat32 mean(ImageFloat32 input, ImageFloat32 output, int radius) {
		return null;
	}

	public static ImageFloat32 median(ImageFloat32 input, ImageFloat32 output, int radius) {
		return null;
	}

	public static ImageFloat32 gaussian(ImageFloat32 input, ImageFloat32 output, int radius) {
		return null;
	}
}
