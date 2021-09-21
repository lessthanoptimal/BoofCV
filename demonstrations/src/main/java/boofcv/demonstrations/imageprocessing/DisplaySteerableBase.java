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

package boofcv.demonstrations.imageprocessing;

import boofcv.abst.distort.FDistort;
import boofcv.alg.filter.kernel.GKernelMath;
import boofcv.alg.filter.kernel.SteerableKernel;
import boofcv.alg.misc.GImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.SelectAlgorithmPanel;
import boofcv.gui.image.VisualizeImageData;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.image.ImageGray;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Visualizes steerable kernels.
 *
 * @author Peter Abeles
 */
public abstract class DisplaySteerableBase<T extends ImageGray<T>, K extends Kernel2D>
		extends SelectAlgorithmPanel {
	protected static int imageSize = 400;
	protected static int radius = 100;

	protected Class<T> imageType;
	protected Class<K> kernelType;

	ListDisplayPanel basisPanel = new ListDisplayPanel();
	ListDisplayPanel steerPanel = new ListDisplayPanel();

	T largeImg;

	List<DisplayGaussianKernelApp.DerivType> order = new ArrayList<>();

	protected DisplaySteerableBase( Class<T> imageType, Class<K> kernelType ) {
		this.imageType = imageType;
		this.kernelType = kernelType;

		largeImg = GeneralizedImageOps.createSingleBand(imageType, imageSize, imageSize);

		addAlgorithm("Deriv X", new DisplayGaussianKernelApp.DerivType(1, 0));
		addAlgorithm("Deriv XX", new DisplayGaussianKernelApp.DerivType(2, 0));
		addAlgorithm("Deriv XXX", new DisplayGaussianKernelApp.DerivType(3, 0));
		addAlgorithm("Deriv XXXX", new DisplayGaussianKernelApp.DerivType(4, 0));
		addAlgorithm("Deriv XY", new DisplayGaussianKernelApp.DerivType(1, 1));
		addAlgorithm("Deriv XXY", new DisplayGaussianKernelApp.DerivType(2, 1));
		addAlgorithm("Deriv XYY", new DisplayGaussianKernelApp.DerivType(1, 2));
		addAlgorithm("Deriv XXXY", new DisplayGaussianKernelApp.DerivType(3, 1));
		addAlgorithm("Deriv XXYY", new DisplayGaussianKernelApp.DerivType(2, 2));
		addAlgorithm("Deriv XYYY", new DisplayGaussianKernelApp.DerivType(1, 3));

		JPanel content = new JPanel(new GridLayout(0, 2));
		content.add(basisPanel);
		content.add(steerPanel);
		setMainGUI(content);
	}

	protected abstract SteerableKernel<K> createKernel( int orderX, int orderY );

	@Override
	public void setActiveAlgorithm( String name, Object cookie ) {
		DisplayGaussianKernelApp.DerivType dt = (DisplayGaussianKernelApp.DerivType)cookie;

		// add basis
		SteerableKernel<K> steerable = createKernel(dt.orderX, dt.orderY);
		basisPanel.reset();

		for (int i = 0; i < steerable.getBasisSize(); i++) {
			T smallImg = GKernelMath.convertToImage(steerable.getBasis(i));
			new FDistort(smallImg, largeImg).scaleExt().interpNN().apply();

			double maxValue = GImageStatistics.maxAbs(largeImg);
			BufferedImage out = VisualizeImageData.colorizeSign(largeImg, null, maxValue);
			basisPanel.addImage(out, "Basis " + i);
		}

		// add steered kernels
		steerPanel.reset();

		for (int i = 0; i <= 20; i++) {
			double angle = Math.PI*i/20.0;

			K kernel = steerable.compute(angle);

			T smallImg = GKernelMath.convertToImage(kernel);
			new FDistort(smallImg, largeImg).scaleExt().interpNN().apply();

			double maxValue = GImageStatistics.maxAbs(largeImg);
			BufferedImage out = VisualizeImageData.colorizeSign(largeImg, null, maxValue);

			steerPanel.addImage(out, String.format("%5d", (int)(180.0*angle/Math.PI)));
		}
		repaint();
	}
}
