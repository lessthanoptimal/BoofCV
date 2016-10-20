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

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionWideFOV;
import boofcv.alg.distort.NarrowToWidePtoP_F32;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.distort.universal.LensDistortionUniversalOmni;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.UtilVector3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.so.Rodrigues_F64;
import org.ejml.data.DenseMatrix64F;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a synthetic pinhole camera for displaying images from the equirectangular image
 *
 * @author Peter Abeles
 */
public class FisheyePinholeApp<T extends ImageBase<T>> extends DemonstrationBase<T>
	implements PinholePanel.Listener
{

	NarrowToWidePtoP_F32 distorter = new NarrowToWidePtoP_F32();
	ImageDistort<T,T> distortImage;

	// output image fort pinhole and equirectangular image
	BufferedImage buffPinhole = new BufferedImage(1,1,BufferedImage.TYPE_INT_BGR);
	BufferedImage buffFisheye = new BufferedImage(1,1,BufferedImage.TYPE_INT_BGR);

	// BoofCV work image for rendering
	T fisheye;
	T pinhole;

	// Camera parameters
	int camWidth = 400;
	int camHeight = 300;
	double hfov = 80; //  in degrees

	CameraPinhole cameraModel = new CameraPinhole();

	ImagePanel panelPinhole = new ImagePanel();
	OmniPanel panelFisheye = new OmniPanel();

	final private Object imageLock = new Object();

	LensDistortionWideFOV fisheyeDistort;

	public FisheyePinholeApp(List<?> exampleInputs, ImageType<T> imageType) {
		super(exampleInputs, imageType);

		CameraUniversalOmni fisheyeModel = CalibrationIO.load("fisheye.yaml");
		fisheyeDistort = new LensDistortionUniversalOmni(fisheyeModel);

		updateIntrinsic();
		distorter.configure(new LensDistortionPinhole(cameraModel),fisheyeDistort);

		BorderType borderType = BorderType.EXTENDED;
		InterpolatePixel<T> interp =
				FactoryInterpolation.createPixel(0, 255, TypeInterpolate.BILINEAR,borderType, imageType);
		distortImage = FactoryDistort.distort(true, interp, imageType);
		distortImage.setRenderAll(true);

		fisheye = imageType.createImage(1,1);
		pinhole = imageType.createImage(camWidth,camHeight);
		buffPinhole = new BufferedImage(camWidth,camHeight,BufferedImage.TYPE_INT_BGR);
		panelPinhole.setPreferredSize( new Dimension(camWidth,camHeight));
		panelPinhole.setBufferedImage(buffFisheye);

		panelFisheye.setScaling(ScaleOptions.DOWN);
		panelFisheye.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {

				double scale = panelFisheye.scale;

				double omniX = e.getX()/scale;
				double omniY = e.getY()/scale;

				if( !fisheye.isInBounds((int)omniX,(int)omniY))
					return;
				panelPinhole.grabFocus();
				synchronized (imageLock) {
					Point3D_F64 norm = new Point3D_F64();
					fisheyeDistort.undistortPtoS_F64().compute(omniX, omniY, norm);

					Rodrigues_F64 rotation = new Rodrigues_F64();

					Vector3D_F64 canonical = new Vector3D_F64(0,0,1);
					rotation.theta = UtilVector3D_F64.acute(new Vector3D_F64(norm),canonical);
					GeometryMath_F64.cross(canonical,norm,rotation.unitAxisRotation);
					rotation.unitAxisRotation.normalize();

					DenseMatrix64F R = ConvertRotation3D_F64.rodriguesToMatrix(rotation,null);
					distorter.setRotationWideToNarrow(R);

					distortImage.setModel(new PointToPixelTransform_F32(distorter));

					if (inputMethod == InputMethod.IMAGE) {
						rerenderPinhole();
					}
				}
			}
		});


		panelPinhole.setFocusable(true);
		panelPinhole.grabFocus();

		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel,BoxLayout.Y_AXIS));
		PinholePanel controlPinhole = new PinholePanel(camWidth,camHeight,hfov,this);
		controlPanel.add( controlPinhole );

		add(controlPanel, BorderLayout.WEST );
		add(panelPinhole, BorderLayout.CENTER);
		add(panelFisheye, BorderLayout.SOUTH);
	}

	@Override
	public void processImage(BufferedImage buffered, T input) {
		synchronized (imageLock) {
			// create a copy of the input image for output purposes
			if (buffFisheye.getWidth() != buffered.getWidth() || buffFisheye.getHeight() != buffered.getHeight()) {
				buffFisheye = new BufferedImage(buffered.getWidth(), buffered.getHeight(), BufferedImage.TYPE_INT_BGR);
				panelFisheye.setPreferredSize(new Dimension(buffered.getWidth()/2, buffered.getHeight()/2));
				panelFisheye.setBufferedImageSafe(buffFisheye);

				distortImage.setModel(new PointToPixelTransform_F32(distorter));
			}
			buffFisheye.createGraphics().drawImage(buffered, 0, 0, null);
			fisheye.setTo(input);

			rerenderPinhole();
		}
	}

	private void rerenderPinhole() {
//		long before = System.nanoTime();
		distortImage.apply(fisheye,pinhole);
//		long after = System.nanoTime();
//		System.out.println("Rendering time "+(after-before)/1e6+" ms");

		ConvertBufferedImage.convertTo(pinhole,buffPinhole,true);
		panelPinhole.setBufferedImageSafe(buffPinhole);
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
	public void updatedPinholeModel(int width, int height, double fov) {
		final boolean shapeChanged = camWidth != width || camHeight != height;

		this.camWidth = width;
		this.camHeight = height;
		this.hfov = fov;

		synchronized (imageLock) {
			if( shapeChanged ) {
				panelPinhole.setPreferredSize(new Dimension(camWidth, camHeight));
				pinhole.reshape(camWidth,camHeight);
				buffPinhole = new BufferedImage(camWidth,camHeight,BufferedImage.TYPE_INT_BGR);
			}
			updateIntrinsic();
			distorter.configure(new LensDistortionPinhole(cameraModel),fisheyeDistort);
			distortImage.setModel(new PointToPixelTransform_F32(distorter));

			if( inputMethod == InputMethod.IMAGE ) {
				rerenderPinhole();
			}
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if( shapeChanged ) {
					panelPinhole.setPreferredSize(new Dimension(camWidth, camHeight));
				}
			}
		});
	}

	/**
	 * Draws a circle around the current view's center
	 */
	private class OmniPanel extends ImagePanel {
		Point2D_F32 pixelOmni = new Point2D_F32();
		BasicStroke stroke0 = new BasicStroke(3);
		BasicStroke stroke1 = new BasicStroke(6);
		Ellipse2D.Double circle = new Ellipse2D.Double();

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			synchronized (imageLock) {
				distorter.compute(camWidth/2, camHeight/2, pixelOmni);
			}

			circle.setFrame(pixelOmni.x*scale-10,pixelOmni.y*scale-10,20,20);

			g2.setStroke(stroke1);
			g2.setColor(Color.BLACK);
			g2.draw(circle);

			g2.setStroke(stroke0);
			g2.setColor(Color.RED);
			g2.draw(circle);
		}
	}

	public static void main(String[] args) {

		ImageType type = ImageType.pl(3, GrayU8.class);

		List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("Test", "/home/pja/projects/accel_robotics/camodocal/split/back/theta_back_00243.png"));

		FisheyePinholeApp app = new FisheyePinholeApp(examples,type);

		app.openFile(new File(examples.get(0).getPath()));

		app.waitUntilDoneProcessing();

		ShowImages.showWindow(app, "Fisheye to Pinhole Camera",true);

	}
}
