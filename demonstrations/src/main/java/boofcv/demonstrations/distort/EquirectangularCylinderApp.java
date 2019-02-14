/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.distort.spherical.CylinderToEquirectangular_F32;
import boofcv.alg.distort.spherical.EquirectangularTools_F32;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.image.ImagePanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.border.BorderType;
import boofcv.struct.geo.GeoLL_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.geometry.ConvertRotation3D_F32;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F32;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates re-rendering an equirectangular image into a cylindrical image.
 *
 * @author Peter Abeles
 */
public class EquirectangularCylinderApp<T extends ImageBase<T>> extends DemonstrationBase
		implements RotationPanel.Listener, CylinderPanel.Listener
{
	final CylinderToEquirectangular_F32 distorter = new CylinderToEquirectangular_F32();
	ImageDistort<T,T> distortImage;

	BufferedImage rendered = new BufferedImage(1,1,BufferedImage.TYPE_INT_BGR);

	ImagePanel panelImage;
	CylinderPanel panelCylinder;

	RotationPanel panelRotate = new RotationPanel(0,0,0,this);

	T distorted;
	T inputCopy;



	public EquirectangularCylinderApp(List<?> exampleInputs, ImageType<T> imageType) {
		super(exampleInputs, imageType);


		BorderType borderType = BorderType.EXTENDED;
		InterpolatePixel<T> interp =
				FactoryInterpolation.createPixel(0, 255, InterpolationType.BILINEAR,borderType, imageType);
		distortImage = FactoryDistort.distort(true, interp, imageType);
		distortImage.setRenderAll(true);


		int imgWidth = 400;
		int imgHeight = 300;
		double vfov = UtilAngle.radian(120);
		distorter.configure(imgWidth, imgHeight, (float)vfov);

		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel,BoxLayout.Y_AXIS));
		panelCylinder = new CylinderPanel(imgWidth,imgHeight,UtilAngle.degree(vfov),this);
		controlPanel.add( panelCylinder );
		controlPanel.add( panelRotate );
		panelImage = new ImagePanel();
		add(panelImage, BorderLayout.CENTER);
		add(controlPanel, BorderLayout.WEST);


		rendered = new BufferedImage(imgWidth,imgHeight,BufferedImage.TYPE_INT_BGR);
		distorted = imageType.createImage(imgWidth, imgHeight);
		inputCopy = imageType.createImage(1,1);

		panelImage.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				GeoLL_F32 geo = new GeoLL_F32();

				synchronized (distorter) {
					EquirectangularTools_F32 tools = distorter.getTools();

					Point2D_F32 distorted = new Point2D_F32();
					double scale = panelImage.scale;
					distorter.compute((int) (e.getX() / scale), (int) (e.getY() / scale),distorted);
					tools.equiToLatLonFV(distorted.x, distorted.y, geo);
					panelRotate.setOrientation(UtilAngle.radianToDegree(geo.lat), UtilAngle.radianToDegree(geo.lon),0);
					distorter.setDirection(geo.lon, geo.lat, 0);
					distortImage.setModel(distorter); // let it know the transform has changed

					if (inputMethod == InputMethod.IMAGE) {
						renderOutput(inputCopy);
					}
				}
			}
		});
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		super.handleInputChange(source, method, width, height);

		if( inputCopy.getWidth() != width || inputCopy.getHeight() != height ) {
			panelImage.setPreferredSize(new Dimension(width,height));
			distortImage.setModel(distorter); // let it know the transform has changed
		}
		distorter.setEquirectangularShape(width, height);
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase input) {

		T in;
		if( inputMethod == InputMethod.IMAGE ) {
			inputCopy.setTo((T)input);
			in = inputCopy;
		} else {
			in = (T)input;
		}

		synchronized (distorter) {
			renderOutput(in);
		}
	}

	private void renderOutput(T in) {
		distortImage.apply(in,distorted);
		ConvertBufferedImage.convertTo(distorted,rendered,true);
		panelImage.setImageUI(rendered);
	}

	@Override
	public void updatedOrientation(double pitch, double yaw, double roll) {
		synchronized (distorter) {
			ConvertRotation3D_F32.eulerToMatrix(EulerType.ZYX,
					(float)UtilAngle.degreeToRadian(yaw),
					(float)UtilAngle.degreeToRadian(pitch),
					(float)UtilAngle.degreeToRadian(roll),
					distorter.getRotation());
			distortImage.setModel(distorter); // let it know the transform has changed

			if (inputMethod == InputMethod.IMAGE) {
				renderOutput(inputCopy);
			}
		}
	}

	@Override
	public void updateCylinder(int width, int height, double vfov) {
		synchronized (distorter) {
			distorter.configure(width,height,(float)UtilAngle.radian(vfov));
			distortImage.setModel(distorter); // let it know the transform has changed

			if( distorted.width != width || distorted.height != height ) {
				rendered = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
				distorted.reshape(width, height);
			}

			if (inputMethod == InputMethod.IMAGE) {
				renderOutput(inputCopy);
			}
		}
	}

	public static void main(String[] args) {

		ImageType type = ImageType.pl(3, GrayU8.class);

		List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("Half Dome 01", UtilIO.pathExample("spherical/equirectangular_half_dome_01.jpg")));
		examples.add(new PathLabel("Half Dome 02", UtilIO.pathExample("spherical/equirectangular_half_dome_02.jpg")));
		examples.add(new PathLabel("Glow Sticks", UtilIO.pathExample("spherical/equirectangular_glowsticks.jpg")));

		EquirectangularCylinderApp app = new EquirectangularCylinderApp(examples,type);

		app.openFile(new File(examples.get(0).getPath()));

		app.display("Equirectanglar to Cylindrical");
	}



}
