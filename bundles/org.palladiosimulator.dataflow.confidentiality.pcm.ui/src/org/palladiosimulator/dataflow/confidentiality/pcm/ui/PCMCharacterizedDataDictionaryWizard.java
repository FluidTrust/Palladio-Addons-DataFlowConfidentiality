package org.palladiosimulator.dataflow.confidentiality.pcm.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.palladiosimulator.dataflow.confidentiality.ui.wizard.CharacterizedDataDictionaryWizard;

public class PCMCharacterizedDataDictionaryWizard extends CharacterizedDataDictionaryWizard {

    @Override
    public boolean performFinish() {
        if (!super.performFinish()) {
            return false;
        }

        IProject project = getFileToCreate().getProject();
        PCMSiriusUtils.initProject(project, new NullProgressMonitor());

        return true;
    }

}
