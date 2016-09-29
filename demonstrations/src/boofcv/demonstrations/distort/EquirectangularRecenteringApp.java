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
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.distort.spherical.EquirectangularRefocus_F32;
import boofcv.alg.distort.spherical.EquirectangularTools_F32;
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
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F32;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates recentering of equirectangular images
 *
 * @author Peter Abeles
 */
public class EquirectangularRecenteringApp<T extends ImageBase> extends DemonstrationBase<T> {


	EquirectangularRefocus_F32 distorter = new EquirectangularRefocus_F32();
	ImageDistort<T,T> distortImage;

	BufferedImage rendered = new BufferedImage(1,1,BufferedImage.TYPE_INT_BGR);

	float centerLat;
	float centerLon;

	// todo add controls which allow lat and lon to be adjusted
	ImagePanel panelImage;

	T distorted;
	T inputCopy;

	public EquirectangularRecenteringApp(List<?> exampleInputs, ImageType<T> imageType) {
		super(exampleInputs, imageType);

		panelImage = new ImagePanel();
		add(panelImage, BorderLayout.CENTER);

		BorderType borderType = BorderType.EXTENDED;
		InterpolatePixel<T> interp =
				FactoryInterpolation.createPixel(0, 255, TypeInterpolate.BILINEAR,borderType, imageType);
		distortImage = FactoryDistort.distort(true, interp, imageType);
		distortImage.setModel( new PointToPixelTransform_F32(distorter));
		distortImage.setRenderAll(true);

		distorted = imageType.createImage(1,1);
		inputCopy = imageType.createImage(1,1);

		panelImage.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Point2D_F32 latlon = new Point2D_F32();
				Point2D_F32 r = new Point2D_F32();

				EquirectangularTools_F32 tools = distorter.getTools();

				distorter.compute(e.getX(),e.getY(),r);
				tools.equiToLonlat(r.x,r.y,latlon);

				System.out.println();
				System.out.println("Old center  lon "+distorter.getLongitudeCenter());
				System.out.println("Recentering lon "+latlon.getX()+"  lat "+latlon.getY());
				distorter.setCenter(latlon.x,latlon.y);

				if( inputMethod == InputMethod.IMAGE ) {
					renderOutput(inputCopy);
				}
			}
		});
	}

	@Override
	protected void handleInputChange(InputMethod method, int width, int height) {
		super.handleInputChange(method, width, height);

		if( rendered.getWidth() != width || rendered.getHeight() != height ) {
			rendered = new BufferedImage(width,height,BufferedImage.TYPE_INT_BGR);
			panelImage.setPreferredSize(new Dimension(width,height));
			distorter.setImageShape(width, height);
		}

		centerLon = centerLat = 0;
		distorted.reshape(width,height);
		distorter.setImageShape(width,height);
	}

	@Override
	public void processImage(BufferedImage buffered, T input) {

		T in;
		if( inputMethod == InputMethod.IMAGE ) {
			inputCopy.setTo(input);
			in = inputCopy;
		} else {
			in = input;
		}

		renderOutput(in);
	}

	private void renderOutput(T in) {
		distortImage.apply(in,distorted);
		ConvertBufferedImage.convertTo(distorted,rendered,true);
		panelImage.setBufferedImageSafe(rendered);
		panelImage.repaint();
	}

	public static void main(String[] args) {

		ImageType type = ImageType.pl(3, GrayU8.class);

		List<PathLabel> examples = new ArrayList<PathLabel>();
		examples.add(new PathLabel("Half Dome", UtilIO.pathExample("spherical/equirectangular_half_dome.jpg")));

		EquirectangularRecenteringApp app = new EquirectangularRecenteringApp(examples,type);

		app.openFile(new File(examples.get(0).getPath()));

		app.waitUntilDoneProcessing();

		ShowImages.showWindow(app, "Tracking Rectangle",true);

	}
}
