/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

@BenchmarkMode({Mode.AverageTime,Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 2)
public class BenchmarkWaveletInverse {
	static int imgWidth = 640;
	static int imgHeight = 480;

	WaveletDescription<WlCoef_F32> desc_F32 = FactoryWaveletDaub.biorthogonal_F32(5,BorderType.REFLECT);
	WaveletDescription<WlCoef_I32> desc_I32 = FactoryWaveletDaub.biorthogonal_I32(5, BorderType.REFLECT);

	GrayF32 tran_F32 = new GrayF32(imgWidth,imgHeight);
	GrayF32 temp1_F32 = new GrayF32(imgWidth,imgHeight);
	GrayF32 temp2_F32 = new GrayF32(imgWidth,imgHeight);
	GrayS32 tran_I32 = new GrayS32(imgWidth,imgHeight);
	GrayS32 temp1_I32 = new GrayS32(imgWidth,imgHeight);
	GrayS32 temp2_I32 = new GrayS32(imgWidth,imgHeight);

	@Setup
	public void setup() {
		Random rand = new Random(234);

		GImageMiscOps.fillUniform(tran_F32, rand,0, 100);
		GImageMiscOps.fillUniform(tran_I32, rand,0, 100);
	}

	@Benchmark public void Naive_F32() {
		ImplWaveletTransformNaive.verticalInverse(desc_F32.getBorder(), desc_F32.getInverse(), tran_F32, temp1_F32);
		ImplWaveletTransformNaive.horizontalInverse(desc_F32.getBorder(), desc_F32.getInverse(), temp1_F32, temp2_F32);
	}

	@Benchmark public void Naive_I32() {
		ImplWaveletTransformNaive.verticalInverse(desc_I32.getBorder(), desc_I32.getInverse(), tran_I32, temp1_I32);
		ImplWaveletTransformNaive.horizontalInverse(desc_I32.getBorder(), desc_I32.getInverse(), temp1_I32, temp2_I32);
	}

	@Benchmark public void Standard_F32() {
		WaveletTransformOps.inverse1(desc_F32,tran_F32,temp1_F32,temp1_F32,0,255);
	}

	@Benchmark public void Standard_I32() {
		WaveletTransformOps.inverse1(desc_I32,tran_I32,temp1_I32,temp1_I32,0,255);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkWaveletInverse.class.getSimpleName())
				.build();

		new Runner(opt).run();
	}
}
