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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Example of how to train a K-NN bow-of-word classifier for scene recognition.  TODO flush out more
 *
 * @author Peter Abeles
 */
public class ExampleLearnSceneKnn extends LearnSceneFromFiles {

	public static int NUMBER_OF_WORDS = 50;
	public static boolean HISTOGRAM_HARD = true;
	public static int NUM_NEIGHBORS = 10;
	public static int MAX_KNN_ITERATIONS = 100;
	public static double DESC_RADIUS = 16;
	public static int DESC_SKIP = 8;


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
		this.cluster = new ClusterVisualWords(clusterer,
				describeImage.createDescription().size(),0xFEEDBEEF);
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
		return "clusters.xml";
	}

	private String getHistogramsName() {
		return "histograms.xml";
	}

	private AssignCluster<double[]> computeClusters() {
		// load all features in the training set
		for( String scene : train.keySet() ) {
			System.out.println("   " + scene);
			List<String> imagePaths = train.get(scene);

			for( String path : imagePaths ) {
				ImageUInt8 image = UtilImageIO.loadImage(path, ImageUInt8.class);

				features.reset();
				describeImage.process(image,features,null);

				// add the features to the overall list which the clusters will be found inside of
				for (int i = 0; i < features.size; i++) {
					cluster.add(features.get(i));
				}
			}
		}

		System.out.println("Clustering");
		// Find the clusters.  This can take a bit
		cluster.process(NUMBER_OF_WORDS);

		UtilIO.save(cluster.getAssignment(), getClusterName());

		return cluster.getAssignment();
	}

	public void learn() {
		System.out.println("======== Learning Classifier");

		File clusterFile = new File(getClusterName());
		File histogramFile = new File(getHistogramsName());

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
		classifier.setClassificationData(memory,getScenes().size());
		classifier.setNumNeighbors(NUM_NEIGHBORS);
	}

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
						new ConfigDenseSample(DESC_RADIUS,DESC_SKIP,DESC_SKIP),ImageUInt8.class);

		ComputeClusters<double[]> clusterer = FactoryClustering.kMeans_F64(null,MAX_KNN_ITERATIONS,20,1e-6);
		clusterer.setVerbose(true);

		NearestNeighbor<HistogramScene> nn = FactoryNearestNeighbor.exhaustive();
		ExampleLearnSceneKnn example = new ExampleLearnSceneKnn(desc,clusterer,nn);

		example.loadSets(new File("/home/pja/projects/bow/brown/data/train"),null,
				new File("/home/pja/projects/bow/brown/data/test"));
		// train
		example.learn();

		// test
		Confusion confusion = example.evaluateTest();
		confusion.getMatrix().print();
		System.out.println("Accuracy = " + confusion.computeAccuracy());

		// current settings 0.4576592819873412
		// best so far 0.4712136547498593 - not sure how I got that, changed how scale is specified after

		// skip 6 = Accuracy = 0.44168243495940473

	}
}
