/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.factory;

import edu.bpl.pwsplugin.acquisitionSequencer.steps.Step;
import edu.bpl.pwsplugin.UI.utils.BuilderJPanel;
import edu.bpl.pwsplugin.UI.utils.ImprovedComponents;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerConsts;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerSettings;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.SoftwareAutofocus;
import edu.bpl.pwsplugin.utils.JsonableParam;
import java.text.NumberFormat;
import javax.swing.JLabel;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author Nick Anthony <nickmanthony at hotmail.com>
 */
public class SoftwareAutofocusFactory extends StepFactory {
    @Override
    public Class<? extends BuilderJPanel> getUI() {
        return SoftwareAutoFocusUI.class;
    }
    
    @Override
    public Class<? extends JsonableParam> getSettings() {
        return SequencerSettings.SoftwareAutoFocusSettings.class;
    }
    
    @Override
    public Class<? extends Step> getStep() {
        return SoftwareAutofocus.class;
    }
    
    @Override
    public String getDescription() {
        return "Run a software autofocus routine.";
    }
    
    @Override
    public String getName() {
        return "Software Autofocus";
    }
    
    @Override
    public String getCategory() {
        return "Utility";
    }

    @Override
    public SequencerConsts.Type getType() {
        return SequencerConsts.Type.AF;
    }
}

class SoftwareAutoFocusUI extends BuilderJPanel<SequencerSettings.SoftwareAutoFocusSettings> {
    //JComboBox<String> afNames = new JComboBox<>();
    ImprovedComponents.FormattedTextField exposure = new ImprovedComponents.FormattedTextField(NumberFormat.getNumberInstance());
    
    public SoftwareAutoFocusUI() {
        super(new MigLayout(), SequencerSettings.SoftwareAutoFocusSettings.class);
        //afNames.setModel(new DefaultComboBoxModel<>(Globals.mm().getAutofocusManager().getAllAutofocusMethods().toArray(new String[0])));   
        //this.add(new JLabel("Autofocus Method:"));
        //this.add(afNames);
        exposure.setColumns(6);
        this.add(new JLabel("Exposure (ms):"));
        this.add(exposure);
    }
    
    @Override
    public SequencerSettings.SoftwareAutoFocusSettings build() {
        SequencerSettings.SoftwareAutoFocusSettings afs = new SequencerSettings.SoftwareAutoFocusSettings();
        //afs.afPluginName = (String) afNames.getSelectedItem();
        afs.exposureMs = ((Number) exposure.getValue()).doubleValue();
        return afs;
    }
    
    @Override
    public void populateFields(SequencerSettings.SoftwareAutoFocusSettings settings) {
        //afNames.setSelectedItem(settings.afPluginName);
        exposure.setValue(settings.exposureMs);
    }
}

