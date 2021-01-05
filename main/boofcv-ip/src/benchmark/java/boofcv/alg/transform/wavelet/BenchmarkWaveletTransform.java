/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.wavelet;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.wavelet.impl.ImplWaveletTransformNaive;
import boofcv.factory.transform.wavelet.FactoryWaveletDaub;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef_F32;
import boofcv.struct.wavelet.WlCoef_I32;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkWaveletTransform {
	static int imgWidth = 640;
	static int imgHeight = 480;

	static WaveletDescription<WlCoef_F32> desc_F32 = FactoryWaveletDaub.biorthogonal_F32(5, BorderType.REFLECT);
	static WaveletDescription<WlCoef_I32> desc_I32 = FactoryWaveletDaub.biorthogonal_I32(5,BorderType.REFLECT);

	GrayF32 orig_F32 = new GrayF32(imgWidth,imgHeight);
	GrayF32 temp1_F32 = new GrayF32(imgWidth,imgHeight);
	GrayF32 temp2_F32 = new GrayF32(imgWidth,imgHeight);
	GrayS32 orig_I32 = new GrayS32(imgWidth,imgHeight);
	GrayS32 temp1_I32 = new GrayS32(imgWidth,imgHeight);
	GrayS32 temp2_I32 = new GrayS32(imgWidth,imgHeight);

	GrayF32 copy = new GrayF32(1,1);

	@Setup public void setup() {
		Random rand = new Random(234);

		GImageMiscOps.fillUniform(orig_F32, rand,0, 100);
		GImageMiscOps.fillUniform(orig_I32, rand,0, 100);
	}

	@Benchmark public void Naive_F32() {
		ImplWaveletTransformNaive.horizontal(desc_F32.getBorder(),desc_F32.getForward(),orig_F32,temp1_F32);
		ImplWaveletTransformNaive.vertical(desc_F32.getBorder(),desc_F32.getForward(),temp1_F32,temp2_F32);
	}

	@Benchmark public void Standard_F32() {
		WaveletTransformOps.transform1(desc_F32,orig_F32,temp1_F32,temp1_F32);
	}

	@Benchmark public void Naive_I32() {
		ImplWaveletTransformNaive.horizontal(desc_I32.getBorder(),desc_I32.getForward(),orig_I32,temp1_I32);
		ImplWaveletTransformNaive.vertical(desc_I32.getBorder(),desc_I32.getForward(),temp1_I32,temp2_I32);
	}

	@Benchmark public void Standard_I32() {
		WaveletTransformOps.transform1(desc_I32,orig_I32,temp1_I32,temp1_I32);
	}

	@Benchmark public void FullLevel3_F32() {
		// don't modify input image
		copy.setTo(orig_F32);
		ImageDimension dim = UtilWavelet.transformDimension(copy,3);
		temp1_F32.reshape(dim.width,dim.height);
		temp2_F32.reshape(dim.width,dim.height);
		WaveletTransformOps.transformN(desc_F32,copy,temp1_F32,temp2_F32,3);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkWaveletTransform.class.getSimpleName())
				.build();

		new Runner(opt).run();
	}
}
