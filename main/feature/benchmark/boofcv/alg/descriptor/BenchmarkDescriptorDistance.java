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

package boofcv.alg.descriptor;

import boofcv.abst.feature.associate.ScoreAssociateHamming_B;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.feature.TupleDesc_B;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkDescriptorDistance {

	static final long TEST_TIME = 1000;
	static Random rand = new Random(234234);
	static int NUM_FEATURES = 5000;

	static List<TupleDesc_B>  binaryA = new ArrayList<TupleDesc_B>();
	static List<TupleDesc_B>  binaryB = new ArrayList<TupleDesc_B>();

	static {
		for (int i = 0; i < NUM_FEATURES; i++) {
			binaryA.add(randomFeature());
			binaryB.add(randomFeature());
		}
	}

	public static class HammingTable extends PerformerBase {

		ScoreAssociateHamming_B table = new ScoreAssociateHamming_B();

		@Override
		public void process() {
			for (int i = 0; i < binaryA.size(); i++) {
				table.score(binaryA.get(i),binaryB.get(i));
			}
		}
	}

	public static class HammingEquationOld extends PerformerBase {
		@Override
		public void process() {
			for (int i = 0; i < binaryA.size(); i++) {
				ExperimentalDescriptorDistance.hamming(binaryA.get(i),binaryB.get(i));
			}
		}
	}

	public static class HammingEquation extends PerformerBase {
		@Override
		public void process() {
			for (int i = 0; i < binaryA.size(); i++) {
				DescriptorDistance.hamming(binaryA.get(i),binaryB.get(i));
			}
		}
	}

	private static TupleDesc_B randomFeature() {
		TupleDesc_B feat = new TupleDesc_B(512);
		for (int j = 0; j < feat.data.length; j++) {
			feat.data[j] = rand.nextInt();
		}
		return feat;
	}

	public static void main(String[] args) {
		System.out.println("Larger numbers are better");

		ProfileOperation.printOpsPerSec(new HammingTable(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new HammingEquationOld(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new HammingEquation(),TEST_TIME);
	}


}
