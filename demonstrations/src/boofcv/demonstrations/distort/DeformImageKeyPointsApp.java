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

package boofcv.demonstrations.distort;

import boofcv.abst.distort.PointDeformKeyPoints;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F32;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Re-renders the image with a new camera model
 *
 * @author Peter Abeles
 */
// todo key point location with lines
// todo let use click to add and drag distorted line
public class DeformImageKeyPointsApp<T extends ImageBase<T>> extends DemonstrationBase<T>
	implements DeformKeypointPanel.Listener
{

	float clickTol = 10;

	CustomImagePanel gui = new CustomImagePanel();
	DeformKeypointPanel control;
	PointToPixelTransform_F32 p2p = new PointToPixelTransform_F32();


	PointDeformKeyPoints alg;

	ImageDistort<T,T> distortImage;
	BufferedImage distortedBuff;
	boolean validTransform = false;

	T undistorted;
	T distorted;

	// key point locations
	final List<Point2D_F32> pointsUndistorted = new ArrayList<>();
	final List<Point2D_F32> pointsDistorted = new ArrayList<>();

	public DeformImageKeyPointsApp(List<?> exampleInputs, ImageType<T> imageType) {
		super(exampleInputs, imageType);

		distorted = imageType.createImage(1,1);
		undistorted = imageType.createImage(1,1);

		distortImage = FactoryDistort.distort(true,InterpolationType.BILINEAR,
				BorderType.ZERO, imageType, imageType);

		control = new DeformKeypointPanel(this);
		handleAlgorithmChange();

		add(control, BorderLayout.WEST);
		add(gui, BorderLayout.CENTER);
	}

	@Override
	public void processImage(final BufferedImage buffered, final T undistorted)
	{
		BufferedImage tmp = distortedBuff;
		distortedBuff = ConvertBufferedImage.checkDeclare(
				undistorted.width,undistorted.height,distortedBuff,BufferedImage.TYPE_INT_RGB);

		if( tmp != distortedBuff ) {
			distorted.reshape(undistorted.width, undistorted.height);
			this.undistorted.reshape(undistorted.width, undistorted.height);
			gui.setPreferredSize(new Dimension(undistorted.width,undistorted.height));
			alg.setImageShape(undistorted.width,undistorted.height);
			synchronized (pointsUndistorted){
				validTransform = false;
				pointsUndistorted.clear();
				pointsDistorted.clear();
				alg.setKeyPoints(pointsUndistorted);
			}
		}

		if( inputMethod == InputMethod.IMAGE ) {
			this.undistorted.setTo(undistorted);
		}
		renderDistorted(buffered, undistorted);
	}

	private void renderDistorted(BufferedImage buffered, T undistorted) {
		// if not enough points have been set it can blow up
		if( validTransform ) {
//			Point2D_F32 point = new Point2D_F32();
//			alg.compute(0,0,point);
//			System.out.println("(0,0) = "+point);
//			alg.compute(undistorted.width-1,undistorted.height-1,point);
//			System.out.println("(w,h) = "+point);
			distortImage.apply(undistorted, distorted);
			ConvertBufferedImage.convertTo(distorted, distortedBuff, true);
		} else {
			if( distortedBuff != buffered )
				distortedBuff.createGraphics().drawImage(buffered,0,0,undistorted.width,undistorted.height,null);
		}
		gui.setBufferedImageSafe(distortedBuff);
	}

	@Override
	public void handleVisualizationChange() {

	}

	@Override
	public void handleAlgorithmChange() {
		alg = FactoryDistort.deformMls(control.getConfigMLS());
		p2p.setTransform(alg);
		controlPointsModified(false);
	}

	private void controlPointsModified(boolean justDistorted ) {
		synchronized (pointsUndistorted){
			try {
				if (!justDistorted) {
					alg.setKeyPoints(pointsUndistorted);
					validTransform = true;
				}
				System.out.println("Set distorted");
				alg.setDistorted(pointsDistorted);
			} catch( RuntimeException e ) {
				System.out.println("   FAILED!!");
				validTransform = false;
			}
		}
		distortImage.setModel(p2p);
		if( inputMethod == InputMethod.IMAGE ) {
			renderDistorted(distortedBuff, undistorted);
		}
		gui.repaint();
	}

	class CustomImagePanel extends ImagePanel implements MouseListener, MouseMotionListener{

		int active = -1;
		boolean selectedUndist;
		Ellipse2D.Double c = new Ellipse2D.Double();
		Line2D.Double line = new Line2D.Double();
		BasicStroke strokeThick = new BasicStroke(3);
		BasicStroke strokeThin = new BasicStroke(1);

		CustomImagePanel() {
			addMouseListener(this);
			addMouseMotionListener(this);
			requestFocus();
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			synchronized (pointsUndistorted) {
				g2.setStroke(strokeThin);
				g2.setColor(Color.GRAY);
				for (int i = 0; i < pointsUndistorted.size(); i++) {
					Point2D_F32 u = pointsUndistorted.get(i);
					Point2D_F32 d = pointsDistorted.get(i);
					line.setLine(u.x*scale,u.y*scale,d.x*scale,d.y*scale);
					g2.draw(line);
				}

				double r = clickTol/2.0;

				g2.setStroke(strokeThick);
				g2.setColor(Color.RED);
				for (int i = 0; i < pointsUndistorted.size(); i++) {
					Point2D_F32 p = pointsUndistorted.get(i);
					c.setFrame(p.x*scale - r, p.y*scale - r, 2 * r, 2 * r);
					g2.draw(c);
				}
				g2.setColor(Color.BLUE);
				for (int i = 0; i < pointsDistorted.size(); i++) {
					Point2D_F32 p = pointsDistorted.get(i);
					c.setFrame(p.x*scale - r, p.y*scale - r, 2 * r, 2 * r);
					g2.draw(c);
				}
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {

			float x = (float)(e.getX()/scale);
			float y = (float)(e.getY()/scale);

			active = -1;
			synchronized (pointsUndistorted) {
				for (int i = 0; i < pointsUndistorted.size(); i++) {
					Point2D_F32 p = pointsUndistorted.get(i);
					if( p.distance(x,y) <= clickTol ) {
						active = i;
						selectedUndist = true;
					}
				}

				if( active < 0 ) {
					active = pointsDistorted.size();
					selectedUndist = false;
					pointsUndistorted.add( new Point2D_F32(x,y));
					pointsDistorted.add( new Point2D_F32(x,y));
					controlPointsModified(false );
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if( active < 0 )
				return;

			active = -1;
		}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mouseDragged(MouseEvent e) {
			if( active < 0 )
				return;

			float x = (float)(e.getX()/scale);
			float y = (float)(e.getY()/scale);

			synchronized (pointsUndistorted) {
				Point2D_F32 u = pointsDistorted.get(active);
				u.set(x,y);
			}
			controlPointsModified(true);
		}

		@Override
		public void mouseMoved(MouseEvent e) {}
	}

	public static void main( String args[] ) {
		ImageType type = ImageType.pl(3, GrayU8.class);

		List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("Mona Lisa", UtilIO.pathExample("standard/mona_lisa.jpg")));

		DeformImageKeyPointsApp app = new DeformImageKeyPointsApp(inputs,type);

		app.openFile(new File(inputs.get(0).getPath()));

		app.waitUntilDoneProcessing();

		ShowImages.showWindow(app, "Deform Image Key Points",true);
	}
}
