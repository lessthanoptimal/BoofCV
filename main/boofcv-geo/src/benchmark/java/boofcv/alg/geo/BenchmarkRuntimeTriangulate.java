/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.triangulate.PixelDepthLinearMetric;
import boofcv.alg.geo.triangulate.Triangulate2ViewsGeometricMetric;
import boofcv.alg.geo.triangulate.TriangulateMetricLinearDLT;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;

/**
 * @author Peter Abeles
 */
public class BenchmarkRuntimeTriangulate extends ArtificialStereoScene {
	static final long TEST_TIME = 1000;
	static final int NUM_POINTS = 500;
	static final boolean FUNDAMENTAL = false;

	public class DLT2 extends PerformerBase {

		TriangulateMetricLinearDLT alg;
		Point4D_F64 found = new Point4D_F64();

		public DLT2() {
			alg = new TriangulateMetricLinearDLT();
		}

		@Override
		public void process() {
			for( int i = 0; i < NUM_POINTS; i++ )
				alg.triangulate(pairs.get(i).p1,pairs.get(i).p2,motion,found);
		}
	}

	public class Geo2 extends PerformerBase {

		Triangulate2ViewsGeometricMetric alg;
		Point3D_F64 found = new Point3D_F64();

		public Geo2() {
			alg = new Triangulate2ViewsGeometricMetric();
		}

		@Override
		public void process() {
			for( int i = 0; i < NUM_POINTS; i++ )
				alg.triangulate(pairs.get(i).p1,pairs.get(i).p2,motion,found);
		}
	}

	public class PixelDepth extends PerformerBase {

		PixelDepthLinearMetric alg;

		public PixelDepth() {
			alg = new PixelDepthLinearMetric();
		}

		@Override
		public void process() {
			for( int i = 0; i < NUM_POINTS; i++ )
				alg.depth2View(pairs.get(i).p1,pairs.get(i).p2,motion);
		}
	}

	
	public void runAll() {
		System.out.println("=========  Profile numFeatures "+NUM_POINTS);
		System.out.println();

		init(NUM_POINTS,FUNDAMENTAL,false);

		ProfileOperation.printOpsPerSec(new DLT2(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Geo2(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new PixelDepth(), TEST_TIME);

		System.out.println();
		System.out.println("Done");
	}
	
	public static void main( String args[] ) {
		BenchmarkRuntimeTriangulate alg = new BenchmarkRuntimeTriangulate();

		alg.runAll();
	}
}
