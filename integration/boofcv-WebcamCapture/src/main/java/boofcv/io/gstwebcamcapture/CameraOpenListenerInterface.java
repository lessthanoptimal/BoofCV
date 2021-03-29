/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package boofcv.io.gstwebcamcapture;

/**
 *
 * @author techgarage
 */
public interface CameraOpenListenerInterface {
    public void cameraOpened(GStreamerCameraInterface cam);
}
