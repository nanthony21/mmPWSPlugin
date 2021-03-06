/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.steps;

import edu.bpl.pwsplugin.Globals;
import edu.bpl.pwsplugin.acquisitionSequencer.AcquisitionStatus;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerConsts;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerFunction;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerSettings;
import java.util.List;

/**
 *
 * @author nick
 */
public class ChangeConfigGroup extends ContainerStep<SequencerSettings.ChangeConfigGroupSettings> {
    
    public ChangeConfigGroup() {
        super(new SequencerSettings.ChangeConfigGroupSettings(), SequencerConsts.Type.CONFIG);
    }

    @Override
    public SequencerFunction getStepFunction(List<SequencerFunction> callbacks) {
        SequencerFunction subStepFunc = getSubstepsFunction(callbacks);
        SequencerSettings.ChangeConfigGroupSettings settings = this.settings;
        return (status) -> {
            String origConfValue = Globals.core().getCurrentConfig(settings.configGroupName);
            status.newStatusMessage(String.format("Changing %s config group to %s", settings.configGroupName, settings.configValue));
            Globals.core().setConfig(settings.configGroupName, settings.configValue);
            Globals.core().waitForConfig(settings.configGroupName, settings.configValue);
            status = subStepFunc.apply(status);
            Globals.core().setConfig(settings.configGroupName, origConfValue);
            Globals.core().waitForConfig(settings.configGroupName, origConfValue);
            status.newStatusMessage(String.format("Changing %s config group back to original setting, %s", settings.configGroupName, origConfValue));
            return status;
        };
    }

    @Override
    protected SimFn getSimulatedFunction() {
        SimFn subStepSimFn = this.getSubStepSimFunction();
        return (Step.SimulatedStatus status) -> {
            status = subStepSimFn.apply(status);
            return status;
        };
    }
    
}
