/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.UI;

import com.cureos.numerics.Calcfc;
import com.cureos.numerics.Cobyla;
import com.cureos.numerics.CobylaExitStatus;
import edu.bpl.pwsplugin.Globals;
import edu.bpl.pwsplugin.UI.utils.PWSAlbum;
import edu.bpl.pwsplugin.hardware.HWConfiguration;
import edu.bpl.pwsplugin.hardware.MMDeviceException;
import edu.bpl.pwsplugin.hardware.configurations.ImagingConfiguration;
import edu.bpl.pwsplugin.settings.ImagingConfigurationSettings;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import net.miginfocom.swing.MigLayout;
import org.micromanager.data.Image;

/**
 *
 * @author Nick Anthony <nickmanthony at hotmail.com>
 */

class UtilityPanel extends JPanel {
    public UtilityPanel() {
        super(new MigLayout());
        
        ExposurePanel exp = new ExposurePanel();
        exp.setBorder(BorderFactory.createEtchedBorder());
        
        this.add(new JLabel("<html><B>Auto Exposure</B></html>"), "wrap");
        this.add(exp);
    }
}

class ExposurePanel extends JPanel implements PropertyChangeListener {
    JSpinner wv = new JSpinner(new SpinnerNumberModel(550, 400, 1000, 10));
    JSpinner targetIntensity = new JSpinner(new SpinnerNumberModel(90, 0, 100, 10)); //Expressed as a percentage of max range of the camera.
    JComboBox<String> config = new JComboBox<>();
    JButton runBtn = new JButton("Run");
    JTextField exposureText = new JTextField();
    PWSAlbum display = new PWSAlbum("Auto Exposure");
            
    public ExposurePanel() {
        super(new MigLayout());
        Globals.addPropertyChangeListener(this);

        exposureText.setEditable(false);
        
        this.add(new JLabel("Configuration:"), "gapleft push");
        this.add(config, "wrap, growx");
        this.add(new JLabel("Wavelength:"), "gapleft push");
        this.add(wv, "wrap, growx");
        this.add(new JLabel("Target Intensity (%):"), "gapleft push");
        this.add(targetIntensity, "wrap, growx");
        this.add(runBtn, "wrap, span, align center, growx");
        this.add(new JLabel("Exposure (ms):"), "gapleft push");
        this.add(exposureText, "growx");
        
        config.addItemListener((evt)->{
            boolean spectral = Globals.getHardwareConfiguration().getImagingConfigurationByName((String) evt.getItem()).hasTunableFilter();
            wv.setEnabled(spectral); //Disable if we aren't using a spectral camera.
        });
        
        runBtn.addActionListener((evt)->{
            ImagingConfiguration conf = Globals.getHardwareConfiguration().getImagingConfigurationByName((String) config.getSelectedItem());
            try {
                display.clear();
                AutoExposeController aec = new AutoExposeController(conf, (Integer) wv.getValue());
                Double exposure = aec.run((Integer) targetIntensity.getValue());
                exposureText.setText(String.format("%.2f", exposure));
            } catch (MMDeviceException | IOException e) {
                Globals.mm().logs().showError(e);
            }
        });
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        //We subscribe to the Globals property changes. This gets fired when a change is detected.
        if (evt.getPropertyName().equals("config")) {
            HWConfiguration cfg = (HWConfiguration) evt.getNewValue();
            List<String> allNames = new ArrayList<>();
            for (ImagingConfigurationSettings setting : cfg.getSettings().configs) {
                allNames.add(setting.name);
            }
            config.setModel(new DefaultComboBoxModel<>(allNames.toArray(new String[allNames.size()])));
        }
    }
    
    class AutoExposeController {
        Integer wavelength = 500;
        ImagingConfiguration config;
        public AutoExposeController(ImagingConfiguration conf, Integer wavelength) throws MMDeviceException {
            config = conf;
            if (config.hasTunableFilter()) {
                config.tunableFilter().setWavelength(wavelength);
            }
        }

        public Double run(Integer targetIntensityPercent) throws MMDeviceException {
            String origCamDevice = Globals.core().getCameraDevice();
            try {
                Globals.core().setCameraDevice(this.config.camera().getName()); //We need our camera to be "The Camera" for the next part to work.
            } catch (Exception e) {
                throw new MMDeviceException(e);
            }
            Integer maxCounts = ((int) Math.round(Math.pow(2, Globals.core().getImageBitDepth()))) - 1; //This should be the count when the image is saturated.        
            Integer targetCounts = (int) Math.round(maxCounts * (targetIntensityPercent / 100.0)); //Calculate target counts from percentage based on camera information.
            
            Calcfc opt = new Calcfc() { //This class is what we provide to the COBYLA optimizer to execute the optimization.
                @Override
                public double Compute(int n, int m, double[] x, double[] con) {
                    //My understanding is that the `con` constraint functions should be negative if they are invalid.
                    //con[0] = 95 - x[0]; //Don't go above 95%
                    con[1] = x[0] - 5; //Don't go below 5ms

                    double newExposure = x[0];
                    try {
                        Globals.core().setExposure(AutoExposeController.this.config.camera().getName(), newExposure);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    Image img = Globals.mm().live().snap(false).get(0); //Get an image
                    display.addImage(img);
                    List<Integer> pix = new ArrayList<>();
                    //Unfortunately getting the pixels from the image into a list is not straightforward.
                    for (int X=0; X<img.getWidth(); X++) {
                        for (int Y=0; Y<img.getHeight(); Y++) {
                            pix.add((int) img.getIntensityAt(X, Y)); //Converting from long to int here is potentially dangerous, but really even high-end cameras are only 16 bit.
                        }
                    }
                    Integer measured = percentile(pix, 99); //The metric that we are optimizing is the intensity (camera counts) of the 99th percentile of camera pixels.
                    Integer error = targetCounts - measured; //How far are we from being optimized.
                    Globals.mm().logs().logMessage(String.format("AutoExposure: Count error of %d at exposure of %.2f ms", error, newExposure));
                    return java.lang.Math.abs(error); //If we don't have abs() here then error can be negative, COBYLA tries to minimize error. so that's not good.
                }
            };

            double[] exposure = { 50 }; //Initial value of 50ms
            double rhoBegin = 50; //The tuning sensitivity at first.
            double rhoEnd = 2; //The tuning sensitivity at the end to finalize.
            CobylaExitStatus status = Cobyla.FindMinimum(opt, 1, 2, exposure, rhoBegin, rhoEnd, 0, 100);
            Globals.mm().logs().logMessage(String.format("AutoExposure: Finished with status: %s", status.toString()));
            try {
                Globals.core().setCameraDevice(origCamDevice); // Set things back the way they were.
            } catch (Exception e) {
                throw new MMDeviceException(e);
            }
            return exposure[0]; //laserPower now contains the optimal value.  
        }

        private Integer percentile(List<Integer> values, double percentile) {
            Collections.sort(values);
            int index = (int) Math.ceil((percentile / 100) * values.size());
            return values.get(index - 1);
        }
    }
}