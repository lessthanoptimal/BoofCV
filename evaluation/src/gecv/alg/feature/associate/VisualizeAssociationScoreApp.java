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

package gecv.alg.feature.associate;

import gecv.abst.feature.describe.ExtractFeatureDescription;
import gecv.abst.feature.detect.interest.InterestPointDetector;
import gecv.core.image.ConvertBufferedImage;
import gecv.core.image.GeneralizedImageOps;
import gecv.factory.feature.describe.FactoryExtractFeatureDescription;
import gecv.factory.feature.detect.interest.FactoryInterestPoint;
import gecv.gui.feature.AssociationScorePanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.UtilImageIO;
import gecv.struct.feature.TupleDesc_F64;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import jgrl.struct.point.Point2D_I32;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


/**
 * Allows the user to click on a feature.  Once selected it shows relative scores of features
 * in the other image.
 *
 * @author Peter Abeles
 */
// TODO make feature selection, feature type, score all changeable in a GUI
public class VisualizeAssociationScoreApp<T extends ImageBase> {

	InterestPointDetector<T> detector;
	ExtractFeatureDescription<T> describe;
	ScoreAssociateTuple scorer;

	T imageLeft;
	T imageRight;
	Class<T> imageType;

	List<Point2D_I32> leftPts = new ArrayList<Point2D_I32>();
	List<Point2D_I32> rightPts = new ArrayList<Point2D_I32>();
	List<TupleDesc_F64> leftDesc = new ArrayList<TupleDesc_F64>();
	List<TupleDesc_F64> rightDesc = new ArrayList<TupleDesc_F64>();

	public VisualizeAssociationScoreApp( InterestPointDetector<T> detector,
										 ExtractFeatureDescription<T> describe,
										 ScoreAssociateTuple scorer,
										 Class<T> imageType )
	{
		this.detector = detector;
		this.describe = describe;
		this.scorer = scorer;
		this.imageType = imageType;

		imageLeft = GeneralizedImageOps.createImage(imageType,1,1);
		imageRight = GeneralizedImageOps.createImage(imageType,1,1);
	}

	public void process( BufferedImage buffLeft , BufferedImage buffRight ) {
		imageLeft.reshape(buffLeft.getWidth(),buffLeft.getHeight());
		imageRight.reshape(buffRight.getWidth(),buffRight.getHeight());

		ConvertBufferedImage.convertFrom(buffLeft,imageLeft,imageType);
		ConvertBufferedImage.convertFrom(buffRight,imageRight,imageType);

		// find feature points  and descriptions
		extractImageFeatures(imageLeft,leftDesc,leftPts);
		extractImageFeatures(imageRight,rightDesc,rightPts);

		MyPanel panel = new MyPanel(640,480);
		panel.setImages(buffLeft,buffRight);
		panel.setLocation(leftPts,rightPts);

		ShowImages.showWindow(panel,"Association Score");
	}

	private void extractImageFeatures( T image , List<TupleDesc_F64> descs , List<Point2D_I32> locs ) {
		descs.clear();
		locs.clear();
		detector.detect(image);
		describe.setImage(image);
		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			Point2D_I32 pt = detector.getLocation(i);
			double scale = detector.getScale(i);

			TupleDesc_F64 d = describe.process(pt.x,pt.y,scale);
			if( d != null ) {
				descs.add( d.copy() );
				locs.add( pt.copy());
			}
		}
	}

	private class MyPanel extends AssociationScorePanel
	{
		double[] score;

		public MyPanel(int maxWidth, int maxHeight) {
			super(maxWidth, maxHeight,0.2);
		}

		@Override
		protected double[] computeScore(boolean isTargetLeft, int targetIndex) {
			if( score == null ) {
				score = new double[ Math.max(leftPts.size(),rightPts.size())];
			}
			if( isTargetLeft ) {
				TupleDesc_F64 t = leftDesc.get(targetIndex);
				for( int i = 0; i < rightDesc.size(); i++ ) {
					TupleDesc_F64 d = rightDesc.get(i);
					score[i] = scorer.score(t,d);
				}
			} else {
				TupleDesc_F64 t = rightDesc.get(targetIndex);
				for( int i = 0; i < leftDesc.size(); i++ ) {
					TupleDesc_F64 d = leftDesc.get(i);
					score[i] = scorer.score(t,d);
				}
			}
			return score;
		}
	}

	public static void main( String args[] ) {

//		String leftName = "evaluation/data/stitch/cave_01.jpg";
//		String rightName = "evaluation/data/stitch/cave_02.jpg";
//		String leftName = "evaluation/data/stitch/kayak_02.jpg";
//		String rightName = "evaluation/data/stitch/kayak_03.jpg";
		String leftName = "evaluation/data/scale/rainforest_01.jpg";
		String rightName = "evaluation/data/scale/rainforest_02.jpg";

		BufferedImage left = UtilImageIO.loadImage(leftName);
		BufferedImage right = UtilImageIO.loadImage(rightName);

		Class imageType = ImageFloat32.class;
		Class derivType = ImageFloat32.class;
		InterestPointDetector detector = FactoryInterestPoint.fromFastHessian(-1,9,4,4);
		ExtractFeatureDescription describe =  FactoryExtractFeatureDescription.surf(true,imageType);
//		ExtractFeatureDescription describe =  FactoryExtractFeatureDescription.steerableGaussian(20,true,imageType,derivType);
		ScoreAssociateTuple scorer = new ScoreAssociateEuclideanSq();

		VisualizeAssociationScoreApp app = new VisualizeAssociationScoreApp(detector,describe,scorer,imageType);

		app.process(left,right);

	}
}
