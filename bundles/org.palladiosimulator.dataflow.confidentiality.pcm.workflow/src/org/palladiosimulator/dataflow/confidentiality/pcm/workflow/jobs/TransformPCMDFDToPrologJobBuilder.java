package org.palladiosimulator.dataflow.confidentiality.pcm.workflow.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.jobs.impl.TransformPCMDFDtoPrologJobImpl;
import org.palladiosimulator.dataflow.confidentiality.transformation.prolog.NameGenerationStrategie;
import org.palladiosimulator.dataflow.confidentiality.transformation.workflow.blackboards.KeyValueMDSDBlackboard;
import org.palladiosimulator.dataflow.confidentiality.transformation.workflow.jobs.InitPartitionJob;
import org.palladiosimulator.dataflow.confidentiality.transformation.workflow.jobs.LoadExistingModelsJob;
import org.palladiosimulator.dataflow.confidentiality.transformation.workflow.jobs.LoadExistingModelsJob.ModelContent;
import org.palladiosimulator.dataflow.confidentiality.transformation.workflow.jobs.SerializeModelToStringJob;
import org.palladiosimulator.dataflow.confidentiality.transformation.workflow.jobs.TransformDFDToPrologJob;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.usagemodel.UsageModel;

import de.uka.ipd.sdq.workflow.jobs.IJob;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.ModelLocation;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.SavePartitionToDiskJob;

public class TransformPCMDFDToPrologJobBuilder {

    private static final String DEFAULT_PCM_INPUT_PARTITION_ID = "pcm";
    private static final ModelLocation DEFAULT_DD_LOCATION = new ModelLocation("dfd", URI.createFileURI("tmp/dd.xmi"));
    private static final ModelLocation DEFAULT_DFD_LOCATION = new ModelLocation("dfd",
            URI.createFileURI("tmp/dfd.xmi"));
    private static final ModelLocation DEFAULT_PROLOG_LOCATION = new ModelLocation("prolog",
            URI.createFileURI("tmp/dfd.pl"));
    public static final String DEFAULT_PROLOG_KEY = "prologProgram";
    private static final String DEFAULT_DFDTRACE_KEY = "dfdTrace";
    private static final String DEFAULT_PCMTRACE_KEY = "pcmTrace";
    private static final String DEFAULT_TRACE_KEY = "transitiveTrace";

    private final Collection<ModelContent> usageModels = new ArrayList<>();
    private ModelContent allocationModel = null;
    private ModelLocation prologLocation = null;
    private boolean serializeToString = false;
    private String prologResultKey;

    public static TransformPCMDFDToPrologJobBuilder create() {
        return new TransformPCMDFDToPrologJobBuilder();
    }
    
    public TransformPCMDFDToPrologJobBuilder addUsageModelsByURI(URI... uris) {
        return addUsageModelsByURI(Arrays.asList(uris));
    }

    public TransformPCMDFDToPrologJobBuilder addUsageModelsByURI(Collection<URI> uris) {
        usageModels.clear();
        usageModels.addAll(uris.stream()
            .map(uri -> new ModelLocation(DEFAULT_PCM_INPUT_PARTITION_ID, uri))
            .map(ModelContent::new)
            .collect(Collectors.toList()));
        return this;
    }

    public TransformPCMDFDToPrologJobBuilder addUsageModels(UsageModel... usageModels) {
        return addUsageModels(Arrays.asList(usageModels));
    }

    public TransformPCMDFDToPrologJobBuilder addUsageModels(Collection<UsageModel> usageModels) {
        this.usageModels.clear();
        for (UsageModel usageModel : usageModels) {
            ModelLocation location = new ModelLocation(DEFAULT_PCM_INPUT_PARTITION_ID, usageModel.eResource()
                .getURI());
            List<EObject> elementsToAdd = new ArrayList<>();
            elementsToAdd.add(usageModel);
            if (usageModel.eResource() != null) {
                // assumption: usage model is a root node and there might be required profile applications
                var additionalRoots = usageModel.eResource()
                    .getContents()
                    .stream()
                    .filter(o -> o != usageModel)
                    .collect(Collectors.toList());
                elementsToAdd
                    .addAll(additionalRoots);
            }
            ModelContent content = new ModelContent(location, elementsToAdd);
            this.usageModels.add(content);
        }
        return this;
    }
    
    public TransformPCMDFDToPrologJobBuilder addAllocationModelByURI(URI uri) {
        var location = new ModelLocation(DEFAULT_PCM_INPUT_PARTITION_ID, uri);
        allocationModel = new ModelContent(location);
        return this;
    }
    
    public TransformPCMDFDToPrologJobBuilder addAllocationModel(Allocation allocation) {
        ModelLocation location = new ModelLocation(DEFAULT_PCM_INPUT_PARTITION_ID, allocation.eResource()
            .getURI());
        allocationModel = new ModelContent(location, allocation);
        return this;
    }

    public TransformPCMDFDToPrologJobBuilder addSerializeModelToFile(URI destinationURI) {
        serializeToString = false;
        prologLocation = new ModelLocation(DEFAULT_PROLOG_LOCATION.getPartitionID(), destinationURI);
        prologResultKey = DEFAULT_PROLOG_KEY;
        return this;
    }

    public TransformPCMDFDToPrologJobBuilder addSerializeModelToString() {
        return addSerializeModelToString(DEFAULT_PROLOG_KEY);
    }

    public TransformPCMDFDToPrologJobBuilder addSerializeModelToString(String blackboardKey) {
        serializeToString = true;
        prologLocation = DEFAULT_PROLOG_LOCATION;
        prologResultKey = blackboardKey;
        return this;
    }
    
    public TransformPCMDFDtoPrologJob<? extends KeyValueMDSDBlackboard> build() {
        // validate builder state
        Validate.notNull(prologLocation);
        Validate.notEmpty(usageModels);
        Validate.notNull(allocationModel);
        if (serializeToString) {
            Validate.notNull(prologResultKey);
        }

        // create job sequence
        var jobSequence = new TransformPCMDFDtoPrologJobImpl<KeyValueMDSDBlackboard>("PCM to Prolog Transformation",
                prologResultKey, DEFAULT_TRACE_KEY);

        // create model loading job
        var allModelContents = new ArrayList<ModelContent>();
        allModelContents.addAll(usageModels);
        allModelContents.add(allocationModel);
        IJob loadJob = new LoadExistingModelsJob<MDSDBlackboard>(allModelContents);
        jobSequence.add(loadJob);

        // create DFD partition initialization job
        IJob initDfdPartitionJob = new InitPartitionJob(DEFAULT_DFD_LOCATION.getPartitionID());
        jobSequence.add(initDfdPartitionJob);

        // create PCM to DFD transformation job
        IJob pcmToDfdJob = new TransformPCMtoDFDJob(DEFAULT_PCM_INPUT_PARTITION_ID, DEFAULT_DFD_LOCATION,
                DEFAULT_DD_LOCATION, DEFAULT_PCMTRACE_KEY);
        jobSequence.add(pcmToDfdJob);

        // create prolog partition initialization job
        IJob initPrologPartitionJob = new InitPartitionJob(prologLocation.getPartitionID());
        jobSequence.add(initPrologPartitionJob);
        
        // create DFD to prolog transformation job
        IJob dfdToPrologJob = new TransformDFDToPrologJob<KeyValueMDSDBlackboard>(DEFAULT_DFD_LOCATION, prologLocation, DEFAULT_DFDTRACE_KEY,
                NameGenerationStrategie.SHORTED_ID);
        jobSequence.add(dfdToPrologJob);
        
        // create transitive trace job
        IJob transitiveTraceJob = new TransitiveTransformationTraceBuilderJob(DEFAULT_PCMTRACE_KEY,
                DEFAULT_DFDTRACE_KEY, DEFAULT_TRACE_KEY);
        jobSequence.add(transitiveTraceJob);

        // create model saving job
        IJob serializeJob;
        if (serializeToString) {
            serializeJob = new SerializeModelToStringJob(prologLocation, prologResultKey);
        } else {
            serializeJob = new SavePartitionToDiskJob(prologLocation.getPartitionID());
        }
        jobSequence.add(serializeJob);

        // return job sequence
        return jobSequence;
    }

}
