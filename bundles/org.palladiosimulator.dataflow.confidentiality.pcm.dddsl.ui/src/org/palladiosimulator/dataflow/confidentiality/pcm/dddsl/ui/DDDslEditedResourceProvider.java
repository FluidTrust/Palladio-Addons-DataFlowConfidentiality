package org.palladiosimulator.dataflow.confidentiality.pcm.dddsl.ui;

import java.util.UUID;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.FileExtensionProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.ui.editor.embedded.IEditedResourceProvider;

import com.google.inject.Inject;

@SuppressWarnings("restriction")
public class DDDslEditedResourceProvider implements IEditedResourceProvider {

    @Inject
    private FileExtensionProvider fileExtensionProvider;
    
    @Override
    public XtextResource createResource() {
        final var rs = new XtextResourceSet();
        final var tmpURI = URI.createURI("virtual:/" + UUID.randomUUID().toString() + "." + fileExtensionProvider.getPrimaryFileExtension());
        return (XtextResource) rs.createResource(tmpURI);
    }

}
