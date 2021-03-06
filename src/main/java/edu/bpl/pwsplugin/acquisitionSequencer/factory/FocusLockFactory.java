/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.factory;

import edu.bpl.pwsplugin.UI.utils.BuilderJPanel;
import edu.bpl.pwsplugin.UI.utils.SingleBuilderJPanel;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerConsts;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerSettings;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.FocusLock;
import edu.bpl.pwsplugin.acquisitionSequencer.steps.Step;
import edu.bpl.pwsplugin.utils.JsonableParam;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JLabel;
import edu.bpl.pwsplugin.UI.utils.ImprovedComponents;
import javax.swing.SpinnerNumberModel;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author Nick Anthony <nickmanthony at hotmail.com>
 */
public class FocusLockFactory extends StepFactory {
    @Override
    public Class<? extends BuilderJPanel> getUI() {
        return FocusLockUI.class;
    }
    
    @Override
    public Class<? extends JsonableParam> getSettings() {
        return SequencerSettings.FocusLockSettings.class;
    }
    
    @Override
    public Class<? extends Step> getStep() {
        return FocusLock.class;
    }
    
    @Override
    public String getDescription() {
        return "Engage continuous hardware autofocus. Focus lock will be checked before execution of each Acquisition within this.";
    }
    
    @Override
    public String getName() {
        return "Optical Focus Lock";
    }
    
    @Override
    public String getCategory() {
        return "Utility";
    }

    @Override
    public SequencerConsts.Type getType() {
        return SequencerConsts.Type.PFS;
    }
}

class FocusLockUI extends SingleBuilderJPanel<SequencerSettings.FocusLockSettings>{
    ImprovedComponents.Spinner delay;
    
    public FocusLockUI() {
        super(new MigLayout(), SequencerSettings.FocusLockSettings.class);
        
        delay = new ImprovedComponents.Spinner(new SpinnerNumberModel(1.0, 0.0, 30.0, 1.0));
        ((ImprovedComponents.Spinner.DefaultEditor) delay.getEditor()).getTextField().setColumns(4);
        
        this.add(new JLabel("Delay (s):"), "gapleft push");
        this.add(delay);
    }
    
    @Override
    public Map<String, Object> getPropertyFieldMap() {
        Map<String,Object> m = new HashMap<>();
        m.put("delay", delay);        
        return m;
    }
}

