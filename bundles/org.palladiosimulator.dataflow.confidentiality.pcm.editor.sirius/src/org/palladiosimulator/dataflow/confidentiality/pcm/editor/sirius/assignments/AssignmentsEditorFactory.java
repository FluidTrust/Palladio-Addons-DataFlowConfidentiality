package org.palladiosimulator.dataflow.confidentiality.pcm.editor.sirius.assignments;

import java.util.Collection;

import org.eclipse.swt.widgets.Shell;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.CharacteristicTypeDictionary;
import org.palladiosimulator.pcm.parameter.VariableUsage;

import com.google.inject.Inject;
import com.google.inject.Injector;

public class AssignmentsEditorFactory {

    @Inject
    private Injector injector;

    public AssignmentsEditorImpl create(Shell parentShell, String outputName, Collection<String> usableInputs,
            Collection<CharacteristicTypeDictionary> dictionaries, VariableUsage existingUsage) {
        var dialog = new AssignmentsEditorImpl(parentShell, outputName, usableInputs, dictionaries, existingUsage);
        injector.injectMembers(dialog);
        return dialog;
    }

}
