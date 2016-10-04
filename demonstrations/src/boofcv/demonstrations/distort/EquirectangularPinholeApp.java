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
import boofcv.alg.distort.spherical.EquirectangularToPinhole_F32;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
public class EquirectangularPinholeApp<T extends ImageBase<T>> extends DemonstrationBase<T>
	implements PinholePanel.Listener
{

	EquirectangularToPinhole_F32 distorter = new EquirectangularToPinhole_F32();
	ImageDistort<T,T> distortImage;

	// output image fort pinhole and equirectangular image
	BufferedImage buffPinhole = new BufferedImage(1,1,BufferedImage.TYPE_INT_BGR);
	BufferedImage buffEqui = new BufferedImage(1,1,BufferedImage.TYPE_INT_BGR);

	// BoofCV work image for rendering
	T equi;
	T pinhole;

	// Camera parameters
	int camWidth = 400;
	int camHeight = 300;
	double hfov = 80; //  in degrees

	CameraPinhole cameraModel = new CameraPinhole();

	ImagePanel panelPinhole = new ImagePanel();
	EquiViewPanel panelEqui = new EquiViewPanel();

	Object imageLock = new Object();

	public EquirectangularPinholeApp(List<?> exampleInputs, ImageType<T> imageType) {
		super(exampleInputs, imageType);

		updateIntrinsic();
		distorter.setPinhole(cameraModel);

		BorderType borderType = BorderType.EXTENDED;
		InterpolatePixel<T> interp =
				FactoryInterpolation.createPixel(0, 255, TypeInterpolate.BILINEAR,borderType, imageType);
		distortImage = FactoryDistort.distort(true, interp, imageType);
		distortImage.setRenderAll(true);

		equi = imageType.createImage(1,1);
		pinhole = imageType.createImage(camWidth,camHeight);
		buffPinhole = new BufferedImage(camWidth,camHeight,BufferedImage.TYPE_INT_BGR);
		panelPinhole.setPreferredSize( new Dimension(camWidth,camHeight));
		panelPinhole.setBufferedImage(buffEqui);
		panelPinhole.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				double pitch=0;
				double yaw=0;
				double roll=0;

				switch( e.getKeyCode() ) {
					case KeyEvent.VK_W: pitch += 0.01; break;
					case KeyEvent.VK_S: pitch -= 0.01; break;
					case KeyEvent.VK_A: yaw -= 0.01; break;
					case KeyEvent.VK_D: yaw += 0.01; break;
					case KeyEvent.VK_Q: roll -= 0.01; break;
					case KeyEvent.VK_E: roll += 0.01; break;
					default:
						return;
				}

				synchronized (imageLock) {
					DenseMatrix64F R = ConvertRotation3D_F64.eulerToMatrix(EulerType.YZX,yaw,roll,pitch,null);
					DenseMatrix64F tmp = distorter.getRotation().copy();
					CommonOps.mult(tmp,R,distorter.getRotation());
					distortImage.setModel(distorter); // dirty the transform
					if (inputMethod == InputMethod.IMAGE) {
						rerenderPinhole();
					}
				}
			}
		});

		panelEqui.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Point2D_F32 latlon = new Point2D_F32();

				double scale = panelEqui.scale;

				int x = (int)(e.getX()/scale);
				int y = (int)(e.getY()/scale);

				if( !equi.isInBounds(x,y))
					return;
				panelPinhole.grabFocus();
				synchronized (imageLock) {
					distorter.getTools().equiToLonlatFV(x,y,latlon);
					distorter.setDirection(latlon.x,latlon.y,0);

					// pinhole has a canonical view along +z
					// equirectangular lon-lat uses +x
					// this compensates for that
					// roll rotation is to make the view appear "up"
					DenseMatrix64F A = ConvertRotation3D_F64.eulerToMatrix(EulerType.YZX,Math.PI/2,0,Math.PI/2,null);
					DenseMatrix64F tmp = distorter.getRotation().copy();
					CommonOps.mult(tmp,A,distorter.getRotation());

					distortImage.setModel(distorter); // let it know the transform has changed

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
		add(panelEqui, BorderLayout.SOUTH);
	}

	@Override
	public void processImage(BufferedImage buffered, T input) {
		synchronized (imageLock) {
			// create a copy of the input image for output purposes
			if (buffEqui.getWidth() != buffered.getWidth() || buffEqui.getHeight() != buffered.getHeight()) {
				buffEqui = new BufferedImage(buffered.getWidth(), buffered.getHeight(), BufferedImage.TYPE_INT_BGR);
				panelEqui.setPreferredSize(new Dimension(buffered.getWidth(), buffered.getHeight()));
				panelEqui.setBufferedImageSafe(buffEqui);

				distorter.setEquirectangularShape(input.width, input.height);
				distortImage.setModel(distorter);
			}
			buffEqui.createGraphics().drawImage(buffered, 0, 0, null);
			equi.setTo(input);

			rerenderPinhole();
		}
	}

	private void rerenderPinhole() {
//		long before = System.nanoTime();
		distortImage.apply(equi,pinhole);
//		long after = System.nanoTime();

//		System.out.println("Rendering time "+(after-before)/1e6+" ms");

		ConvertBufferedImage.convertTo(pinhole,buffPinhole,true);
		panelPinhole.setBufferedImageSafe(buffPinhole);
		panelEqui.repaint();
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
			distorter.setPinhole(cameraModel);
			distortImage.setModel(distorter);

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
	private class EquiViewPanel extends ImagePanel {
		Vector3D_F64 v = new Vector3D_F64();
		Point2D_F32 p = new Point2D_F32();
		BasicStroke stroke0 = new BasicStroke(3);
		BasicStroke stroke1 = new BasicStroke(6);
		Ellipse2D.Double circle = new Ellipse2D.Double();

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			DenseMatrix64F R = distorter.getRotation();

			v.set(0,0,1); // canonical view is +z for pinhole cvamera
			GeometryMath_F64.mult(R,v,v);

			distorter.getTools().normToEquiFV((float)v.x,(float)v.y,(float)v.z,p);

			circle.setFrame(p.x*scale-10,p.y*scale-10,20,20);

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

		List<PathLabel> examples = new ArrayList<PathLabel>();
		examples.add(new PathLabel("Half Dome 01", UtilIO.pathExample("spherical/equirectangular_half_dome_01.jpg")));
		examples.add(new PathLabel("Half Dome 02", UtilIO.pathExample("spherical/equirectangular_half_dome_02.jpg")));
		examples.add(new PathLabel("Glow Sticks", UtilIO.pathExample("spherical/equirectangular_glowsticks.jpg")));

		EquirectangularPinholeApp app = new EquirectangularPinholeApp(examples,type);

		app.openFile(new File(examples.get(0).getPath()));

		app.waitUntilDoneProcessing();

		ShowImages.showWindow(app, "Equirectanglar to Pinhole Camera",true);

	}
}
