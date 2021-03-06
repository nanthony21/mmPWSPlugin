/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.imgSharpnessPlugin;

import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.PixelMath;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import com.google.common.base.Preconditions;
import com.google.common.eventbus.Subscribe;
import ij.gui.Roi;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.micromanager.Studio;
import org.micromanager.data.DataProviderHasNewImageEvent;
import org.micromanager.data.Image;
import org.micromanager.data.internal.DefaultImage;
import org.micromanager.display.DataViewer;
import org.micromanager.display.DisplayWindow;
import org.micromanager.display.inspector.AbstractInspectorPanelController;
import org.micromanager.display.inspector.internal.panels.intensity.ImageStatsPublisher;
import org.micromanager.events.StagePositionChangedEvent;
import org.micromanager.internal.utils.MustCallOnEDT;

/**
 *
 * @author N2-LiveCell
 */
public class SharpnessInspectorController extends AbstractInspectorPanelController {
    private boolean expanded_ = true;
    private final SharpnessInspectorPanel panel_ = new SharpnessInspectorPanel();
    private DataViewer viewer_;
    private final Studio studio_;
    private int denoiseRadius = 3; 
    private boolean autoImageEvaluation_ = true;
    
    private SharpnessInspectorController(Studio studio) {
        studio_ = studio;
        studio_.events().registerForEvents(this);
        
        panel_.setDenoiseRadius(denoiseRadius);
        panel_.addDenoiseRadiusValueChangedListener((evt) -> {
            this.denoiseRadius = ((Long) evt.getNewValue()).intValue();
        });
        
        panel_.addScanRequestedListener((evt) -> {
            SwingWorker worker = new SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    SharpnessInspectorController.this.beginScan(evt.intervalUm(), evt.rangeUm());
                    return null;
                }
            };
            
            worker.execute();
            
        });
    }
    
    public static SharpnessInspectorController create(Studio studio) {
        return new SharpnessInspectorController(studio);
    }

    @Override
    public String getTitle() {
       return "Image Sharpness";
    }

    @Override
    public JPanel getPanel() {
       return panel_;
    }

    @Override
    @MustCallOnEDT
    public void attachDataViewer(DataViewer viewer) {
       Preconditions.checkNotNull(viewer);
       if (!(viewer instanceof ImageStatsPublisher)) {
          throw new IllegalArgumentException("Programming error");
       }
       detachDataViewer();
       viewer_ = viewer;
       viewer.registerForEvents(this);
       viewer.getDataProvider().registerForEvents(this);
    }

    @Override
    @MustCallOnEDT
    public void detachDataViewer() {
       if (viewer_ == null) {
          return;
       }
       viewer_.getDataProvider().unregisterForEvents(this);
       viewer_.unregisterForEvents(this);
       //setUpChannelHistogramsPanel(0);
       viewer_ = null;
    }

    @Override
    public boolean isVerticallyResizableByUser() {
       return true;
    }


    @Override
    public void setExpanded(boolean status) {
       expanded_ = status;
    }

    @Override
    public boolean initiallyExpand() {
       return expanded_;
    }
    
    @Subscribe
    public void onNewImage(DataProviderHasNewImageEvent evt) {
        ///This is fired because we register for the dataprovider events. Happens each time a new image is available from the provider.
        if (!this.autoImageEvaluation_) {
            return;
        }
        DefaultImage img = (DefaultImage) evt.getImage();
        Roi roi = ((DisplayWindow) viewer_).getImagePlus().getRoi();
        if (roi == null || !roi.isArea()) {
            this.panel_.setRoiSelected(false);
            return;
        }
        this.panel_.setRoiSelected(true);
        Rectangle r = roi.getBounds();
        if (r.width < 5 || r.height < 5) {
            return; //Rectangle must be larger than the kernel used to calculate gradient which is 1x3
        }
        double grad = evaluateGradient(img, r);
        double z = img.getMetadata().getZPositionUm();
        this.panel_.setValue(z, grad);
    }
    
    @Subscribe
    public void onZPosChanged(StagePositionChangedEvent evt) { //TODO Many z stages don't fire this. use polling instead
        if (!studio_.core().getFocusDevice().equals(evt.getDeviceName())) {
            return; //Stage device names don't match. We only want to use the default focus device.
        }
        this.panel_.setZPos(evt.getPos());
    }
    
    private double evaluateGradient(Image img, Rectangle r) {
        GrayF32 im = new GrayF32(r.width, r.height);
        for (int i=0; i<r.width; i++) {
            for (int j=0; j<r.height; j++) {
                long intensity = img.getIntensityAt(r.x + i, r.y + j);
                im.set(i, j, (int) intensity);
            }
        }
        GrayF32 blurred = BlurImageOps.gaussian(im, null, -1, this.denoiseRadius, null);
        GrayF32 dx = new GrayF32(im.width, im.height);
        GrayF32 dy = new GrayF32(im.width, im.height);
        GImageDerivativeOps.gradient(DerivativeType.THREE, blurred, dx, dy, BorderType.EXTENDED);
        //Calculate magnitude of the gradient
        PixelMath.pow2(dx, dx);
        PixelMath.pow2(dy, dy);
        GrayF32 mag = new GrayF32(dx.width, dx.height);
        PixelMath.add(dx, dy, mag);
        PixelMath.sqrt(mag, mag);
        float[] arr = mag.getData();
        double[] dubArr = new double[arr.length];
        for (int i = 0; i < arr.length; i++) { // must convert from float[] to double[]
            dubArr[i] = arr[i];
        }
        return new Percentile().evaluate(dubArr, 95);
    }
    
    private void beginScan(double intervalUm, double rangeUm) {
        this.panel_.clearData();
        this.autoImageEvaluation_ = false;
        
        if (studio_.live().getIsLiveModeOn()) {
            studio_.live().setLiveMode(false);
        }
        
        Roi roi = ((DisplayWindow) viewer_).getImagePlus().getRoi();
        Rectangle r;
        if (roi == null || !roi.isArea()) {
            r = new Rectangle(  // use full image fov
                    ((DisplayWindow) viewer_).getImagePlus().getWidth(),
                    ((DisplayWindow) viewer_).getImagePlus().getHeight());
        } else {
            r = roi.getBounds();
            //Rectangle must be larger than the kernel used to calculate gradient which is 1x3
            if (r.width < 5) {
                r.setSize(5, r.height);
            } if (r.height < 5) {
                r.setSize(r.width, 5);
            }
        }
        //TODO we should use the PWS Plugin ZStage device not the default one. How is that going to work?
        try {
            long numSteps = Math.round(rangeUm / intervalUm);
            double startingPos = studio_.core().getPosition();
            studio_.core().setRelativePosition(-(rangeUm/2.0)); // Move down by half of the range so that the scan is centered at the starting point.
            while (studio_.core().deviceBusy(studio_.core().getFocusDevice())) { // make sure we moved
                Thread.sleep(50);
            }
            for (int i=0; i<numSteps; i++) {
                studio_.core().setRelativePosition(intervalUm);
                while (studio_.core().deviceBusy(studio_.core().getFocusDevice())) { // make sure we moved
                    Thread.sleep(50);
                }
                
                Image img = studio_.live().snap(true).get(0);
                double sharpness = this.evaluateGradient(img, r);
                
                double pos = studio_.core().getPosition();
                panel_.setValue(pos, sharpness);
            }
            studio_.core().setPosition(startingPos);
        } catch (Exception e) {
            studio_.logs().showError(e);
        } finally {
            this.autoImageEvaluation_ = true;
        }
    }
}


class RequestScanEvent extends ActionEvent {
    private final double interval;
    private final double range;
    
    public RequestScanEvent(Object source, double intervalUm, double rangeUm) {
        super(source, 0, "startScan");
        interval = intervalUm;
        range = rangeUm;
    }
    
    public double intervalUm() { return interval; }
    public double rangeUm() { return range; }
}


interface RequestScanListener {
    public void actionPerformed(RequestScanEvent evt);
}