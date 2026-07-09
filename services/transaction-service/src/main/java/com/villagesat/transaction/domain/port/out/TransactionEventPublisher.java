package com.villagesat.transaction.domain.port.out;

import com.villagesat.transaction.domain.model.Transaction;

public interface TransactionEventPublisher {

    void publishTransactionCompleted(Transaction transaction);

    void publishTransactionFailed(Transaction transaction);
}
