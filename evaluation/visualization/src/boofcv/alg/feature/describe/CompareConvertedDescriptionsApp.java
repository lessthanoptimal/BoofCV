/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.describe;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.ConvertTupleDesc;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.describe.DescribeRegionPointConvert;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryConvertTupleDesc;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.gui.feature.AssociationPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.feature.TupleDesc_S8;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Visualizes the difference between two "equivalent" descriptions after conversion by associating image features.
 * If the descriptions are truly equivalent then they will generate the same set of associations.
 *
 * @author Peter Abeles
 */
public class CompareConvertedDescriptionsApp {

	public static <TD extends TupleDesc>
	void visualize( String title ,
					BufferedImage image1, BufferedImage image2,
					InterestPointDetector<ImageFloat32> detector ,
					DescribeRegionPoint<ImageFloat32,TD> describe ,
					ScoreAssociation<TD> scorer ) {

		AssociateDescription<TD> assoc = FactoryAssociation.greedy(scorer,Double.MAX_VALUE,false);

		List<Point2D_F64> locationSrc = new ArrayList<Point2D_F64>();
		List<Point2D_F64> locationDst = new ArrayList<Point2D_F64>();

		ImageFloat32 input1 = ConvertBufferedImage.convertFrom(image1,(ImageFloat32)null);
		ImageFloat32 input2 = ConvertBufferedImage.convertFrom(image2,(ImageFloat32)null);

		FastQueue<TD> listSrc = describeImage(input1,detector,describe,locationSrc);
		FastQueue<TD> listDst = describeImage(input2,detector,describe,locationDst);

		assoc.setSource(listSrc);
		assoc.setDestination(listDst);
		assoc.associate();

		FastQueue<AssociatedIndex> matches = assoc.getMatches();

		AssociationPanel panel = new AssociationPanel(20);
		panel.setImages(image1,image2);
		panel.setAssociation(locationSrc,locationDst,matches);

		ShowImages.showWindow(panel,title);
	}

	public static <TD extends TupleDesc>
	FastQueue<TD>  describeImage( ImageFloat32 input ,
								  InterestPointDetector<ImageFloat32> detector ,
								  DescribeRegionPoint<ImageFloat32,TD> describe ,
								  List<Point2D_F64> location )
	{
		FastQueue<TD> list = new FastQueue<TD>(100,describe.getDescriptionType(),false);

		System.out.println("Detecting");
		detector.detect(input);
		System.out.println("Describing");
		describe.setImage(input);

		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			Point2D_F64 p = detector.getLocation(i);
			double scale = detector.getScale(i);
			double ori = detector.getOrientation(i);

			TD d = describe.createDescription();
			if( describe.process(p.x,p.y,ori,scale,d) ) {
				list.add( d );
				location.add( p.copy() );
			}
		}

		return list;
	}

	public static void main( String args[] ) {
		String file1 = "../data/evaluation/stitch/kayak_01.jpg";
		String file2 = "../data/evaluation/stitch/kayak_02.jpg";

		InterestPointDetector<ImageFloat32> detector =
				FactoryInterestPoint.fastHessian(new ConfigFastHessian(1,10,-1,2,9,4,4));

		DescribeRegionPoint<ImageFloat32,TupleDesc_F64> describeA =
				(DescribeRegionPoint)FactoryDescribeRegionPoint.surfStable(null, ImageFloat32.class);

		ConvertTupleDesc<TupleDesc_F64,TupleDesc_S8> converter =
				FactoryConvertTupleDesc.real_F64_S8(describeA.createDescription().size());

		DescribeRegionPoint<ImageFloat32,TupleDesc_S8> describeB =
				new DescribeRegionPointConvert<ImageFloat32,TupleDesc_F64,TupleDesc_S8>(describeA,converter);

		ScoreAssociation<TupleDesc_F64> scoreA = FactoryAssociation.scoreSad(TupleDesc_F64.class);
		ScoreAssociation<TupleDesc_S8> scoreB = FactoryAssociation.scoreSad(TupleDesc_S8.class);

		BufferedImage image1 = UtilImageIO.loadImage(file1);
		BufferedImage image2 = UtilImageIO.loadImage(file2);

		visualize("Original",image1,image2,detector,describeA,scoreA);
		visualize("Modified",image1,image2,detector,describeB,scoreB);

		System.out.println("Done");
	}
}
