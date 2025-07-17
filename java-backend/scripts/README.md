# Neon Database Sync Scripts

This directory contains scripts to help you sync your data with the Neon database and manage the dual database setup with DevCycle feature flags.

## 📁 Available Scripts

### 🚀 `sync-with-neon.sh` (Bash Version)

Comprehensive bash script that logs in as admin and syncs all data with Neon.

**Requirements:**

- `curl` (usually pre-installed)
- `jq` (JSON processor)
  - macOS: `brew install jq`
  - Ubuntu: `sudo apt-get install jq`

**Usage:**

```bash
# Basic usage
./scripts/sync-with-neon.sh

# With custom settings
NEON_DATABASE_URL="postgresql://user:pass@host/db" ./scripts/sync-with-neon.sh

# Show help
./scripts/sync-with-neon.sh --help
```

### 🐍 `sync-with-neon.py` (Python Version)

Python equivalent with the same functionality, better for Windows users or Python environments.

**Requirements:**

- Python 3.6+
- `requests` library: `pip install requests`

**Usage:**

```bash
# Basic usage
python3 scripts/sync-with-neon.py

# With custom settings
API_BASE_URL="http://prod.example.com/api" python3 scripts/sync-with-neon.py

# Show help
python3 scripts/sync-with-neon.py --help
```

### 🔍 `verify-setup.py` (Quick Verification)

Quick script to verify your setup without performing sync operations.

## 🔧 Environment Variables

Both scripts support these environment variables:

| Variable         | Default                     | Description              |
| ---------------- | --------------------------- | ------------------------ |
| `API_BASE_URL`   | `http://localhost:3002/api` | Base URL for your API    |
| `ADMIN_USERNAME` | `admin`                     | Admin username for login |
| `ADMIN_PASSWORD` | `password`                  | Admin password for login |

## 🎯 What the Scripts Do

1. **🔌 Connectivity Test**: Verify API is reachable
2. **🔐 Admin Login**: Authenticate and get JWT token
3. **🎛️ DevCycle Check**: Verify feature flag integration
4. **📊 Consistency Check**: Compare primary vs secondary database
5. **🔄 Data Sync**: Sync cart items and perform bidirectional sync
6. **✅ Verification**: Confirm data consistency after sync
7. **🧪 Feature Flag Test**: Test the use-neon flag
8. **📋 Summary**: Show completion status and next steps

## 📋 Prerequisites

Before running the scripts, ensure:

1. **Java Backend Running**: Your Spring Boot application must be running
2. **Neon Database Configured**: Set the following environment variables:
   ```bash
   export NEON_DATABASE_URL="postgresql://username:password@ep-xxx.neon.tech/dbname"
   export SECONDARY_DATABASE_ENABLED=true
   ```
3. **Admin User Exists**: Default admin user should be created (username: `admin`, password: `password`)

## 🔄 Typical Workflow

### 1. Initial Setup

```bash
# Set your Neon database URL
export NEON_DATABASE_URL="postgresql://user:pass@ep-xxx.neon.tech/dbname"

# Start your Java backend
./run.sh
```

### 2. Run Sync Script

```bash
# Choose either bash or Python version
./scripts/sync-with-neon.sh
# OR
python3 scripts/sync-with-neon.py
```

### 3. Monitor Results

The script will show colored output indicating progress:

- 🔵 **[INFO]** - General information
- 🟢 **[SUCCESS]** - Successful operations
- 🟡 **[WARNING]** - Non-critical issues
- 🔴 **[ERROR]** - Critical errors

### 4. Verify Setup

```bash
# Quick verification without sync
python3 scripts/verify-setup.py
```

## 🎛️ DevCycle Feature Flag Setup

After successful sync, configure the `use-neon` flag in DevCycle:

1. **Create Flag**: Key `use-neon`, Type `Boolean`, Default `false`
2. **Set Targeting**: Start with admin users only
3. **Gradual Rollout**: Increase percentage over time
4. **Monitor**: Use admin endpoints to check consistency

## 🔍 Manual Testing

After running the scripts, you can manually test endpoints:

```bash
# Get JWT token first
JWT_TOKEN=$(curl -s -X POST http://localhost:3002/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | jq -r '.accessToken')

# Check consistency
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:3002/api/admin/database/consistency

# Test feature flag
curl -H "Authorization: Bearer $JWT_TOKEN" \
  "http://localhost:3002/api/admin/feature-flags/use-neon/test?userIds=admin,user1"

# Check DevCycle status
curl -H "Authorization: Bearer $JWT_TOKEN" \
  http://localhost:3002/api/admin/feature-flags/devcycle/status
```

## 🚨 Troubleshooting

### Script Fails to Connect

- Ensure Java backend is running on port 3002
- Check `API_BASE_URL` environment variable
- Verify no firewall blocking the connection

### Login Fails

- Verify admin user exists in database
- Check username/password (default: admin/password)
- Ensure JWT configuration is correct

### Sync Fails

- Verify `NEON_DATABASE_URL` is set correctly
- Check `SECONDARY_DATABASE_ENABLED=true`
- Ensure Neon database is accessible
- Check database credentials and permissions

### DevCycle Not Connected

- Set `DEVCYCLE_SERVER_SDK_KEY` environment variable
- Verify SDK key is valid and starts with `dvc_server_`
- Check DevCycle dashboard for flag configuration

## 📞 Support

If you encounter issues:

1. Check the Java application logs
2. Run with `--help` for usage information
3. Verify all environment variables are set
4. Ensure all prerequisites are met

## 🎉 Success Indicators

You'll know the sync was successful when you see:

- ✅ All green success messages
- Consistent database record counts
- Working feature flag evaluation
- No critical errors in the summary
