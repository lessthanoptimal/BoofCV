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

import boofcv.alg.filter.derivative.impl.HessianThree_Standard;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;

/**
 * Benchmarks related to computing image derivatives
 * 
 * @author Peter Abeles
 */
public class BenchmarkImageDerivative extends BenchmarkDerivativeBase {

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

	public static class DerivativeThree_F32 extends PerformerBase
	{
		@Override
		public void process() {
			GradientThree.process(imgFloat32,derivX_F32,derivY_F32,borderF32);
		}
	}

	public static class DerivativeThree_I8 extends PerformerBase
	{
		@Override
		public void process() {
			GradientThree.process(imgInt8,derivX_I16,derivY_I16,borderI32);
		}
	}

	public static class HessianThree_Std_I8 extends PerformerBase
	{
		@Override
		public void process() {
			HessianThree_Standard.process(imgInt8,derivX_I16,derivY_I16,derivXY_I16);
		}
	}

	public static class HessianThree_I8 extends PerformerBase
	{
		@Override
		public void process() {
			HessianThree.process(imgInt8,derivX_I16,derivY_I16,derivXY_I16,borderI32);
		}
	}

	public static class HessianThree_Std_F32 extends PerformerBase
	{
		@Override
		public void process() {
			HessianThree_Standard.process(imgFloat32,derivX_F32,derivY_F32,derivXY_F32);
		}
	}

	public static class HessianThree_F32 extends PerformerBase
	{
		@Override
		public void process() {
			HessianThree.process(imgFloat32,derivX_F32,derivY_F32,derivXY_F32,borderF32);
		}
	}

	public static class HessianSobel_I8 extends PerformerBase
	{
		@Override
		public void process() {
			HessianSobel.process(imgInt8,derivX_I16,derivY_I16,derivXY_I16,borderI32);
		}
	}

	public static class HessianSobel_F32 extends PerformerBase
	{
		@Override
		public void process() {
			HessianSobel.process(imgFloat32,derivX_F32,derivY_F32,derivXY_F32,borderF32);
		}
	}

	public static class LaplacianEdge_F32 extends PerformerBase
	{
		@Override
		public void process() {
			LaplacianEdge.process(imgFloat32,derivX_F32);
		}
	}

	public static class LaplacianEdge_I8 extends PerformerBase
	{
		@Override
		public void process() {
			LaplacianEdge.process(imgInt8,derivX_I16);
		}
	}

	public static class Prewitt_I8 extends PerformerBase
	{
		@Override
		public void process() {
			GradientPrewitt.process(imgInt8,derivX_I16,derivY_I16,borderI32);
		}
	}

	public static class Prewitt_F32 extends PerformerBase
	{
		@Override
		public void process() {
			GradientPrewitt.process(imgFloat32,derivX_F32,derivY_F32,borderF32);
		}
	}

	@Override
	public void profile_I8() {
		ProfileOperation.printOpsPerSec(new Sobel_I8(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new Prewitt_I8(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new DerivativeThree_I8(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new HessianThree_I8(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new HessianSobel_I8(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new LaplacianEdge_I8(),TEST_TIME);
	}

	@Override
	public void profile_F32() {
		ProfileOperation.printOpsPerSec(new Sobel_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new Prewitt_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new DerivativeThree_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new HessianThree_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new HessianSobel_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new LaplacianEdge_F32(),TEST_TIME);
	}

	public static void main( String args[] ) {
		BenchmarkImageDerivative benchmark = new BenchmarkImageDerivative();

		BenchmarkImageDerivative.border = true;
		benchmark.process();
	}

}
