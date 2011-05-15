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

import gecv.abst.detect.corner.GeneralCornerDetector;
import gecv.abst.detect.corner.GeneralCornerIntensity;
import gecv.abst.detect.extract.CornerExtractor;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
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
	GeneralCornerDetector<InputImage,DerivativeImage> detector;
	PyramidKltTracker<InputImage,DerivativeImage> tracker;

	QueueCorner excludeList = new QueueCorner(10);

	ImagePyramid<InputImage> image;
	DerivativeImage[] derivX;
	DerivativeImage[] derivY;

	public GenericPkltFeatSelector( GeneralCornerDetector<InputImage,DerivativeImage> detector,
									PyramidKltTracker<InputImage, DerivativeImage> tracker) {
		this.detector = detector;
		this.tracker = tracker;


		if( detector.getRequiresHessian() )
			throw new IllegalArgumentException("Corner selectors which require the Hessian are not allowed");
	}

	public void setTracker(PyramidKltTracker<InputImage, DerivativeImage> tracker) {
		this.tracker = tracker;
	}

	@Override
	public void setInputs(ImagePyramid<InputImage> image,
					   DerivativeImage[] derivX, DerivativeImage[] derivY)
	{
		this.image = image;
		this.derivX = derivX;
		this.derivY = derivY;
	}

	@Override
	public void compute(List<PyramidKltFeature> active, List<PyramidKltFeature> availableData ) {

		// exclude active tracks
		excludeList.reset();
		for( int i = 0; i < active.size(); i++ ) {
			PyramidKltFeature f = active.get(i);
			excludeList.add((int)f.x,(int)f.y);
		}
		// find new tracks
		detector.setExcludedCorners(excludeList);
		detector.setBestNumber(availableData.size());
		detector.process(image.getLayer(0),derivX[0],derivY[0],null,null,null);

		// extract the features
		QueueCorner found = detector.getCorners();

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
