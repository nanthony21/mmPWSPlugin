/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bpl.pwsplugin.acquisitionSequencer.steps;

import edu.bpl.pwsplugin.acquisitionSequencer.AcquisitionStatus;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerConsts;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerFunction;
import edu.bpl.pwsplugin.acquisitionSequencer.SequencerSettings;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.List;

/**
 *
 * @author nick
 */
public class EnterSubfolderStep extends ContainerStep<SequencerSettings.EnterSubfolderSettings> {
    
    private Integer cellNum = 0;
    private Integer simCellNum = 0;

    public EnterSubfolderStep() {
        super(new SequencerSettings.EnterSubfolderSettings(), SequencerConsts.Type.SUBFOLDER);
    }

    @Override
    public SequencerFunction getStepFunction(List<SequencerFunction> callbacks) {
        SequencerFunction stepFunction = super.getSubstepsFunction(callbacks);
        cellNum = 0; //initialize cell num
        SequencerSettings.EnterSubfolderSettings settings = this.settings;
        return new SequencerFunction() {
            @Override
            public AcquisitionStatus applyThrows(AcquisitionStatus status) throws Exception {
                String origPath = status.getSavePath();
                Integer origCellNum = status.getCellNum();
                status.newStatusMessage(String.format("Moving to subfolder: %s", settings.relativePath));
                status.setSavePath(Paths.get(origPath).resolve(settings.relativePath).toString());
                status.setCellNum(cellNum); // Even if we exit and enter this subfolder multiple times we should still remember which cell num we're on.
                status = stepFunction.apply(status);
                cellNum = status.getCellNum(); //Update our placeholder with whatever we left off on.
                status.setSavePath(origPath);
                status.setCellNum(origCellNum);
                return status;
            }
        };
    }

    @Override
    protected SimFn getSimulatedFunction() {
        SimFn subStepSimFn = this.getSubStepSimFunction();
        simCellNum = 0; //Initialize cell number.
        return (Step.SimulatedStatus status) -> {
            String path = this.settings.relativePath;
            Integer origCellNum = status.cellNum;
            String origDir = status.workingDirectory;
            status.cellNum = simCellNum; // Even if we exit and enter this subfolder multiple times we should still remember which cell num we're on.
            status.workingDirectory = Paths.get(status.workingDirectory, path).toString();
            status = subStepSimFn.apply(status);
            simCellNum = status.cellNum;
            status.workingDirectory = origDir;
            status.cellNum = origCellNum;
            return status;
        };
    }

    @Override
    public List<String> validate() {
        List<String> errs = super.validate();
        String path = this.settings.relativePath;
        if (path.equals("")) {
            errs.add("The `EnterSubFolder` path may not be empty.");
        }
        if (path.contains(".")) {
            errs.add("The `.` character is not allowed the in `EnterSubFolder` step.");
        }
        try {
            Paths.get(path);
        } catch (InvalidPathException e) {
            errs.add(String.format("Relative path %s is invalid", path));
        }
        return errs;
    }
    
}
