package org.palladiosimulator.dataflow.confidentiality.pcm.editor.sirius.assignments;

import org.palladiosimulator.dataflow.confidentiality.pcm.dddsl.ui.internal.DddslActivator;

public final class AssignmentsEditorFactoryProvider {

    private static final AssignmentsEditorFactory FACTORY = createInstance();

    private AssignmentsEditorFactoryProvider() {
        // intentionally left blank
    }

    public static AssignmentsEditorFactory getInstance() {
        return FACTORY;
    }

    private static AssignmentsEditorFactory createInstance() {
        var injector = DddslActivator.getInstance()
            .getInjector(DddslActivator.ORG_PALLADIOSIMULATOR_DATAFLOW_CONFIDENTIALITY_PCM_DDDSL_DDDSL);
        return injector.getInstance(AssignmentsEditorFactory.class);
    }
}
