package com.villagesat.fraud.adapter.out.stub;

import com.villagesat.fraud.domain.port.out.TransactionHistoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stub simulant l'historique de transactions — sera remplacé par un appel au transaction-service.
 */
@Component
public class TransactionHistoryStubAdapter implements TransactionHistoryPort {

    private static final Logger log = LoggerFactory.getLogger(TransactionHistoryStubAdapter.class);

    private final ConcurrentMap<UUID, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Override
    public int getRecentTransactionCount(UUID userId, int hours) {
        int count = counters.computeIfAbsent(userId, k -> new AtomicInteger(0)).get();
        log.debug("Stub: {} recent transactions for user {} in last {}h", count, userId, hours);
        return count;
    }

    public void incrementCount(UUID userId) {
        counters.computeIfAbsent(userId, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void reset() {
        counters.clear();
    }
}
