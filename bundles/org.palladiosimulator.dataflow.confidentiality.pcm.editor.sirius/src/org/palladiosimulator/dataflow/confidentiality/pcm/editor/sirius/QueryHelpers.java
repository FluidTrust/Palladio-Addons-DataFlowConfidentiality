package org.palladiosimulator.dataflow.confidentiality.pcm.editor.sirius;

import java.util.Collection;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.sirius.business.api.session.Session;
import org.eclipse.sirius.business.api.session.SessionManager;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.CharacteristicTypeDictionary;

public final class QueryHelpers {

    private QueryHelpers() {
        // intentionally left blank
    }

    public static Collection<CharacteristicTypeDictionary> findCharacteristicTypeDictionariesInSemanticResources(EObject semanticDiagramElement) {
        Session session = SessionManager.INSTANCE.getSession(semanticDiagramElement);
        return session.getSemanticResources()
            .stream()
            .map(Resource::getContents)
            .flatMap(Collection::stream)
            .filter(CharacteristicTypeDictionary.class::isInstance)
            .map(CharacteristicTypeDictionary.class::cast)
            .collect(Collectors.toList());
    }

}
