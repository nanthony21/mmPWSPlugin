///////////////////////////////////////////////////////////////////////////////
//PROJECT:       PWS Plugin for Micro-Manager
//
//-----------------------------------------------------------------------------
//
// AUTHOR:      Nick Anthony 2019
//
// COPYRIGHT:    Northwestern University, Evanston, IL.  2019
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
//
package edu.bpl.pwsplugin.acquisitionManagers;

import edu.bpl.pwsplugin.hardware.configurations.ImagingConfiguration;
import edu.bpl.pwsplugin.metadata.MetadataBase;


interface Acquisition <S> {
    public void acquireImages(String savePath, int cellNum, MetadataBase metadata) throws Exception; //Begin the acquisition process.
    //public String getSavePath(String savePath, int cellNum) throws FileAlreadyExistsException; // given a parent directory and a cell number, return the full path to save to.
    //public FileSpecs.Type getFileType(); //Return the type enumerator for this acquisition, used for file saving information.
    public void setSettings(S settings);
    //public S getSettings();
    public ImagingConfiguration getImgConfig();
    //public Integer numFrames();
}
