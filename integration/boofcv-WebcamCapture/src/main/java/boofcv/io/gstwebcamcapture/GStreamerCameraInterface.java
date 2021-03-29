/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package boofcv.io.gstwebcamcapture;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import org.freedesktop.gstreamer.Pipeline;
/**
 *
 * @author techgarage
 */
public interface GStreamerCameraInterface {
    public Pipeline getPipeline();
    public boolean setPipeline(Pipeline pipeline);
    public boolean open();
    public boolean addCameraOpenListener(CameraOpenListenerInterface manager);
    public String getPipelineString();
    public String getName();
    public GStreamerImagePipeline getImagePipeline();
    public boolean setImagePipeline(GStreamerImagePipeline imagePipeline);
    public Dimension getResolution();
    public Dimension getViewSize();
    public Dimension[] getCustomViewSizes();
    public boolean setCustomViewSizes(Dimension[] dimensionList);
    public boolean setViewSize(Dimension resolution);
    public GStreamerCameraInterface getDevice();
    public BufferedImage getImage();
    public boolean close();
    public boolean addCameraCloseListener(CameraClosedListenerInterface manager);
}
