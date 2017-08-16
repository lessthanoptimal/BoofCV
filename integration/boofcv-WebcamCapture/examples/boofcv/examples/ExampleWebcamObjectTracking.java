/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.examples;

import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import com.github.sarxos.webcam.Webcam;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.Rectangle2D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

/**
 * Example of how to open a webcam and track a user selected object.  Click and drag the mouse
 * to select an object to track.
 *
 * @author Peter Abeles
 */
public class ExampleWebcamObjectTracking<T extends ImageBase<T>> extends JPanel
		implements MouseListener, MouseMotionListener {

	TrackerObjectQuad<T> tracker;

	// location of the target being tracked
	Quadrilateral_F64 target = new Quadrilateral_F64();

	// location selected by the mouse
	Point2D_I32 point0 = new Point2D_I32();
	Point2D_I32 point1 = new Point2D_I32();

	int desiredWidth,desiredHeight;
	volatile int mode = 0;

	BufferedImage workImage;

	JFrame window;

	/**
	 * Configures the tracking application
	 *
	 * @param tracker The object tracker
	 * @param desiredWidth Desired size of the input stream
	 * @param desiredHeight Desired height of the input stream
	 */
	public ExampleWebcamObjectTracking( TrackerObjectQuad<T> tracker,
										int desiredWidth , int desiredHeight)
	{
		this.tracker = tracker;
		this.desiredWidth = desiredWidth;
		this.desiredHeight = desiredHeight;

		addMouseListener(this);
		addMouseMotionListener(this);

		window = new JFrame("Object Tracking");
		window.setContentPane(this);
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}

	/**
	 * Invoke to start the main processing loop.
	 */
	public void process() {
		Webcam webcam = UtilWebcamCapture.openDefault(desiredWidth,desiredHeight);

		// adjust the window size and let the GUI know it has changed
		Dimension actualSize = webcam.getViewSize();
		setPreferredSize(actualSize);
		setMinimumSize(actualSize);
		window.setMinimumSize(actualSize);
		window.setPreferredSize(actualSize);
		window.setVisible(true);

		// create
		T input = tracker.getImageType().createImage(actualSize.width,actualSize.height);

		workImage = new BufferedImage(input.getWidth(),input.getHeight(),BufferedImage.TYPE_INT_RGB);

		while( true ) {
			BufferedImage buffered = webcam.getImage();
			if( buffered == null ) break;
			ConvertBufferedImage.convertFrom(webcam.getImage(),input,true);

			// mode is read/written to by the GUI also
			int mode = this.mode;

			boolean success = false;
			if( mode == 2 ) {
				Rectangle2D_F64 rect = new Rectangle2D_F64();
				rect.set(point0.x, point0.y, point1.x, point1.y);
				UtilPolygons2D_F64.convert(rect, target);
				success = tracker.initialize(input,target);
				this.mode = success ? 3 : 0;
			} else if( mode == 3 ) {
				success = tracker.process(input,target);
			}

			synchronized( workImage ) {
				// copy the latest image into the work buffered
				Graphics2D g2 = workImage.createGraphics();
				g2.drawImage(buffered,0,0,null);

				// visualize the current results
				if (mode == 1) {
					drawSelected(g2);
				} else if (mode == 3) {
					if( success ) {
						drawTrack(g2);
					}
				}
			}

			repaint();
		}
	}

	@Override
	public void paint (Graphics g) {
		if( workImage != null ) {
			// draw the work image and be careful to make sure it isn't being manipulated at the same time
			synchronized (workImage) {
				((Graphics2D) g).drawImage(workImage, 0, 0, null);
			}
		}
	}

	private void drawSelected( Graphics2D g2 ) {
		g2.setColor(Color.RED);
		g2.setStroke( new BasicStroke(3));
		g2.drawLine(point0.getX(),point0.getY(),point1.getX(),point0.getY());
		g2.drawLine(point1.getX(),point0.getY(),point1.getX(),point1.getY());
		g2.drawLine(point1.getX(),point1.getY(),point0.getX(),point1.getY());
		g2.drawLine(point0.getX(),point1.getY(),point0.getX(),point0.getY());
	}

	private void drawTrack( Graphics2D g2 ) {
		g2.setStroke(new BasicStroke(3));
		g2.setColor(Color.RED);
		g2.drawLine((int)target.a.getX(),(int)target.a.getY(),(int)target.b.getX(),(int)target.b.getY());
		g2.setColor(Color.BLUE);
		g2.drawLine((int)target.b.getX(),(int)target.b.getY(),(int)target.c.getX(),(int)target.c.getY());
		g2.setColor(Color.GREEN);
		g2.drawLine((int)target.c.getX(),(int)target.c.getY(),(int)target.d.getX(),(int)target.d.getY());
		g2.setColor(Color.DARK_GRAY);
		g2.drawLine((int)target.d.getX(),(int)target.d.getY(),(int)target.a.getX(),(int)target.a.getY());
	}

	private void drawTarget( Graphics2D g2 ) {
		g2.setColor(Color.RED);
		g2.setStroke( new BasicStroke(2));
		g2.drawLine(point0.getX(),point0.getY(),point1.getX(),point0.getY());
		g2.drawLine(point1.getX(),point0.getY(),point1.getX(),point1.getY());
		g2.drawLine(point1.getX(),point1.getY(),point0.getX(),point1.getY());
		g2.drawLine(point0.getX(),point1.getY(),point0.getX(),point0.getY());
	}

	@Override
	public void mousePressed(MouseEvent e) {
		point0.set(e.getX(),e.getY());
		point1.set(e.getX(),e.getY());
		mode = 1;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		point1.set(e.getX(),e.getY());
		mode = 2;
	}

	@Override public void mouseClicked(MouseEvent e) {mode = 0;}

	@Override public void mouseEntered(MouseEvent e) {}

	@Override public void mouseExited(MouseEvent e) {}

	@Override public void mouseDragged(MouseEvent e) {
		if( mode == 1 ) {
			point1.set(e.getX(),e.getY());
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	public static void main(String[] args) {

		ImageType<Planar<GrayU8>> colorType = ImageType.pl(3,GrayU8.class);

		TrackerObjectQuad tracker =
				FactoryTrackerObjectQuad.circulant(null, GrayU8.class);
//				FactoryTrackerObjectQuad.sparseFlow(null,GrayU8.class,null);
//				FactoryTrackerObjectQuad.tld(null,GrayU8.class);
//				FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(), colorType);
//				FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(true),colorType);
//				FactoryTrackerObjectQuad.meanShiftLikelihood(30,5,255, MeanShiftLikelihoodType.HISTOGRAM,colorType);


		ExampleWebcamObjectTracking app = new ExampleWebcamObjectTracking(tracker,640,480);

		app.process();
	}
}
