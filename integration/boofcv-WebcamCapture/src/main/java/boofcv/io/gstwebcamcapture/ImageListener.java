/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package boofcv.io.gstwebcamcapture;

import java.awt.image.BufferedImage;

/**
 * Image listener to receive new images from Image Producers
 * @author Devin Willis
 */
public interface ImageListener {
    
    public void newImage(BufferedImage image);
}
