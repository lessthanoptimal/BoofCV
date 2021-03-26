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
import boofcv.alg.distort.spherical.CameraToEquirectangular_F32;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.image.ImagePanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.GeoLL_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.geometry.ConvertRotation3D_F32;
import georegression.geometry.GeometryMath_F32;
import georegression.metric.UtilAngle;
import georegression.misc.GrlConstants;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Vector3D_F32;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_FDRM;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
public class EquirectangularPinholeApp<T extends ImageBase<T>> extends DemonstrationBase
	implements PinholeSimplifiedPanel.Listener
{

	CameraToEquirectangular_F32 distorter = new CameraToEquirectangular_F32();
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

	final Object imageLock = new Object();

	public EquirectangularPinholeApp(List<?> exampleInputs, ImageType<T> imageType) {
		super(exampleInputs, imageType);

		updateIntrinsic();
		distorter.setCameraModel(cameraModel);

		BorderType borderType = BorderType.EXTENDED;
		InterpolatePixel<T> interp =
				FactoryInterpolation.createPixel(0, 255, InterpolationType.BILINEAR,borderType, imageType);
		distortImage = FactoryDistort.distort(true, interp, imageType);
		distortImage.setRenderAll(true);

		equi = imageType.createImage(1,1);
		pinhole = imageType.createImage(camWidth,camHeight);
		buffPinhole = new BufferedImage(camWidth,camHeight,BufferedImage.TYPE_INT_BGR);
		panelPinhole.setPreferredSize( new Dimension(camWidth,camHeight));
		panelPinhole.setImage(buffEqui);
		panelPinhole.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				float pitch=0;
				float yaw=0;
				float roll=0;

				switch( e.getKeyCode() ) {
					case KeyEvent.VK_W: pitch += 0.01f; break;
					case KeyEvent.VK_S: pitch -= 0.01f; break;
					case KeyEvent.VK_A: yaw -= 0.01f; break;
					case KeyEvent.VK_D: yaw += 0.01f; break;
					case KeyEvent.VK_Q: roll -= 0.01f; break;
					case KeyEvent.VK_E: roll += 0.01f; break;
					default:
						return;
				}

				synchronized (imageLock) {
					FMatrixRMaj R = ConvertRotation3D_F32.eulerToMatrix(EulerType.YZX,yaw,roll,pitch,null);
					FMatrixRMaj tmp = distorter.getRotation().copy();
					CommonOps_FDRM.mult(tmp,R,distorter.getRotation());
					distortImage.setModel(distorter); // dirty the transform
					if (inputMethod == InputMethod.IMAGE) {
						rerenderPinhole();
					}
				}
			}
		});

		MouseAdapter mouseAdapter = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				GeoLL_F32 geo = new GeoLL_F32();

				double scale = panelEqui.scale;

				int x = (int)(e.getX()/scale);
				int y = (int)(e.getY()/scale);

				if( !equi.isInBounds(x,y))
					return;
				panelPinhole.grabFocus();
				synchronized (imageLock) {
					distorter.getTools().equiToLatLonFV(x,y,geo);
					distorter.setDirection(geo.lon,geo.lat,0);

					// pinhole has a canonical view along +z
					// equirectangular lon-lat uses +x
					// this compensates for that
					// roll rotation is to make the view appear "up"
					FMatrixRMaj A = ConvertRotation3D_F32.eulerToMatrix(EulerType.YZX, GrlConstants.F_PI/2,0,GrlConstants.F_PI/2,null);
					FMatrixRMaj tmp = distorter.getRotation().copy();
					CommonOps_FDRM.mult(tmp,A,distorter.getRotation());

					distortImage.setModel(distorter); // let it know the transform has changed

					if (inputMethod == InputMethod.IMAGE) {
						rerenderPinhole();
					}
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				mouseClicked(e);
			}
		};

		panelEqui.addMouseListener(mouseAdapter);
		panelEqui.addMouseMotionListener(mouseAdapter);

		panelPinhole.setFocusable(true);
		panelPinhole.grabFocus();

		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel,BoxLayout.Y_AXIS));
		PinholeSimplifiedPanel controlPinhole = new PinholeSimplifiedPanel(camWidth,camHeight,hfov,this);
		controlPanel.add( controlPinhole );

		add(controlPanel, BorderLayout.WEST );
		add(panelPinhole, BorderLayout.CENTER);
		add(panelEqui, BorderLayout.SOUTH);
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase input) {
		synchronized (imageLock) {
			// create a copy of the input image for output purposes
			if (buffEqui.getWidth() != buffered.getWidth() || buffEqui.getHeight() != buffered.getHeight()) {
				buffEqui = new BufferedImage(buffered.getWidth(), buffered.getHeight(), BufferedImage.TYPE_INT_BGR);
				panelEqui.setPreferredSize(new Dimension(buffered.getWidth(), buffered.getHeight()));
				panelEqui.setImageUI(buffEqui);

				distorter.setEquirectangularShape(input.width, input.height);
				distortImage.setModel(distorter);
			}
			buffEqui.createGraphics().drawImage(buffered, 0, 0, null);
			equi.setTo((T)input);

			rerenderPinhole();
		}
	}

	private void rerenderPinhole() {
//		long before = System.nanoTime();
		distortImage.apply(equi,pinhole);
//		long after = System.nanoTime();

//		System.out.println("Rendering time "+(after-before)/1e6+" ms");

		ConvertBufferedImage.convertTo(pinhole,buffPinhole,true);
		panelPinhole.setImageUI(buffPinhole);
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
			distorter.setCameraModel(cameraModel);
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
		Vector3D_F32 v = new Vector3D_F32();
		Point2D_F32 p = new Point2D_F32();
		BasicStroke stroke0 = new BasicStroke(3);
		BasicStroke stroke1 = new BasicStroke(6);
		Ellipse2D.Double circle = new Ellipse2D.Double();

		int pointsPerEdge = 12;
		Point2D_F32 corners[] = new Point2D_F32[pointsPerEdge*4];
		Line2D.Double line0 = new Line2D.Double();
		Point2D_F32 distored = new Point2D_F32();

		public EquiViewPanel() {
			for (int i = 0; i < corners.length; i++) {
				corners[i] = new Point2D_F32();
			}
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = BoofSwingUtil.antialiasing(g);

			// center the center circle
			FMatrixRMaj R = distorter.getRotation();

			v.setTo(0,0,1); // canonical view is +z for pinhole camera
			GeometryMath_F32.mult(R,v,v);

			distorter.getTools().normToEquiFV((float)v.x,(float)v.y,(float)v.z,p);
			circle.setFrame(p.x*scale-10,p.y*scale-10,20,20);

			g2.setStroke(stroke1);
			g2.setColor(Color.BLACK);
			g2.draw(circle);

			g2.setStroke(stroke0);
			g2.setColor(Color.RED);
			g2.draw(circle);

			// render the pinhole's frame in equirectangular image
			renderLine(0         ,0          ,camWidth-1,0          ,pointsPerEdge,0);
			renderLine(camWidth-1,0          ,camWidth-1,camHeight-1,pointsPerEdge,pointsPerEdge);
			renderLine(camWidth-1,camHeight-1,0         ,camHeight-1,pointsPerEdge,pointsPerEdge*2);
			renderLine(0         ,camHeight-1,0         ,0          ,pointsPerEdge,pointsPerEdge*3);

			g2.setStroke(stroke0);
			for (int i = 0; i < corners.length; i++) {
				int j = (i+1)%corners.length;
				if( i < pointsPerEdge)
					g2.setColor(Color.BLUE);
				else if( i < pointsPerEdge*2 )
					g2.setColor(Color.GREEN);
				else if( i < pointsPerEdge*3 )
					g2.setColor(Color.cyan);
				else
					g2.setColor(Color.RED);
				// handle screen wrapping by skipping lines while go from one end to the other
				if( corners[j].distance(corners[i]) < equi.width/2) {
					line0.setLine(corners[j].x * scale, corners[j].y * scale, corners[i].x * scale, corners[i].y * scale);
					g2.draw(line0);
				}
			}
		}

		private void renderLine( int x0 , int y0 , int x1 , int y1 , int segments , int offset ) {
			for (int i = 0; i < segments; i++) {
				int x = (x1-x0)*i/segments + x0;
				int y = (y1-y0)*i/segments + y0;

				distorter.compute(x,y,distored);
				corners[i+offset].setTo(distored.x,distored.y);
			}
		}
	}

	public static void main(String[] args) {

		ImageType type = ImageType.pl(3, GrayU8.class);

		List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("Half Dome 01", UtilIO.pathExample("spherical/equirectangular_half_dome_01.jpg")));
		examples.add(new PathLabel("Half Dome 02", UtilIO.pathExample("spherical/equirectangular_half_dome_02.jpg")));
		examples.add(new PathLabel("Glow Sticks", UtilIO.pathExample("spherical/equirectangular_glowsticks.jpg")));

		EquirectangularPinholeApp app = new EquirectangularPinholeApp(examples,type);

		app.openFile(new File(examples.get(0).getPath()));
		app.display("Equirectanglar to Pinhole Camera");
	}
}
