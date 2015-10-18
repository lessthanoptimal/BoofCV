/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.interpolate;

import boofcv.abst.distort.FDistort;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.border.BorderType;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;

/**
 * Compares different types of interpolation algorithms by enlarging an image.
 *
 * @author Peter Abeles
 */
public class EvaluateInterpolateEnlargeApp<T extends ImageBase>
	extends SelectAlgorithmAndInputPanel implements ComponentListener
{
	ImageType<T> imageType;
	T color;
	T scaledImage;

	ImagePanel panel = new ImagePanel();
	boolean hasProcessed = false;

	public EvaluateInterpolateEnlargeApp( ImageType<T> imageType ) {
		super(1);
		this.imageType = imageType;

		panel.setScaling(ScaleOptions.NONE);
		setMainGUI(panel);

		color = imageType.createImage(1,1);
		scaledImage = imageType.createImage(1,1);

		addAlgorithm(0, "Nearest Neighbor", TypeInterpolate.NEAREST_NEIGHBOR);
		addAlgorithm(0, "Bilinear",TypeInterpolate.BILINEAR);
		addAlgorithm(0, "Bicubic Kernel",TypeInterpolate.BICUBIC);
		addAlgorithm(0, "Polynomial 4",TypeInterpolate.POLYNOMIAL4);

		setPreferredSize(new Dimension(300,300));
		addComponentListener(this);
	}

	public void process(BufferedImage image) {
		setInputImage(image);

		color.reshape(image.getWidth(),image.getHeight());
		ConvertBufferedImage.convertFrom(image, color, true);

		hasProcessed = true;
		doRefreshAll();
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}
	
	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		if( color == null || 0 == panel.getWidth() || 0 == panel.getHeight() ) {
			return;
		}

		TypeInterpolate typeInterpolate = (TypeInterpolate)cookie;

		scaledImage.reshape(panel.getWidth(),panel.getHeight());
		new FDistort(color,scaledImage).interp(typeInterpolate).border(BorderType.EXTENDED).scale().apply();

		BufferedImage out = ConvertBufferedImage.convertTo(scaledImage,null,true);
		panel.setBufferedImage(out);
		panel.repaint();
	}

	@Override
	public void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		if( image != null ) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return hasProcessed;
	}

	@Override
	public void componentResized(ComponentEvent e) {
		setActiveAlgorithm(0,null, getAlgorithmCookie(0));
	}

	@Override
	public void componentMoved(ComponentEvent e) {}

	@Override
	public void componentShown(ComponentEvent e) {}

	@Override
	public void componentHidden(ComponentEvent e) {}

	public static void main( String args[] ) {

		ImageType type = ImageType.ms(3,ImageFloat32.class);
//		ImageType type = ImageType.ms(3,ImageUInt8.class);
//		ImageType type = ImageType.single(ImageUInt8.class);
//		ImageType type = ImageType.il(3, InterleavedF32.class);

		EvaluateInterpolateEnlargeApp app = new EvaluateInterpolateEnlargeApp(type);

		app.setPreferredSize(new Dimension(500,500));

		app.setBaseDirectory(UtilIO.pathExample(""));
		app.loadInputData(UtilIO.pathExample("interpolation.txt"));

//		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
//		inputs.add(new PathLabel("eye 1",UtilIO.pathExample("eye01.jpg"));
//		inputs.add(new PathLabel("eye 2",UtilIO.pathExample("eye02.jpg"));
//
//		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Interpolation Enlarge",true);
	}
}
