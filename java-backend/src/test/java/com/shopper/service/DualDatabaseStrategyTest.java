package com.shopper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DualDatabaseStrategy implementation
 * Tests feature flag controlled routing and error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DualDatabaseStrategy Unit Tests")
class DualDatabaseStrategyTest {

    @Mock
    private FeatureFlagService featureFlagService;

    @InjectMocks
    private DualDatabaseStrategyImpl dualDatabaseStrategy;

    private static final String TEST_USER_ID = "test-user-123";
    private static final String USE_NEON_FLAG = "use-neon";

    @BeforeEach
    void setUp() {
        // Set secondary database as enabled for testing
        ReflectionTestUtils.setField(dualDatabaseStrategy, "secondaryDatabaseEnabled", true);
    }

    @Test
    @DisplayName("Should use primary database when secondary is disabled")
    void testUsePrimaryWhenSecondaryDisabled() {
        // Disable secondary database
        ReflectionTestUtils.setField(dualDatabaseStrategy, "secondaryDatabaseEnabled", false);

        Supplier<String> primaryOperation = () -> "primary-result";
        Supplier<String> secondaryOperation = () -> "secondary-result";

        String result = dualDatabaseStrategy.executeRead(TEST_USER_ID, primaryOperation, secondaryOperation);

        assertThat(result).isEqualTo("primary-result");
        verifyNoInteractions(featureFlagService);
    }

    @Test
    @DisplayName("Should use primary database when feature flag is false")
    void testUsePrimaryWhenFeatureFlagFalse() {
        when(featureFlagService.getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false))
            .thenReturn(false);

        Supplier<String> primaryOperation = () -> "primary-result";
        Supplier<String> secondaryOperation = () -> "secondary-result";

        String result = dualDatabaseStrategy.executeRead(TEST_USER_ID, primaryOperation, secondaryOperation);

        assertThat(result).isEqualTo("primary-result");
        verify(featureFlagService).getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false);
    }

    @Test
    @DisplayName("Should use secondary database when feature flag is true")
    void testUseSecondaryWhenFeatureFlagTrue() {
        when(featureFlagService.getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false))
            .thenReturn(true);

        Supplier<String> primaryOperation = () -> "primary-result";
        Supplier<String> secondaryOperation = () -> "secondary-result";

        String result = dualDatabaseStrategy.executeRead(TEST_USER_ID, primaryOperation, secondaryOperation);

        assertThat(result).isEqualTo("secondary-result");
        verify(featureFlagService).getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false);
    }

    @Test
    @DisplayName("Should fallback to primary when secondary fails during read")
    void testFallbackToPrimaryOnSecondaryFailure() {
        when(featureFlagService.getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false))
            .thenReturn(true);

        Supplier<String> primaryOperation = () -> "primary-result";
        Supplier<String> secondaryOperation = () -> {
            throw new RuntimeException("Secondary database connection failed");
        };

        String result = dualDatabaseStrategy.executeRead(TEST_USER_ID, primaryOperation, secondaryOperation);

        assertThat(result).isEqualTo("primary-result");
        verify(featureFlagService).getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false);
    }

    @Test
    @DisplayName("Should execute write operations on both databases")
    void testWriteToBothDatabases() {
        when(featureFlagService.getBooleanValue("system", USE_NEON_FLAG, false))
            .thenReturn(false);

        Supplier<String> primaryOperation = () -> "primary-write-result";
        Supplier<String> secondaryOperation = () -> "secondary-write-result";

        String result = dualDatabaseStrategy.executeWrite(primaryOperation, secondaryOperation);

        assertThat(result).isEqualTo("primary-write-result");
    }

    @Test
    @DisplayName("Should execute write operations with user context")
    void testWriteWithUserContext() {
        when(featureFlagService.getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false))
            .thenReturn(true);

        Supplier<String> primaryOperation = () -> "primary-write-result";
        Supplier<String> secondaryOperation = () -> "secondary-write-result";

        String result = dualDatabaseStrategy.executeWriteWithUser(TEST_USER_ID, primaryOperation, secondaryOperation);

        // When feature flag is true, should return secondary result
        assertThat(result).isEqualTo("secondary-write-result");
        verify(featureFlagService).getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false);
    }

    @Test
    @DisplayName("Should return primary result when feature flag is false for writes")
    void testWriteReturnsPrimaryWhenFeatureFlagFalse() {
        when(featureFlagService.getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false))
            .thenReturn(false);

        Supplier<String> primaryOperation = () -> "primary-write-result";
        Supplier<String> secondaryOperation = () -> "secondary-write-result";

        String result = dualDatabaseStrategy.executeWriteWithUser(TEST_USER_ID, primaryOperation, secondaryOperation);

        assertThat(result).isEqualTo("primary-write-result");
        verify(featureFlagService).getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false);
    }

    @Test
    @DisplayName("Should handle primary write failure gracefully")
    void testHandlePrimaryWriteFailure() {
        when(featureFlagService.getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false))
            .thenReturn(true);

        Supplier<String> primaryOperation = () -> {
            throw new RuntimeException("Primary database write failed");
        };
        Supplier<String> secondaryOperation = () -> "secondary-write-result";

        String result = dualDatabaseStrategy.executeWriteWithUser(TEST_USER_ID, primaryOperation, secondaryOperation);

        // Should still return secondary result even if primary fails
        assertThat(result).isEqualTo("secondary-write-result");
    }

    @Test
    @DisplayName("Should throw exception when primary write fails and feature flag is false")
    void testThrowExceptionWhenPrimaryFailsAndFeatureFlagFalse() {
        when(featureFlagService.getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false))
            .thenReturn(false);

        Supplier<String> primaryOperation = () -> {
            throw new RuntimeException("Primary database write failed");
        };
        Supplier<String> secondaryOperation = () -> "secondary-write-result";

        assertThatThrownBy(() -> 
            dualDatabaseStrategy.executeWriteWithUser(TEST_USER_ID, primaryOperation, secondaryOperation)
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Primary database operation failed");
    }

    @Test
    @DisplayName("Should handle secondary write failure gracefully")
    void testHandleSecondaryWriteFailure() {
        when(featureFlagService.getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false))
            .thenReturn(false);

        Supplier<String> primaryOperation = () -> "primary-write-result";
        Supplier<String> secondaryOperation = () -> {
            throw new RuntimeException("Secondary database write failed");
        };

        String result = dualDatabaseStrategy.executeWriteWithUser(TEST_USER_ID, primaryOperation, secondaryOperation);

        // Should return primary result when feature flag is false
        assertThat(result).isEqualTo("primary-write-result");
    }

    @Test
    @DisplayName("Should throw exception when both writes fail")
    void testThrowExceptionWhenBothWritesFail() {
        when(featureFlagService.getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false))
            .thenReturn(false);

        Supplier<String> primaryOperation = () -> {
            throw new RuntimeException("Primary database write failed");
        };
        Supplier<String> secondaryOperation = () -> {
            throw new RuntimeException("Secondary database write failed");
        };

        assertThatThrownBy(() -> 
            dualDatabaseStrategy.executeWriteWithUser(TEST_USER_ID, primaryOperation, secondaryOperation)
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Primary database operation failed");
    }

    @Test
    @DisplayName("Should handle feature flag service failure gracefully")
    void testHandleFeatureFlagServiceFailure() {
        when(featureFlagService.getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false))
            .thenThrow(new RuntimeException("Feature flag service unavailable"));

        Supplier<String> primaryOperation = () -> "primary-result";
        Supplier<String> secondaryOperation = () -> "secondary-result";

        // Should default to false when feature flag service fails
        String result = dualDatabaseStrategy.executeRead(TEST_USER_ID, primaryOperation, secondaryOperation);

        assertThat(result).isEqualTo("primary-result");
        verify(featureFlagService).getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false);
    }

    @Test
    @DisplayName("Should return correct secondary database enabled status")
    void testSecondaryDatabaseEnabledStatus() {
        assertThat(dualDatabaseStrategy.isSecondaryDatabaseEnabled()).isTrue();

        ReflectionTestUtils.setField(dualDatabaseStrategy, "secondaryDatabaseEnabled", false);
        assertThat(dualDatabaseStrategy.isSecondaryDatabaseEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should return correct feature flag evaluation for secondary usage")
    void testShouldUseSecondaryForRead() {
        when(featureFlagService.getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false))
            .thenReturn(true);

        boolean shouldUseSecondary = dualDatabaseStrategy.shouldUseSecondaryForRead(TEST_USER_ID);

        assertThat(shouldUseSecondary).isTrue();
        verify(featureFlagService).getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false);
    }

    @Test
    @DisplayName("Should return false for secondary usage when secondary database is disabled")
    void testShouldUseSecondaryReturnsFalseWhenDisabled() {
        ReflectionTestUtils.setField(dualDatabaseStrategy, "secondaryDatabaseEnabled", false);

        boolean shouldUseSecondary = dualDatabaseStrategy.shouldUseSecondaryForRead(TEST_USER_ID);

        assertThat(shouldUseSecondary).isFalse();
        verifyNoInteractions(featureFlagService);
    }

    @Test
    @DisplayName("Should return false for secondary usage when feature flag service fails")
    void testShouldUseSecondaryReturnsFalseOnFeatureFlagFailure() {
        when(featureFlagService.getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false))
            .thenThrow(new RuntimeException("Feature flag service error"));

        boolean shouldUseSecondary = dualDatabaseStrategy.shouldUseSecondaryForRead(TEST_USER_ID);

        assertThat(shouldUseSecondary).isFalse();
        verify(featureFlagService).getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false);
    }

    @Test
    @DisplayName("Should execute multiple operations consistently")
    void testMultipleOperationsConsistency() {
        when(featureFlagService.getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false))
            .thenReturn(true);

        Supplier<String> primaryOperation = () -> "primary-result";
        Supplier<String> secondaryOperation = () -> "secondary-result";

        // Execute multiple read operations
        for (int i = 0; i < 5; i++) {
            String result = dualDatabaseStrategy.executeRead(TEST_USER_ID, primaryOperation, secondaryOperation);
            assertThat(result).isEqualTo("secondary-result");
        }

        verify(featureFlagService, times(5)).getBooleanValue(TEST_USER_ID, USE_NEON_FLAG, false);
    }

    @Test
    @DisplayName("Should handle different user IDs correctly")
    void testDifferentUserIds() {
        String user1 = "user1";
        String user2 = "user2";

        when(featureFlagService.getBooleanValue(user1, USE_NEON_FLAG, false))
            .thenReturn(true);
        when(featureFlagService.getBooleanValue(user2, USE_NEON_FLAG, false))
            .thenReturn(false);

        Supplier<String> primaryOperation = () -> "primary-result";
        Supplier<String> secondaryOperation = () -> "secondary-result";

        String result1 = dualDatabaseStrategy.executeRead(user1, primaryOperation, secondaryOperation);
        String result2 = dualDatabaseStrategy.executeRead(user2, primaryOperation, secondaryOperation);

        assertThat(result1).isEqualTo("secondary-result");
        assertThat(result2).isEqualTo("primary-result");

        verify(featureFlagService).getBooleanValue(user1, USE_NEON_FLAG, false);
        verify(featureFlagService).getBooleanValue(user2, USE_NEON_FLAG, false);
    }
} 