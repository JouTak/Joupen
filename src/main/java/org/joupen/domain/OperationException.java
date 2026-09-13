package org.joupen.domain;

public class OperationException extends IllegalArgumentException {
    public OperationException(String code) {
        super(code);
    }
}
