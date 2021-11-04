package org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd;

public interface PcmAnnotations {
    public enum PCMContainingType {
        ScenarioBehaviour, Component, DataChannel, Store
    }

    public enum PCMActionType {
        CallSending, CallReceiving, SEFFEntry, SEFFExit
    }
}
