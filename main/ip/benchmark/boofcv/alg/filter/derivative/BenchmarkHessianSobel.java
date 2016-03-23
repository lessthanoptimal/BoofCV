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

import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;

/**
 * Benchmarks related to computing image derivatives
 * 
 * @author Peter Abeles
 */
public class BenchmarkHessianSobel extends BenchmarkDerivativeBase {

	static GrayF32 tempA_F32 = new GrayF32(imgWidth,imgHeight);
	static GrayF32 tempB_F32 = new GrayF32(imgWidth,imgHeight);
	static GrayS16 tempA_I16 = new GrayS16(imgWidth,imgHeight);
	static GrayS16 tempB_I16 = new GrayS16(imgWidth,imgHeight);


	public static class Hessian_I8 extends PerformerBase
	{
		@Override
		public void process() {
			HessianSobel.process(imgInt8,derivX_I16,derivY_I16,derivXY_I16,borderI32);
		}
	}

	public static class Hessian_F32 extends PerformerBase
	{
		@Override
		public void process() {
			HessianSobel.process(imgFloat32,derivX_F32,derivY_F32,derivXY_F32,borderF32);
		}
	}

	public static class HessianFromDeriv_I8 extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel.process(imgInt8,tempA_I16,tempB_I16,borderI32);
			HessianFromGradient.hessianSobel(tempA_I16,tempB_I16,derivX_I16,derivY_I16,derivXY_I16,borderI32);
		}
	}

	public static class HessianFromDeriv_F32 extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel.process(imgFloat32,tempA_F32,tempB_F32,borderF32);
			HessianFromGradient.hessianSobel(tempA_F32,tempB_F32,derivX_F32,derivY_F32,derivXY_F32,borderF32);
		}
	}

	@Override
	public void profile_I8() {
		ProfileOperation.printOpsPerSec(new Hessian_I8(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new HessianFromDeriv_I8(),TEST_TIME);
	}

	@Override
	public void profile_F32() {
		ProfileOperation.printOpsPerSec(new Hessian_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new HessianFromDeriv_F32(),TEST_TIME);
	}

	public static void main( String args[] ) {
		BenchmarkHessianSobel benchmark = new BenchmarkHessianSobel();

		BenchmarkHessianSobel.border = true;
		benchmark.process();
	}
}
