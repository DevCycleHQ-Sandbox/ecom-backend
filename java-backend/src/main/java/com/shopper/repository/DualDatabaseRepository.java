package com.shopper.repository;

import com.shopper.service.DualDatabaseStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Abstract base class for repositories that need dual database support.
 * Provides methods to execute operations on both primary and secondary databases
 * with feature flag controlled routing.
 */
@Slf4j
public abstract class DualDatabaseRepository<T, ID> {
    
    @Autowired
    protected DualDatabaseStrategy dualDatabaseStrategy;
    
    @Autowired
    @Qualifier("primaryDataSource")
    protected DataSource primaryDataSource;
    
    @Autowired(required = false)
    @Qualifier("secondaryDataSource")
    protected DataSource secondaryDataSource;
    
    /**
     * Get the primary repository instance
     * @return Primary repository
     */
    protected abstract JpaRepository<T, ID> getPrimaryRepository();
    
    /**
     * Get the secondary repository instance
     * @return Secondary repository (null if not available)
     */
    protected abstract JpaRepository<T, ID> getSecondaryRepository();
    
    /**
     * Find entity by ID using dual database strategy
     * @param userId User ID for feature flag evaluation
     * @param id Entity ID
     * @return Optional containing the entity if found
     */
    protected Optional<T> findByIdDual(String userId, ID id) {
        return dualDatabaseStrategy.executeRead(
            userId,
            () -> getPrimaryRepository().findById(id),
            () -> getSecondaryRepository() != null ? getSecondaryRepository().findById(id) : Optional.empty()
        );
    }
    
    /**
     * Find all entities using dual database strategy
     * @param userId User ID for feature flag evaluation
     * @return List of entities
     */
    protected List<T> findAllDual(String userId) {
        return dualDatabaseStrategy.executeRead(
            userId,
            () -> getPrimaryRepository().findAll(),
            () -> getSecondaryRepository() != null ? getSecondaryRepository().findAll() : List.of()
        );
    }
    
    /**
     * Save entity to both databases
     * @param userId User ID for feature flag evaluation
     * @param entity Entity to save
     * @return Saved entity
     */
    protected T saveDual(String userId, T entity) {
        return dualDatabaseStrategy.executeWriteWithUser(
            userId,
            () -> getPrimaryRepository().save(entity),
            () -> getSecondaryRepository() != null ? getSecondaryRepository().save(entity) : entity
        );
    }
    
    /**
     * Save entity to both databases (system operation)
     * @param entity Entity to save
     * @return Saved entity
     */
    protected T saveDual(T entity) {
        return dualDatabaseStrategy.executeWrite(
            () -> getPrimaryRepository().save(entity),
            () -> getSecondaryRepository() != null ? getSecondaryRepository().save(entity) : entity
        );
    }
    
    /**
     * Delete entity from both databases
     * @param userId User ID for feature flag evaluation
     * @param id Entity ID to delete
     */
    protected void deleteByIdDual(String userId, ID id) {
        dualDatabaseStrategy.executeWriteWithUser(
            userId,
            () -> {
                getPrimaryRepository().deleteById(id);
                return null;
            },
            () -> {
                if (getSecondaryRepository() != null) {
                    getSecondaryRepository().deleteById(id);
                }
                return null;
            }
        );
    }
    
    /**
     * Delete entity from both databases (system operation)
     * @param id Entity ID to delete
     */
    protected void deleteByIdDual(ID id) {
        dualDatabaseStrategy.executeWrite(
            () -> {
                getPrimaryRepository().deleteById(id);
                return null;
            },
            () -> {
                if (getSecondaryRepository() != null) {
                    getSecondaryRepository().deleteById(id);
                }
                return null;
            }
        );
    }
    
    /**
     * Execute a custom read operation using dual database strategy
     * @param userId User ID for feature flag evaluation
     * @param primaryOperation Operation to execute on primary database
     * @param secondaryOperation Operation to execute on secondary database
     * @param <R> Return type
     * @return Operation result
     */
    protected <R> R executeReadOperation(String userId, Supplier<R> primaryOperation, Supplier<R> secondaryOperation) {
        return dualDatabaseStrategy.executeRead(userId, primaryOperation, secondaryOperation);
    }
    
    /**
     * Execute a custom write operation using dual database strategy
     * @param userId User ID for feature flag evaluation
     * @param primaryOperation Operation to execute on primary database
     * @param secondaryOperation Operation to execute on secondary database
     * @param <R> Return type
     * @return Operation result
     */
    protected <R> R executeWriteOperation(String userId, Supplier<R> primaryOperation, Supplier<R> secondaryOperation) {
        return dualDatabaseStrategy.executeWriteWithUser(userId, primaryOperation, secondaryOperation);
    }
    
    /**
     * Delete all entities from both databases
     */
    protected void deleteAllDual() {
        dualDatabaseStrategy.executeWrite(
            () -> {
                getPrimaryRepository().deleteAll();
                return null;
            },
            () -> {
                if (getSecondaryRepository() != null) {
                    getSecondaryRepository().deleteAll();
                }
                return null;
            }
        );
    }
    
    /**
     * Count all entities using dual database strategy
     * @return Total count of entities
     */
    protected long countDual() {
        return dualDatabaseStrategy.executeRead(
            "system",
            () -> getPrimaryRepository().count(),
            () -> getSecondaryRepository() != null ? getSecondaryRepository().count() : 0L
        );
    }
    
    /**
     * Check if secondary database is available
     * @return true if secondary database is available
     */
    protected boolean isSecondaryAvailable() {
        return dualDatabaseStrategy.isSecondaryDatabaseEnabled() && getSecondaryRepository() != null;
    }
} 