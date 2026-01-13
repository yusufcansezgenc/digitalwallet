package com.inghubs.digitalwallet.utilities.exceptions;

public class WithdrawalDeniedException extends RuntimeException {

    public WithdrawalDeniedException(String message) {
        super(message);
    }
}
