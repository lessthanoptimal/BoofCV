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

import gecv.alg.feature.detect.interest.GeneralFeatureDetector;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.ImagePyramid;
import jgrl.struct.point.Point2D_I16;

import java.util.List;

/**
 * Automatically selects features for the {@link PyramidKltTracker} using a {@link gecv.alg.feature.detect.interest.GeneralFeatureDetector}.
 *
 * @author Peter Abeles
 */
public class GenericPkltFeatSelector<InputImage extends ImageBase, DerivativeImage extends ImageBase>
		implements PyramidKltFeatureSelector<InputImage,DerivativeImage>
{
	// selects corners
	GeneralFeatureDetector<InputImage,DerivativeImage> detector;
	// used to set feature decription
	PyramidKltTracker<InputImage,DerivativeImage> tracker;

	// list of corners which should be ignored by the corner detector
	QueueCorner excludeList = new QueueCorner(10);

	// input images
	ImagePyramid<InputImage> image;
	ImagePyramid<DerivativeImage> derivX;
	ImagePyramid<DerivativeImage> derivY;

	public GenericPkltFeatSelector( GeneralFeatureDetector<InputImage,DerivativeImage> detector,
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
	public void setInputs( ImagePyramid<InputImage> image,
						ImagePyramid<DerivativeImage> derivX,
						ImagePyramid<DerivativeImage> derivY)
	{
		this.image = image;
		this.derivX = derivX;
		this.derivY = derivY;
	}

	@Override
	public void compute(List<PyramidKltFeature> active, List<PyramidKltFeature> availableData ) {

		float scaleBottom = (float)image.getScale(0);

		// exclude active tracks
		excludeList.reset();
		for( int i = 0; i < active.size(); i++ ) {
			PyramidKltFeature f = active.get(i);
			excludeList.add((int)(f.x/scaleBottom),(int)(f.y/scaleBottom));
		}
		
		// find new tracks
		detector.setExcludedCorners(excludeList);
		detector.setBestNumber(availableData.size());
		detector.process(image.getLayer(0),derivX.getLayer(0),derivY.getLayer(0),null,null,null);

		// extract the features
		QueueCorner found = detector.getFeatures();

		for( int i = 0; i < found.size() && !availableData.isEmpty(); i++ ) {
			Point2D_I16 pt = found.get(i);

			PyramidKltFeature feat = availableData.remove( availableData.size()-1);
			feat.x = pt.x*scaleBottom;
			feat.y = pt.y*scaleBottom;

			tracker.setDescription(feat);
			active.add(feat);
		}
	}
}
