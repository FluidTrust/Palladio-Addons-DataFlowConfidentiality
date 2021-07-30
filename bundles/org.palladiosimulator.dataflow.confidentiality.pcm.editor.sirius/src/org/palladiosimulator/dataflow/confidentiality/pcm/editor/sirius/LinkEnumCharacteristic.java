package org.palladiosimulator.dataflow.confidentiality.pcm.editor.sirius;

import java.util.Collection;

import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.Characteristic;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.EnumCharacteristic;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.profile.ProfileConstants;
import org.palladiosimulator.mdsdprofiles.api.ProfileAPI;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;

public class LinkEnumCharacteristic extends EnumCharacteristicLinkingBase {

    @Override
    protected void execute(EObject stereotypeTarget, EnumCharacteristic characteristic) {
        if (!ProfileAPI.isProfileApplied(stereotypeTarget.eResource(), ProfileConstants.profileName)) {
            ProfileAPI.applyProfile(stereotypeTarget.eResource(), ProfileConstants.profileName);
        }

        if (!StereotypeAPI.isStereotypeApplied(stereotypeTarget, ProfileConstants.characterisable.getStereotype())) {
            StereotypeAPI.applyStereotype(stereotypeTarget, ProfileConstants.characterisable.getStereotype());
        }

        Collection<Characteristic<?>> characteristicsCollection = StereotypeAPI.getTaggedValue(stereotypeTarget,
                ProfileConstants.characterisable.getValue(), ProfileConstants.characterisable.getStereotype());
        characteristicsCollection.add(characteristic);
    }

}
