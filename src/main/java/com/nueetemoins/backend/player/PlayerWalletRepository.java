package com.nueetemoins.backend.player;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PlayerWalletRepository extends JpaRepository<PlayerWallet, String> {

    /** Verrou pessimiste : évite les doubles ouvertures de pack / doubles dépenses. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from PlayerWallet w where w.userId = :userId")
    Optional<PlayerWallet> findForUpdate(@Param("userId") String userId);
}
