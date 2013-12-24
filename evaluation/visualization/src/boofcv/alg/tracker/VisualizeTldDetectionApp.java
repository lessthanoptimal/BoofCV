/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.associate.DescriptorDistance;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.tracker.tld.TldParameters;
import boofcv.alg.tracker.tld.TldRegion;
import boofcv.alg.tracker.tld.TldTemplateMatching;
import boofcv.alg.tracker.tld.TldTracker;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.ImageRectangle;
import boofcv.struct.feature.NccFeature;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class VisualizeTldDetectionApp<T extends ImageSingleBand,D extends ImageSingleBand>
		extends JPanel implements MouseListener {

	BufferedImage input;

	T gray;

	TldTracker<T,D> tracker;

	int numClicks = 0;
	ImageRectangle target = new ImageRectangle();

	private FastQueue<TldRegion> candidateDetections = new FastQueue<TldRegion>(TldRegion.class,true);


	public VisualizeTldDetectionApp( BufferedImage input , Class<T> imageType ) {
		super(new BorderLayout());
		this.input = input;
		gray = GeneralizedImageOps.createSingleBand(imageType,input.getWidth(),input.getHeight());
		ConvertBufferedImage.convertFrom(input,gray,true);

		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);

		InterpolatePixelS<T> interpolate = FactoryInterpolation.bilinearPixelS(imageType);
		ImageGradient<T,D> gradient =  FactoryDerivative.sobel(imageType, derivType);

		tracker = new TldTracker<T,D>(new TldParameters(),interpolate,gradient,imageType,derivType);
		tracker.setPerformLearning(false);


		addMouseListener(this);
		requestFocus();
		setPreferredSize(new Dimension(gray.width,gray.height));
		ShowImages.showWindow(this,"Visualize Detection");
	}

	@Override
	protected synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;

		g2.drawImage(input,0,0,null);

//		FastQueue<TldRegion> detected = tracker.getDetectedTargets();
		FastQueue<TldRegion> detected = tracker.getDetection().getCandidateDetections();

		drawDetections(g2, detected,0);
//		drawFerns(g2,0);

		if( tracker.getDetection().isAmbiguous())
			drawDetections(g2, tracker.getDetection().getLocalMaximums(),Color.RED);
		else {
			TldRegion r = tracker.getDetection().getBest();
			if( r != null )
				drawRectangle(g2,r.rect,Color.RED,3);
		}

		drawRectangle(g2,target,Color.GREEN,3);

		if( detected.size() != 0 ) {
//			drawRectangle(g2,target,Color.RED,3);
		}
	}

	private void drawDetections(Graphics2D g2, FastQueue<TldRegion> detected , int shift ) {
		double max = 0;
		double min = Double.MAX_VALUE;

		for( int i = 0; i < detected.size; i++ ) {
			TldRegion r = detected.get(i);

			if( r.confidence > max ) {
				max = r.confidence;
			}
			if( r.confidence < min ) {
				min = r.confidence;
			}
		}
		double range = max-min;

		for( TldRegion r : detected.toList() ) {
			int v = (int)(255*(r.confidence-min)/range);
			int rgb = v << shift;
			drawRectangle(g2,r.rect,new Color(rgb),3);
		}
	}

	private void drawFerns(Graphics2D g2 , int shift ) {
		double max = 0;
		double min = Double.MAX_VALUE;

		GrowQueue_F64 value = tracker.getDetection().getStorageMetric();
		java.util.List<ImageRectangle> rects = tracker.getDetection().getStorageRect();

		for( int i = 0; i < value.size; i++ ) {
			double r = -value.get(i);

			if( r > max ) {
				max = r;
			}
			if( r < min ) {
				min = r;
			}
		}
		double range = max-min;

		for( int i = 0; i < value.size; i++ ) {
			double r = value.get(i);
			ImageRectangle rect = rects.get(i);

			int v = (int)(255*(r-min)/range);
			int rgb = v << shift;
			drawRectangle(g2,rect,new Color(rgb),3);
		}
	}

	private void drawDetections(Graphics2D g2, FastQueue<TldRegion> detected , Color c ) {

		for( TldRegion r : detected.toList() ) {
			drawRectangle(g2,r.rect,c,3);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if( numClicks == 0 ) {
			numClicks++;
			target.x0 = e.getX();
			target.y0 = e.getY();
		} else {
			numClicks = 0;
			target.x1 = e.getX();
			target.y1 = e.getY();
			tracker.initialize(gray,target.x0,target.y0,target.x1,target.y1);
			tracker.track(gray);
			printDetectedConfidence();
			printDescriptions();
			repaint();
		}
	}

	private void printDetectedConfidence() {

		FastQueue<TldRegion> detected = tracker.getDetection().getLocalMaximums();

		System.out.println("Target: "+target);
		for( int i = 0; i < detected.size; i++ ) {
			TldRegion r = detected.get(i);
			System.out.println(r.rect+" confidence: "+r.confidence+"  connections "+r.connections);
		}
	}

	private void printDescriptions() {
		TldTemplateMatching<T> matching = tracker.getTemplateMatching();

		FastQueue<TldRegion> detected = tracker.getDetection().getLocalMaximums();

		NccFeature t = matching.createDescriptor();
		NccFeature f = matching.createDescriptor();

		matching.computeNccDescriptor(t, target.x0, target.y0, target.x1, target.y1);
		System.out.println("Target:");
		printDescription(t);

		for( int i = 0; i < detected.size; i++ ) {
			TldRegion r = detected.get(i);
			matching.computeNccDescriptor(f,r.rect.x0,r.rect.y0,r.rect.x1,r.rect.y1);
			System.out.println("Detected:");
			System.out.println("  "+r.rect);
			printDescription(f);
			System.out.println("  NCC score = "+ DescriptorDistance.ncc(t,f));
			System.out.println("  Confidence = "+ matching.computeConfidence(r.rect));
			System.out.println("  Distance = "+ matching.distance(f,matching.getTemplatePositive()));

		}
	}

	private void printDescription(NccFeature f) {
		System.out.println("  sigma "+f.sigma);
		for( int j = 0; j < f.value.length; j++ ) {
			System.out.printf("%6.1f ",f.value[j]);
		}
		System.out.println();
	}

	@Override

	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	private void drawRectangle(Graphics2D g2 , ImageRectangle r , Color c , int size) {
		g2.setColor(c);
		g2.setStroke(new BasicStroke(size));

		g2.drawLine(r.x0,r.y0,r.x1,r.y0);
		g2.drawLine(r.x1,r.y0,r.x1,r.y1);
		g2.drawLine(r.x1,r.y1,r.x0,r.y1);
		g2.drawLine(r.x0,r.y1,r.x0,r.y0);
	}

	public static void main(String[] args) {

		BufferedImage image = UtilImageIO.loadImage("/home/pja/projects/ValidationBoof/data/track_rect/TLD/01_david/00050.jpg");
		new VisualizeTldDetectionApp(image,ImageUInt8.class);

//		String fileName = "/home/pja/Downloads/multi_face_turning/motinas_multi_face_turning.avi";

//		SimpleImageSequence<ImageUInt8> sequence =
//				new XugglerSimplified<ImageUInt8>(fileName, ImageDataType.single(ImageUInt8.class));
//
//		sequence.hasNext();
//		sequence.next();
//		sequence.hasNext();
//		sequence.next();
//
//		new VisualizeTldDetectionApp((BufferedImage)sequence.getGuiImage(),ImageUInt8.class);
	}
}
