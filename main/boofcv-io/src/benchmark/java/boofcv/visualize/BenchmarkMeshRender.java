/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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
public class BenchmarkMeshRender {
	VertexMesh mesh = new VertexMesh();
	VertexMesh meshTri = new VertexMesh();

	MeshRender rasterize = new MeshRasterizer();
	MeshRender raytrace = new MeshRayTracer();

	@Setup public void setup() {
		var intrinsics = new CameraPinhole();
		PerspectiveOps.createIntrinsic(400, 300, 80, -1, intrinsics);

		// Randomly generate square shapes of different sizes and distances
		var rand = new Random(2342);

		mesh.reset();
		addRegularGrid(intrinsics, rand);
		addRandomBehind(intrinsics, rand);

		rasterize.setCamera(new LensDistortionPinhole(intrinsics), intrinsics.width, intrinsics.height);
		raytrace.setCamera(new LensDistortionPinhole(intrinsics), intrinsics.width, intrinsics.height);
	}

	/// Create a regular grid so that polygons share points and overlap
	private void addRegularGrid( CameraPinhole intrinsics, Random rand ) {
		var norm = new Point2D_F64();
		int period = 3;

		int cols = intrinsics.width/period + 1;
		int rows = intrinsics.height/period + 1;

		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				int x = col*period;
				int y = row*period;

				// don't have points lie exactly on a pixel
				double px = x + rand.nextDouble() - 0.5;
				double py = y + rand.nextDouble() - 0.5;

				// select a random point in front of the camera
				double z = 0.5 + rand.nextDouble();

				PerspectiveOps.convertPixelToNorm(intrinsics, px, py, norm);

				mesh.vertexes.append(norm.x*z, norm.y*z, z);
			}
		}

		// Add two sets of squares at different sizes
		addSquareInGrid(rand, rows, cols, 1);
		addSquareInGrid(rand, rows, cols, 2);
		addSquareInGrid(rand, rows, cols, 3);
	}

	private void addSquareInGrid( Random rand, int rows, int cols, int size ) {
		// create small square polygons
		for (int row = size; row < rows; row++) {
			for (int col = size; col < cols; col++) {
				int idx = (row - size)*cols + col - size;

				// swap the winding order
				if (rand.nextBoolean()) {
					mesh.faceVertexes.add(idx);
					mesh.faceVertexes.add(idx + size);
					mesh.faceVertexes.add(idx + size + size*cols);
					mesh.faceVertexes.add(idx + size*cols);
				} else {
					mesh.faceVertexes.add(idx + size*cols);
					mesh.faceVertexes.add(idx + size + size*cols);
					mesh.faceVertexes.add(idx + size);
					mesh.faceVertexes.add(idx);
				}
				mesh.faceOffsets.add(mesh.faceVertexes.size);
			}
		}
	}

	/// Adds random polygons behind the camera
	private void addRandomBehind( CameraPinhole intrinsics, Random rand ) {
		addFlatSquares(intrinsics, rand, 500, -1.5, -0.5);
	}

	/// Adds flat squares of random size and depth. Each square has its own vertexes.
	///
	/// @param minZ Smallest depth of a square. Negative is behind the camera.
	/// @param maxZ Largest depth of a square.
	private void addFlatSquares( CameraPinhole intrinsics, Random rand, int count, double minZ, double maxZ ) {
		var norm = new Point2D_F64();
		var shape = new DogArray<>(Point3D_F64::new);
		shape.resize(4);
		for (int i = 0; i < count; i++) {
			// select pixels that it should appear at
			double x0 = rand.nextDouble()*intrinsics.width*0.6;
			double y0 = rand.nextDouble()*intrinsics.height*0.6;
			double x1 = x0 + 10 + rand.nextDouble()*intrinsics.width*0.35;
			double y1 = y0 + 10 + rand.nextDouble()*intrinsics.height*0.35;

			// Randomly select the depth
			double z = minZ + rand.nextDouble()*(maxZ - minZ);

			// convert into a 3D point
			PerspectiveOps.convertPixelToNorm(intrinsics, x0, y0, norm);
			x0 = norm.x*z;
			y0 = norm.y*z;
			PerspectiveOps.convertPixelToNorm(intrinsics, x1, y1, norm);
			x1 = norm.x*z;
			y1 = norm.y*z;

			// Create the shape
			shape.get(0).setTo(x0, y0, z);
			shape.get(1).setTo(x1, y0, z);
			shape.get(2).setTo(x1, y1, z);
			shape.get(3).setTo(x0, y1, z);

			mesh.addFaceVectors(shape.toList());
		}
	}

	@Benchmark public void rasterize() {
		rasterize.setMesh(mesh);
		rasterize.render();
	}

	@Benchmark public void raytrace() {
		mesh.toTriangles(meshTri);
		raytrace.setMesh(meshTri);
		raytrace.render();
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkMeshRender.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
