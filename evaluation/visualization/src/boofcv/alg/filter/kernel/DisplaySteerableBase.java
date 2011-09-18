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

package boofcv.alg.filter.kernel;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.VisualizeImageData;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.image.ImageBase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Peter Abeles
 */
public abstract class DisplaySteerableBase<T extends ImageBase, K extends Kernel2D>
		extends JPanel implements ActionListener
{
	protected static int imageSize = 400;
	protected static int radius = 100;

	protected Class<T> imageType;
	protected Class<K> kernelType;

	ListDisplayPanel basisPanel = new ListDisplayPanel();
	ListDisplayPanel steerPanel = new ListDisplayPanel();

	JToolBar toolbar;
	JComboBox derivBox;

	T largeImg;

	List<DisplayGaussianKernelApp.DerivType> order = new ArrayList<DisplayGaussianKernelApp.DerivType>();

	public DisplaySteerableBase(Class<T> imageType, Class<K> kernelType) {
		super(new BorderLayout());

		this.imageType = imageType;
		this.kernelType = kernelType;

		largeImg = GeneralizedImageOps.createImage(imageType,imageSize,imageSize);

		toolbar = new JToolBar();
		derivBox = new JComboBox();
		toolbar.add(derivBox);

		congigureComboBox();

		add(toolbar, BorderLayout.PAGE_START);

		basisPanel.setPreferredSize(new Dimension(imageSize+100,imageSize));
		steerPanel.setPreferredSize(new Dimension(imageSize+60,imageSize));
		JPanel content = new JPanel(new GridLayout(0,2));
		content.add(basisPanel);
		content.add(steerPanel);
		add(content, BorderLayout.CENTER);

		derivBox.addActionListener(this);
		setActive(order.get(0));
	}

	protected abstract SteerableKernel<K> createKernel( int orderX , int orderY );

	protected void setActive( DisplayGaussianKernelApp.DerivType dt )
	{
		// add basis
		SteerableKernel<K> steerable = createKernel(dt.orderX,dt.orderY);
		basisPanel.reset();

		for( int i = 0; i < steerable.getBasisSize(); i++ ) {
			T smallImg = GKernelMath.convertToImage(steerable.getBasis(i));
			DistortImageOps.scale(smallImg,largeImg, TypeInterpolate.NEAREST_NEIGHBOR);

			double maxValue = GPixelMath.maxAbs(largeImg);
			BufferedImage out = VisualizeImageData.colorizeSign(largeImg,null,maxValue);
			basisPanel.addImage(out,"Basis "+i);
		}

		// add steered kernels
		steerPanel.reset();

		for( int i = 0; i <= 20; i++  ) {
			double angle = Math.PI*i/20.0;

			K kernel = steerable.compute(angle);

			T smallImg = GKernelMath.convertToImage(kernel);
			DistortImageOps.scale(smallImg,largeImg, TypeInterpolate.NEAREST_NEIGHBOR);

			double maxValue = GPixelMath.maxAbs(largeImg);
			BufferedImage out = VisualizeImageData.colorizeSign(largeImg,null,maxValue);

			steerPanel.addImage(out,String.format("%5d",(int)(180.0*angle/Math.PI)));
		}
		repaint();
	}

	private void congigureComboBox() {
		addDerivative("Deriv X",1,0);
		addDerivative("Deriv XX",2,0);
		addDerivative("Deriv XXX",3,0);
		addDerivative("Deriv XXXX",4,0);
		addDerivative("Deriv XY",1,1);
		addDerivative("Deriv XXY",2,1);
		addDerivative("Deriv XYY",1,2);
		addDerivative("Deriv XXXY",3,1);
		addDerivative("Deriv XXYY",2,2);
		addDerivative("Deriv XYYY",1,3);
	}

	private void addDerivative( String name , int x , int y ) {
		derivBox.addItem(name);
		order.add(new DisplayGaussianKernelApp.DerivType(x,y));
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int index = derivBox.getSelectedIndex();

		setActive(order.get(index));
	}
}
