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
import gecv.io.image.ProcessImageSequence;
import gecv.io.image.SimpleImageSequence;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.ImagePyramid;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
// todo display feature numbers
// todo indicate how many new features have been detected
public abstract class TrackVideoPyramidKLT<InputImage extends ImageBase, DerivativeImage extends ImageBase>
		extends ProcessImageSequence<InputImage> {

	PyramidKltTracker<InputImage, DerivativeImage> tracker;

	PyramidUpdater<InputImage> pyramidUpdater;
	ImagePyramid<InputImage> pyramid;
	DerivativeImage derivX[];
	DerivativeImage derivY[];

	int maxFeatures = 200;
	int radius;
	List<PyramidKltFeature> active = new ArrayList<PyramidKltFeature>();
	List<PyramidKltFeature> unused = new ArrayList<PyramidKltFeature>();

	public TrackVideoPyramidKLT(SimpleImageSequence<InputImage> sequence,
								PyramidKltTracker<InputImage, DerivativeImage> tracker) {
		super(sequence);
		this.tracker = tracker;

	}

	@Override
	public void processFrame(InputImage image) {
		if (pyramid == null) {
			createPyramid(image.width, image.height, 1, 2, 2);
			int numLayers = pyramid.getNumLayers();
			for (int i = 0; i < maxFeatures; i++) {
				unused.add(new PyramidKltFeature(numLayers, radius));
			}
		}
		pyramidUpdater.setPyramid(pyramid);

		if (tracker.getRequiresDerivative()) {
			for (int i = 0; i < pyramid.getNumLayers(); i++) {
				InputImage img = pyramid.getLayer(i);
				computeDerivatives(img, derivX[i], derivY[i]);
			}
			tracker.setImage(pyramid, derivX, derivY);
		} else {
			tracker.setImage(pyramid, null, null);
		}
	}

	@Override
	public void updateGUI(BufferedImage guiImage, InputImage origImage) {
	}

	protected abstract void createPyramid(int width, int height, int... scales);

	protected abstract void computeDerivatives(InputImage image, DerivativeImage derivX, DerivativeImage derivY);
}
