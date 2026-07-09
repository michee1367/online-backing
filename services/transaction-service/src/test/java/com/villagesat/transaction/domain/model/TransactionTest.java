package com.villagesat.transaction.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static com.villagesat.transaction.support.TransactionTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionTest {

    @Test
    void lifecycle_pendingToCompleted() {
        UUID idempotencyKey = UUID.randomUUID();
        Transaction tx = Transaction.createInternalTransfer(
                idempotencyKey, SOURCE_WALLET, DEST_WALLET,
                new BigDecimal("1000"), new BigDecimal("100"), "CDF", "test", USER_ID);

        assertThat(tx.status()).isEqualTo(Transaction.TransactionStatus.PENDING);

        Transaction processing = tx.markProcessing();
        assertThat(processing.status()).isEqualTo(Transaction.TransactionStatus.PROCESSING);

        Transaction completed = processing.complete();
        assertThat(completed.status()).isEqualTo(Transaction.TransactionStatus.COMPLETED);
        assertThat(completed.completedAt()).isNotNull();
    }

    @Test
    void fail_setsFailedStatusAndReason() {
        Transaction tx = Transaction.createInternalTransfer(
                UUID.randomUUID(), SOURCE_WALLET, DEST_WALLET,
                new BigDecimal("500"), BigDecimal.ZERO, "CDF", null, USER_ID);

        Transaction failed = tx.markProcessing().fail("wallet debit error");

        assertThat(failed.status()).isEqualTo(Transaction.TransactionStatus.FAILED);
        assertThat(failed.failedReason()).isEqualTo("wallet debit error");
    }
}
