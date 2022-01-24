package org.palladiosimulator.dataflow.confidentiality.pcm.transformation.pcm2dfd.impl.devided;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.uka.ipd.sdq.identifier.Identifier;

public class IdGenerationHelper {

    private static final String INPUT_SEPARATOR = "|--sep--|";
    private final Function<String, String> hashFunction = createHashFunction("SHA-256");

    public void setCalculatedId(Identifier element, Object... objects) {
        String generatedId = generateId(objects);
        element.setId(generatedId);
    }

    public String generateId(Object... objects) {
        List<String> generationInputs = getGenerationInputs(objects);
        var idGenerationInput = generationInputs.stream()
            .collect(Collectors.joining(INPUT_SEPARATOR));
        return getStringHash(idGenerationInput);
    }

    protected String getStringHash(String idGenerationInput) {
        return hashFunction.apply(idGenerationInput);
    }

    protected List<String> getGenerationInputs(Object[] inputs) {
        List<String> generationInputs = new ArrayList<String>();
        Queue<Object> queue = new ArrayDeque<>();
        queue.addAll(Arrays.asList(inputs));
        while (!queue.isEmpty()) {
            Object currentObject = queue.poll();
            if (currentObject instanceof Iterable<?>) {
                for (Object content : (Iterable<?>) currentObject) {
                    queue.add(content);
                }
            } else if (currentObject instanceof Identifier) {
                generationInputs.add(((Identifier) currentObject).getId());
            } else {
                generationInputs.add(currentObject.toString());
            }
        }
        return generationInputs;
    }

    protected static Function<String, String> createHashFunction(String algorithmName) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException e) {
            return Function.identity();
        }
        return input -> {
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            byte[] stringBytes = Base64.getEncoder()
                .encode(encodedhash);
            return new String(stringBytes);
        };
    }

}
