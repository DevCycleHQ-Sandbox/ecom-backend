package com.shopper.service;

import com.shopper.entity.CartItem;
import com.shopper.entity.Order;
import com.shopper.entity.Product;
import com.shopper.entity.User;
import com.shopper.repository.primary.PrimaryCartItemRepository;
import com.shopper.repository.primary.PrimaryProductRepository;
import com.shopper.repository.secondary.SecondaryCartItemRepository;
import com.shopper.repository.secondary.SecondaryProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for synchronizing data between primary and secondary databases.
 * Used for database migration and ensuring data consistency.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "secondary.datasource.enabled", havingValue = "true")
public class DatabaseSyncService {
    
    private final PrimaryCartItemRepository primaryCartItemRepository;
    private final PrimaryProductRepository primaryProductRepository;
    
    @Autowired(required = false)
    private SecondaryCartItemRepository secondaryCartItemRepository;
    
    @Autowired(required = false)
    private SecondaryProductRepository secondaryProductRepository;
    
    @Autowired(required = false)
    @Qualifier("secondaryEntityManagerFactory")
    private EntityManagerFactory secondaryEntityManagerFactory;
    
    private final DualDatabaseStrategy dualDatabaseStrategy;
    
    /**
     * Sync all products from primary to secondary database
     * @return Number of synced records
     */
    public int syncProductsToSecondary() {
        if (!dualDatabaseStrategy.isSecondaryDatabaseEnabled() || secondaryProductRepository == null) {
            log.warn("Secondary database not available, skipping products sync");
            return 0;
        }
        
        log.info("Starting products synchronization from primary to secondary database");
        
        try {
            List<Product> primaryProducts = primaryProductRepository.findAll();
            log.info("Found {} products in primary database", primaryProducts.size());
            
            AtomicInteger syncedCount = new AtomicInteger(0);
            
            for (Product product : primaryProducts) {
                try {
                    // Check if product already exists in secondary
                    if (!secondaryProductRepository.existsById(product.getId())) {
                        Product savedProduct = secondaryProductRepository.save(product);
                        syncedCount.incrementAndGet();
                        log.debug("Synced product: {} ({})", savedProduct.getName(), savedProduct.getId());
                    } else {
                        log.debug("Product already exists in secondary: {} ({})", product.getName(), product.getId());
                    }
                } catch (Exception e) {
                    log.error("Failed to sync product {}: {}", product.getId(), e.getMessage());
                }
            }
            
            int finalCount = syncedCount.get();
            log.info("Products synchronization completed. Synced {} out of {} products", finalCount, primaryProducts.size());
            return finalCount;
            
        } catch (Exception e) {
            log.error("Error during products sync: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Sync all products from secondary to primary database
     * @return Number of synced records
     */
    public int syncProductsToPrimary() {
        if (!dualDatabaseStrategy.isSecondaryDatabaseEnabled() || secondaryProductRepository == null) {
            log.warn("Secondary database not available, skipping reverse products sync");
            return 0;
        }
        
        log.info("Starting products synchronization from secondary to primary database");
        
        try {
            List<Product> secondaryProducts = secondaryProductRepository.findAll();
            log.info("Found {} products in secondary database", secondaryProducts.size());
            
            AtomicInteger syncedCount = new AtomicInteger(0);
            
            for (Product product : secondaryProducts) {
                try {
                    // Check if product already exists in primary
                    if (!primaryProductRepository.existsById(product.getId())) {
                        Product savedProduct = primaryProductRepository.save(product);
                        syncedCount.incrementAndGet();
                        log.debug("Reverse synced product: {} ({})", savedProduct.getName(), savedProduct.getId());
                    } else {
                        log.debug("Product already exists in primary: {} ({})", product.getName(), product.getId());
                    }
                } catch (Exception e) {
                    log.error("Failed to reverse sync product {}: {}", product.getId(), e.getMessage());
                }
            }
            
            int finalCount = syncedCount.get();
            log.info("Reverse products synchronization completed. Synced {} out of {} products", finalCount, secondaryProducts.size());
            return finalCount;
            
        } catch (Exception e) {
            log.error("Error during reverse products sync: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Sync all cart items from primary to secondary database
     * @return Number of synced records
     */
    public int syncCartItemsToSecondary() {
        if (!dualDatabaseStrategy.isSecondaryDatabaseEnabled() || secondaryCartItemRepository == null) {
            log.warn("Secondary database not available, skipping cart items sync");
            return 0;
        }
        
        log.info("Starting cart items synchronization from primary to secondary database");
        
        try {
            // Use a simple query to avoid eager loading issues with Product relationships
            List<CartItem> primaryCartItems = primaryCartItemRepository.findAll();
            log.info("Found {} cart items in primary database", primaryCartItems.size());
            AtomicInteger syncCount = new AtomicInteger(0);
            
            for (CartItem cartItem : primaryCartItems) {
                try {
                    // Check if item already exists in secondary database
                    boolean exists = secondaryCartItemRepository.findById(cartItem.getId()).isPresent();
                    
                    if (!exists) {
                        // Create a detached copy without relationships to avoid cross-database issues
                        CartItem detachedCartItem = CartItem.builder()
                            .id(cartItem.getId())
                            .userId(cartItem.getUserId())
                            .productId(cartItem.getProductId())
                            .quantity(cartItem.getQuantity())
                            .createdAt(cartItem.getCreatedAt())
                            .updatedAt(cartItem.getUpdatedAt())
                            .build();
                        
                        secondaryCartItemRepository.save(detachedCartItem);
                        syncCount.incrementAndGet();
                        log.debug("Synced cart item: {}", cartItem.getId());
                    } else {
                        log.debug("Cart item already exists in secondary: {}", cartItem.getId());
                    }
                } catch (Exception e) {
                    log.error("Failed to sync cart item {}: {}", cartItem.getId(), e.getMessage());
                }
            }
            
            log.info("Cart items synchronization completed. Synced {} out of {} items", 
                    syncCount.get(), primaryCartItems.size());
            return syncCount.get();
            
        } catch (Exception e) {
            log.error("Error during cart items synchronization: {}", e.getMessage(), e);
            throw new RuntimeException("Cart items synchronization failed", e);
        }
    }
    
    /**
     * Sync all cart items from secondary to primary database
     * @return Number of synced records
     */
    public int syncCartItemsToPrimary() {
        if (!dualDatabaseStrategy.isSecondaryDatabaseEnabled() || secondaryCartItemRepository == null) {
            log.warn("Secondary database not available, skipping reverse cart items sync");
            return 0;
        }
        
        log.info("Starting cart items synchronization from secondary to primary database");
        
        try {
            List<CartItem> secondaryCartItems = secondaryCartItemRepository.findAll();
            AtomicInteger syncCount = new AtomicInteger(0);
            
            for (CartItem cartItem : secondaryCartItems) {
                try {
                    // Check if item already exists in primary database
                    boolean exists = primaryCartItemRepository.findById(cartItem.getId()).isPresent();
                    
                    if (!exists) {
                        // Create a detached copy without relationships to avoid cross-database issues
                        CartItem detachedCartItem = CartItem.builder()
                            .id(cartItem.getId())
                            .userId(cartItem.getUserId())
                            .productId(cartItem.getProductId())
                            .quantity(cartItem.getQuantity())
                            .createdAt(cartItem.getCreatedAt())
                            .updatedAt(cartItem.getUpdatedAt())
                            .build();
                        
                        primaryCartItemRepository.save(detachedCartItem);
                        syncCount.incrementAndGet();
                        log.debug("Synced cart item to primary: {}", cartItem.getId());
                    } else {
                        log.debug("Cart item already exists in primary: {}", cartItem.getId());
                    }
                } catch (Exception e) {
                    log.error("Failed to sync cart item to primary {}: {}", cartItem.getId(), e.getMessage());
                }
            }
            
            log.info("Reverse cart items synchronization completed. Synced {} out of {} items", 
                    syncCount.get(), secondaryCartItems.size());
            return syncCount.get();
            
        } catch (Exception e) {
            log.error("Error during reverse cart items synchronization: {}", e.getMessage(), e);
            throw new RuntimeException("Reverse cart items synchronization failed", e);
        }
    }
    
    /**
     * Perform bidirectional sync of cart items
     * @return SyncResult containing sync statistics
     */
    public SyncResult performBidirectionalSync() {
        if (!dualDatabaseStrategy.isSecondaryDatabaseEnabled()) {
            log.warn("Secondary database not available, skipping bidirectional sync");
            return new SyncResult(0, 0, false, "Secondary database not available");
        }
        
        log.info("Starting bidirectional synchronization");
        
        try {
            // First sync products (prerequisite for cart items)
            log.info("Starting product synchronization phase");
            CompletableFuture<Integer> productsToSecondary = CompletableFuture.supplyAsync(this::syncProductsToSecondary);
            CompletableFuture<Integer> productsToPrimary = CompletableFuture.supplyAsync(this::syncProductsToPrimary);
            
            int productsPrimaryToSecondary = productsToSecondary.get();
            int productsSecondaryToPrimary = productsToPrimary.get();
            
            log.info("Product sync completed. Primary->Secondary: {}, Secondary->Primary: {}", 
                    productsPrimaryToSecondary, productsSecondaryToPrimary);
            
            // Then sync cart items asynchronously
            log.info("Starting cart items synchronization phase");
            CompletableFuture<Integer> cartItemsToSecondary = CompletableFuture.supplyAsync(this::syncCartItemsToSecondary);
            CompletableFuture<Integer> cartItemsToPrimary = CompletableFuture.supplyAsync(this::syncCartItemsToPrimary);
            
            // Wait for both operations to complete
            int cartItemsPrimaryToSecondary = cartItemsToSecondary.get();
            int cartItemsSecondaryToPrimary = cartItemsToPrimary.get();
            
            int totalPrimaryToSecondary = productsPrimaryToSecondary + cartItemsPrimaryToSecondary;
            int totalSecondaryToPrimary = productsSecondaryToPrimary + cartItemsSecondaryToPrimary;
            
            log.info("Bidirectional synchronization completed. Products (P->S: {}, S->P: {}), Cart Items (P->S: {}, S->P: {})", 
                    productsPrimaryToSecondary, productsSecondaryToPrimary, cartItemsPrimaryToSecondary, cartItemsSecondaryToPrimary);
            
            return new SyncResult(totalPrimaryToSecondary, totalSecondaryToPrimary, true, 
                String.format("Sync completed successfully. Products: %d->%d, Cart Items: %d->%d", 
                    productsPrimaryToSecondary, productsSecondaryToPrimary, cartItemsPrimaryToSecondary, cartItemsSecondaryToPrimary));
            
        } catch (Exception e) {
            log.error("Error during bidirectional synchronization: {}", e.getMessage(), e);
            return new SyncResult(0, 0, false, "Sync failed: " + e.getMessage());
        }
    }
    
    /**
     * Verify data consistency between databases
     * @return ConsistencyReport
     */
    public ConsistencyReport verifyDataConsistency() {
        if (!dualDatabaseStrategy.isSecondaryDatabaseEnabled() || secondaryCartItemRepository == null) {
            return new ConsistencyReport(0, 0, 0, false, "Secondary database not available");
        }
        
        log.info("Starting data consistency verification");
        
        try {
            List<CartItem> primaryItems = primaryCartItemRepository.findAll();
            List<CartItem> secondaryItems = secondaryCartItemRepository.findAll();
            
            int primaryCount = primaryItems.size();
            int secondaryCount = secondaryItems.size();
            int inconsistencies = Math.abs(primaryCount - secondaryCount);
            
            // More detailed consistency check could be implemented here
            // For now, we just compare counts
            
            boolean isConsistent = primaryCount == secondaryCount;
            String message = isConsistent ? 
                "Databases are consistent" : 
                String.format("Inconsistency detected: Primary has %d items, Secondary has %d items", 
                             primaryCount, secondaryCount);
            
            log.info("Data consistency check completed: {}", message);
            
            return new ConsistencyReport(primaryCount, secondaryCount, inconsistencies, isConsistent, message);
            
                } catch (Exception e) {
            log.error("Error during consistency verification: {}", e.getMessage(), e);
            return new ConsistencyReport(0, 0, -1, false, "Consistency check failed: " + e.getMessage());
        }
    }
    
    /**
     * Fix PostgreSQL schema to use proper UUID columns instead of VARCHAR
     * This method converts existing VARCHAR(36) columns to UUID type in PostgreSQL
     */
    @Transactional("secondaryTransactionManager")
    public String fixPostgreSQLSchema() {
        log.info("Starting PostgreSQL schema fix to convert VARCHAR columns to UUID");
        
        try {
            // Get the secondary entity manager to execute native SQL
            EntityManager entityManager = secondaryEntityManagerFactory.createEntityManager();
            
            try {
                entityManager.getTransaction().begin();
                
                StringBuilder result = new StringBuilder();
                
                // First, drop ALL foreign key constraints
                try {
                    Query dropAllConstraintsQuery = entityManager.createNativeQuery(
                        "DO $$ " +
                        "DECLARE " +
                        "    r RECORD; " +
                        "BEGIN " +
                        "    FOR r IN (SELECT constraint_name, table_name " +
                        "              FROM information_schema.table_constraints " +
                        "              WHERE constraint_type = 'FOREIGN KEY' " +
                        "              AND table_schema = 'public') " +
                        "    LOOP " +
                        "        EXECUTE 'ALTER TABLE ' || r.table_name || ' DROP CONSTRAINT ' || r.constraint_name; " +
                        "    END LOOP; " +
                        "END $$;"
                    );
                    dropAllConstraintsQuery.executeUpdate();
                    result.append("Dropped all foreign key constraints\n");
                } catch (Exception e) {
                    log.warn("Could not drop all foreign key constraints: {}", e.getMessage());
                }
                
                // Convert products table ID from VARCHAR to UUID
                Query convertProductsQuery = entityManager.createNativeQuery(
                    "ALTER TABLE products ALTER COLUMN id TYPE UUID USING CAST(id AS UUID)"
                );
                convertProductsQuery.executeUpdate();
                result.append("Converted products.id to UUID\n");
                
                // Convert cart_items table columns from VARCHAR to UUID
                Query convertCartItemsIdQuery = entityManager.createNativeQuery(
                    "ALTER TABLE cart_items ALTER COLUMN id TYPE UUID USING CAST(id AS UUID)"
                );
                convertCartItemsIdQuery.executeUpdate();
                result.append("Converted cart_items.id to UUID\n");
                
                Query convertCartItemsUserIdQuery = entityManager.createNativeQuery(
                    "ALTER TABLE cart_items ALTER COLUMN user_id TYPE UUID USING CAST(user_id AS UUID)"
                );
                convertCartItemsUserIdQuery.executeUpdate();
                result.append("Converted cart_items.user_id to UUID\n");
                
                Query convertCartItemsProductIdQuery = entityManager.createNativeQuery(
                    "ALTER TABLE cart_items ALTER COLUMN product_id TYPE UUID USING CAST(product_id AS UUID)"
                );
                convertCartItemsProductIdQuery.executeUpdate();
                result.append("Converted cart_items.product_id to UUID\n");
                
                // Convert other tables if they exist
                try {
                    Query convertOrdersQuery = entityManager.createNativeQuery(
                        "DO $$ " +
                        "BEGIN " +
                        "    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'orders') THEN " +
                        "        ALTER TABLE orders ALTER COLUMN id TYPE UUID USING CAST(id AS UUID); " +
                        "        ALTER TABLE orders ALTER COLUMN user_id TYPE UUID USING CAST(user_id AS UUID); " +
                        "    END IF; " +
                        "    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'order_items') THEN " +
                        "        ALTER TABLE order_items ALTER COLUMN id TYPE UUID USING CAST(id AS UUID); " +
                        "        ALTER TABLE order_items ALTER COLUMN order_id TYPE UUID USING CAST(order_id AS UUID); " +
                        "        ALTER TABLE order_items ALTER COLUMN product_id TYPE UUID USING CAST(product_id AS UUID); " +
                        "    END IF; " +
                        "    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN " +
                        "        ALTER TABLE users ALTER COLUMN id TYPE UUID USING CAST(id AS UUID); " +
                        "    END IF; " +
                        "END $$;"
                    );
                    convertOrdersQuery.executeUpdate();
                    result.append("Converted other tables to UUID (if they exist)\n");
                } catch (Exception e) {
                    log.warn("Could not convert other tables: {}", e.getMessage());
                }
                
                // Re-add foreign key constraints
                try {
                    // Add cart_items -> products foreign key
                    Query addCartItemsProductConstraint = entityManager.createNativeQuery(
                        "ALTER TABLE cart_items ADD CONSTRAINT fk_cart_items_product_id " +
                        "FOREIGN KEY (product_id) REFERENCES products(id)"
                    );
                    addCartItemsProductConstraint.executeUpdate();
                    
                    // Add other constraints if tables exist
                    Query addOtherConstraints = entityManager.createNativeQuery(
                        "DO $$ " +
                        "BEGIN " +
                        "    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'orders') THEN " +
                        "        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN " +
                        "            ALTER TABLE orders ADD CONSTRAINT fk_orders_user_id " +
                        "            FOREIGN KEY (user_id) REFERENCES users(id); " +
                        "        END IF; " +
                        "    END IF; " +
                        "    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'order_items') THEN " +
                        "        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'orders') THEN " +
                        "            ALTER TABLE order_items ADD CONSTRAINT fk_order_items_order_id " +
                        "            FOREIGN KEY (order_id) REFERENCES orders(id); " +
                        "        END IF; " +
                        "        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'products') THEN " +
                        "            ALTER TABLE order_items ADD CONSTRAINT fk_order_items_product_id " +
                        "            FOREIGN KEY (product_id) REFERENCES products(id); " +
                        "        END IF; " +
                        "    END IF; " +
                        "END $$;"
                    );
                    addOtherConstraints.executeUpdate();
                    result.append("Re-added foreign key constraints\n");
                } catch (Exception e) {
                    log.warn("Could not re-add some foreign key constraints: {}", e.getMessage());
                    result.append("Warning: Some foreign key constraints could not be re-added\n");
                }
                
                entityManager.getTransaction().commit();
                
                String finalResult = result.toString();
                log.info("PostgreSQL schema fix completed successfully: {}", finalResult);
                return finalResult;
                
            } finally {
                entityManager.close();
            }
            
        } catch (Exception e) {
            log.error("Failed to fix PostgreSQL schema: {}", e.getMessage(), e);
            throw new RuntimeException("PostgreSQL schema fix failed: " + e.getMessage(), e);
        }
    }

    /**
     * Result of a synchronization operation
     */
    public static class SyncResult {
        public final int primaryToSecondaryCount;
        public final int secondaryToPrimaryCount;
        public final boolean success;
        public final String message;
        public final LocalDateTime timestamp;
        
        public SyncResult(int primaryToSecondaryCount, int secondaryToPrimaryCount, boolean success, String message) {
            this.primaryToSecondaryCount = primaryToSecondaryCount;
            this.secondaryToPrimaryCount = secondaryToPrimaryCount;
            this.success = success;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }
    }
    
    /**
     * Report of data consistency verification
     */
    public static class ConsistencyReport {
        public final int primaryCount;
        public final int secondaryCount;
        public final int inconsistencies;
        public final boolean isConsistent;
        public final String message;
        public final LocalDateTime timestamp;
        
        public ConsistencyReport(int primaryCount, int secondaryCount, int inconsistencies, 
                               boolean isConsistent, String message) {
            this.primaryCount = primaryCount;
            this.secondaryCount = secondaryCount;
            this.inconsistencies = inconsistencies;
            this.isConsistent = isConsistent;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }
    }
} 