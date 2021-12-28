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

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionWideFOV;
import boofcv.alg.distort.NarrowToWidePtoP_F32;
import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.distort.universal.LensDistortionUniversalOmni;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.distort.PointToPixelTransform_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.geometry.ConvertRotation3D_F32;
import georegression.geometry.GeometryMath_F32;
import georegression.geometry.UtilVector3D_F32;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Vector3D_F32;
import georegression.struct.so.Rodrigues_F32;
import org.ejml.data.FMatrixRMaj;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a synthetic pinhole camera for displaying images from the equirectangular image
 *
 * @author Peter Abeles
 */
// TODO Show which calibration file is being used
// TODO when a file is opened look for a calibration file
@SuppressWarnings({"NullAway.Init"})
public class FisheyePinholeApp<T extends ImageBase<T>> extends DemonstrationBase
		implements PinholeSimplifiedPanel.Listener {

	NarrowToWidePtoP_F32 distorter = new NarrowToWidePtoP_F32();
	ImageDistort<T, T> distortImage;

	// output image fort pinhole and equirectangular image
	BufferedImage buffPinhole = new BufferedImage(1, 1, BufferedImage.TYPE_INT_BGR);
	BufferedImage buffFisheye = new BufferedImage(1, 1, BufferedImage.TYPE_INT_BGR);

	// BoofCV work image for rendering
	T fisheye;
	T pinhole;

	// Camera parameters
	int camWidth = 400;
	int camHeight = 300;
	double hfov = 80; //  in degrees

	CameraPinhole cameraModel = new CameraPinhole();

	JSplitPane imageView;
	ImagePanel panelPinhole = new ImagePanel();
	OmniPanel panelFisheye = new OmniPanel();

	final private Object imageLock = new Object();

	LensDistortionWideFOV fisheyeDistort;

	public FisheyePinholeApp( List<?> exampleInputs, ImageType<T> imageType ) {
		super(exampleInputs, imageType);

		updateIntrinsic();

		setPreferredSize(new Dimension(800, 800));

		BorderType borderType = BorderType.EXTENDED;
		InterpolatePixel<T> interp =
				FactoryInterpolation.createPixel(0, 255, InterpolationType.BILINEAR, borderType, imageType);
		distortImage = FactoryDistort.distort(true, interp, imageType);
		distortImage.setRenderAll(true);

		fisheye = imageType.createImage(1, 1);
		pinhole = imageType.createImage(camWidth, camHeight);
		buffPinhole = new BufferedImage(camWidth, camHeight, BufferedImage.TYPE_INT_BGR);
		panelPinhole.setPreferredSize(new Dimension(camWidth, camHeight));
		panelPinhole.setImage(buffFisheye);
		final PinholeSimplifiedPanel controlPinhole = new PinholeSimplifiedPanel(camWidth, camHeight, hfov, this);

		MouseAdapter adapter = new MouseAdapter() {
			@Override
			public void mouseClicked( MouseEvent e ) {
				changeFocus(e);
			}

			@Override
			public void mouseDragged( MouseEvent e ) {
				changeFocus(e);
			}

			private void changeFocus( MouseEvent e ) {
				double scale = panelFisheye.scale;

				double omniX = e.getX()/scale;
				double omniY = e.getY()/scale;

				if (!fisheye.isInBounds((int)omniX, (int)omniY))
					return;
				panelPinhole.grabFocus();
				synchronized (imageLock) {
					Point3D_F32 norm = new Point3D_F32();
					fisheyeDistort.undistortPtoS_F32().compute((float)omniX, (float)omniY, norm);

					Rodrigues_F32 rotation = new Rodrigues_F32();

					Vector3D_F32 canonical = new Vector3D_F32(0, 0, 1);
					rotation.theta = UtilVector3D_F32.acute(new Vector3D_F32(norm), canonical);
					GeometryMath_F32.cross(canonical, norm, rotation.unitAxisRotation);
					rotation.unitAxisRotation.normalize();

					FMatrixRMaj R = ConvertRotation3D_F32.rodriguesToMatrix(rotation, null);
					distorter.setRotationWideToNarrow(R);

					distortImage.setModel(new PointToPixelTransform_F32(distorter));

					if (inputMethod == InputMethod.IMAGE) {
						rerenderPinhole();
					}
				}
			}

			@Override
			public void mouseWheelMoved( MouseWheelEvent e ) {
				int notches = e.getWheelRotation();
				controlPinhole.addToFOV(notches*2);
			}
		};

		panelFisheye.setScaling(ScaleOptions.DOWN);
		panelFisheye.addMouseListener(adapter);
		panelFisheye.addMouseMotionListener(adapter);
		panelFisheye.addMouseWheelListener(adapter);

		panelPinhole.setFocusable(true);
		panelPinhole.grabFocus();
		panelPinhole.setPreferredSize(new Dimension(camWidth, camHeight));

		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
		controlPanel.add(controlPinhole);

		imageView = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		imageView.setTopComponent(panelPinhole);
		imageView.setBottomComponent(panelFisheye);
		imageView.setDividerLocation(camHeight);

		add(controlPanel, BorderLayout.WEST);
		add(imageView, BorderLayout.CENTER);
	}

	@Override
	public void openFile( File file ) {
		File intrinsicFile = new File(file.getParent(), "front.yaml");
		CameraUniversalOmni fisheyeModel = CalibrationIO.load(intrinsicFile);
		fisheyeDistort = new LensDistortionUniversalOmni(fisheyeModel);
		distorter.configure(new LensDistortionPinhole(cameraModel), fisheyeDistort);

		super.openFile(file);
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase input ) {
		synchronized (imageLock) {
			// create a copy of the input image for output purposes
			if (buffFisheye.getWidth() != buffered.getWidth() || buffFisheye.getHeight() != buffered.getHeight()) {
				buffFisheye = new BufferedImage(buffered.getWidth(), buffered.getHeight(), BufferedImage.TYPE_INT_BGR);

				panelFisheye.setPreferredSize(new Dimension(buffered.getWidth(), buffered.getHeight()));
				panelFisheye.setImageUI(buffFisheye);

				distortImage.setModel(new PointToPixelTransform_F32(distorter));
			}
			buffFisheye.createGraphics().drawImage(buffered, 0, 0, null);
			fisheye.setTo((T)input);

			rerenderPinhole();
		}
	}

	private void rerenderPinhole() {
//		long before = System.nanoTime();
		distortImage.apply(fisheye, pinhole);
//		long after = System.nanoTime();
//		System.out.println("Rendering time "+(after-before)/1e6+" ms");

		ConvertBufferedImage.convertTo(pinhole, buffPinhole, true);
		panelPinhole.setImageUI(buffPinhole);
		panelFisheye.repaint();
	}

	private void updateIntrinsic() {
		cameraModel.width = camWidth;
		cameraModel.height = camHeight;
		cameraModel.cx = camWidth/2;
		cameraModel.cy = camHeight/2;

		double f = (camWidth/2.0)/Math.tan(UtilAngle.degreeToRadian(hfov)/2.0);

		cameraModel.fx = cameraModel.fy = f;
		cameraModel.skew = 0;
	}

	@Override
	public void updatedPinholeModel( int width, int height, double fov ) {
		final boolean shapeChanged = camWidth != width || camHeight != height;

		this.camWidth = width;
		this.camHeight = height;
		this.hfov = fov;

		synchronized (imageLock) {
			if (shapeChanged) {
				panelPinhole.setPreferredSize(new Dimension(camWidth, camHeight));
				pinhole.reshape(camWidth, camHeight);
				buffPinhole = new BufferedImage(camWidth, camHeight, BufferedImage.TYPE_INT_BGR);
			}
			updateIntrinsic();
			distorter.configure(new LensDistortionPinhole(cameraModel), fisheyeDistort);
			distortImage.setModel(new PointToPixelTransform_F32(distorter));

			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					if (shapeChanged) {
						panelPinhole.setPreferredSize(new Dimension(camWidth, camHeight));
						panelPinhole.setMinimumSize(new Dimension(camWidth, camHeight));
						panelPinhole.setMaximumSize(new Dimension(camWidth, camHeight));
						imageView.setDividerLocation(-1);
					}
				}
			});

			if (inputMethod == InputMethod.IMAGE) {
				rerenderPinhole();
			}
		}
	}

	/**
	 * Draws a circle around the current view's center
	 */
	private class OmniPanel extends ImagePanel {
		int pointsPerEdge = 8;
		Point2D_F32 corners[] = new Point2D_F32[pointsPerEdge*4];
		Point2D_F32 center = new Point2D_F32();

		BasicStroke stroke0 = new BasicStroke(3);
		BasicStroke stroke1 = new BasicStroke(6);
		Line2D.Double line0 = new Line2D.Double();
		Ellipse2D.Double circle = new Ellipse2D.Double();

		public OmniPanel() {
			for (int i = 0; i < corners.length; i++) {
				corners[i] = new Point2D_F32();
			}
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);
			Graphics2D g2 = BoofSwingUtil.antialiasing(g);

			synchronized (imageLock) {
				distorter.compute(camWidth/2, camHeight/2, center);
				renderLine(0, 0, camWidth, 0, pointsPerEdge, 0);
				renderLine(camWidth, 0, camWidth, camHeight, pointsPerEdge, pointsPerEdge);
				renderLine(camWidth, camHeight, 0, camHeight, pointsPerEdge, pointsPerEdge*2);
				renderLine(0, camHeight, 0, 0, pointsPerEdge, pointsPerEdge*3);
			}

			circle.setFrame(center.x*scale - 5, center.y*scale - 5, 10, 10);

			g2.setStroke(stroke1);
			g2.setColor(Color.BLACK);
			for (int i = 0, j = corners.length - 1; i < corners.length; j = i, i++) {
				line0.setLine(corners[j].x*scale, corners[j].y*scale, corners[i].x*scale, corners[i].y*scale);
				g2.draw(line0);
			}
			g2.draw(circle);

			g2.setStroke(stroke0);
			for (int i = 0; i < corners.length; i++) {
				int j = (i + 1)%corners.length;
				if (i < pointsPerEdge)
					g2.setColor(Color.BLUE);
				else if (i < pointsPerEdge*2)
					g2.setColor(Color.GREEN);
				else if (i < pointsPerEdge*3)
					g2.setColor(Color.cyan);
				else
					g2.setColor(Color.RED);
				line0.setLine(corners[j].x*scale, corners[j].y*scale, corners[i].x*scale, corners[i].y*scale);
				g2.draw(line0);
			}
			g2.setColor(Color.RED);
			g2.draw(circle);
		}

		private void renderLine( double x0, double y0, double x1, double y1, int segments, int offset ) {
			for (int i = 0; i < segments; i++) {
				double x = (x1 - x0)*i/segments + x0;
				double y = (y1 - y0)*i/segments + y0;

				distorter.compute((float)x, (float)y, corners[i + offset]);
			}
		}
	}

	public static void main( String[] args ) {

		ImageType type = ImageType.pl(3, GrayU8.class);

		List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("Dining Room", UtilIO.pathExample("fisheye/theta/front_table.jpg")));
		examples.add(new PathLabel("Hike", UtilIO.pathExample("fisheye/theta/front_hike.jpg")));

		FisheyePinholeApp app = new FisheyePinholeApp(examples, type);

		app.openFile(new File(examples.get(0).getPath()));
		app.display("Fisheye to Pinhole Camera");
	}
}
