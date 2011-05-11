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

import gecv.abst.detect.corner.GeneralCornerIntensity;
import gecv.abst.detect.extract.CornerExtractor;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.ImagePyramid;
import pja.geometry.struct.point.Point2D_I16;

import java.util.List;

/**
 * @author Peter Abeles
 */
// todo comment and unit test
public class GenericPkltFeatSelector<InputImage extends ImageBase, DerivativeImage extends ImageBase> 
		implements PyramidKltFeatureSelector<InputImage,DerivativeImage>
{
	int numFeatures;
	QueueCorner found;
	GeneralCornerIntensity<InputImage,DerivativeImage> intensity;
	CornerExtractor extractor;
	PyramidKltTracker<InputImage,DerivativeImage> tracker;

	public GenericPkltFeatSelector(int numFeatures,
									 GeneralCornerIntensity<InputImage, DerivativeImage> intensity,
									 CornerExtractor extractor,
									 PyramidKltTracker<InputImage, DerivativeImage> tracker) {
		this.numFeatures = numFeatures;
		this.intensity = intensity;
		this.extractor = extractor;
		this.tracker = tracker;

		found = new QueueCorner(numFeatures);

		if( intensity.getRequiresGradient() )
			throw new IllegalArgumentException("Corner selectors which require the Hessian are not allowed");
	}

	@Override
	public void setInputs(ImagePyramid<InputImage> image, DerivativeImage[] derivX, DerivativeImage[] derivY) {
		intensity.process(image.getLayer(0),derivX[0],derivY[0],null,null,null);
	}

	@Override
	public void compute(List<PyramidKltFeature> active, List<PyramidKltFeature> availableData) {
		found.reset();
		// exclude active
		for( int i = 0; i < active.size(); i++ ) {
			PyramidKltFeature f = active.get(i);
			found.add((int)f.x,(int)f.y);
		}
		// extract the feaures
		extractor.process(intensity.getIntensity(),intensity.getCandidates(),numFeatures,found);

		for( int i = 0; i < found.size() && !availableData.isEmpty(); i++ ) {
			Point2D_I16 pt = found.get(i);

			PyramidKltFeature feat = availableData.remove( availableData.size()-1);
			feat.x = pt.x;
			feat.y = pt.y;

			tracker.setDescription(feat);
			active.add(feat);
		}
	}
}
