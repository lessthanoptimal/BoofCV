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

package boofcv.demonstrations.distort;

import boofcv.abst.distort.PointDeformKeyPoints;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.border.BorderType;
import boofcv.struct.distort.PointToPixelTransform_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F32;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Lets the user manually add control points for a distortion and shows the results
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DeformImageKeyPointsApp<T extends ImageBase<T>> extends DemonstrationBase
		implements DeformKeypointPanel.Listener {

	float clickTol = 10;

	CustomImagePanel gui = new CustomImagePanel();
	DeformKeypointPanel control;
	PointToPixelTransform_F32 p2p = new PointToPixelTransform_F32();

	PointDeformKeyPoints alg;

	ImageDistort<T, T> distortImage;
	BufferedImage distortedBuff;
	boolean validTransform = false;

	T undistorted;
	T distorted;

	// key point locations
	final List<Point2D_F32> pointsUndistorted = new ArrayList<>();
	final List<Point2D_F32> pointsDistorted = new ArrayList<>();

	public DeformImageKeyPointsApp( List<?> exampleInputs, ImageType<T> imageType ) {
		super(exampleInputs, imageType);

		undistorted = imageType.createImage(1, 1);

		distortImage = FactoryDistort.distort(true, InterpolationType.BILINEAR,
				BorderType.ZERO, imageType, imageType);

		control = new DeformKeypointPanel(this);
		handleAlgorithmChange();

		gui.setPreferredSize(new Dimension(600, 600));

		add(control, BorderLayout.WEST);
		add(gui, BorderLayout.CENTER);

		gui.addMouseWheelListener(new MouseAdapter() {
			@Override
			public void mouseWheelMoved( MouseWheelEvent e ) {
				control.setZoom(BoofSwingUtil.mouseWheelImageZoom(control.zoom, e));
			}
		});
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, final int width, final int height ) {
		super.handleInputChange(source, method, width, height);

		BoofSwingUtil.invokeNowOrLater(new Runnable() {
			@Override
			public void run() {
				double zoom = BoofSwingUtil.selectZoomToShowAll(gui, width, height);
				control.setZoom(zoom);
				gui.getVerticalScrollBar().setValue(0);
				gui.getHorizontalScrollBar().setValue(0);
			}
		});
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage buffered, final ImageBase undistorted ) {
		// do this here instead of a reshape to ensure the number of bands is the same
		distorted = (T)undistorted.createSameShape();

		BufferedImage tmp = distortedBuff;
		distortedBuff = ConvertBufferedImage.checkDeclare(
				undistorted.width, undistorted.height, distortedBuff, buffered.getType());

		if (tmp != distortedBuff) {
			distorted.reshape(undistorted.width, undistorted.height);
			this.undistorted.reshape(undistorted.width, undistorted.height);
			gui.setPreferredSize(new Dimension(undistorted.width, undistorted.height));
			alg.setImageShape(undistorted.width, undistorted.height);
			synchronized (pointsUndistorted) {
				validTransform = false;
				pointsUndistorted.clear();
				pointsDistorted.clear();
			}
		}

		if (inputMethod == InputMethod.IMAGE) {
			this.undistorted.setTo((T)undistorted);
		}
		renderDistorted(buffered, (T)undistorted);
	}

	private void renderDistorted( @Nullable BufferedImage buffered, T undistorted ) {
		// if not enough points have been set it can blow up
		if (!control.isShowOriginal() && validTransform) {
			distortImage.apply(undistorted, distorted);
			ConvertBufferedImage.convertTo(distorted, distortedBuff, true);
		} else {
			if (buffered != null)
				distortedBuff.createGraphics().drawImage(buffered, 0, 0, undistorted.width, undistorted.height, null);
			else {
				ConvertBufferedImage.convertTo(undistorted, distortedBuff, true);
			}
		}
		BoofSwingUtil.invokeNowOrLater(() -> {
			gui.setImage(distortedBuff);
			gui.repaint();
		});
	}

	@Override
	public void handleVisualizationChange() {

		if (inputMethod == InputMethod.IMAGE) {
			renderDistorted(null, undistorted);
		}
		BoofSwingUtil.invokeNowOrLater(() -> gui.setScale(control.zoom));
	}

	@Override
	public void handleAlgorithmChange() {
		alg = FactoryDistort.deformMls(control.getConfigMLS());
		p2p.setTransform(alg);
		alg.setImageShape(undistorted.width, undistorted.height);
		controlPointsModified();
	}

	@Override
	public void handleClearPoints() {
		synchronized (pointsUndistorted) {
			pointsDistorted.clear();
			pointsUndistorted.clear();
		}
		controlPointsModified();
	}

	private void controlPointsModified() {
		synchronized (pointsUndistorted) {
			try {
				// transform needs to go from distorted image to undistorted image
				alg.setSource(pointsDistorted);
				alg.setDestination(pointsUndistorted);
				validTransform = true;
			} catch (RuntimeException e) {
//				System.out.println("Failed because of "+e.getMessage());
//				System.out.println("   total points "+pointsDistorted.size());
				validTransform = false;
			}
		}
		distortImage.setModel(p2p);
		if (inputMethod == InputMethod.IMAGE) {
			renderDistorted(null, undistorted);
		}
		gui.repaint();
	}

	class CustomImagePanel extends ImageZoomPanel implements MouseListener, MouseMotionListener {

		int active = -1;
		boolean selectedUndist;
		Ellipse2D.Double c = new Ellipse2D.Double();
		Line2D.Double line = new Line2D.Double();
		BasicStroke strokeThick = new BasicStroke(4);
		BasicStroke strokeThin = new BasicStroke(2);

		CustomImagePanel() {
			getImagePanel().addMouseListener(this);
			getImagePanel().addMouseMotionListener(this);
			requestFocus();
		}

		@Override
		protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
			BoofSwingUtil.antialiasing(g2);

			if (active == -1 && !control.showPoints)
				return;

			synchronized (pointsUndistorted) {
				g2.setStroke(strokeThin);
				g2.setColor(Color.GRAY);

				double r = clickTol/2.0;

				for (int i = 0; i < pointsDistorted.size(); i++) {
					if (active != -1 && i != active) continue;

					Point2D_F32 u = pointsUndistorted.get(i);
					Point2D_F32 d = pointsDistorted.get(i);
					float x0 = (float)(u.x*scale);
					float y0 = (float)(u.y*scale);

					float x1 = (float)(d.x*scale);
					float y1 = (float)(d.y*scale);

					g2.setStroke(strokeThick);
					g2.setColor(Color.BLUE);
					VisualizeShapes.drawArrow(x0, y0, x1, y1, r, line, g2);
					g2.setStroke(strokeThin);
					g2.setColor(Color.LIGHT_GRAY);
					VisualizeShapes.drawArrow(x0, y0, x1, y1, r, line, g2);
				}

				for (int i = 0; i < pointsUndistorted.size(); i++) {
					if (active != -1 && i != active) continue;

					Point2D_F32 p = pointsUndistorted.get(i);
					c.setFrame(p.x*scale - r, p.y*scale - r, 2*r, 2*r);

					g2.setColor(Color.RED);
					g2.fill(c);
				}
			}
		}

		@Override
		public void mouseClicked( MouseEvent e ) {}

		@Override
		public void mousePressed( MouseEvent e ) {
			if (!SwingUtilities.isLeftMouseButton(e))
				return;

			boolean delete = e.isControlDown();

			float x = (float)(e.getX()/scale);
			float y = (float)(e.getY()/scale);

			active = -1;
			synchronized (pointsUndistorted) {
				float bestDistance = clickTol;
				for (int i = 0; i < pointsDistorted.size(); i++) {
					Point2D_F32 p = pointsDistorted.get(i);
					float d = p.distance(x, y);
					if (d < bestDistance) {
						active = i;
						bestDistance = d;
						selectedUndist = false;
					}
				}

				for (int i = 0; i < pointsUndistorted.size(); i++) {
					Point2D_F32 p = pointsUndistorted.get(i);
					float d = p.distance(x, y);
					if (d < bestDistance) {
						active = i;
						bestDistance = d;
						selectedUndist = true;
					}
				}

				if (active < 0) {
					active = pointsDistorted.size();
					selectedUndist = false;
					pointsUndistorted.add(new Point2D_F32(x, y));
					pointsDistorted.add(new Point2D_F32(x, y));
					controlPointsModified();
				} else if (delete) {
					pointsUndistorted.remove(active);
					pointsDistorted.remove(active);
					active = -1;
					controlPointsModified();
				}
			}
		}

		@Override
		public void mouseReleased( MouseEvent e ) {
			if (!SwingUtilities.isLeftMouseButton(e))
				return;
			if (active < 0)
				return;

			active = -1;
			repaint();
		}

		@Override
		public void mouseEntered( MouseEvent e ) {}

		@Override
		public void mouseExited( MouseEvent e ) {}

		@Override
		public void mouseDragged( MouseEvent e ) {
			if (!SwingUtilities.isLeftMouseButton(e))
				return;
			if (active < 0)
				return;

			float x = (float)(e.getX()/scale);
			float y = (float)(e.getY()/scale);

			synchronized (pointsUndistorted) {
				Point2D_F32 u;
				if (selectedUndist) {
					u = pointsUndistorted.get(active);
				} else {
					u = pointsDistorted.get(active);
				}
				u.setTo(x, y);
			}
			controlPointsModified();
		}

		@Override
		public void mouseMoved( MouseEvent e ) {}
	}

	public static void main( String[] args ) {
		ImageType type = ImageType.pl(3, GrayU8.class);

		List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("Man MLS", UtilIO.pathExample("standard/man_mls.jpg")));
		inputs.add(new PathLabel("Mona Lisa", UtilIO.pathExample("standard/mona_lisa.jpg")));
		inputs.add(new PathLabel("Drawing Face", UtilIO.pathExample("drawings/drawing_face.png")));

		DeformImageKeyPointsApp app = new DeformImageKeyPointsApp(inputs, type);

		app.openFile(new File(inputs.get(0).getPath()));

		app.waitUntilInputSizeIsKnown();

		ShowImages.showWindow(app, "Deform Image Key Points", true);
	}
}
