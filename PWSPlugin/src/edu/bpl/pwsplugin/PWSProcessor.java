package edu.bpl.pwsplugin;

import org.micromanager.internal.utils.ReportingUtils;
import java.util.concurrent.LinkedBlockingQueue;
import mmcorej.StrVector;
import org.micromanager.data.Processor;
import org.micromanager.PropertyMap;
import org.micromanager.Studio;
import org.micromanager.data.ProcessorContext;
import org.micromanager.data.Image;
import org.micromanager.data.SummaryMetadata;
import org.micromanager.data.Metadata;
import org.micromanager.data.internal.DefaultMetadata;
import org.micromanager.alerts.UpdatableAlert;
import org.micromanager.internal.MMStudio;



public class PWSProcessor extends Processor {
 
    Studio studio_;
    int numAverages_;
    LinkedBlockingQueue imageQueue;
    boolean debugLogEnabled_ = true;
    Image[] imageArray;
    int[] wv;
    String filtLabel;
    String filtProp;
    Boolean hardwareSequence;
    String savePath;
    int delayMs;
    
    public PWSProcessor(Studio studio, PropertyMap settings){
        studio_ = studio;
        wv = settings.getIntegerList("wv");
        filtLabel = settings.getString("filtLabel", "");
        hardwareSequence = settings.getBoolean("sequence", false);
        savePath = settings.getString("savepath", "");
        delayMs = settings.getInteger("delayMs", 0);
        filtProp = "Wavelength";
        studio_.acquisitions().attachRunnable(-1, -1, -1, -1, new PWSRunnable(this)); 
        imageQueue = new LinkedBlockingQueue();
        
        if (hardwareSequence) {
            try {
                if (!studio_.core().isPropertySequenceable(filtLabel, filtProp)){
                    ReportingUtils.showError("The filter device does not have a sequenceable 'Wavelength' property.");
                }
                if (studio_.core().getPropertySequenceMaxLength(filtLabel, filtProp) < wv.length) {
                    ReportingUtils.showError("The filter device does not support sequencing as many wavelenghts as have been specified. Max is " + Integer.toString(studio_.core().getPropertySequenceMaxLength(filtLabel, filtProp)));
                }
                StrVector strv = new StrVector();
                for (int i = 0; i < wv.length; i++) {   //Convert wv from int to string for sending to the device.
                    strv.add(String.valueOf(wv[i]));
                }
                studio_.core().loadPropertySequence(filtLabel, filtProp, strv);
            }
            catch (Exception ex) {
                ReportingUtils.showError(ex);
            }
        }             
    }
    
    @Override
    public void cleanup(ProcessorContext context) {
            studio_.acquisitions().clearRunnables();
    }
    
    @Override
    public SummaryMetadata processSummaryMetadata(SummaryMetadata metadata) {
        SummaryMetadata.Builder builder = metadata.copyBuilder();
        builder.userName("PWSAcquisition");
        return builder.build();
    }
    
    @Override
    public void processImage(Image image, ProcessorContext context) {
        Image imageOnError = image;
        try { 
            if (studio_.live().getIsLiveModeOn()) { //Not supported
                context.outputImage(imageOnError); 
                return;
            }
            else {
                Metadata md = image.getMetadata();
                ImSaverRaw imsaver = new ImSaverRaw(studio_, savePath, imageQueue, (DefaultMetadata) md, wv, true);
                Image retIm;
                if (!studio_.acquisitions().isAcquisitionRunning()) { //This means we must be in snap mode. There is no runnable so we must acquire the image here.
                    acquireImages();
                }
                //Nothing extra needs to be done for acquisition mode
                if (debugLogEnabled_) {
                    ReportingUtils.logMessage("Queue has" + Integer.toString(imageQueue.size()));
                }
                context.outputImage(imageOnError);   //Return the middle image.
            }

        } catch (Exception ex) {
            context.outputImage(imageOnError);            
            ReportingUtils.logError("PWSPlugin, in processor: " + ex.toString());
            imageQueue.clear();
        }
    }

    
         
    public void acquireImages() {
        try {
            studio_.core().waitForDevice(studio_.core().getCameraDevice());
            studio_.core().clearCircularBuffer();
            String cam = studio_.core().getCameraDevice();
            long now = System.currentTimeMillis();
            
            if (hardwareSequence) {
    //          CMMCore::startSequenceAcquisition(long numImages, double intervalMs, bool stopOnOverflow)
    //          @param numImages Number of images requested from the camera
    //          @param intervalMs interval between images, currently only supported by Andor cameras
    //          @param stopOnOverflow whether or not the camera stops acquiring when the circular buffer is full
                studio_.core().startPropertySequence(filtLabel, filtProp);

                studio_.core().startSequenceAcquisition(wv.length, delayMs, false);

                int frame = 1;// keep 0 free for the image from engine
                // reference BurstExample.bsh

                boolean canExit = false;
                while (true) {
                    boolean remaining = (studio_.core().getRemainingImageCount() > 0);
                    boolean running = (studio_.core().isSequenceRunning(cam));
                    if ((!remaining) && (canExit)) {
                        studio_.core().stopSequenceAcquisition(); 
                        break;  //Everything is taken care of.
                    }
                    if (remaining) {    //Process images
                       imageQueue.add(studio_.data().convertTaggedImage(studio_.core().popNextTaggedImage()));
                       frame++;
                       /*
                        if (proc_.display_ != null) {
                            if (proc_.display_.acquisitionIsRunning()) {
                                proc_.display_.displayStatusLine("Image Avg. Acquiring No. " + frame);
                            }
                        }
                        */
                    }
                    if (!running) {
                        studio_.core().stopPropertySequence(filtLabel, filtProp);
                        canExit = true;
                    }
                 }
            }
            else {  //Software sequenced acquisition
                for (int i=0; i<wv.length; i++) {
                    studio_.core().setProperty(filtLabel, filtProp, wv[i]);
                    while (studio_.core().deviceBusy(filtLabel)) {Thread.sleep(1);} //Wait until the device says it is tuned.
                    studio_.core().snapImage();
                    imageQueue.add(studio_.data().convertTaggedImage(studio_.core().getTaggedImage()));
                }
                studio_.core().setProperty(filtLabel, filtProp, wv[0]);
            }
            long itTook = System.currentTimeMillis() - now;         
            if (debugLogEnabled_) {
                ReportingUtils.logMessage("PWS Acquisition took: " + itTook + " milliseconds for "+wv.length + " frames");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            ReportingUtils.logMessage("ERROR: PWSPlugin: " + ex.getMessage());
        }
    }
    
}