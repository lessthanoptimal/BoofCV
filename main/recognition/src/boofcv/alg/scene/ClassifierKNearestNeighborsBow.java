/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
 * <p>
 * Scene classification which uses bag-of-word model and K-Nearest Neighbors.  Classification data consists of a labeled
 * set of word histograms.  Each word histogram was created by 1) extracting all the features inside an image which
 * was known to belong to a specific scene, 2) the word which best matches each feature is found and added to the
 * histogram, and 3) the histogram is then normalized so that it adds up to one.
 * </p>
 *
 * <p>
 * When classifying an image its word histogram is computed the same way.  Using the just computed histogram the
 * K-nearest neighbors to its word histogram are found in the classification data.  The most frequent scene
 * (the mode) of the k-neighbors is the selected scene type of the image being considered.
 * </p>
 *
 * @author Peter Abeles
 */
// todo add option to do weighted histogram from NN data
public class ClassifierKNearestNeighborsBow<T extends ImageBase,Desc extends TupleDesc> {

	// Used to look up the histograms in memory which are the most similar
	private NearestNeighbor<HistogramScene> nn;
	// Computes all the features in the image
	private DescribeImageDense<T,Desc> describe;
	// Converts the set of image features into visual words into a histogram which describes the frequency
	// of visual words
	private FeatureToWordHistogram<Desc> featureToHistogram;

	// storage for NN results
	private FastQueue<NnData<HistogramScene>> resultsNN = new FastQueue(NnData.class,true);

	// number of neighbors it will consider
	private int numNeighbors;

	// used what the most frequent neighbor is
	private double scenes[];

	/**
	 * Configures internal algorithms.
	 *
	 * @param nn Used to perform nearest-neighbor search
	 * @param describe Computes the dense image features
	 * @param featureToHistogram Converts a set of features into a word histogram
	 */
	public ClassifierKNearestNeighborsBow(NearestNeighbor<HistogramScene> nn,
										  final DescribeImageDense<T, Desc> describe,
										  FeatureToWordHistogram<Desc> featureToHistogram) {
		this.nn = nn;
		this.describe = describe;
		this.featureToHistogram = featureToHistogram;
	}

	/**
	 * Specifies the number of neighbors it should search for when classifying\
	 */
	public void setNumNeighbors(int numNeighbors) {
		this.numNeighbors = numNeighbors;
	}

	/**
	 * Provides a set of labeled word histograms to use to classify a new image
	 * @param memory labeled histograms
	 */
	public void setClassificationData(List<HistogramScene> memory , int numScenes ) {

		List<double[]> points = new ArrayList<>(memory.size());
		for (int i = 0; i < memory.size(); i++) {
			points.add( memory.get(i).getHistogram() );
		}

		int numWords = featureToHistogram.getTotalWords();

		nn.init(numWords);
		nn.setPoints(points, memory);

		scenes = new double[ numScenes ];
	}

	/**
	 * Finds the scene which most resembles the provided image
	 * @param image Image that's to be classified
	 * @return The index of the scene it most resembles
	 */
	public int classify(T image) {
		if( numNeighbors == 0 )
			throw new IllegalArgumentException("Must specify number of neighbors!");

		// compute all the features inside the image
		describe.process(image);

		// find which word the feature matches and construct a frequency histogram
		featureToHistogram.reset();
		List<Desc> imageFeatures = describe.getDescriptions();
		for (int i = 0; i < imageFeatures.size(); i++) {
			Desc d = imageFeatures.get(i);
			featureToHistogram.addFeature(d);
		}
		featureToHistogram.process();
		double[] hist = featureToHistogram.getHistogram();

		// Find the N most similar image histograms
		resultsNN.reset();
		nn.findNearest(hist,-1,numNeighbors,resultsNN);

		// Find the most common scene among those neighbors
		Arrays.fill(scenes,0);
		for (int i = 0; i < resultsNN.size; i++) {
			NnData<HistogramScene> data = resultsNN.get(i);
			HistogramScene n = data.data;

//			scenes[n.type]++;
			scenes[n.type] += 1.0/(data.distance+0.005); // todo
//			scenes[n.type] += 1.0/(Math.sqrt(data.distance)+0.005); // todo
		}

		// pick the scene with the highest frequency
		int bestIndex = 0;
		double bestCount = 0;

		for (int i = 0; i < scenes.length; i++) {
			if( scenes[i] > bestCount ) {
				bestCount = scenes[i];
				bestIndex = i;
			}
		}

		return bestIndex;
	}
}
