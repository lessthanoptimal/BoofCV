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

package boofcv;

import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import org.ejml.data.Complex_F64;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class OldBenchmarkComplexMath {

	public int TEST_TIME = 2000;

	int N = 20000;

	Random rand = new Random(234);

	Complex_F64 number = new Complex_F64(1.5, 0.3);

	Complex_F64[] objectInput = new Complex_F64[N];
	Complex_F64[] objectOutput = new Complex_F64[N];

	double[] arrayInput = new double[2*N];
	double[] arrayOutput = new double[2*N];

	public OldBenchmarkComplexMath() {
		for (int i = 0; i < N; i++) {
			objectInput[i] = new Complex_F64();
			objectOutput[i] = new Complex_F64();
		}
	}

	private class ComplexObject extends PerformerBase {
		@Override
		public void process() {
			Complex_F64 a = number;

			for (int i = 0; i < N; i++) {
				Complex_F64 b = objectInput[i];
				Complex_F64 o = objectOutput[i];

				o.real = a.real*b.real - a.imaginary*b.imaginary;
				o.imaginary = a.real*b.imaginary + a.imaginary*b.real;
			}
		}
	}

	private class PureArray extends PerformerBase {
		@Override
		public void process() {
			double ar = number.real;
			double ai = number.imaginary;

			for (int i = 0; i < N; i++) {
				int index = i*2;
				double bi = arrayInput[index];
				double br = arrayInput[index + 1];

				arrayOutput[index] = ar*br - ai*bi;
				arrayOutput[index + 1] = ar*bi + ai*br;
			}
		}
	}

	public void run() {
		for (int i = 0; i < N; i++) {
			objectInput[i].setTo(rand.nextGaussian(), rand.nextGaussian());
			arrayInput[i*2] = objectInput[i].real;
			arrayInput[i*2 + 1] = objectInput[i].imaginary;
		}

		ComplexObject object = new ComplexObject();
		PureArray array = new PureArray();

		ProfileOperation.printOpsPerSec(array, TEST_TIME);
		ProfileOperation.printOpsPerSec(object, TEST_TIME);
	}

	public static void main( String[] args ) {
		OldBenchmarkComplexMath benchmark = new OldBenchmarkComplexMath();
		benchmark.run();
	}
}
