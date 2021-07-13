package org.palladiosimulator.dataflow.confidentiality.pcm.dddsl.service;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.dictionary.DictionaryPackage;
import org.palladiosimulator.dataflow.dictionary.characterized.dsl.service.CharacterizedDataDictionaryTransientValueService;

import de.uka.ipd.sdq.identifier.IdentifierPackage;

public class DDDslTransientValueService extends CharacterizedDataDictionaryTransientValueService {

    @Override
    public boolean isTransient(EObject owner, EStructuralFeature feature, int index) {
        var superDecision = super.isTransient(owner, feature, index);
        if (feature == IdentifierPackage.Literals.IDENTIFIER__ID) {
            return superDecision && owner.eClass() != DictionaryPackage.Literals.PCM_DATA_DICTIONARY;
        }
        return superDecision;
    }

}
