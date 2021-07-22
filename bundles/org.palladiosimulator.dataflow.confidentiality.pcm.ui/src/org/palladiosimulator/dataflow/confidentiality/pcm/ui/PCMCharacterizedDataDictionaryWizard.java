package org.palladiosimulator.dataflow.confidentiality.pcm.ui;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtext.serializer.ISerializer;
import org.eclipse.xtext.util.StringInputStream;
import org.palladiosimulator.dataflow.confidentiality.pcm.dddsl.ui.internal.DddslActivator;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.dictionary.DictionaryFactory;
import org.palladiosimulator.dataflow.confidentiality.ui.wizard.AbstractCharacterizedDataDictionaryWizard;

public class PCMCharacterizedDataDictionaryWizard extends AbstractCharacterizedDataDictionaryWizard {

    public PCMCharacterizedDataDictionaryWizard() {
        super(DddslActivator.getInstance()
            .getInjector(DddslActivator.ORG_PALLADIOSIMULATOR_DATAFLOW_CONFIDENTIALITY_PCM_DDDSL_DDDSL));
    }

    @Override
    public boolean performFinish() {
        if (!super.performFinish()) {
            return false;
        }

        IProject project = getFileToCreate().getProject();
        PCMSiriusUtils.initProject(project, new NullProgressMonitor());

        return true;
    }

    @Override
    protected void createDataDictionary(IFile file, IProgressMonitor progressMonitor) throws CoreException {
        var serializer = injector.getInstance(ISerializer.class);
        var emptyDictionary = DictionaryFactory.eINSTANCE.createPCMDataDictionary();
        ResourceSetImpl rs = new ResourceSetImpl();
        Resource r = rs.createResource(URI.createURI("virtual:/tmp/foo." + file.getFileExtension()));
        r.getContents()
            .add(emptyDictionary);
        String fileContent = serializer.serialize(emptyDictionary);

        try (InputStream sis = new StringInputStream(fileContent)) {
            file.create(sis, true, progressMonitor);
        } catch (IOException e) {
            throw new CoreException(
                    new Status(IStatus.ERROR, PCMCharacterizedDataDictionaryWizard.class, "Could not create file.", e));
        }
    }

}
