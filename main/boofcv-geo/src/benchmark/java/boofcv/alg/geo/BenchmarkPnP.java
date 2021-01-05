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

package boofcv.alg.geo;

import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.alg.geo.pose.PnPLepetitEPnP;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.Point2D3D;
import georegression.struct.se.Se3_F64;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkPnP extends ArtificialStereoScene {
	static final int NUM_POINTS = 20000;

	Se3_F64 found = new Se3_F64();

	PnPLepetitEPnP epnp = new PnPLepetitEPnP();
	Estimate1ofPnP grunert = FactoryMultiView.pnp_1(EnumPNP.P3P_GRUNERT, -1, 1);
	Estimate1ofPnP finster = FactoryMultiView.pnp_1(EnumPNP.P3P_FINSTERWALDER, -1, 1);
	Estimate1ofPnP ippe = FactoryMultiView.pnp_1(EnumPNP.IPPE, -1, 1);

	List<Point2D3D> obs = new ArrayList<>();

	@State(Scope.Benchmark) public static class PlanarState {
		ArtificialStereoScene scene = new ArtificialStereoScene();

		@Setup public void setup() {
			scene.init(NUM_POINTS, true, true);
		}
	}

	@Setup public void setup() {
		init(NUM_POINTS, false, false);
	}

	@Benchmark public void EPnP_0() {
		epnp.setNumIterations(0);
		epnp.process(worldPoints, observationCurrent, found);
	}

	@Benchmark public void EPnP_5() {
		epnp.setNumIterations(5);
		epnp.process(worldPoints, observationCurrent, found);
	}

	// @formatter:off
	@Benchmark public void grunert() {processMin(grunert);}
	@Benchmark public void finster() {processMin(finster);}
	@Benchmark public void ippe( PlanarState s ) {ippe.process(s.scene.observationPose, found);}
	// @formatter:on

	public void processMin( Estimate1ofPnP alg ) {
		int maxConsider = Math.min(20000, observationPose.size());
		int N = alg.getMinimumPoints();

		for (int i = N; i < maxConsider; i++) {
			obs.clear();
			for (int j = 0; j < alg.getMinimumPoints(); j++)
				obs.add(observationPose.get(i + j - N));
			alg.process(obs, found);
		}
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkPnP.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
