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
import boofcv.factory.feature.dense.ConfigDenseSample;
import boofcv.factory.feature.dense.FactoryDescribeImageDense;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.learning.Confusion;
import org.ddogleg.clustering.AssignCluster;
import org.ddogleg.clustering.ComputeClusters;
import org.ddogleg.clustering.FactoryClustering;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.struct.FastQueue;
import org.ejml.ops.MatrixVisualization;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Example of how to train a K-NN bow-of-word classifier for scene recognition.  The resulting classifier
 * produces results which are correct 46% of the time.  To provide a point of comparison, a
 * SVM One vs One RBF classifier can produce accuracy of around 74% and other people using different techniques
 * claim to have achieved around 85% accurate.
 * </p>
 *
 * Training Steps:
 * <ol>
 * <li>Compute dense SURF features across the training data set.</li>
 * <li>Cluster using k-means to create works.</li>
 * <li>For each image compute the histogram of words found in the image<li>
 * <li>Save word histograms and image scene labels in a classifier</li>
 * </ol>
 *
 * Testing Steps:
 * <ol>
 * <li>For each image in the testing data set compute its histogram</li>
 * <li>Look up the k-nearest-neighbors for that histogram</li>
 * <li>Classify an image by by selecting the scene type with the most neighbors</li>
 * </ol>
 *
 * <p>NOTE: Scene recognition is still very much a work in progress and the code is likely to be significantly
 * modified in the future.</p>
 *
 * @author Peter Abeles
 */
public class ExampleLearnSceneKnn extends LearnSceneFromFiles {

	// Tuning parameters
	public static int NUMBER_OF_WORDS = 50;
	public static boolean HISTOGRAM_HARD = true;
	public static int NUM_NEIGHBORS = 10;
	public static int MAX_KNN_ITERATIONS = 100;
	public static double DESC_SCALE = 1.0;
	public static int DESC_SKIP = 8;

	// Algorithms
	ClusterVisualWords cluster;
	DescribeImageDense<ImageUInt8,TupleDesc_F64> describeImage;
	NearestNeighbor<HistogramScene> nn;

	ClassifierKNearestNeighborsBow<ImageUInt8,TupleDesc_F64> classifier;

	// Storage for detected features
	FastQueue<TupleDesc_F64> features;

	public ExampleLearnSceneKnn(final DescribeImageDense<ImageUInt8, TupleDesc_F64> describeImage,
								ComputeClusters<double[]> clusterer,
								NearestNeighbor<HistogramScene> nn ) {
		this.describeImage = describeImage;
		this.cluster = new ClusterVisualWords(clusterer, describeImage.createDescription().size(),0xFEEDBEEF);
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

	private String getClusterName() {
		return "clusters.obj";
	}

	private String getHistogramsName() {
		return "histograms.obj";
	}

	/**
	 * Extract dense features across the training set.  Then clusters are found within those features.
	 */
	private AssignCluster<double[]> computeClusters() {
		// load all features in the training set
		features.reset();
		for( String scene : train.keySet() ) {
			List<String> imagePaths = train.get(scene);
			System.out.println("   " + scene);

			for( String path : imagePaths ) {
				ImageUInt8 image = UtilImageIO.loadImage(path, ImageUInt8.class);
				describeImage.process(image,features,null);
			}
		}
		// add the features to the overall list which the clusters will be found inside of
		for (int i = 0; i < features.size; i++) {
			cluster.addReference(features.get(i));
		}

		System.out.println("Clustering");
		// Find the clusters.  This can take a bit
		cluster.process(NUMBER_OF_WORDS);

		UtilIO.save(cluster.getAssignment(), getClusterName());

		return cluster.getAssignment();
	}

	/**
	 * Process all the data in the training data set to learn the classifications.  See code for details.
	 */
	public void learn() {
		System.out.println("======== Learning Classifier");

		File clusterFile = new File(getClusterName());
		File histogramFile = new File(getHistogramsName());

		// Either load pre-computed words or compute the words from the training images
		AssignCluster<double[]> assignment;
		if( clusterFile.exists() ) {
			System.out.println(" Loading "+clusterFile.getName());
			assignment = UtilIO.load(clusterFile.getName());
		} else {
			System.out.println(" Computing clusters");
			assignment = computeClusters();
		}

		// Use these clusters to assign features to words
		FeatureToWordHistogram_F64 featuresToHistogram = new FeatureToWordHistogram_F64(assignment,HISTOGRAM_HARD);

		// Storage for the work histogram in each image in the training set and their label
		List<HistogramScene> memory;

		if( histogramFile.exists() ) {
			System.out.println(" Loading "+histogramFile.getName());
			memory = UtilIO.load(histogramFile.getName());
		} else {
			System.out.println(" computing histograms");
			memory = computeHistograms(featuresToHistogram);
			UtilIO.save(memory,histogramFile.getName());
		}


		// Provide the training results to K-NN and it will preprocess these results for quick lookup later on
		classifier = new ClassifierKNearestNeighborsBow<ImageUInt8,TupleDesc_F64>(nn,describeImage,featuresToHistogram);
		classifier.setClassificationData(memory, getScenes().size());
		classifier.setNumNeighbors(NUM_NEIGHBORS);
	}

	/**
	 * For all the images in the training data set it computes a {@link HistogramScene}.  That data structure
	 * contains the word histogram and the scene that the histogram belongs to.
	 */
	private List<HistogramScene> computeHistograms(FeatureToWordHistogram_F64 featuresToHistogram ) {

		List<String> scenes = getScenes();

		List<HistogramScene> memory;// Processed results which will be passed into the k-NN algorithm
		memory = new ArrayList<HistogramScene>();

		for( int sceneIndex = 0; sceneIndex < scenes.size(); sceneIndex++ ) {
			String scene = scenes.get(sceneIndex);
			System.out.println("   " + scene);
			List<String> imagePaths = train.get(scene);

			for (String path : imagePaths) {
				ImageUInt8 image = UtilImageIO.loadImage(path, ImageUInt8.class);

				// reset before processing a new image
				featuresToHistogram.reset();
				features.reset();
				describeImage.process(image, features,null);
				for (int i = 0; i < features.size; i++) {
					featuresToHistogram.addFeature(features.get(i));
				}
				featuresToHistogram.process();

				// The histogram is already normalized so that it sums up to 1.  This provides invariance
				// against the overall number of features changing.
				double[] histogram = featuresToHistogram.getHistogram();

				// Create the data structure used by the KNN classifier
				HistogramScene imageHist = new HistogramScene(NUMBER_OF_WORDS);
				imageHist.setHistogram(histogram);
				imageHist.type = sceneIndex;

				memory.add(imageHist);
			}
		}
		return memory;
	}

	@Override
	protected int classify(String path) {
		ImageUInt8 image = UtilImageIO.loadImage(path, ImageUInt8.class);

		return classifier.classify(image);
	}

	public static void main(String[] args) {

		DescribeImageDense<ImageUInt8,TupleDesc_F64> desc = (DescribeImageDense)
				FactoryDescribeImageDense.surfStable(null,
						new ConfigDenseSample(DESC_SCALE,DESC_SKIP,DESC_SKIP),ImageUInt8.class);

		ComputeClusters<double[]> clusterer = FactoryClustering.kMeans_F64(null, MAX_KNN_ITERATIONS, 20, 1e-6);
		clusterer.setVerbose(true);

		NearestNeighbor<HistogramScene> nn = FactoryNearestNeighbor.exhaustive();
		ExampleLearnSceneKnn example = new ExampleLearnSceneKnn(desc,clusterer,nn);

		File trainingDir = new File("../data/applet/learning/scene/train");
		File testingDir = new File("../data/applet/learning/scene/test");

		if( !trainingDir.exists() || !testingDir.exists() ) {
			System.err.println("Please follow instructions in data/applet/learning/scene and download the");
			System.err.println("required files");
			System.exit(1);
		}

		example.loadSets(trainingDir,null,testingDir);
		// train the classifier
		example.learn();

		// test the classifier
		Confusion confusion = example.evaluateTest();
		confusion.getMatrix().print();
		System.out.println("Accuracy = " + confusion.computeAccuracy());

		// Show confusion matrix
		// Not the best coloration scheme...  perfect = red diagonal and black elsewhere.
		MatrixVisualization.show(confusion.getMatrix(),"Confusion Matrix");

		// Using the default settings you should get an accuracy of 0.4643560924582769
		// 0.4938983163196912
	}
}
