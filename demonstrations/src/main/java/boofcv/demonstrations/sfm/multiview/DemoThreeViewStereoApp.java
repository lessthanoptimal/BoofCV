/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.sfm.multiview;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.feature.associate.AssociateThreeByPairs;
import boofcv.alg.sfm.structure.ThreeViewEstimateMetricScene;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.detdesc.FactoryDetectDescribe;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.image.ImagePanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.struct.feature.AssociatedTripleIndex;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class DemoThreeViewStereoApp<T extends ImageGray<T>> extends DemonstrationBase {

	ImagePanel gui = new ImagePanel();
	DemoThreeViewControls controls = new DemoThreeViewControls();

	DetectDescribePoint<T, BrightFeature> detDesc;
	ScoreAssociation<BrightFeature> scorer = FactoryAssociation.scoreEuclidean(BrightFeature.class,true);
	AssociateDescription<BrightFeature> associate = FactoryAssociation.greedy(scorer, 0.1, true);

	AssociateThreeByPairs<BrightFeature> associateThree = new AssociateThreeByPairs<>(associate,BrightFeature.class);
	FastQueue<AssociatedTriple> associated = new FastQueue<>(AssociatedTriple.class,true);

	ThreeViewEstimateMetricScene structureEstimator = new ThreeViewEstimateMetricScene();

	FastQueue<Point2D_F64> locations[] = new FastQueue[3];
	FastQueue<BrightFeature> features[] = new FastQueue[3];
	ImageDimension dimensions[] = new ImageDimension[3];

	public DemoThreeViewStereoApp(List<PathLabel> examples,
								  Class<T> imageType ) {
		super(false, false, examples, ImageType.single(imageType));

		detDesc = FactoryDetectDescribe.surfStable( new ConfigFastHessian(
				0, 4, 1000, 1, 9, 4, 2), null,null, imageType);

		for (int i = 0; i < 3; i++) {
			locations[i] = new FastQueue<>(Point2D_F64.class,true);
			features[i] = UtilFeature.createQueue(detDesc,100);
			dimensions[i] = new ImageDimension();
		}

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, gui);

		setPreferredSize(new Dimension(1200,800));
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase input) {

		if( sourceID == 0 )
			gui.setImageRepaint(buffered);

		dimensions[sourceID].set(input.width,input.height);

		// assume the image center is the principle point
		double cx = input.width/2;
		double cy = input.height/2;

		// detect features
		detDesc.detect((T)input);
		locations[sourceID].reset();
		features[sourceID].reset();

		// save results
		for (int i = 0; i < detDesc.getNumberOfFeatures(); i++) {
			Point2D_F64 pixel = detDesc.getLocation(i);
			locations[sourceID].grow().set(pixel.x-cx,pixel.y-cy);
			features[sourceID].grow().setTo(detDesc.getDescription(i));
		}

		if( sourceID < 2 )
			return;

		associateThree.setFeaturesA(features[0]);
		associateThree.setFeaturesB(features[1]);
		associateThree.setFeaturesC(features[2]);
		associateThree.associate();

		FastQueue<AssociatedTripleIndex> associatedIdx = associateThree.getMatches();
		associated.reset();
		for (int i = 0; i < associatedIdx.size; i++) {
			AssociatedTripleIndex p = associatedIdx.get(i);
			associated.grow().set(locations[0].get(p.a),locations[1].get(p.b),locations[2].get(p.c));
		}

		if( !structureEstimator.process(associated.toList(),input.width,input.height) )
			return;

		System.out.println("Success!");
	}


	private static PathLabel createExample( String name ) {
		String path0 = UtilIO.pathExample("triple/"+name+"_01.jpg");
		String path1 = UtilIO.pathExample("triple/"+name+"_01.jpg");
		String path2 = UtilIO.pathExample("triple/"+name+"_01.jpg");

		return new PathLabel(name,path0,path1,path2);
	}

	public static void main(String[] args) {
		List<PathLabel> examples = new ArrayList<>();

		examples.add(createExample("rock_leaves"));
		examples.add(createExample("rockview"));
		examples.add(createExample("mono_wall"));
		examples.add(createExample("bobcats"));
		examples.add(createExample("books"));
		examples.add(createExample("chicken"));
		examples.add(createExample("minecraft_cave1"));
		examples.add(createExample("minecraft_distant"));
		examples.add(createExample("skull"));
		examples.add(createExample("triflowers"));
		examples.add(createExample("turkey"));

		SwingUtilities.invokeLater(()->{
			DemoThreeViewStereoApp app = new DemoThreeViewStereoApp(examples,GrayU8.class);

			app.openExample(examples.get(0));
			app.display("QR-Code Detector");
		});
	}
}
