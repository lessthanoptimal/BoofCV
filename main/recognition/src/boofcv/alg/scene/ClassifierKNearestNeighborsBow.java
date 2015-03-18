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

package boofcv.alg.scene;

import boofcv.abst.feature.dense.DescribeImageDense;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Scene classification which uses bag-of-word model and K-Nearest Neighbors.  When an image is processed
 * its dense features are extracted.  These features are then converted into words and a frequency histogram created.
 * When a new image is observers its word frequency histogram is computed.  Then the K-nearest neighbors is found
 * to its histogram.  The scene with the most hits in that neighbor hood is the scene's classification.
 *
 * TODO handle and describe above case where multiple neighbors have number
 *
 * @author Peter Abeles
 */
// todo add option to do weighted histogram from NN data
public class ClassifierKNearestNeighborsBow<T extends ImageBase,Desc extends TupleDesc> {

	// Used to look up the histograms in memory which are the most similar
	NearestNeighbor<HistogramScene> nn;
	// Computes all the features in the image
	DescribeImageDense<T,Desc> describe;
	// Converts the set of image features into visual words into a histogram which describes the frequency
	// of visual words
	FeatureToWordHistogram<Desc> featureToHistogram;

	// storage for NN results
	FastQueue<NnData<HistogramScene>> resultsNN = new FastQueue(NnData.class,true);

	// storage for image features
	FastQueue<Desc> imageFeatures;

	// number of neighbors it will consider
	int numNeighbors;

	// used what the most frequent neighbor is
	int scenes[];

	public ClassifierKNearestNeighborsBow(NearestNeighbor<HistogramScene> nn,
										  DescribeImageDense<T, Desc> describe,
										  FeatureToWordHistogram<Desc> featureToHistogram) {
		this.nn = nn;
		this.describe = describe;
		this.featureToHistogram = featureToHistogram;

		scenes = new int[ featureToHistogram.getTotalWords() ];
	}

	public void setTrainingData( List<HistogramScene> memory ) {

		List<double[]> points = new ArrayList<double[]>();
		for (int i = 0; i < memory.size(); i++) {
			points.add( memory.get(i).getHistogram() );
		}

		int numWords = featureToHistogram.getTotalWords();

		nn.init(numWords);
		nn.setPoints(points,memory);
	}

	public int process( T image ) {

		// compute all the features inside the image
		imageFeatures.reset();
		describe.process(image, imageFeatures, null);

		// find which word the feature matches and construct a frequency histogram
		featureToHistogram.reset();
		for (int i = 0; i < imageFeatures.size; i++) {
			Desc d = imageFeatures.get(i);
			featureToHistogram.addFeature(d);
		}
		featureToHistogram.process();
		double[] hist = featureToHistogram.getHistogram();

		// see which previously seen images are the closest match
		resultsNN.reset();
		nn.findNearest(hist,-1,numNeighbors,resultsNN);

		Arrays.fill(hist,0);
		for (int i = 0; i < resultsNN.size; i++) {
			NnData<HistogramScene> data = resultsNN.get(i);
			HistogramScene n = data.data;

			hist[n.type]++;
		}

		// pick the scene with the highest frequency
		int bestIndex = 0;
		int bestCount = 0;

		for (int i = 0; i < scenes.length; i++) {
			if( scenes[i] > bestCount ) {
				bestCount = scenes[i];
				bestIndex = i;
			}
		}

		return bestIndex;
	}
}
