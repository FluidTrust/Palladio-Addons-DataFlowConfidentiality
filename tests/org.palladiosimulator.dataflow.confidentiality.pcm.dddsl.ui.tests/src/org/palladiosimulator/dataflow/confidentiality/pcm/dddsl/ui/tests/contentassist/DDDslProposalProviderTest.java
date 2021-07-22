package org.palladiosimulator.dataflow.confidentiality.pcm.dddsl.ui.tests.contentassist;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.FileExtensionProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.ui.testing.ContentAssistProcessorTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.palladiosimulator.dataflow.confidentiality.pcm.dddsl.ui.tests.DDDslUiInjectorProvider;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

import tools.mdsd.junit5utils.annotations.PluginTestOnly;

@ExtendWith(InjectionExtension.class)
@InjectWith(DDDslUiInjectorProvider.class)
@PluginTestOnly
public class DDDslProposalProviderTest {

    @Inject
    private Injector injector;

    @Inject
    private Provider<XtextResourceSet> resourceSetProvider;

    @Inject
    private FileExtensionProvider fileExtensionProvider;

    private ContentAssistProcessorTestBuilder builder;

    public DDDslProposalProviderTest() {
        super();
    }

    @BeforeEach
    public void setup() throws Exception {
        builder = new ContentAssistProcessorTestBuilder(injector, this::getResourceFor);
    }

    @Test
    public void testSuggestOutputVariables() throws Exception {
        builder = builder.append(
                "dictionary id \"123\" behavior b1 {input in11 input in12 output out11 } behavior b2 {input in21 input in22 output out21 ");
        builder.assertProposal("output");
        builder.assertProposal("}");
        builder.assertProposal("out21");
    }

    @Test
    public void testSuggestInputVariables() throws Exception {
        builder = builder.append(
                "dictionary id \"123\" behavior b1 {input in11 input in12 output out11 } behavior b2 {input in21 input in22 output out21 out21.*.* := ");
        builder.assertProposal("in21");
        builder.assertProposal("in22");
    }

    protected XtextResource getResourceFor(InputStream stream) {
        XtextResourceSet resourceSet = resourceSetProvider.get();
        try {
            URI resourceUri = URI.createURI("Test." + fileExtensionProvider.getPrimaryFileExtension());
            Resource resource = resourceSet.createResource(resourceUri);
            resource.load(stream, null);
            return (XtextResource) resource;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
