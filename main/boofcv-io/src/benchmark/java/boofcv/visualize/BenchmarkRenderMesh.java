/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

package boofcv.visualize;

import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.mesh.VertexMesh;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 2)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkRenderMesh {
	VertexMesh mesh = new VertexMesh();

	RenderMesh renderer = new RenderMesh();

	@Setup public void setup() {
		var intrinsics = new CameraPinhole();
		PerspectiveOps.createIntrinsic(400, 300, 80, -1, intrinsics);

		// Randomly generate square shapes of different sizes and distances
		var rand = new Random(2342);

		createFlatSquareScene(intrinsics, rand);

		renderer.setCamera(new LensDistortionPinhole(intrinsics), intrinsics.width, intrinsics.height);
	}

	private void createFlatSquareScene( CameraPinhole intrinsics, Random rand ) {
		mesh.reset();
		var norm = new Point2D_F64();
		var shape = new DogArray<>(Point3D_F64::new);
		shape.resize(4);
		for (int i = 0; i < 5000; i++) {
			// select pixels that it should appear at
			double x0 = rand.nextDouble()*intrinsics.width*0.6;
			double y0 = rand.nextDouble()*intrinsics.height*0.6;
			double x1 = x0 + 10 + rand.nextDouble()*intrinsics.width*0.35;
			double y1 = y0 + 10 + rand.nextDouble()*intrinsics.height*0.35;

			// Randomly select the bepth
			double z = 0.5 + rand.nextDouble();

			// convert into a 3D point
			PerspectiveOps.convertPixelToNorm(intrinsics, x0, y0, norm);
			x0 = norm.x*z;
			y0 = norm.x*z;
			PerspectiveOps.convertPixelToNorm(intrinsics, x1, y1, norm);
			x1 = norm.x*z;
			y1 = norm.x*z;

			// Create the shape
			shape.get(0).setTo(x0, y0, z);
			shape.get(1).setTo(x1, y0, z);
			shape.get(2).setTo(x1, y1, z);
			shape.get(3).setTo(x0, y1, z);

			mesh.addFaceVectors(shape.toList());
		}
	}

	/**
	 * Simple scenario with planar squares
	 */
	@Benchmark public void planarSquares(){
		renderer.render(mesh);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkRenderMesh.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
