///////////////////////////////////////////////////////////////////////////////
//PROJECT:       PWS Plugin
//
//-----------------------------------------------------------------------------
//
// AUTHOR:       Nick Anthony, 2021
//
// COPYRIGHT:    Northwestern University, 2021
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

package edu.bpl.pwsplugin.acquisitionsequencer.steps;

import edu.bpl.pwsplugin.utils.JsonableParam;

/**
 * @author N2-LiveCell
 */
public abstract class IteratingContainerStep<T extends JsonableParam> extends ContainerStep<T> {
   //A container step that runs it's substeps multiple times.

   public IteratingContainerStep(T settings, String type) {
      super(settings, type);
   }

   public IteratingContainerStep(IteratingContainerStep step) {
      super(step); //Required copy constructor
   }

   public abstract Integer getTotalIterations();

   public abstract Integer getCurrentIteration();

}