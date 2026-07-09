package com.villagesat.payment.adapter.out.persistence;

import com.villagesat.payment.adapter.out.persistence.entity.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantJpaRepository extends JpaRepository<MerchantEntity, UUID> {

    List<MerchantEntity> findByUserId(UUID userId);

    Optional<MerchantEntity> findByMerchantCode(String merchantCode);

    boolean existsByUserIdAndBusinessName(UUID userId, String businessName);

    boolean existsByUserId(UUID userId);

    // AJOUT : Requête automatique pour lister toutes les entités par UserId
    List<MerchantEntity> findAllByUserId(UUID userId);
}
