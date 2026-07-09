package com.villagesat.transaction.support;

import java.util.UUID;

public final class TransactionTestFixtures {

    /** Aligné sur DevMockAuthFilter (common-lib). */
    public static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    public static final UUID SOURCE_WALLET = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID DEST_WALLET = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private TransactionTestFixtures() {}
}
