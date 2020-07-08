/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.hardware.illumination;

import edu.bpl.pwsplugin.Globals;
import edu.bpl.pwsplugin.hardware.Device;
import edu.bpl.pwsplugin.hardware.MMDeviceException;
import edu.bpl.pwsplugin.hardware.settings.IlluminatorSettings;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Nick Anthony <nickmanthony at hotmail.com>
 */
public class SimulatedIlluminator implements Illuminator {
    private IlluminatorSettings settings;
    
    public SimulatedIlluminator(IlluminatorSettings settings) throws Device.IDException {
        this.settings = settings;
        if (!this.identify()) {
            throw new Device.IDException(String.format("Failed to identify class %s for device name %s", this.getClass().toString(), settings.name));
        }
    }
    
    @Override
    public void setShutter(boolean on) throws MMDeviceException {
        try {
            Globals.core().setShutterOpen(this.settings.name, on);
        } catch (Exception e) {
            throw new MMDeviceException(e);
        }
    }
    
    @Override
    public boolean identify() {
        try {
            return Globals.core().getDeviceName(this.settings.name).equals("DShutter");
        } catch (Exception e) {
            return false;
        } 
    }
    
    @Override
    public List<String> validate() {
        List<String> errs = new ArrayList<>();
        try {
            if (!identify()) {
                errs.add(this.settings.name + " is not a simulated DemoShutter device");
            }

        } catch (Exception e) {
            errs.add(e.getMessage());
        }
        return errs;
    }

    @Override
    public void initialize() {}//Not sure what to do here
    
    @Override
    public void activate() {}//Not sure what to do here
    
}