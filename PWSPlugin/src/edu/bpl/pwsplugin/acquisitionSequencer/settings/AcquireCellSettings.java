/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.settings;

import edu.bpl.pwsplugin.settings.DynSettings;
import edu.bpl.pwsplugin.settings.FluorSettings;
import edu.bpl.pwsplugin.settings.PWSSettings;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author nick
 */
public class AcquireCellSettings extends SequencerSettings {
    public PWSSettings pwsSettings = new PWSSettings();
    public DynSettings dynSettings = new DynSettings();
    public List<FluorSettings> fluorSettings = new ArrayList<>();
    public String directory = "";
    
    public AcquireCellSettings() {
        fluorSettings.add(new FluorSettings());
    }
}