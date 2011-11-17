/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.benchmark.feature.homography;

import boofcv.abst.feature.associate.GeneralAssociation;
import boofcv.alg.feature.associate.ScoreAssociateEuclideanSq;
import boofcv.alg.feature.associate.ScoreAssociation;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.struct.feature.TupleDesc_F64;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.homo.Homography2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.transform.homo.HomographyPointOps;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Benchmarks algorithms against a sequence of real images where the homography between the images
 * is known.
 *
 * @author Peter Abeles
 */
public class BenchmarkFeatureDetectStability {
	GeneralAssociation<TupleDesc_F64> assoc;
	List<Homography2D_F32> transforms;
	String imageSuffix;
	double tolerance;
	double scaleTolerance = 0.25;

	List<String> nameBase = new ArrayList<String>();

	int numMatches;
	double fractionCorrect;

	double fractionAmbiguous;

	int totalMatches;
	double totalCorrect;
	double totalAmbiguous;

	List<String> directories = new ArrayList<String>();

	PrintStream output;

	// image dimensions
	int width;
	int height;

	public BenchmarkFeatureDetectStability(GeneralAssociation<TupleDesc_F64> assoc,
										   String imageSuffix,
										   double tolerance) {

		this.assoc = assoc;
		this.imageSuffix = imageSuffix;
		this.tolerance = tolerance;
	}

	public void addDirectory( String dir ) {
		directories.add(dir);
	}

	/**
	 * Scans the directory for images with the specified suffix.  These names are
	 * used to find all the description files.
	 *
	 * @param directory Directory containing images and description files.
	 * @param imageSuffix Type of input images.
	 * @return Names of input images.
	 */
	private List<String> loadNameBase(String directory, String imageSuffix) {
		List<String> ret = new ArrayList<String>();
		File dir = new File(directory);

		for( File f : dir.listFiles() ) {
			if( !(f.isFile() && f.getName().endsWith(imageSuffix))) {
				continue;
			}

			String name = f.getName();
			ret.add( name.substring(0,name.length()-imageSuffix.length()));
		}

		// put the names into order
		Collections.sort(ret);

		return ret;
	}

	/**
	 * For each input image it loads the specified descriptions.  These are then associated
	 * against each other and the results compared.
	 *
	 * @param algSuffix String used to identify feature description files.
	 */
	public void evaluate( String algSuffix ) throws FileNotFoundException {
		System.out.println("\n"+algSuffix);
		output = new PrintStream("detect_stability_"+algSuffix+".txt");
		output.println("tolerance = "+tolerance);
		output.println("scaleTolerance = "+scaleTolerance);
		output.println();

		totalCorrect = 0;
		totalMatches = 0;
		for( String dir : directories ) {
			processDirectory(dir,algSuffix);
		}

		System.out.println("Summary Score:");
		System.out.println("   num matches     = "+totalMatches);
		System.out.println("   total correct   = "+totalCorrect);
		System.out.println("   total ambiguous = "+totalAmbiguous);
		output.println("Summary Score:");
		output.println("   num matches     = "+totalMatches);
		output.println("   total correct   = "+totalCorrect);
		output.println("   total ambiguous = "+totalAmbiguous);

		output.close();
	}

	private void processDirectory( String directory , String algSuffix ) {
		System.out.println("Directory: "+directory);
		output.println("---------- Directory: "+directory);

		findImageSize(directory);

		nameBase = loadNameBase( directory , imageSuffix );

		transforms = new ArrayList<Homography2D_F32>();
		for( int i=1; i < nameBase.size(); i++ ) {
			String fileName = "H1to"+(i+1)+"p";
			transforms.add( LoadBenchmarkFiles.loadHomography(directory+"/"+fileName));
		}

		List<DetectionInfo> detections[] = new ArrayList[nameBase.size()];
		for( int i = 0; i < nameBase.size(); i++ ) {
			String detectName = String.format("%s/DETECTED_%s_%s.txt",directory,nameBase.get(i),algSuffix);
			detections[i] = LoadBenchmarkFiles.loadDetection(detectName);
		}

		List<Integer> matches = new ArrayList<Integer>();
		List<Double> fractions = new ArrayList<Double>();

		for( int i = 1; i < nameBase.size(); i++ ) {

			Homography2D_F32 keyToTarget = transforms.get(i-1);

			associationScore(detections[0],detections[i],keyToTarget);
			totalCorrect += fractionCorrect;
			totalMatches += numMatches;
			totalAmbiguous += fractionAmbiguous;
			matches.add(numMatches);
			fractions.add(fractionCorrect);
			output.print(nameBase.get(i)+" ");
			System.out.printf(" %5d %4.2f %4.2f\n",numMatches,fractionCorrect,fractionAmbiguous);
		}
		output.println();

		for( int m : matches ) {
			output.print(m+" ");
		}
		output.println();
		for( double f : fractions ) {
			output.printf("%6.4f ", f);
		}
		output.println();
	}

	private void findImageSize(String directory) {
		String imageName = String.format("%s/img%d.png", directory, 1);

		try {
			BufferedImage image = ImageIO.read(new File(imageName));
			width = image.getWidth();
			height = image.getHeight();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Associates two sets of features against each other.
	 * @param keyFrame
	 * @param targetFrame
	 * @param keyToTarget
	 */
	private void associationScore(List<DetectionInfo> keyFrame,
								  List<DetectionInfo> targetFrame,
								  Homography2D_F32 keyToTarget) {


		// the number of key frame features which have a correspondence
		int maxCorrect = 0;
		// number of correct associations
		int numCorrect = 0;
		// number of ambiguous matches
		int numAmbiguous = 0;

		Point2D_F32 src = new Point2D_F32();
		Point2D_F32 expected = new Point2D_F32();

		Point2D_F32 sample[] = new Point2D_F32[4];
		for( int i = 0; i < sample.length; i++ )
			sample[i] = new Point2D_F32();
		Point2D_F32 sampleDst = new Point2D_F32();

		for( DetectionInfo k : keyFrame ) {
			src.set((float)k.location.x,(float)k.location.y);
			sample[0].set(src.x + 1, src.y);
			sample[1].set(src.x - 1, src.y);
			sample[2].set(src.x,src.y+1);
			sample[3].set(src.x,src.y-1);

			HomographyPointOps.transform(keyToTarget,src,expected);
			// estimate how the transform would rescale the image
			double expectedScale = 0;
			for( Point2D_F32 s : sample ) {
				HomographyPointOps.transform(keyToTarget,s,sampleDst);
				expectedScale += expected.distance(sampleDst);
			}
			expectedScale /= sample.length;
			expectedScale = k.getScale()*expectedScale;

			if( expected.x < 0 || expected.y < 0 || expected.x >= width || expected.y >= height) {
				continue;
			}

//			maxCorrect++;

			int numMatched = 0;
			for( DetectionInfo t : targetFrame ) {
				double dist = UtilPoint2D_F64.distance(expected.x,expected.y,t.location.x,t.location.y);
				double scaleDiff = Math.abs(t.scale-expectedScale)/expectedScale;
				if( dist < tolerance && scaleDiff < scaleTolerance ) {
//					numCorrect++;
//					break;
					numMatched++;
				}
			}

			if( numMatched <= 1 )
				maxCorrect++;

			if( numMatched == 1 ) {
				numCorrect++;
			} else if( numMatched > 1 )
				numAmbiguous++;
		}

		numMatches = maxCorrect;
		fractionCorrect = ((double)numCorrect)/((double)maxCorrect);
		fractionAmbiguous = ((double)numAmbiguous)/((double)maxCorrect);
	}

	public static void main( String args[] ) throws FileNotFoundException {
		double tolerance = 1.5;

		ScoreAssociation score = new ScoreAssociateEuclideanSq();
		GeneralAssociation<TupleDesc_F64> assoc = FactoryAssociation.greedy(score, Double.MAX_VALUE, -1, true);

		BenchmarkFeatureDetectStability app = new BenchmarkFeatureDetectStability(assoc,".png",tolerance);

		app.addDirectory("data/mikolajczk/bikes/");
		app.addDirectory("data/mikolajczk/boat/");
		app.addDirectory("data/mikolajczk/graf/");
		app.addDirectory("data/mikolajczk/leuven/");
		app.addDirectory("data/mikolajczk/ubc/");
		app.addDirectory("data/mikolajczk/trees/");
		app.addDirectory("data/mikolajczk/wall/");
		app.addDirectory("data/mikolajczk/bark/");

		app.evaluate("FH");
		app.evaluate("PanOMatic");
		app.evaluate("OpenSURF");
		app.evaluate("OpenCV");
		app.evaluate("SURF");
		app.evaluate("JOpenSURF");
		app.evaluate("JavaSURF");
	}
}
