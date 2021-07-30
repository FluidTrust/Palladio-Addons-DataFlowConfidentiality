package org.palladiosimulator.dataflow.confidentiality.pcm.editor.sirius;

import java.util.Collection;

import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.Characteristic;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.EnumCharacteristic;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.profile.ProfileConstants;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;

public class UnlinkEnumCharacteristics extends EnumCharacteristicLinkingBase {

    @Override
    protected void execute(EObject stereotypeTarget, EnumCharacteristic characteristic) {
        Collection<Characteristic<?>> characteristicsCollection = StereotypeAPI.getTaggedValue(stereotypeTarget,
                ProfileConstants.characterisable.getValue(), ProfileConstants.characterisable.getStereotype());
        characteristicsCollection.remove(characteristic);

        if (characteristicsCollection.isEmpty()) {
            StereotypeAPI.unapplyStereotype(stereotypeTarget, ProfileConstants.characterisable.getStereotype());
        }
    }

}
