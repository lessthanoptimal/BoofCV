/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package boofcv.io.gstwebcamcapture;

/**
 * Interface for classes that produce buffered images
 * @author Devin Willis
 */
public interface ImageProducer {
    
    public ImageProducer addImageListener(ImageListener imageListener);
    
}
