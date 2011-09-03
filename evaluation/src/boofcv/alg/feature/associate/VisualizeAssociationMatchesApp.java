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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.GeneralAssociation;
import boofcv.abst.feature.describe.ExtractFeatureDescription;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.associate.FactoryAssociationTuple;
import boofcv.factory.feature.describe.FactoryExtractFeatureDescription;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDescQueue;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I32;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


/**
 * Visually shows the location of matching pairs of associated features in two images.
 *
 * @author Peter Abeles
 */
// todo add waiting tool bar
// todo show partial results
public class VisualizeAssociationMatchesApp<T extends ImageBase> {

	InterestPointDetector<T> detector;
	ExtractFeatureDescription<T> describe;
	GeneralAssociation<TupleDesc_F64> matcher;

	T imageLeft;
	T imageRight;
	Class<T> imageType;

	FastQueue<AssociatedIndex> matches = new FastQueue<AssociatedIndex>(10,AssociatedIndex.class,true);

	List<Point2D_I32> leftPts = new ArrayList<Point2D_I32>();
	List<Point2D_I32> rightPts = new ArrayList<Point2D_I32>();
	TupleDescQueue leftDesc;
	TupleDescQueue rightDesc;

	public VisualizeAssociationMatchesApp( InterestPointDetector<T> detector,
										ExtractFeatureDescription<T> describe,
										GeneralAssociation<TupleDesc_F64> matcher,
										Class<T> imageType )
	{
		this.detector = detector;
		this.describe = describe;
		this.matcher = matcher;
		this.imageType = imageType;

		imageLeft = GeneralizedImageOps.createImage(imageType,1,1);
		imageRight = GeneralizedImageOps.createImage(imageType,1,1);

		leftDesc = new TupleDescQueue(describe.getDescriptionLength());
		rightDesc = new TupleDescQueue(describe.getDescriptionLength());
	}

	public void process( BufferedImage buffLeft , BufferedImage buffRight ) {
		imageLeft.reshape(buffLeft.getWidth(),buffLeft.getHeight());
		imageRight.reshape(buffRight.getWidth(),buffRight.getHeight());

		ConvertBufferedImage.convertFrom(buffLeft,imageLeft,imageType);
		ConvertBufferedImage.convertFrom(buffRight,imageRight,imageType);

		// find feature points  and descriptions
		extractImageFeatures(imageLeft,leftDesc,leftPts);
		extractImageFeatures(imageRight,rightDesc,rightPts);

		matcher.associate(leftDesc,rightDesc);

		AssociationPanel panel = new AssociationPanel(20,500,500);
		panel.setImages(buffLeft,buffRight);
		panel.setAssociation(leftPts,rightPts,matcher.getMatches());

		ShowImages.showWindow(panel,"Associated Features");
	}

	private void extractImageFeatures( T image , FastQueue<TupleDesc_F64> descs , List<Point2D_I32> locs ) {
		descs.reset();
		locs.clear();
		detector.detect(image);
		describe.setImage(image);
		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			Point2D_I32 pt = detector.getLocation(i);
			double scale = detector.getScale(i);

			TupleDesc_F64 d = describe.process(pt.x,pt.y,scale,null);
			if( d != null ) {
				descs.pop().set(d.value);
				locs.add( pt.copy());
			}
		}
	}

	public static void main( String args[] ) {

//		String leftName = "evaluation/data/stitch/cave_01.jpg";
//		String rightName = "evaluation/data/stitch/cave_02.jpg";
		String leftName = "evaluation/data/stitch/kayak_02.jpg";
		String rightName = "evaluation/data/stitch/kayak_03.jpg";
//		String leftName = "evaluation/data/scale/rainforest_01.jpg";
//		String rightName = "evaluation/data/scale/rainforest_02.jpg";

		BufferedImage left = UtilImageIO.loadImage(leftName);
		BufferedImage right = UtilImageIO.loadImage(rightName);

		Class imageType = ImageFloat32.class;
		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);
		InterestPointDetector detector = FactoryInterestPoint.fromFastHessian(200,9,4,4);
		ExtractFeatureDescription describe =  FactoryExtractFeatureDescription.surf(true,imageType);
//		ExtractFeatureDescription describe =  FactoryExtractFeatureDescription.steerableGaussian(20,true,imageType,derivType);
//		ExtractFeatureDescription describe =  FactoryExtractFeatureDescription.gaussian12(20,imageType,derivType);

		ScoreAssociateTuple scorer = new ScoreAssociateEuclideanSq();
//		ScoreAssociateTuple scorer = new ScoreAssociateCorrelation();

//		GeneralAssociation<TupleDesc_F64> matcher = FactoryAssociationTuple.inlierError(scorer,200,10);
		GeneralAssociation<TupleDesc_F64> matcher = FactoryAssociationTuple.forwardBackwards(scorer,150);
//		GeneralAssociation<TupleDesc_F64> matcher = FactoryAssociationTuple.maxMatches(scorer,100);
//		GeneralAssociation<TupleDesc_F64> matcher = FactoryAssociationTuple.maxError(scorer,10);

		VisualizeAssociationMatchesApp app = new VisualizeAssociationMatchesApp(detector,describe,matcher,imageType);

		app.process(left,right);

	}
}
