package com.edu.com.enrollment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class EnrollmentConsistencyLock {
    private final JdbcTemplate jdbc;

    public void forEnrollmentChange() {
        requireTransaction();
        jdbc.execute("select pg_advisory_xact_lock_shared(1701082467, 1)");
    }

    public void forCatalogChange() {
        requireTransaction();
        jdbc.execute("select pg_advisory_xact_lock(1701082467, 1)");
    }

    private void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Enrollment consistency locks require an active transaction");
        }
    }
}
