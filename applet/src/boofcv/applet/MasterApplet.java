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

package boofcv.applet;

import boofcv.alg.filter.derivative.ShowImageDerivative;
import boofcv.gui.image.ShowImages;
import boofcv.struct.image.ImageFloat32;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;


/**
 * @author Peter Abeles
 */
public class MasterApplet extends JApplet {

	Class inputType = ImageFloat32.class;
	Class derivType = ImageFloat32.class;

	BufferedImage image;

	@Override
	public void init() {
		image = loadImage("data/indoors01.jpg");

//		ShowImages.showWindow(image,"Image");
		ShowImageDerivative deriv = new ShowImageDerivative(inputType,derivType);

		deriv.process(image);
//		ShowImages.showWindow(deriv,"Derivative");
		getContentPane().add(deriv,BorderLayout.CENTER);
	}

//	@Override
//	public void paint(Graphics g) {
//		Graphics2D g2 = (Graphics2D)g;
//
//		if( image == null ) {
//			g.setColor(Color.RED);
//		} else {
//			g2.drawImage(image,0,0,image.getWidth(),image.getHeight(),null);
//			g.setColor(Color.BLACK);
//		}
//
//		g.fillOval(50,50,100,100);
//		g.setColor(Color.BLUE);
//		g.fillOval(75,75,125,125);
//	}

	public BufferedImage loadImage( String path ) {
		try {
		   URL url = new URL(getCodeBase(), path);
		   return ImageIO.read(url);
		} catch (IOException e) {
			System.err.println("Failed to load image: "+path);
			return null;
		}
	}

	@Override
	public String getAppletInfo() {
		return "Draws a sin graph.";
	}
}
