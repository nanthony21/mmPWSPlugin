///////////////////////////////////////////////////////////////////////////////
//PROJECT:       PWS Plugin for Micro-Manager
//
//-----------------------------------------------------------------------------
//
// AUTHOR:      Nick Anthony 2019
//
// COPYRIGHT:    Northwestern University, Evanston, IL 2019
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
//

package edu.bpl.pwsplugin.acquisitionManagers;

import edu.bpl.pwsplugin.Globals;
import edu.bpl.pwsplugin.PWSAlbum;
import edu.bpl.pwsplugin.fileSavers.MMSaver;
import edu.bpl.pwsplugin.hardware.cameras.Camera;
import edu.bpl.pwsplugin.hardware.tunableFilters.TunableFilter;
import edu.bpl.pwsplugin.settings.PWSPluginSettings;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import org.micromanager.internal.utils.ReportingUtils;
import java.util.concurrent.LinkedBlockingQueue;
import org.micromanager.data.Image;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONArray;
import org.json.JSONObject;
import org.micromanager.data.Coords;
import org.micromanager.data.Pipeline;
import org.micromanager.data.PipelineErrorException;


public class PWSAcqManager implements AcquisitionManager{
    int[] wv; //The array of wavelengths to image at.
    final String filtProp  = "Wavelength"; //The property name of the filter that we want to tune.
    Boolean hardwareSequence; // Whether or not to attempt to use TTL triggering between the camera and spectral filter.
    Boolean useExternalTrigger; // Whether or not to let the spectral filter TTL trigger a new camera frame when it is done tuning.
    double exposure_; // The camera exposure.
    PWSAlbum album_;
    PWSPluginSettings.HWConfiguration config;
    
    public PWSAcqManager(PWSAlbum album, PWSPluginSettings.HWConfiguration config) {
        album_ = album;
        this.config = config;
    }
    
    public void setSequenceSettings(PWSPluginSettings.PWSSettings settings) throws Exception {
        TunableFilter filter = this.config.imagingConfig.tunableFilter();
        exposure_ = settings.exposure;
        useExternalTrigger = settings.externalCamTriggering;
        wv = settings.getWavelengthArray();
        hardwareSequence =  settings.ttlTriggering;           
        
        if (hardwareSequence) {
            if (!filter.supportsSequencing()) {
                throw new Exception("The filter device does not support hardware TTL sequencing.");
            }
            if (filter.getMaxSequenceLength() < wv.length) {
                throw new Exception("The filter device does not support sequencing as many wavelengths as have been specified. Max is " + filter.getMaxSequenceLength());
            }
            filter.loadSequence(wv);
        }  
    }
    
    @Override
    public int getExpectedFrames() {
        return wv.length;
    }
    
    @Override
    public String getFilePrefix() {
        return "pws";
    }
    
    @Override
    public String getSavePath(String savePath, int cellNum) throws FileAlreadyExistsException{
        Path path = Paths.get(savePath).resolve("Cell" + String.valueOf(cellNum)).resolve("PWS");
        if (Files.isDirectory(path)){
            throw new FileAlreadyExistsException("Cell " + cellNum + " PWS already exists.");
        } 
        return path.toString();
    }
      
    @Override
    public void acquireImages(String savePath, int cellNum, LinkedBlockingQueue imagequeue, JSONObject metadata) {
        long configStartTime = System.currentTimeMillis();
        try {album_.clear();} catch (IOException e) {ReportingUtils.logError(e, "Error from PWSALBUM");}
        int initialWv = 550;
        Camera camera = this.config.imagingConfig.camera();
        TunableFilter filter = this.config.imagingConfig.tunableFilter();
        try {    
            initialWv = filter.getWavelength(); //Get initial wavelength
            Globals.core().clearCircularBuffer();     
            camera.setExposure(exposure_);
            Pipeline pipeline = Globals.mm().data().copyApplicationPipeline(Globals.mm().data().createRAMDatastore(), true);
            
            //Prepare metadata and start imsaver
            JSONArray WV = new JSONArray();
            for (int i = 0; i < wv.length; i++) {
                WV.put(wv[i]);
            }        
            metadata.put("wavelengths", WV);
            metadata.put("exposure", camera.getExposure()); //This must happen after we have set the camera to our desired exposure.
            MMSaver imSaver_ = new MMSaver(this.getSavePath(savePath, cellNum), imagequeue, this.getExpectedFrames(), this.getFilePrefix());
            imSaver_.setMetadata(metadata);
            imSaver_.start();
            
            long seqEndTime=0;
            long collectionEndTime=0;
            long seqStartTime=0;
            if (hardwareSequence) {
                try {
                    double delayMs = filter.getDelayMs(); //Use the delay defined by the tunable filter's device adapter.
                    if (useExternalTrigger) {
                        camera.startSequence(this.wv.length, delayMs, true);
                        filter.startSequence(); //This should trigger a pulse which sets the whole thing off. 
                    }
                    else { //Since we're not using an external trigger we need to have the camera control the timing.
                        filter.startSequence();
                        camera.startSequence(this.wv.length, delayMs, false);
                    }
                    seqStartTime = System.currentTimeMillis();
                    
                    boolean canExit = false;
                    int i = 0;
                    int oldi = -1;
                    long lastImTime = System.currentTimeMillis();
                    while (true) {
                        boolean remaining = (Globals.core().getRemainingImageCount() > 0);
                        boolean running = (Globals.core().isSequenceRunning(camera.getName()));
                        if ((!remaining) && (canExit)) {
                            break;  //Everything is taken care of.
                        }
                        if (remaining) {    //Process images
                            Image im = Globals.mm().data().convertTaggedImage(Globals.core().popNextTaggedImage());
                            addImage(im, i, album_, pipeline, imSaver_.queue);
                            i++;
                            lastImTime = System.currentTimeMillis();
                            collectionEndTime = System.currentTimeMillis();
                        }
                        if ((System.currentTimeMillis() - lastImTime) > 10000) { //Check for timeout if for some reason the acquisition is stalled.
                            seqEndTime = System.currentTimeMillis();
                            ReportingUtils.showError("PWSAcquisition timed out while waiting for images from camera.");
                            canExit = true;
                        }
                        if (!running) {
                            seqEndTime = System.currentTimeMillis();
                            canExit = true;
                        }
                    }
                }
                finally {
                    try {
                        camera.stopSequence();
                        filter.stopSequence();//Got to make sure to stop the sequencing behaviour.
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        ReportingUtils.logMessage("ERROR: PWSPlugin: Stopping property sequence: " + ex.getMessage());
                    }
                    String timeMsg = "PWSPlugin: Hardware Sequenced Acq: ConfigurationTime:" + (seqStartTime-configStartTime)/1000.0 + "HWAcqTime:"+(seqEndTime-seqStartTime)/1000.0+"ImgCollectionTime:"+(collectionEndTime-seqEndTime)/1000.0;
                    ReportingUtils.logMessage(timeMsg);
                }
            }
            else {  //Software sequenced acquisition
                for (int i=0; i<wv.length; i++) {
                    filter.setWavelength(wv[i]);
                    while (filter.isBusy()) {Thread.sleep(1);} //Wait until the device says it is tuned.
                    Image im = camera.snapImage(); //TODO what if the camera is not the core image.
                    addImage(im, i, album_, pipeline, imSaver_.queue);
                }
            }
            imSaver_.join();
        } catch (Exception ex) {
            ex.printStackTrace();
            ReportingUtils.logMessage("ERROR: PWSPlugin: " + ex.getMessage());
        } finally {
            try{
                filter.setWavelength(initialWv); //Set back to initial wavelength
            } catch (Exception ex) {
                ReportingUtils.showError(ex);
                ex.printStackTrace();
                ReportingUtils.logMessage("ERROR: PWSPlugin: " + ex.getMessage());
            }
        }
    }
    
    
    private void addImage(Image im, int idx, PWSAlbum album, Pipeline pipeline, LinkedBlockingQueue imageQueue) throws IOException, PipelineErrorException{
        Coords newCoords = im.getCoords().copyBuilder().t(idx).build();
        im = im.copyAtCoords(newCoords);
        pipeline.insertImage(im); //Add image to the data pipeline for processing
        im = pipeline.getDatastore().getImage(newCoords); //Retrieve the processed image.
        album.addImage(im); //Add the image to the album for display
        imageQueue.add(im); //Add the image to a queue for multithreaded saving.
    }
}