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

package gecv.alg.wavelet;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.alg.misc.ImageTestingOps;
import gecv.alg.wavelet.impl.ImplWaveletTransformNaive;
import gecv.struct.image.ImageDimension;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt32;
import gecv.struct.wavelet.WaveletDescription;
import gecv.struct.wavelet.WlCoef_F32;
import gecv.struct.wavelet.WlCoef_I32;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class BenchmarkWaveletTransform {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static long TEST_TIME = 1000;

	static WaveletDescription<WlCoef_F32> desc_F32 = FactoryWaveletDaub.biorthogonal_F32(5,WaveletBorderType.REFLECT);
	static WaveletDescription<WlCoef_I32> desc_I32 = FactoryWaveletDaub.biorthogonal_I32(5,WaveletBorderType.REFLECT);

	static ImageFloat32 orig_F32 = new ImageFloat32(imgWidth,imgHeight);
	static ImageFloat32 temp1_F32 = new ImageFloat32(imgWidth,imgHeight);
	static ImageFloat32 temp2_F32 = new ImageFloat32(imgWidth,imgHeight);
	static ImageSInt32 orig_I32 = new ImageSInt32(imgWidth,imgHeight);
	static ImageSInt32 temp1_I32 = new ImageSInt32(imgWidth,imgHeight);
	static ImageSInt32 temp2_I32 = new ImageSInt32(imgWidth,imgHeight);

	public static class Naive_F32 extends PerformerBase {

		@Override
		public void process() {
			ImplWaveletTransformNaive.horizontal(desc_F32.getBorder(),desc_F32.getForward(),orig_F32,temp1_F32);
			ImplWaveletTransformNaive.vertical(desc_F32.getBorder(),desc_F32.getForward(),temp1_F32,temp2_F32);
		}
	}

	public static class Standard_F32 extends PerformerBase {

		@Override
		public void process() {
			WaveletTransformOps.transform1(desc_F32,orig_F32,temp1_F32,temp1_F32);
		}
	}

	public static class Naive_I32 extends PerformerBase {

		@Override
		public void process() {
			ImplWaveletTransformNaive.horizontal(desc_I32.getBorder(),desc_I32.getForward(),orig_I32,temp1_I32);
			ImplWaveletTransformNaive.vertical(desc_I32.getBorder(),desc_I32.getForward(),temp1_I32,temp2_I32);
		}
	}

	public static class Standard_I32 extends PerformerBase {

		@Override
		public void process() {
			WaveletTransformOps.transform1(desc_I32,orig_I32,temp1_I32,temp1_I32);
		}
	}

	public static class FullLevel3_F32 extends PerformerBase {

		static ImageFloat32 copy = new ImageFloat32(imgWidth,imgHeight);
		ImageFloat32 tran;
		ImageFloat32 storage;

		public FullLevel3_F32() {
			ImageDimension dim = UtilWavelet.transformDimension(copy,3);
			tran = new ImageFloat32(dim.width,dim.height);
			storage = new ImageFloat32(dim.width,dim.height);
		}

		@Override
		public void process() {
			// don't modify the input image
			copy.setTo(orig_F32);
			WaveletTransformOps.transformN(desc_F32,copy,tran,storage,3);
		}
	}


	public static void main(String args[]) {

		Random rand = new Random(234);
		ImageTestingOps.randomize(orig_F32, rand, 0, 100);
		ImageTestingOps.randomize(orig_I32, rand, 0, 100);

		System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new FullLevel3_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Naive_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Standard_F32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Naive_I32(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Standard_I32(), TEST_TIME);
	}
}
