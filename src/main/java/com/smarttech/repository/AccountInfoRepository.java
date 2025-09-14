package com.smarttech.repository;

import com.smarttech.entity.AccountInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountInfoRepository extends JpaRepository<AccountInfo, Long> {
    Optional<AccountInfo> findByAccountNumber(String accountNumber);
    List<AccountInfo> findByIsActiveTrue();
}
