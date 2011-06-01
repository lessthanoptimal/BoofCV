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

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.alg.filter.derivative.impl.GradientThree_Standard;

/**
 * Benchmarks related to computing image derivatives
 * 
 * @author Peter Abeles
 */
public class BenchmarkThree extends BenchmarkDerivativeBase {

	public static class Three_I8 extends PerformerBase
	{
		@Override
		public void process() {
			GradientThree.process(imgInt8,derivX_I16,derivY_I16,border);
		}
	}

	public static class Three_F32 extends PerformerBase
	{
		@Override
		public void process() {
			GradientThree.process(imgFloat32,derivX_F32,derivY_F32,border);
		}
	}

	public static class ThreeStandard_I8 extends PerformerBase
	{
		@Override
		public void process() {
			GradientThree_Standard.process(imgInt8,derivX_I16,derivY_I16);
		}
	}

	public static class ThreeStandard_F32 extends PerformerBase
	{
		@Override
		public void process() {
			GradientThree_Standard.process(imgFloat32,derivX_F32,derivY_F32);
		}
	}

	@Override
	public void profile_I8() {
		ProfileOperation.printOpsPerSec(new Three_I8(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new ThreeStandard_I8(),TEST_TIME);
	}

	@Override
	public void profile_F32() {
		ProfileOperation.printOpsPerSec(new Three_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new ThreeStandard_F32(),TEST_TIME);
	}

	public static void main( String args[] ) {
		BenchmarkThree benchmark = new BenchmarkThree();

		BenchmarkThree.border = true;
		benchmark.process();
	}
}
