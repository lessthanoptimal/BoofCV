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

package boofcv.simulation;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSimulatePlanarWorld {
	/**
	 * Sees if a box appears to be the correct size using pinhole distortion
	 */
	@Test
	public void checkApparentSize() {
		double markerZ = 2;
		double markerWidth = 0.5;
		CameraPinhole pinhole = new CameraPinhole(400,400,0,300,250,600,500);

		GrayF32 marker = new GrayF32(40,30);
		GImageMiscOps.fill(marker,255);

		Se3_F64 markerToWorld = SpecialEuclideanOps_F64.setEulerXYZ(0,Math.PI,0,0,0,markerZ,null);

		SimulatePlanarWorld alg = new SimulatePlanarWorld();

		alg.setCamera(pinhole);
		alg.addSurface(markerToWorld,markerWidth,marker);

		alg.render();

		// Project the points from 3D onto the image and compute the distance the pixels are apart along
		// the x and y axis
		Point2D_F64 p0 = new Point2D_F64();
		Point2D_F64 p1 = new Point2D_F64();
		double ratio = marker.height/(double)marker.width;
		WorldToCameraToPixel w2p = new WorldToCameraToPixel();
		w2p.configure(pinhole,new Se3_F64());
		assertTrue(w2p.transform(new Point3D_F64(-markerWidth/2,-ratio*markerWidth/2,markerZ),p0));
		assertTrue(w2p.transform(new Point3D_F64(markerWidth/2,ratio*markerWidth/2,markerZ),p1));

		double expectedWidth = p1.x-p0.x;
		double expectedHeight = p1.y-p0.y;

		GrayF32 image = alg.getOutput();

		assertEquals(expectedWidth,findWidth(image,image.height/2),1);
		assertEquals(expectedWidth,findWidth(image,image.height/2-10),1);
		assertEquals(expectedWidth,findWidth(image,image.height/2+10),1);
		assertEquals(expectedHeight,findHeight(image,image.width/2),1);
		assertEquals(expectedHeight,findHeight(image,image.width/2-10),1);
		assertEquals(expectedHeight,findHeight(image,image.width/2+10),1);
	}

	public double findWidth( GrayF32 image , int row ) {
		int x0 = 0;
		while( x0 < image.width ) {
			if( image.get(x0,row) > 200 ) {
				break;
			}
			x0++;
		}

		int x1 = x0;
		while( x1 < image.width ) {
			if( image.get(x1,row) < 200 ) {
				break;
			}
			x1++;
		}

		return x1-x0+1;
	}
	public double findHeight( GrayF32 image , int col ) {
		int y0 = 0;
		while( y0 < image.height ) {
			if( image.get(col,y0) > 200 ) {
				break;
			}
			y0++;
		}

		int y1 = y0;
		while( y1 < image.height ) {
			if( image.get(col,y1) < 200 ) {
				break;
			}
			y1++;
		}

		return y1-y0+1;
	}

	/**
	 * Do features appear at the right location or are they mirrors/flipped?
	 */
	@Test
	public void checkOrientation() {
		checkOrientation(100, 255, 255, 255, 0);
		checkOrientation(255, 255, 255, 100, Math.PI/2);
		checkOrientation(255, 255, 100, 255, Math.PI);

	}

	private void checkOrientation(int expectedAA, int expectedAB, int expectedBB, int expectedBA, double rotZ) {
		double markerZ = 2;
		double markerWidth = 0.5;
		CameraPinhole pinhole = new CameraPinhole(400,400,0,300,250,600,500);

		GrayF32 marker = new GrayF32(40,30);
		GImageMiscOps.fill(marker,255);
		// fill the top left corner so that we can tell the orientation
		GImageMiscOps.fillRectangle(marker,100,0,0,20,15);

		Se3_F64 markerToWorld = SpecialEuclideanOps_F64.setEulerXYZ(0,Math.PI,rotZ,0,0,markerZ,null);

		SimulatePlanarWorld alg = new SimulatePlanarWorld();

		alg.setCamera(pinhole);
		alg.addSurface(markerToWorld,markerWidth,marker);

		alg.render();

		GrayF32 image = alg.getOutput();

//		ShowImages.showWindow(image,"Adsasd");
//		try {
//			Thread.sleep(10000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}

		// figure out where the marker was rendered
		Point2D_F64 p0 = new Point2D_F64();
		Point2D_F64 p1 = new Point2D_F64();
		double ratio = marker.height/(double)marker.width;
		WorldToCameraToPixel w2p = new WorldToCameraToPixel();
		w2p.configure(pinhole,markerToWorld);
		assertTrue(w2p.transform(new Point3D_F64(-markerWidth/2,-ratio*markerWidth/2,0),p0));
		assertTrue(w2p.transform(new Point3D_F64(markerWidth/2,ratio*markerWidth/2,0),p1));

		int x0 = (int)Math.min(p0.x,p1.x);
		int y0 = (int)Math.min(p0.y,p1.y);
		int x1 = (int)Math.max(p0.x,p1.x);
		int y1 = (int)Math.max(p0.y,p1.y);
		int xm = (x0+x1)/2;
		int ym = (y0+y1)/2;
		double area = (x1-x0)*(y1-y0)/4.0;

		double regionAA = image.get((x0+xm)/2,(y0+ym)/2);
		double regionAB = image.get((x0+xm)/2,(ym+y1)/2);
		double regionBB = image.get((xm+x1)/2,(ym+y1)/2);
		double regionBA = image.get((xm+x1)/2,(y0+ym)/2);

		assertEquals(expectedAA,regionAA,5);
		assertEquals(expectedAB,regionAB,5);
		assertEquals(expectedBB,regionBB,5);
		assertEquals(expectedBA,regionBA,5);
	}

	@Test
	public void computePixel() {
		double markerZ = 2;
		double markerWidth = 0.5;
		CameraPinhole pinhole = new CameraPinhole(400,400,0,300,250,600,500);

		GrayF32 marker = new GrayF32(40,30);
		GImageMiscOps.fill(marker,255);

		Se3_F64 markerToWorld = SpecialEuclideanOps_F64.setEulerXYZ(0,Math.PI,0,0,0,markerZ,null);

		SimulatePlanarWorld alg = new SimulatePlanarWorld();

		alg.setCamera(pinhole);
		alg.addSurface(markerToWorld,markerWidth,marker);
		alg.getImageRect(0).rectInCamera();

		double ratio = marker.height/(double)marker.width;
		Point2D_F64 p = new Point2D_F64();
		WorldToCameraToPixel w2p = new WorldToCameraToPixel();
		w2p.configure(pinhole,new Se3_F64());

		w2p.transform(new Point3D_F64(-markerWidth/2,-ratio*markerWidth/2,markerZ),p);

		// marker coordinate system is +y up. I think that's how this works. I'm tired.
		Point2D_F64 found = new Point2D_F64();
		alg.computePixel(0,-markerWidth/2,ratio*markerWidth/2, found);

		assertEquals( p.x , found.x , UtilEjml.TEST_F64_SQ);
		assertEquals( p.y , found.y , UtilEjml.TEST_F64_SQ);

		// should be in the top left quadrant
		assertTrue( p.x < pinhole.width/2);
		assertTrue( p.y < pinhole.height/2);
	}

	@Test
	public void computeProjectionTable() {
		CameraPinhole pinhole = new CameraPinhole(400,400,0,300,250,600,500);

		SimulatePlanarWorld alg = new SimulatePlanarWorld();

		alg.setCamera(pinhole);
//		alg.computeProjectionTable(pinhole.width,pinhole.height);

		Point2D_F64 n = new Point2D_F64();
		Point3D_F64 E = new Point3D_F64();
		Point3D_F64 F = new Point3D_F64();

		int index = 0;
		for (int y = 0; y < pinhole.height; y++) {
			for (int x = 0; x < pinhole.width; x++) {
				F.x = alg.pointing[index++];
				F.y = alg.pointing[index++];
				F.z = alg.pointing[index++];

				PerspectiveOps.convertPixelToNorm(pinhole,new Point2D_F64(x,y),n);
				E.set(n.x,n.y,1);
				E.divideIP(E.norm());

				assertEquals(0,F.distance(E), UtilEjml.TEST_F64_SQ);
			}
		}
	}

//	public static void main(String[] args) {
//		GrayF32 image = new GrayF32(400,300);
//		GImageMiscOps.fill(image,255);
//		GImageMiscOps.fillRectangle(image,90,20,20,40,40);
//		GImageMiscOps.fillRectangle(image,90,60,60,40,40);
//		GImageMiscOps.fillRectangle(image,90,100,20,40,40);
//
//		GImageMiscOps.fillRectangle(image,90,300,200,60,60);
//
//		Se3_F64 rectToWorld = new Se3_F64();
//		rectToWorld.T.set(0,0,0.2);
//		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0,Math.PI,0,rectToWorld.R);
//
//		Se3_F64 rectToWorld2 = new Se3_F64();
//		rectToWorld2.T.set(0,-0.20,0.3);
//
//		String fisheyePath = UtilIO.pathExample("fisheye/theta/");
//		CameraUniversalOmni model = CalibrationIO.load(new File(fisheyePath,"front.yaml"));
//
//		SimulatePlanarWorld alg = new SimulatePlanarWorld();
//
//		alg.setCamera(model);
//		alg.addSurface(rectToWorld,0.3,image);
//		alg.addSurface(rectToWorld2,0.15,image);
//		alg.render();
//
//		BufferedImage output = new BufferedImage(model.width,model.height,BufferedImage.TYPE_INT_RGB);
//
//		ConvertBufferedImage.convertTo(alg.getOutput(),output);
//
//		ImagePanel panel = ShowImages.showWindow(output,"Rendered Fisheye",true);
//
//		for (int i = 0; i < 2000; i++) {
//			alg.getImageRect(0).rectToWorld.T.x = 0.7*Math.sin(i*0.01-Math.PI/4);
//			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0,Math.PI,i*0.05,
//					alg.getImageRect(1).rectToWorld.R);
//			alg.render();
//			ConvertBufferedImage.convertTo(alg.getOutput(),output);
//			panel.repaint();
//			BoofMiscOps.sleep(10);
//		}
//	}
}
