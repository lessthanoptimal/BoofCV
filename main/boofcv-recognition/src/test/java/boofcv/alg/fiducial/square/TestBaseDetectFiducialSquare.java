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

package boofcv.alg.fiducial.square;

import boofcv.alg.distort.radtan.LensDistortionRadialTangential;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.ConvertImage;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.simulation.SimulatePlanarWorld;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestBaseDetectFiducialSquare {

	/**
	 * Apply heavy lens distortion. This test will only pass if it's correctly removed.
	 */
	@Test
	public void heavyLensRemoval() {
		int width = 640,height=640;
		CameraPinholeRadial intrinsic = new CameraPinholeRadial(500,500,0,width/2,height/2,width,height).fsetRadial(-0.01,-0.15);
		checkDetectRender(width, height, intrinsic,true);
	}

	@Test
	public void noLensSpecified() {
		int width = 640,height=640;
		CameraPinholeRadial intrinsic = new CameraPinholeRadial(500,500,0,width/2,height/2,width,height);
		checkDetectRender(width, height, intrinsic,false);
	}


	private void checkDetectRender(int width, int height, CameraPinholeRadial intrinsic, boolean applyLens ) {
		SimulatePlanarWorld simulator = new SimulatePlanarWorld();
		simulator.setCamera(intrinsic);

		double simulatedTargetWidth = 0.4;
		Se3_F64 markerToWorld = new Se3_F64();
		markerToWorld.T.set(0,0,0.32);


		GrayF32 pattern = new GrayF32(100,100);
		ImageMiscOps.fill(pattern,0);
		ImageMiscOps.fillRectangle(pattern,255,25,25,50,50);
		simulator.setBackground(255);
		simulator.resetScene();
		simulator.addTarget(markerToWorld, simulatedTargetWidth, pattern);
		simulator.render();

//		ShowImages.showWindow(simulator.getOutput(),"Simulated");
//		BoofMiscOps.sleep(10000);

		GrayU8 grayU8 = new GrayU8(width,height);
		ConvertImage.convert(simulator.getOutput(),grayU8);

		DetectCorner detector = new DetectCorner();
		if( applyLens )
			detector.configure(new LensDistortionRadialTangential(intrinsic),width,height,false);
		detector.process(grayU8);
		assertEquals(1,detector.getFound().size);
	}

	@Test
	public void computeFractionBoundary() {
		Dummy alg = new Dummy();

		alg.borderWidthFraction = 0.25;
		alg.square.reshape(100, 100);
		GImageMiscOps.fillRectangle(alg.square,200,25,25,50,50);
		double found = alg.computeFractionBoundary(100);
		assertEquals(1.0, found, 1e-8);

		GImageMiscOps.fillRectangle(alg.square,200,0,0,100,50);
		found = alg.computeFractionBoundary(100);
		assertEquals(0.5, found, 1e-8);
	}

	public static class Dummy extends BaseDetectFiducialSquare<GrayU8> {

		public List<GrayF32> detected = new ArrayList<>();

		protected Dummy() {
			super(FactoryThresholdBinary.globalFixed(50,true,GrayU8.class),
					FactoryShapeDetector.polygon(new ConfigPolygonDetector(false, 4,4),GrayU8.class), true, 0.25, 0.65, 100, GrayU8.class);
		}

		@Override
		public boolean processSquare(GrayF32 square, Result result, double a , double b) {
			detected.add(square.clone());
			return true;
		}
	}

	/**
	 * Accepts the pattern when it's in the lower left corner
	 */
	public static class DetectCorner extends BaseDetectFiducialSquare<GrayU8> {
		protected DetectCorner() {
			super(FactoryThresholdBinary.globalFixed(50, true, GrayU8.class),
					FactoryShapeDetector.polygon(new ConfigPolygonDetector(false, 4,4),GrayU8.class), true, 0.25, 0.65, 100, GrayU8.class);
		}

		@Override
		public boolean processSquare(GrayF32 square, Result result, double a , double b) {

			int q1 = square.width/4;
			int q3 = 3*square.width/4;

			int error = 0;
			int total = 0;

			int m = square.width/2;

			for (int i = q1+2; i < q3-2; i++) {
				int x0,x1,y0,y1;
				for ( x0 = 2; x0 <m; x0++ ) {
					if( square.get(x0,i) > 125 )
						break;
				}
				for ( x1 = m; x1 < square.width; x1++ ) {
					if( square.get(x1,i) < 125 )
						break;
				}

				for ( y0 = 2; y0 <m; y0++ ) {
					if( square.get(i,y0) > 125 )
						break;
				}
				for ( y1 = m; y1 < square.width; y1++ ) {
					if( square.get(i,y1) < 125 )
						break;
				}

				error += Math.abs(x0-q1);
				error += Math.abs(y0-q1);
				error += Math.abs(x1-q3-1);
				error += Math.abs(y1-q3-1);

				total += 4;
			}

			double averageError = error / (double)total;

//			ShowImages.showWindow(square,"Square");
//			BoofMiscOps.sleep(10000);

			assertTrue(averageError < 2);

			result.lengthSide = 2.0;
			result.rotation = 1;
			return true;
		}
	}
}