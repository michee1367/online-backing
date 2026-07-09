package com.villagesat.payment.domain.port.out;

import com.villagesat.payment.domain.model.Merchant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository {

    Merchant save(Merchant merchant);

    Optional<Merchant> findById(UUID id);

    List<Merchant> findByUserId(UUID userId); // Déjà présente sous cette forme ?

    Optional<Merchant> findByMerchantCode(String merchantCode);

    boolean existsByUserIdAndBusinessName(UUID userId, String businessName);

    boolean existsByUserId(UUID userId);

    // AJOUT : Récupérer tous les marchands d'un utilisateur
    List<Merchant> findAllByUserId(UUID userId);
}
