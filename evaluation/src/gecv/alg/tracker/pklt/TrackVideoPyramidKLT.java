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

import gecv.alg.pyramid.PyramidUpdater;
import gecv.alg.tracker.klt.KltTrackFault;
import gecv.gui.image.ImagePanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.ProcessImageSequence;
import gecv.io.image.SimpleImageSequence;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.ImagePyramid;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */

public abstract class TrackVideoPyramidKLT<InputImage extends ImageBase, DerivativeImage extends ImageBase>
		extends ProcessImageSequence<InputImage> {

	private PyramidKltTracker<InputImage, DerivativeImage> tracker;
	private PyramidKltFeatureSelector<InputImage, DerivativeImage> featureSelector;

	private PyramidUpdater<InputImage> pyramidUpdater;
	private ImagePyramid<InputImage> pyramid;
	protected DerivativeImage derivX[];
	protected DerivativeImage derivY[];

	private int maxFeatures = 200;
	private int minFeatures = 100;
	private int radius=2;
	private List<PyramidKltFeature> active = new ArrayList<PyramidKltFeature>();
	private List<PyramidKltFeature> unused = new ArrayList<PyramidKltFeature>();


	ImagePanel panel;

	public TrackVideoPyramidKLT(SimpleImageSequence<InputImage> sequence,
								PyramidKltTracker<InputImage, DerivativeImage> tracker,
								PyramidKltFeatureSelector<InputImage, DerivativeImage> featureSelector,
								PyramidUpdater<InputImage> pyramidUpdater ) {
		super(sequence);
		this.tracker = tracker;
		this.featureSelector = featureSelector;
		this.pyramidUpdater = pyramidUpdater;
	}

	@Override
	public void processFrame(InputImage image) {
		if (pyramid == null) {
			pyramid = createPyramid(image.width, image.height, 1, 2);
			int numLayers = pyramid.getNumLayers();
			for (int i = 0; i < maxFeatures; i++) {
				unused.add(new PyramidKltFeature(numLayers, radius));
			}
			pyramidUpdater.setPyramid(pyramid);
			tracker.setImage(pyramid, derivX, derivY);
		}

		pyramidUpdater.update(image);
		if (tracker.getRequiresDerivative() || active.size() < minFeatures ) {
			for (int i = 0; i < pyramid.getNumLayers(); i++) {
				InputImage img = pyramid.getLayer(i);
				computeDerivatives(img, derivX[i], derivY[i]);
			}
		}

		if( active.size() < minFeatures ) {
			System.out.println("   Selecting New Features");
			featureSelector.setInputs(pyramid,derivX,derivY);
			featureSelector.compute(active,unused);
		} else {
			tracker.setImage(pyramid,derivX,derivY);
			for( int i = 0; i < active.size(); ) {
				PyramidKltFeature f = active.get(i);
				KltTrackFault result = tracker.track(f);
				if( result != KltTrackFault.SUCCESS ) {
					unused.add(f);
					active.remove(i);
				} else {
					i++;
				}
			}
		}
		System.out.println(" total features: "+active.size());
	}

	@Override
	public void updateGUI(BufferedImage guiImage, InputImage origImage) {
		Graphics2D g2 = guiImage.createGraphics();

		for (int i = 0; i < active.size(); i++) {
			PyramidKltFeature pt = active.get(i);

			int x = (int)pt.x;
			int y = (int)pt.y;

			g2.setColor(Color.BLACK);
			g2.fillOval(x - 4, y - 4, 9, 9);
			g2.setColor(Color.RED);
			g2.fillOval(x - 2, y - 2, 5, 5);
		}

		if (panel == null) {
			panel = ShowImages.showWindow(guiImage, "KLT Pyramidal Tracker");
			addComponent(panel);
		} else {
			panel.setBufferedImage(guiImage);
			panel.repaint();
		}
	}

	protected abstract ImagePyramid<InputImage> createPyramid(int width, int height, int... scales);

	protected abstract void computeDerivatives(InputImage input, DerivativeImage derivX, DerivativeImage derivY);
}
