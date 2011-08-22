/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.tracker.pklt;

import gecv.abst.detect.extract.FeatureExtractor;
import gecv.abst.detect.extract.WrapperNonMax;
import gecv.abst.detect.intensity.GeneralFeatureIntensity;
import gecv.abst.detect.intensity.WrapperGradientCornerIntensity;
import gecv.abst.filter.derivative.ImageGradient;
import gecv.alg.feature.detect.extract.FastNonMaxExtractor;
import gecv.alg.feature.detect.interest.GeneralFeatureDetector;
import gecv.alg.interpolate.InterpolateRectangle;
import gecv.alg.tracker.klt.KltConfig;
import gecv.alg.transform.pyramid.PyramidOps;
import gecv.alg.transform.pyramid.PyramidUpdateIntegerDown;
import gecv.factory.feature.detect.intensity.FactoryPointIntensityAlg;
import gecv.factory.filter.derivative.FactoryDerivative;
import gecv.factory.filter.kernel.FactoryKernelGaussian;
import gecv.factory.interpolate.FactoryInterpolation;
import gecv.gui.image.DiscretePyramidPanel;
import gecv.gui.image.ImagePanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.ProcessImageSequence;
import gecv.io.image.SimpleImageSequence;
import gecv.io.wrapper.xuggler.XugglerSimplified;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.pyramid.ImagePyramid;
import gecv.struct.pyramid.PyramidDiscrete;
import gecv.struct.pyramid.PyramidUpdaterDiscrete;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Runs a KLT tracker through a video sequence
 *
 * @author Peter Abeles
 */
public class TrackVideoPyramidKLT<I extends ImageBase, D extends ImageBase>
		extends ProcessImageSequence<I> {

	private PkltManager<I, D> tracker;

	ImagePanel panel;
	DiscretePyramidPanel pyramidPanel;
	int totalRespawns;

	ImageGradient<I,D> gradient;

	PyramidUpdaterDiscrete<I> pyramidUpdater;
	PyramidDiscrete<I> basePyramid;
	ImagePyramid<D> derivX;
	ImagePyramid<D> derivY;


	@SuppressWarnings({"unchecked"})
	public TrackVideoPyramidKLT(SimpleImageSequence<I> sequence,
								PkltManager<I, D> tracker ,
								PyramidUpdaterDiscrete<I> pyramidUpdater ,
								ImageGradient<I,D> gradient ) {
		super(sequence);
		this.pyramidUpdater = pyramidUpdater;
		this.tracker = tracker;
		this.gradient = gradient;
		PkltManagerConfig<I, D> config = tracker.getConfig();

		// declare the image pyramid
		basePyramid = new PyramidDiscrete<I>(config.typeInput,true,config.pyramidScaling);
		derivX = new PyramidDiscrete<D>(config.typeDeriv,false,config.pyramidScaling);
		derivY = new PyramidDiscrete<D>(config.typeDeriv,false,config.pyramidScaling);
	}


	@Override
	public void processFrame(I image) {

		pyramidUpdater.update(image,basePyramid);
		PyramidOps.gradient(basePyramid, gradient, derivX,derivY);

		tracker.processFrame(basePyramid,derivX,derivY);
	}

	@Override
	public void updateGUI(BufferedImage guiImage, I origImage) {
		Graphics2D g2 = guiImage.createGraphics();
		
		drawFeatures(g2, tracker.getTracks(), Color.RED);
		drawFeatures(g2, tracker.getSpawned(), Color.BLUE);

		if (panel == null) {
			panel = ShowImages.showWindow(guiImage, "KLT Pyramidal Tracker");
			addComponent(panel);
		} else {
			panel.setBufferedImage(guiImage);
			panel.repaint();
		}

//		if( pyramidPanel == null ) {
//			pyramidPanel = new DiscretePyramidPanel(tracker.getPyramid());
//			ShowImages.showWindow(pyramidPanel,"Pyramid");
//			addComponent(pyramidPanel);
//		} else {
//			pyramidPanel.render();
//			pyramidPanel.repaint();
//		}

		if( tracker.getSpawned().size() != 0 )
			totalRespawns++;
		System.out.println(" total features: "+tracker.getTracks().size()+" totalRespawns "+totalRespawns);
	}

	private void drawFeatures(Graphics2D g2,
							  java.util.List<PyramidKltFeature> list,
							  Color color ) {
		int r = 3;
		int w = r*2+1;
		int ro = r+2;
		int wo = ro*2+1;

		for (int i = 0; i < list.size(); i++) {
			PyramidKltFeature pt = list.get(i);

			int x = (int)pt.x;
			int y = (int)pt.y;

			g2.setColor(Color.BLACK);
			g2.fillOval(x - ro, y - ro, wo, wo);
			g2.setColor(color);
			g2.fillOval(x - r, y - r, w, w);
		}
	}

	public static <I extends ImageBase, D extends ImageBase>
	void run( String fileName , Class<I> imageType , Class<D> derivType ) {

		SimpleImageSequence<I> sequence = new XugglerSimplified<I>(fileName, imageType);

		ImageBase<?> image = sequence.next();

		KltConfig configKLt = new KltConfig();
		configKLt.forbiddenBorder = 0;
		configKLt.maxPerPixelError = 25.0f;
		configKLt.maxIterations = 15;
		configKLt.minDeterminant = 0.001f;
		configKLt.minPositionDelta = 0.01f;

		PkltManagerConfig<I,D> config = new PkltManagerConfig<I,D>();
		config.config = configKLt;
		config.typeInput = imageType;
		config.typeDeriv = derivType;
		config.pyramidScaling = new int[]{1,2,4,8};
		config.imgWidth = image.width;
		config.imgHeight = image.height;
		config.minFeatures = 80;
		config.maxFeatures = 100;
		config.featureRadius = 3;

		int scalingTop = config.computeScalingTop();

		InterpolateRectangle<I> interp = FactoryInterpolation.bilinearRectangle(imageType);
		InterpolateRectangle<D> interpD = FactoryInterpolation.bilinearRectangle(derivType);

		GeneralFeatureIntensity<I,D> intensity =
				new WrapperGradientCornerIntensity<I,D>(
						FactoryPointIntensityAlg.createKlt(derivType, config.featureRadius));
		FeatureExtractor extractor = new WrapperNonMax(
				new FastNonMaxExtractor(config.featureRadius+2,
						config.featureRadius*scalingTop, configKLt.minDeterminant));
		GeneralFeatureDetector<I,D> detector =
				new GeneralFeatureDetector<I,D>(intensity,extractor,config.maxFeatures);

		GenericPkltFeatSelector<I, D> featureSelector =
				new GenericPkltFeatSelector<I,D>(detector,null);

		PyramidUpdateIntegerDown<I> pyrUpdater =
				new PyramidUpdateIntegerDown<I>(FactoryKernelGaussian.gaussian(Kernel1D_F32.class,-1,2),imageType);

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(imageType,derivType);

		PkltManager<I,D> manager =
				new PkltManager<I,D>(config,interp,interpD,featureSelector);

		TrackVideoPyramidKLT<I,D> alg = new TrackVideoPyramidKLT<I,D>(sequence,manager,
				pyrUpdater,gradient);

		alg.process();
	}

	public static void main( String args[] ) {
		String fileName = "/home/pja/kayaking.ogv";

		run(fileName,ImageFloat32.class,ImageFloat32.class);
//		run(fileName, ImageUInt8.class, ImageSInt16.class);
	}
}
