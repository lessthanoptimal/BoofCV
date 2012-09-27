/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm;

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.sfm.StereoVisualOdometry;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.gui.d3.Orientation3D;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.io.wrapper.xuggler.XugglerSimplified;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageFloat32;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class VisualizeStereoVisualOdometry implements MouseListener
{
	boolean paused = false;

	private static void drawFeatures( AccessSfmPointTracks tracker , BufferedImage image )  {

		Graphics2D g2 = image.createGraphics();

//		drawFeatureID(tracker, g2);
//		drawFeatureZ(tracker,g2);

		for( Point2D_F64 p : tracker.getNewTracks() ) {
			VisualizeFeatures.drawPoint(g2,(int)p.x,(int)p.y,3,Color.GREEN);
		}

		g2.setColor(Color.BLUE);
		g2.setStroke(new BasicStroke(3));
		for( Point2D_F64 p : tracker.getAllTracks() ) {
			VisualizeFeatures.drawCross(g2,(int)p.x,(int)p.y,3);
		}

		List<Point2D_F64> inliers = tracker.getInlierTracks();
		if( inliers.size() > 0 ) {
			double ranges[] = new double[tracker.getInlierTracks().size() ];
			for( int i = 0; i < tracker.getInlierTracks().size(); i++ ) {

				int indexAll = tracker.fromInlierToAllIndex(i);
				Point3D_F64 p3 = tracker.getTrackLocation(indexAll);

				ranges[i] = p3.z;
			}
			Arrays.sort(ranges);

			double maxRange = ranges[(int)(ranges.length*0.8)];

			for( int i = 0; i < tracker.getInlierTracks().size(); i++ ) {

				int indexAll = tracker.fromInlierToAllIndex(i);

				Point2D_F64 pixel = tracker.getInlierTracks().get(i);
				Point3D_F64 p3 = tracker.getTrackLocation(indexAll);

				double r = p3.z/maxRange;
				if( r < 0 ) r = 0;
				else if( r > 1 ) r = 1;

				int color = (255 << 16) | ((int)(255*r) << 8);

				VisualizeFeatures.drawPoint(g2,(int)pixel.x,(int)pixel.y,3,new Color(color));
			}

		}

		g2.setColor(Color.BLACK);
		g2.fillRect(25,15,80,45);
		g2.setColor(Color.CYAN);
		g2.drawString("Total: " + tracker.getAllTracks().size(), 30, 30);
		g2.drawString("Inliers: "+inliers.size(),30,50);
	}

	private static void drawFeatureID(AccessSfmPointTracks tracker, Graphics2D g2) {
		g2.setFont(new Font(Font.MONOSPACED,Font.BOLD,10));
		int N = tracker.getAllTracks().size();
		FontMetrics fm = g2.getFontMetrics();
		int width = fm.stringWidth("00000");
		int height = fm.getHeight();
		for( int i = 0; i < N; i++ ) {
			Point2D_F64 p = tracker.getAllTracks().get(i);
			long id = tracker.getTrackId(i);
			String text = String.format("%5d",id);

			int x0 = (int)p.x-width-5;
			int y0 = (int)p.y+height/2-1;

//			g2.setColor(Color.WHITE);
//			g2.fillRect(x0-1,y0-height,width+3,height+2);
			g2.setColor(Color.BLUE);
			g2.drawString(text,x0,y0);
		}
	}

	private static void drawFeatureZ(AccessSfmPointTracks tracker, Graphics2D g2) {
		g2.setFont(new Font(Font.MONOSPACED,Font.BOLD,10));
		int N = tracker.getAllTracks().size();
		FontMetrics fm = g2.getFontMetrics();
		int width = fm.stringWidth("0000000");
		int height = fm.getHeight();

		for( int i = 0; i < N; i++ ) {
			Point3D_F64 P = tracker.getTrackLocation(i);
			if( P == null )
				continue;
			Point2D_F64 p = tracker.getAllTracks().get(i);
			long id = tracker.getTrackId(i);
			String text = String.format("%7.2E",P.z);

			int x0 = (int)p.x-width/2;
			int y0 = (int)p.y-5;

//			g2.setColor(Color.WHITE);
//			g2.fillRect(x0-1,y0-height,width+3,height+2);

			g2.setColor(Color.CYAN);
			g2.drawString(text,x0,y0);
		}
	}

	private void process(SimpleImageSequence<ImageFloat32> videoLeft,
						 SimpleImageSequence<ImageFloat32> videoRight,
						 StereoVisualOdometry<ImageFloat32> alg)
	{
		int frameNumber = 0;

		Orientation3D orientation = new Orientation3D();
		ImagePanel gui = new ImagePanel();
		gui.addMouseListener(this);
		gui.setPreferredSize(new Dimension(640,480));
		ShowImages.showWindow(gui, "Left Video");
		ShowImages.showWindow(orientation,"Orientation");

		int totalInliers = 0;

		while( videoLeft.hasNext() && videoRight.hasNext() ) {
			ImageFloat32 left = videoLeft.next();
			ImageFloat32 right = videoRight.next();

			long before = System.nanoTime();
			boolean worked = alg.process(left,right);
			long after = System.nanoTime();

			BufferedImage imageGuiLeft = videoLeft.getGuiImage();

			if( alg instanceof AccessSfmPointTracks ) {
				drawFeatures((AccessSfmPointTracks)alg,imageGuiLeft);

				totalInliers += ((AccessSfmPointTracks)alg).getInlierTracks().size();
			}

			if( worked ) {
				Se3_F64 pose = alg.getCameraToWorld();

				Vector3D_F64 v = new Vector3D_F64(0,0,1);
				GeometryMath_F64.mult(pose.getR(), v, v);

				orientation.setVector(v);
				orientation.repaint();

				System.out.println(frameNumber+"   location: "+pose.getT()+"  ms = "+(after-before)/1e6+"  "+totalInliers);
			} else {
				System.out.println(frameNumber+"   failed");
			}

			gui.setBufferedImage(imageGuiLeft);
			gui.repaint();
			frameNumber++;

			while( paused ) {
				BoofMiscOps.pause(50);
			}
		}
		System.out.println("Done");
	}

	@Override
	public void mouseClicked(MouseEvent e) {paused = !paused;}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	public static void main( String args[] ) throws FileNotFoundException {

		MediaManager media = DefaultMediaManager.INSTANCE;

		StereoParameters stereoParam = BoofMiscOps.loadXML("stereo.xml");

		String fileLeft = "/home/pja/temp/left.mjpeg";
		String fileRight = "/home/pja/temp/right.mjpeg";
//		String fileLeft = "/home/pja/temp/left_test.avi";
//		String fileRight = "/home/pja/temp/right_test.avi";

		SimpleImageSequence<ImageFloat32> videoLeft =
				new XugglerSimplified<ImageFloat32>(fileLeft, ImageFloat32.class);
		SimpleImageSequence<ImageFloat32> videoRight =
				new XugglerSimplified<ImageFloat32>(fileLeft, ImageFloat32.class);

//		SimpleImageSequence<ImageFloat32> videoLeft =
//				new MjpegStreamSequence<ImageFloat32>(fileLeft,ImageFloat32.class);
//		SimpleImageSequence<ImageFloat32> videoRight =
//				new MjpegStreamSequence<ImageFloat32>(fileRight,ImageFloat32.class);

		ImagePointTracker<ImageFloat32> tracker =
				FactoryPointSequentialTracker.klt(400,new int[]{1,2,4,8},3,3,2,ImageFloat32.class,ImageFloat32.class);

		StereoDisparitySparse<ImageFloat32> disparity =
				FactoryStereoDisparity.regionSparseWta(0,150,3,3,20,-1,true,ImageFloat32.class);

//		StereoVisualOdometry<ImageFloat32> alg = FactoryVisualOdometry.stereoSimple(300,3,tracker,stereoParam,
//				disparity,ImageFloat32.class);
		StereoVisualOdometry<ImageFloat32> alg = FactoryVisualOdometry.stereoDepth(100, 1, tracker, stereoParam,
				disparity, ImageFloat32.class);
//		StereoVisualOdometry<ImageFloat32> alg = FactoryVisualOdometry.stereoEpipolar(75, 1, tracker, stereoParam,
//				disparity, ImageFloat32.class);

		VisualizeStereoVisualOdometry gui = new VisualizeStereoVisualOdometry();
		gui.process(videoLeft, videoRight, alg);
	}
}
