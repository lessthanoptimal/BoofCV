/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.edge;

import boofcv.abst.feature.detect.edge.CannyEdgeContourDynamic;
import boofcv.abst.feature.detect.edge.DetectEdgeContour;
import boofcv.abst.filter.blur.BlurFilter;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.binary.SelectHistogramThresholdPanel;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.edge.FactoryDetectEdgeContour;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

/**
 * Displays contours selected using different algorithms.
 *
 * @author Peter Abeles
 */
public class ShowEdgeContourApp<T extends ImageBase, D extends ImageBase>
		extends SelectAlgorithmImagePanel implements ProcessInput , CannyControlBar.Listener , SelectHistogramThresholdPanel.Listener
{
	// shows panel for displaying input image
	ImagePanel panel = new ImagePanel();

	BufferedImage input;
	T workImage;
	Class<T> imageType;
	Class<D> derivType;
	boolean processedImage = false;

	CannyControlBar barCanny;
	SelectHistogramThresholdPanel barBinary;
	JPanel bodyPanel = new JPanel();
	int activeAlg;

	public ShowEdgeContourApp(Class<T> imageType, Class<D> derivType) {
		super(1);
		this.imageType = imageType;
		this.derivType = derivType;

		addAlgorithm(0, "Canny", 0);
		addAlgorithm(0, "Binary Simple", 1);

		barCanny = new CannyControlBar(1,5);
		barCanny.setListener(this);

		barBinary = new SelectHistogramThresholdPanel(50,true);
		barBinary.setListener(this);

		bodyPanel.setLayout(new BorderLayout());
		bodyPanel.add(panel,BorderLayout.CENTER);
		bodyPanel.add(barCanny,BorderLayout.NORTH);

		setMainGUI(bodyPanel);
	}

	public void process( BufferedImage input ) {
		setInputImage(input);
		this.input = input;
		workImage = ConvertBufferedImage.convertFrom(input, null, imageType);

		// update the binary histogram threshold for this image
		final double threshold = GPixelMath.sum(workImage)/(workImage.width*workImage.height);


		final int width = input.getWidth();
		final int height = input.getHeight();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				barBinary.setThreshold((int)threshold);
				barBinary.getHistogramPanel().update(workImage);
				panel.setPreferredSize(new Dimension(width,height));
				processedImage = true;
				doRefreshAll();
			}});
	}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {

		activeAlg = ((Integer)cookie).intValue();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// swap control bar on top
				if( activeAlg == 0 ){
					bodyPanel.remove(barBinary);
					bodyPanel.add(barCanny,BorderLayout.NORTH);
					barCanny.repaint();
				} else {
					bodyPanel.remove(barCanny);
					bodyPanel.add(barBinary,BorderLayout.NORTH);
					barBinary.repaint();
				}
				validate();
			}});

		doProcess();
	}

	private void doProcess() {
		if( input == null )
			return;

		DetectEdgeContour<T> contour = declareAlgorithm();

		contour.process(workImage);
		List<List<Point2D_I32>> found = contour.getContours();

		final BufferedImage temp = new BufferedImage(input.getWidth(),input.getHeight(),input.getType());

		Random rand = new Random(234);
		for( List<Point2D_I32> l : found ) {
			int c = rand.nextInt(0xFFFFFF);
			for( Point2D_I32 p : l ) {
				temp.setRGB(p.x,p.y,c);
			}
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panel.setBufferedImage(temp);
				panel.repaint();
			}});
	}

	private DetectEdgeContour<T> declareAlgorithm(){
		switch( activeAlg ) {
			case 0:
				return createCanny();

			case 1:
				return FactoryDetectEdgeContour.binarySimple(barBinary.getThreshold(),barBinary.isDown());
		}

		throw new RuntimeException("Unknown algorithm");
	}

	@Override
	public void changeImage(String name, int index) {
		ImageListManager manager = getInputManager();

		BufferedImage image = manager.loadImage(index);
		if( image != null ) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	private DetectEdgeContour<T> createCanny() {
		double thresh = barCanny.getThreshold()/100.0;
		BlurFilter<T> blur = FactoryBlurFilter.gaussian(imageType, -1, barCanny.getBlurRadius());
		ImageGradient<T,D> gradient = FactoryDerivative.sobel(imageType, derivType);

		return new CannyEdgeContourDynamic<T,D>(blur,gradient,(float)thresh*0.1f,(float)thresh);
	}

	@Override
	public void changeCanny() {
		doProcess();
	}

	@Override
	public void histogramThresholdChange() {
		doProcess();
	}

	public static void main( String args[] ) {
		ShowEdgeContourApp<ImageFloat32,ImageFloat32> app =
				new ShowEdgeContourApp<ImageFloat32, ImageFloat32>(ImageFloat32.class, ImageFloat32.class);
//		ShowFeatureOrientationApp<ImageUInt8, ImageSInt16> app =
//				new ShowFeatureOrientationApp<ImageUInt8,ImageSInt16>(input,ImageUInt8.class, ImageSInt16.class);

		ImageListManager manager = new ImageListManager();
		manager.add("shapes","data/shapes01.png");
		manager.add("Room","data/indoors01.jpg");
		manager.add("Particles","data/particles01.jpg");
		manager.add("sunflowers","data/sunflowers.png");
		manager.add("beach","data/scale/beach02.jpg");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Contours");

		System.out.println("Done");
	}
}
