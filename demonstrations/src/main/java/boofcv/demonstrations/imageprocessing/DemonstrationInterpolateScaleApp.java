/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.imageprocessing;

import boofcv.abst.distort.FDistort;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Scales an image using the specified interpolation method.
 *
 * @author Peter Abeles
 */
public class DemonstrationInterpolateScaleApp<T extends ImageBase<T>>
	extends DemonstrationBase implements ItemListener, ComponentListener
{
	private final T latestImage;
	private final T scaledImage;

	private final ImagePanel panel = new ImagePanel();
	private final JComboBox combo = new JComboBox();
	private volatile InterpolationType interpType = InterpolationType.values()[0];

	public DemonstrationInterpolateScaleApp(List<String> examples , ImageType<T> imageType ) {
		super(examples, imageType);

		panel.setScaling(ScaleOptions.NONE);

		latestImage = imageType.createImage(1,1);
		scaledImage = imageType.createImage(1,1);

		for( InterpolationType type : InterpolationType.values() ) {
			combo.addItem( type.toString() );
		}
		combo.addItemListener(this);

		menuBar.add(combo);
		panel.addComponentListener(this);
		add(BorderLayout.CENTER, panel);
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase input) {
		synchronized (latestImage) {
			latestImage.setTo((T)input);
			applyScaling();
		}
	}

	private void applyScaling() {
		scaledImage.reshape(panel.getWidth(), panel.getHeight());
		if( scaledImage.width <= 0 || scaledImage.height <= 0 ) {
			return;
		}

		if( latestImage.width != 0 && latestImage.height != 0 ) {
			new FDistort(latestImage, scaledImage).interp(interpType).border(BorderType.EXTENDED).scale().apply();
			BufferedImage out = ConvertBufferedImage.convertTo(scaledImage, null, true);

			panel.setImageUI(out);
			panel.repaint();
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		interpType = InterpolationType.values()[ combo.getSelectedIndex() ];
		synchronized (latestImage) {
			applyScaling();
		}
	}

	@Override
	public void componentResized(ComponentEvent e) {
		synchronized (latestImage) {
			if( inputMethod == InputMethod.IMAGE )
				applyScaling();
		}
	}

	@Override
	public void componentMoved(ComponentEvent e) {}

	@Override
	public void componentShown(ComponentEvent e) {}

	@Override
	public void componentHidden(ComponentEvent e) {}

	public static void main( String[] args ) {

		ImageType type = ImageType.pl(3,GrayF32.class);
//		ImageType type = ImageType.pl(3,GrayU8.class);
//		ImageType type = ImageType.single(GrayU8.class);
//		ImageType type = ImageType.il(3, InterleavedF32.class);

		List<String> examples = new ArrayList<>();
		examples.add( UtilIO.pathExample("eye01.jpg") );
		examples.add( UtilIO.pathExample("small_sunflower.jpg"));

		DemonstrationInterpolateScaleApp app = new DemonstrationInterpolateScaleApp(examples,type);
		app.setPreferredSize(new Dimension(500,500));

		app.openFile(new File(examples.get(0)));
		app.display("Interpolation Enlarge");
	}
}
