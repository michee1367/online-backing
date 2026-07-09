package com.villagesat.fraud.domain.port.out;

import java.util.UUID;

public interface TransactionHistoryPort {

    int getRecentTransactionCount(UUID userId, int hours);
}
