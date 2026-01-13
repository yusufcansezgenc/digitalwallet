package com.inghubs.digitalwallet.utilities.enums;

public enum BalanceOperation {
    COMPLETE_PENDING_DEPOSIT("Processing deposit for walletId: {}"),
    COMPLETE_PENDING_WITHDRAW("Processing withdrawal for walletId: {}"),
    REVERT_PENDING_DEPOSIT("Reverting deposit for walletId: {}"),
    REVERT_PENDING_WITHDRAW("Reverting withdrawal for walletId: {}"),
    COMPLETE_APPROVED_DEPOSIT("Completing approved deposit for walletId: {}"),
    COMPLETE_APPROVED_WITHDRAW("Completing approved withdrawal for walletId: {}");

    private final String logMessage;

    BalanceOperation(String logMessage) {
        this.logMessage = logMessage;
    }

    public String getLogMessage() {
        return logMessage;
    }
}
