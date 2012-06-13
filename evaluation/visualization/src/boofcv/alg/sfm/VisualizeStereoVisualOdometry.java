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
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.MediaManager;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.io.wrapper.images.MjpegStreamSequence;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;

/**
 * @author Peter Abeles
 */
public class VisualizeStereoVisualOdometry implements MouseListener
{
	boolean paused = false;

	private static void drawFeatures( AccessSfmPointTracks tracker , BufferedImage image )  {

		Graphics2D g2 = image.createGraphics();

//		drawFeatureID(tracker, g2);
		drawFeatureZ(tracker,g2);

		for( Point2D_F64 p : tracker.getNewTracks() ) {
			VisualizeFeatures.drawPoint(g2,(int)p.x,(int)p.y,3,Color.YELLOW);
		}

		g2.setColor(Color.BLUE);
		for( Point2D_F64 p : tracker.getAllTracks() ) {
			VisualizeFeatures.drawCross(g2,(int)p.x,(int)p.y,3);
		}

		for( Point2D_F64 p : tracker.getInlierTracks() ) {
			VisualizeFeatures.drawPoint(g2,(int)p.x,(int)p.y,3,Color.RED);
		}
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

		ImagePanel gui = new ImagePanel();
		gui.addMouseListener(this);
		gui.setPreferredSize(new Dimension(640,480));
		ShowImages.showWindow(gui, "Left Video");

		while( videoLeft.hasNext() ) {
			ImageFloat32 left = videoLeft.next();
			ImageFloat32 right = videoRight.next();

			long before = System.nanoTime();
			boolean worked = alg.process(left,right);
			long after = System.nanoTime();

			drawFeatures((AccessSfmPointTracks)alg,videoLeft.getGuiImage());

			if( worked ) {
				Se3_F64 pose = alg.getCameraToWorld();

				System.out.println(frameNumber+"   location: "+pose.getT()+"  ms = "+(after-before)/1e6);
			} else {
				System.out.println(frameNumber+"   failed");
			}

			gui.setBufferedImage(videoLeft.getGuiImage());
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

		SimpleImageSequence<ImageFloat32> videoLeft =
				new MjpegStreamSequence<ImageFloat32>("/home/pja/temp/left.mjpeg",ImageFloat32.class);
		SimpleImageSequence<ImageFloat32> videoRight =
				new MjpegStreamSequence<ImageFloat32>("/home/pja/temp/right.mjpeg",ImageFloat32.class);

		ImagePointTracker<ImageFloat32> tracker =
				FactoryPointSequentialTracker.klt(300,new int[]{1,2,4,8},3,3,2,ImageFloat32.class,ImageFloat32.class);

		StereoDisparitySparse<ImageFloat32> disparity =
				FactoryStereoDisparity.regionSparseWta(2,100,3,3,40,-1,true,ImageFloat32.class);

		StereoVisualOdometry<ImageFloat32> alg = FactoryVisualOdometry.stereoSimple(300,3,tracker,stereoParam,
				disparity,ImageFloat32.class);

		VisualizeStereoVisualOdometry gui = new VisualizeStereoVisualOdometry();
		gui.process(videoLeft, videoRight, alg);
	}
}
