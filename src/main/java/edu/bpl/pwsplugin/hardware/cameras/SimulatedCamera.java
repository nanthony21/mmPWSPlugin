/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.hardware.cameras;

import edu.bpl.pwsplugin.Globals;
import edu.bpl.pwsplugin.hardware.Device;
import edu.bpl.pwsplugin.hardware.settings.CamSettings;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Nick Anthony <nickmanthony at hotmail.com>
 */
public class SimulatedCamera extends DefaultCamera {
    
    public SimulatedCamera(CamSettings settings) throws Device.IDException {
        super(settings);
    }
    
    @Override
    public boolean supportsExternalTriggering() { return false; }
    
    @Override
    public boolean supportsTriggerOutput() { return false; }
    
    @Override
    public void configureTriggerOutput(boolean enable) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean identify() {
        try {
            return (Globals.core().getDeviceName(this.settings.name).equals("DCam"));
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public List<String> validate() {
        List<String> errs = new ArrayList<>();
        try {
            if (!identify()) {
                errs.add(settings.name + " is not a simulated DemoCamera device");
            }
        } catch (Exception e) {
            errs.add(e.getMessage());
        }
        return errs;
    }
}
