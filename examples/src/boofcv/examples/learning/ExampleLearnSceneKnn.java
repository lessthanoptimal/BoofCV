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

package boofcv.examples.learning;

import boofcv.abst.feature.dense.DescribeImageDense;
import boofcv.alg.bow.ClusterVisualWords;
import boofcv.alg.bow.LearnSceneFromFiles;
import boofcv.alg.scene.ClassifierKNearestNeighborsBow;
import boofcv.alg.scene.FeatureToWordHistogram_F64;
import boofcv.alg.scene.HistogramScene;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageUInt8;
import org.ddogleg.clustering.AssignCluster;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Example of how to train a K-NN bow-of-word classifier for scene recognition.  TODO flush out more
 *
 * @author Peter Abeles
 */
public class ExampleLearnSceneKnn {

	public static int NUMBER_OF_WORDS = 200;
	public static boolean HISTOGRAM_HARD = true;

	public static class Learning extends LearnSceneFromFiles {

		ClusterVisualWords cluster;
		DescribeImageDense<ImageUInt8,TupleDesc_F64> describeImage;
		NearestNeighbor<HistogramScene> nn;


		ClassifierKNearestNeighborsBow<ImageUInt8,TupleDesc_F64> classifier;

		// Storage for detected features
		FastQueue<TupleDesc_F64> features;

		public Learning(final DescribeImageDense<ImageUInt8, TupleDesc_F64> describeImage,
						ClusterVisualWords cluster,
						NearestNeighbor<HistogramScene> nn ) {
			this.describeImage = describeImage;
			this.cluster = cluster;
			this.nn = nn;


			// This list can be dynamically grown.  However TupleDesc doesn't have a no argument constructor so
			// you must to it how to cosntruct the data
			features = new FastQueue<TupleDesc_F64>(TupleDesc_F64.class,true) {
				@Override
				protected TupleDesc_F64 createInstance() {
					return describeImage.createDescription();
				}
			};
		}

		public void learn() {
			// load all features in the training set
			for( String scene : train.keySet() ) {
				List<String> imagePaths = train.get(scene);

				for( String path : imagePaths ) {
					ImageUInt8 image = UtilImageIO.loadImage(path, ImageUInt8.class);

					features.reset();
					describeImage.process(image,features);

					// add the features to the overall list which the clusters will be found inside of
					for (int i = 0; i < features.size; i++) {
						cluster.add(features.get(i));
					}
				}
			}

			// Find the clusters.  This can take a bit
			cluster.process(NUMBER_OF_WORDS);

			// Use these clusters to assign features to words
			AssignCluster<double[]> assignment =  cluster.getAssignment();
			FeatureToWordHistogram_F64 featuresToHistogram = new FeatureToWordHistogram_F64(assignment,HISTOGRAM_HARD);

			// Must use this list of the scenes.  Otherwise the index will be different
			List<String> scenes = getScenes();

			// Processed results which will be passed into the k-NN algorithm
			List<HistogramScene> memory = new ArrayList<HistogramScene>();

			for( int sceneIndex = 0; sceneIndex < scenes.size(); sceneIndex++ ) {
				String scene = scenes.get(sceneIndex);
				List<String> imagePaths = train.get(scene);

				// reset before processing a new image
				featuresToHistogram.reset();

				for (String path : imagePaths) {
					ImageUInt8 image = UtilImageIO.loadImage(path, ImageUInt8.class);

					features.reset();
					describeImage.process(image, features);
					for (int i = 0; i < features.size; i++) {
						featuresToHistogram.addFeature(features.get(i));
					}
				}

				// The histogram is already normalized so that it sums up to 1.  This provides same invariance
				// against the overall number of features changing.
				double[] histogram = featuresToHistogram.getHistogram();

				// Create the data structure used by the KNN classifier
				HistogramScene imageHist = new HistogramScene(NUMBER_OF_WORDS);
				imageHist.setHistogram(histogram);
				imageHist.type = sceneIndex;

				memory.add(imageHist);
			}

			// Provide the training results to K-NN and it will preprocess these results for quick lookup later on
			classifier = new ClassifierKNearestNeighborsBow<ImageUInt8,TupleDesc_F64>(nn,describeImage,featuresToHistogram);
			classifier.setTrainingData(memory);
		}

		@Override
		protected int classify(String path) {
			ImageUInt8 image = UtilImageIO.loadImage(path, ImageUInt8.class);

			return classifier.process(image);
		}


	}

	public static void main(String[] args) {

		// train

		// test

		// save results
		// TODO save cluster assignment
		// TODO save memory
	}
}
