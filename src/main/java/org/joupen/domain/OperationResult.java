package org.joupen.domain;

public record OperationResult(PlayerOperation operation, boolean applied) {
    public static OperationResult duplicate(PlayerOperation operation, String request) {
        if (!operation.getRequest().equals(request)) {
            throw new IllegalArgumentException("External ID already belongs to another request");
        }
        return new OperationResult(operation, false);
    }
}
