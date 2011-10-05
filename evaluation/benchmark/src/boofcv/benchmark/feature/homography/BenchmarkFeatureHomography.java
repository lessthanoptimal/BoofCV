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
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F64;
import georegression.geometry.UtilPoint2D_F32;
import georegression.struct.homo.Homography2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I32;
import georegression.transform.homo.HomographyPointOps;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Benchmarks algorithms against a sequence of real images where the homography between the images
 * is known.
 *
 * @author Peter Abeles
 */
public class BenchmarkFeatureHomography {
	GeneralAssociation<TupleDesc_F64> assoc;
	List<Homography2D_F32> transforms;
	String directory;
	double tolerance;

	List<String> nameBase = new ArrayList<String>();

	int numMatches;
	double fractionCorrect;

	public BenchmarkFeatureHomography(GeneralAssociation<TupleDesc_F64> assoc,
									  String directory,
									  String imageSuffix ,
									  double tolerance) {
		this.assoc = assoc;
		this.directory = directory;
		this.tolerance = tolerance;

		nameBase = loadNameBase( directory , imageSuffix );

		transforms = new ArrayList<Homography2D_F32>();
		for( int i=1; i < nameBase.size(); i++ ) {
			String fileName = "H1to"+(i+1)+"p";
			transforms.add( LoadBenchmarkFiles.loadHomography(directory+"/"+fileName));
		}
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
	public void evaluate( String algSuffix ) {
		System.out.println("\n"+algSuffix);

		String descriptionName = directory+"DESCRIBE_"+nameBase.get(0)+"_"+algSuffix;
		// load descriptions in the keyframe
		List<FeatureInfo> keyFrame = LoadBenchmarkFiles.loadDescription(descriptionName);

		for( int i = 1; i < nameBase.size(); i++ ) {
			System.out.print("Examining "+nameBase.get(i));

			descriptionName = directory+"DESCRIBE_"+nameBase.get(i)+"_"+algSuffix;
			List<FeatureInfo> targetFrame = LoadBenchmarkFiles.loadDescription(descriptionName);

			Homography2D_F32 keyToTarget = transforms.get(i-1);

			associationScore(keyFrame,targetFrame,keyToTarget);
			System.out.printf(" %5d %4.2f\n",numMatches,fractionCorrect);
		}
	}

	/**
	 * Associates two sets of features against each other.
	 * @param keyFrame
	 * @param targetFrame
	 * @param keyToTarget
	 */
	private void associationScore(List<FeatureInfo> keyFrame,
								  List<FeatureInfo> targetFrame,
								  Homography2D_F32 keyToTarget) {

		FastQueue<TupleDesc_F64> listSrc = new FastQueue<TupleDesc_F64>(keyFrame.size(),TupleDesc_F64.class,false);
		FastQueue<TupleDesc_F64> listDst = new FastQueue<TupleDesc_F64>(keyFrame.size(),TupleDesc_F64.class,false);

		for( FeatureInfo f : keyFrame ) {
			listSrc.add(f.getDescription());
		}

		for( FeatureInfo f : targetFrame ) {
			listDst.add(f.getDescription());
		}

		assoc.associate(listSrc,listDst);

		FastQueue<AssociatedIndex> matches = assoc.getMatches();

		Point2D_F32 src = new Point2D_F32();
		Point2D_F32 expected = new Point2D_F32();

		// the number of key frame features which have a correspondence
		int maxCorrect = 0;
		// number of correct associations
		int numCorrect = 0;

		for( int i = 0; i < matches.size; i++ ) {
			AssociatedIndex a = matches.get(i);
			Point2D_I32 s = keyFrame.get(a.src).getLocation();
			Point2D_I32 d = targetFrame.get(a.dst).getLocation();

			src.set(s.x,s.y);

			HomographyPointOps.transform(keyToTarget,src,expected);

			double dist = UtilPoint2D_F32.distance(expected.x,expected.y,d.x,d.y);

			if( dist <= tolerance ) {
				numCorrect++;
				maxCorrect++;
			} else {
				if( hasCorrespondence(expected,targetFrame)) {
					maxCorrect++;
				}
			}
		}

		numMatches = maxCorrect;
		fractionCorrect = ((double)numCorrect)/((double)maxCorrect);
	}

	private boolean hasCorrespondence( Point2D_F32 expected, List<FeatureInfo> targetFrame) {

		for( FeatureInfo t : targetFrame ) {
			Point2D_I32 d = t.getLocation();
			double dist = UtilPoint2D_F32.distance(expected.x,expected.y,d.x,d.y);
			if( dist <= tolerance)
				return true;
		}
		return false;
	}

	public static void main( String args[] ) {
		double tolerance = 3;

		ScoreAssociation score = new ScoreAssociateEuclideanSq();
		GeneralAssociation<TupleDesc_F64> assoc = FactoryAssociation.greedy(score, Double.MAX_VALUE, -1, true);

		BenchmarkFeatureHomography app = new BenchmarkFeatureHomography(assoc,"data/mikolajczk/graf/",".png",tolerance);

//		app.evaluate("SURF.txt");
		app.evaluate("SAMPLE.txt");
//		app.evaluate("SAMPLEZ.txt");
//		app.evaluate("SAMPLEDIFF.txt");
//		app.evaluate("NEW.txt");
		app.evaluate("OpenSURF.txt");
//		app.evaluate("BRIEFO.txt");
//		app.evaluate("BRIEF.txt");
//		app.evaluate("BoofCV_SURF.txt");
//		app.evaluate("NEW2.txt");
	}
}
