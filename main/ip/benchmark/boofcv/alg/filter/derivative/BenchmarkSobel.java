/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.derivative.impl.GradientSobel_Naive;
import boofcv.alg.filter.derivative.impl.GradientSobel_Outer;
import boofcv.alg.filter.derivative.impl.GradientSobel_UnrolledOuter;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;

/**
 * Benchmarks related to computing image derivatives
 * 
 * @author Peter Abeles
 */
public class BenchmarkSobel extends BenchmarkDerivativeBase{

	public static class Sobel_I8 extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel.process(imgInt8,derivX_I16,derivY_I16,borderI32);
		}
	}

	public static class Sobel_F32 extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel.process(imgFloat32,derivX_F32,derivY_F32,borderF32);
		}
	}

	public static class SobelNaive_I8 extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel_Naive.process(imgInt8,derivX_I16,derivY_I16);
		}
	}

	public static class SobelNaive_F32 extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel_Naive.process(imgFloat32,derivX_F32,derivY_F32);
		}
	}

	public static class SobelOuter_I8 extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel_Outer.process_I8(imgInt8,derivX_I16,derivY_I16);
		}
	}

	public static class SobelOuter_I8_Sub extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel_Outer.process_I8_sub(imgInt8,derivX_I16,derivY_I16);
		}
	}

	public static class SobelOuter_F32 extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel_Outer.process_F32(imgFloat32,derivX_F32,derivY_F32);
		}
	}

	public static class SobelUnrolledOuter_I8 extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel_UnrolledOuter.process_I8(imgInt8,derivX_I16,derivY_I16);
		}
	}

	public static class SobelUnrolledOuter_F32 extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel_UnrolledOuter.process_F32(imgFloat32,derivX_F32,derivY_F32);
		}
	}

	public static class SobelUnrolledOuter_F32_Sub extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel_UnrolledOuter.process_F32_sub(imgFloat32,derivX_F32,derivY_F32);
		}
	}

	@Override
	public void profile_I8() {
		ProfileOperation.printOpsPerSec(new Sobel_I8(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new SobelNaive_I8(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new SobelOuter_I8(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new SobelOuter_I8_Sub(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new SobelUnrolledOuter_I8(),TEST_TIME);
	}

	@Override
	public void profile_F32() {
		ProfileOperation.printOpsPerSec(new Sobel_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new SobelNaive_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new SobelOuter_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new SobelUnrolledOuter_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new SobelUnrolledOuter_F32_Sub(),TEST_TIME);
	}

	public static void main( String args[] ) {
		BenchmarkSobel benchmark = new BenchmarkSobel();

		BenchmarkSobel.border = true;
		benchmark.process();
	}
}
