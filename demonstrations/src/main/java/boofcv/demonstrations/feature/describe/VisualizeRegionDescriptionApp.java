/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.feature.describe;

import boofcv.abst.feature.describe.ConfigBrief;
import boofcv.abst.feature.describe.DescribePointRadiusAngle;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.describe.FactoryDescribePointRadiusAngle;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.feature.SelectRegionDescriptionPanel;
import boofcv.gui.feature.TupleDescPanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I32;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Allows the user to select a point and show the description of the region at that point
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class VisualizeRegionDescriptionApp<T extends ImageGray<T>>
		extends DemonstrationBase implements SelectRegionDescriptionPanel.Listener {
	BufferedImage image;

	private final Object lock = new Object();
	private DescribePointRadiusAngle describe;

	private SelectRegionDescriptionPanel panel = new SelectRegionDescriptionPanel();
	private Controls controls = new Controls();

	private TupleDescPanel tuplePanel = new TupleDescPanel();

	// most recently requested pixel description. Used when the algorithm is changed
	private @Nullable Point2D_I32 targetPt;
	private double targetRadius;
	private double targetOrientation;

	T gray;

	public VisualizeRegionDescriptionApp( java.util.List<PathLabel> examples, Class<T> imageType ) {
		super(examples, ImageType.pl(3, imageType));

		gray = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		panel.setListener(this);
		tuplePanel.setPreferredSize(new Dimension(100, 50));

		createAlgorithm();

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, panel);
		add(BorderLayout.SOUTH, tuplePanel);
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		panel.setPreferredSize(new Dimension(width, height));
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage bufferedIn, ImageBase input ) {
		this.image = bufferedIn;

		GConvertImage.convert(input, gray);

		synchronized (lock) {
			if (describe != null) {
				if (describe.getImageType().getFamily() == ImageType.Family.PLANAR) {
					describe.setImage(input);
				} else {
					describe.setImage(gray);
				}
			}
		}

		SwingUtilities.invokeLater(() -> {
			panel.setBackground(image);
			panel.repaint();
			updateTargetDescription();
		});
	}

	@Override
	public synchronized void descriptionChanged( @Nullable Point2D_I32 pt, double radius, double orientation ) {
		if (pt == null || radius < 1) {
			targetPt = null;
		} else {
			this.targetPt = pt;
			this.targetRadius = radius;
			this.targetOrientation = orientation;
		}
		updateTargetDescription();
	}

	/**
	 * Extracts the target description and updates the panel. Should only be called from a swing thread
	 */
	private void updateTargetDescription() {
		synchronized (lock) {
			if (targetPt != null) {
				TupleDesc feature = describe.createDescription();
				describe.process(targetPt.x, targetPt.y, targetOrientation, targetRadius, feature);
				tuplePanel.setDescription(feature);
			} else {
				tuplePanel.setDescription(null);
			}
		}
		tuplePanel.repaint();
	}

	private void createAlgorithm() {
		Class<T> imageType = super.getImageType(0).getImageClass();
		synchronized (lock) {
			describe = switch (controls.selectedDescriptor) {
				case 0 -> FactoryDescribePointRadiusAngle.surfStable(null, imageType);
				case 1 -> FactoryDescribePointRadiusAngle.surfColorStable(null, ImageType.pl(3, imageType));
				case 2 -> FactoryDescribePointRadiusAngle.sift(null, null, imageType);
				case 3 -> FactoryDescribePointRadiusAngle.brief(new ConfigBrief(true), imageType);
				case 4 -> FactoryDescribePointRadiusAngle.brief(new ConfigBrief(false), imageType);
				case 5 -> FactoryDescribePointRadiusAngle.pixel(5, 5, imageType);
				case 6 -> FactoryDescribePointRadiusAngle.pixelNCC(5, 5, imageType);
				default -> throw new IllegalArgumentException("Unknown selection");
			};
		}
	}

	class Controls extends StandardAlgConfigPanel implements ActionListener {
		int selectedDescriptor = 0;
		JComboBox<String> comboDescribe = combo(selectedDescriptor, "SURF-S", "SURF-S Color", "SIFT", "BRIEF", "BRIEFSO", "Pixel 5x5", "NCC 5x5");

		Controls() {
			addAlignLeft(comboDescribe);
		}

		@Override
		public void actionPerformed( ActionEvent e ) {
			if (e.getSource() == comboDescribe) {
				selectedDescriptor = comboDescribe.getSelectedIndex();
				createAlgorithm();
				reprocessInput();
			}
		}
	}

	public static void main( String[] args ) {
		java.util.List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("Cave", UtilIO.pathExample("stitch/cave_01.jpg")));
		inputs.add(new PathLabel("Kayak", UtilIO.pathExample("stitch/kayak_02.jpg")));
		inputs.add(new PathLabel("Forest", UtilIO.pathExample("scale/rainforest_01.jpg")));

		SwingUtilities.invokeLater(() -> {
			var app = new VisualizeRegionDescriptionApp<>(inputs, GrayF32.class);

			// Processing time takes a bit so don't open right away
			app.openExample(inputs.get(0));
			app.display("Region Descriptor Visualization");
		});
	}
}
