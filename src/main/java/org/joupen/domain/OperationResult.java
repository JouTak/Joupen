package org.joupen.domain;

public record OperationResult(PlayerOperation operation, boolean applied) {
    public static OperationResult duplicate(PlayerOperation operation, String request) {
        if (!operation.getRequest().equals(request)) {
            throw new OperationException("external-id-conflict");
        }
        return new OperationResult(operation, false);
    }
}
