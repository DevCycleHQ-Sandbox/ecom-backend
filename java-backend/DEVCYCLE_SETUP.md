# DevCycle Feature Flag Setup Guide

## 🎯 Overview

This guide will help you set up the "use-neon" feature flag in DevCycle to control which database your application reads from during the migration process.

## 📋 Prerequisites

1. DevCycle account (sign up at [devcycle.com](https://devcycle.com))
2. Java backend application running
3. Access to environment variables configuration

## 🔧 Step 1: DevCycle Account Setup

### 1.1 Create a DevCycle Project

1. Log into your DevCycle dashboard
2. Create a new project or use an existing one
3. Navigate to **Settings > API Keys**
4. Copy the **Server SDK Key** (starts with `dvc_server_`)

### 1.2 Environment Configuration

Set the following environment variables:

```bash
# Required - Your DevCycle Server SDK Key
DEVCYCLE_SERVER_SDK_KEY=dvc_server_your_actual_key_here

# Optional - Client SDK Key (for frontend integration)
DEVCYCLE_CLIENT_SDK_KEY=dvc_client_your_actual_key_here
```

Or add to your `application.yml`:

```yaml
app:
  devcycle:
    server-sdk-key: ${DEVCYCLE_SERVER_SDK_KEY:your-devcycle-server-sdk-key}
    client-sdk-key: ${DEVCYCLE_CLIENT_SDK_KEY:your-devcycle-client-sdk-key}
```

## 🚀 Step 2: Create the "use-neon" Feature Flag

### 2.1 Create the Flag in DevCycle Dashboard

1. **Navigate to Features**: Go to your DevCycle dashboard → Features
2. **Create Feature**: Click "Create Feature"
3. **Fill in Details**:
   - **Key**: `use-neon` (must match exactly)
   - **Name**: `Use Neon Database`
   - **Description**: `Controls whether to read from Neon (PostgreSQL) or SQLite database`
   - **Type**: `Boolean`
   - **Default Value**: `false`

### 2.2 Configure Variations

Create the following variations:

- **Control**: `false` (use SQLite - primary database)
- **Treatment**: `true` (use Neon - secondary database)

### 2.3 Set Up Targeting Rules

#### Option A: User-based targeting

```
IF user.user_id IN ["admin", "test-user", "beta-user"]
THEN serve "Treatment" (true)
ELSE serve "Control" (false)
```

#### Option B: Percentage rollout

```
Serve "Treatment" to 10% of users
Serve "Control" to 90% of users
```

#### Option C: Environment-based

```
IF user.environment = "staging"
THEN serve "Treatment" (true)
ELSE serve "Control" (false)
```

## 🧪 Step 3: Test the Integration

### 3.1 Start Your Application

```bash
# Make sure your environment variables are set
export DEVCYCLE_SERVER_SDK_KEY=dvc_server_your_actual_key_here

# Start the application
./run.sh
```

### 3.2 Check DevCycle Connection Status

```bash
curl -X GET "http://localhost:3002/api/admin/feature-flags/devcycle/status" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Expected response when connected:

```json
{
  "success": true,
  "status": {
    "isDevCycleConnected": true,
    "source": "DevCycle/OpenFeature",
    "useNeonFlagTests": {
      "admin": false,
      "user1": false,
      "test-user": false
    }
  }
}
```

### 3.3 Test the use-neon Flag

```bash
# Test flag for specific users
curl -X GET "http://localhost:3002/api/admin/feature-flags/use-neon/test?userIds=admin,user1,test-user" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Expected response:

```json
{
  "success": true,
  "flagName": "use-neon",
  "results": {
    "admin": false,
    "user1": false,
    "test-user": false
  },
  "source": "DevCycle/OpenFeature"
}
```

## 🔄 Step 4: Migration Process

### 4.1 Initial State (All users on SQLite)

1. Set flag to `false` for all users
2. Verify all reads come from primary database (SQLite)

### 4.2 Sync Data to Neon

```bash
# Sync existing data to Neon database
curl -X POST "http://localhost:3002/api/admin/database/sync/cart-items" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 4.3 Gradual Rollout

1. **Phase 1**: Enable for test users only

   ```
   Target: user.user_id IN ["admin", "test-user"]
   Serve: true
   ```

2. **Phase 2**: Enable for 10% of users

   ```
   Target: 10% of users
   Serve: true
   ```

3. **Phase 3**: Enable for 50% of users

   ```
   Target: 50% of users
   Serve: true
   ```

4. **Phase 4**: Enable for all users
   ```
   Target: All users
   Serve: true
   ```

### 4.4 Monitor and Verify

```bash
# Check data consistency between databases
curl -X GET "http://localhost:3002/api/admin/database/consistency" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Perform bidirectional sync if needed
curl -X POST "http://localhost:3002/api/admin/database/sync/bidirectional" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 🛠️ Step 5: Advanced Configuration

### 5.1 User Context Properties

You can use additional properties for targeting:

```javascript
// Example user context
{
  "user_id": "user123",
  "email": "user@example.com",
  "plan": "premium",
  "region": "us-east",
  "environment": "production"
}
```

### 5.2 Custom Targeting Rules

```
IF user.plan = "premium" AND user.region = "us-east"
THEN serve "Treatment" (true)
ELSE serve "Control" (false)
```

### 5.3 Emergency Rollback

In case of issues, you can instantly rollback:

1. Go to DevCycle dashboard
2. Set `use-neon` flag to `false` for all users
3. All reads will immediately go back to SQLite

## 📊 Step 6: Monitoring and Metrics

### 6.1 DevCycle Analytics

- Track flag evaluation metrics in DevCycle dashboard
- Monitor user distribution between variations
- View performance impact

### 6.2 Application Logs

Monitor application logs for:

```
INFO  - 🎛️ DevCycle feature flag 'use-neon' evaluated to: true for admin user
INFO  - Using secondary (Neon) database for read operation for user: user123
INFO  - Primary database write completed successfully for user: user123
INFO  - Secondary database write completed successfully for user: user123
```

### 6.3 Health Checks

```bash
# Regular consistency checks
curl -X GET "http://localhost:3002/api/admin/database/consistency"

# Monitor flag status
curl -X GET "http://localhost:3002/api/admin/feature-flags/devcycle/status"
```

## 🚨 Troubleshooting

### Issue: "DevCycle not available"

**Cause**: Invalid or missing SDK key
**Solution**:

1. Verify `DEVCYCLE_SERVER_SDK_KEY` is set correctly
2. Check key starts with `dvc_server_`
3. Ensure key has proper permissions

### Issue: Flag always returns default value

**Cause**: Flag not found or user context missing
**Solution**:

1. Verify flag key is exactly `use-neon`
2. Check flag is published in DevCycle
3. Ensure user context includes `user_id`

### Issue: Inconsistent data between databases

**Cause**: Sync issues or write failures
**Solution**:

1. Run bidirectional sync
2. Check database connection health
3. Monitor write operation logs

## 📝 Example Integration Code

If you need to check the flag in your service layer:

```java
@Service
public class YourService {
    private final FeatureFlagService featureFlagService;

    public void someMethod(String userId) {
        boolean useNeon = featureFlagService.getBooleanValue(userId, "use-neon", false);

        if (useNeon) {
            log.info("User {} is using Neon database", userId);
            // Logic for Neon database usage
        } else {
            log.info("User {} is using SQLite database", userId);
            // Logic for SQLite database usage
        }
    }
}
```

## 🎉 Success!

You now have a fully functional DevCycle-powered feature flag system for database migration! The "use-neon" flag will control which database users read from while maintaining dual-write for data consistency.
