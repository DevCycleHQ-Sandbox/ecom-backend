#!/usr/bin/env python3
"""
Quick Setup Verification Script
This script verifies your Neon database setup without performing sync operations
"""

import requests
import os
import sys

# Configuration
API_BASE_URL = os.getenv('API_BASE_URL', 'http://localhost:3002/api')
ADMIN_USERNAME = os.getenv('ADMIN_USERNAME', 'admin')
ADMIN_PASSWORD = os.getenv('ADMIN_PASSWORD', 'password')

# Colors
class Colors:
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    BLUE = '\033[0;34m'
    NC = '\033[0m'

def print_check(message: str, status: bool, details: str = ""):
    icon = "✅" if status else "❌"
    color = Colors.GREEN if status else Colors.RED
    print(f"{icon} {color}{message}{Colors.NC}")
    if details:
        print(f"   {details}")

def print_info(message: str):
    print(f"{Colors.BLUE}ℹ️  {message}{Colors.NC}")

def print_warning(message: str):
    print(f"{Colors.YELLOW}⚠️  {message}{Colors.NC}")

def check_api_connectivity():
    """Check if API is reachable"""
    try:
        response = requests.get(f"{API_BASE_URL}/auth/login", timeout=5)
        return True, f"API reachable at {API_BASE_URL}"
    except requests.RequestException as e:
        return False, f"Cannot reach API: {e}"

def check_admin_login():
    """Check admin login"""
    try:
        response = requests.post(
            f"{API_BASE_URL}/auth/login",
            json={"username": ADMIN_USERNAME, "password": ADMIN_PASSWORD},
            timeout=10
        )
        
        if response.status_code == 200:
            data = response.json()
            if 'accessToken' in data:
                return True, f"Admin login successful", data['accessToken']
            else:
                return False, "No access token in response", None
        else:
            return False, f"Login failed with status {response.status_code}", None
    except requests.RequestException as e:
        return False, f"Login request failed: {e}", None

def check_devcycle_status(token):
    """Check DevCycle integration"""
    try:
        headers = {'Authorization': f'Bearer {token}'}
        response = requests.get(f"{API_BASE_URL}/admin/feature-flags/devcycle/status", headers=headers)
        
        if response.status_code == 200:
            data = response.json()
            if data.get('success'):
                status = data.get('status', {})
                is_connected = status.get('isDevCycleConnected', False)
                source = status.get('source', 'Unknown')
                return is_connected, f"DevCycle status: {source}"
            else:
                return False, "DevCycle status check failed"
        else:
            return False, f"DevCycle check failed with status {response.status_code}"
    except requests.RequestException as e:
        return False, f"DevCycle check failed: {e}"

def check_database_consistency(token):
    """Check database consistency"""
    try:
        headers = {'Authorization': f'Bearer {token}'}
        response = requests.get(f"{API_BASE_URL}/admin/database/consistency", headers=headers)
        
        if response.status_code == 200:
            data = response.json()
            if data.get('success'):
                primary_count = data.get('primaryCount', 0)
                secondary_count = data.get('secondaryCount', 0)
                is_consistent = data.get('isConsistent', False)
                message = data.get('message', '')
                
                return True, f"Primary: {primary_count}, Secondary: {secondary_count} - {message}"
            else:
                return False, "Consistency check failed"
        elif response.status_code == 503:
            return False, "Secondary database not available (check NEON_DATABASE_URL)"
        else:
            return False, f"Consistency check failed with status {response.status_code}"
    except requests.RequestException as e:
        return False, f"Consistency check failed: {e}"

def check_use_neon_flag(token):
    """Test use-neon feature flag"""
    try:
        headers = {'Authorization': f'Bearer {token}'}
        response = requests.get(
            f"{API_BASE_URL}/admin/feature-flags/use-neon/test?userIds=admin",
            headers=headers
        )
        
        if response.status_code == 200:
            data = response.json()
            if data.get('success'):
                source = data.get('source', 'Unknown')
                results = data.get('results', {})
                admin_flag = results.get('admin', False)
                return True, f"Flag source: {source}, Admin value: {admin_flag}"
            else:
                return False, "Feature flag test failed"
        else:
            return False, f"Feature flag test failed with status {response.status_code}"
    except requests.RequestException as e:
        return False, f"Feature flag test failed: {e}"

def main():
    """Main verification function"""
    print("🔍 Verifying Neon Database Setup...")
    print("=" * 50)
    
    all_checks_passed = True
    jwt_token = None
    
    # 1. API Connectivity
    status, details = check_api_connectivity()
    print_check("API Connectivity", status, details)
    if not status:
        all_checks_passed = False
        print_info("Make sure your Java backend is running")
        return False
    
    # 2. Admin Login
    status, details, token = check_admin_login()
    print_check("Admin Authentication", status, details)
    if not status:
        all_checks_passed = False
        print_info("Check admin credentials or ensure admin user exists")
        return False
    
    jwt_token = token
    
    # 3. DevCycle Integration
    status, details = check_devcycle_status(jwt_token)
    print_check("DevCycle Integration", status, details)
    if not status:
        all_checks_passed = False
        print_info("Set DEVCYCLE_SERVER_SDK_KEY environment variable for full integration")
    
    # 4. Database Consistency
    status, details = check_database_consistency(jwt_token)
    print_check("Database Setup", status, details)
    if not status:
        all_checks_passed = False
        if "not available" in details:
            print_info("Set NEON_DATABASE_URL and SECONDARY_DATABASE_ENABLED=true")
        else:
            print_info("Check your Neon database configuration")
    
    # 5. Feature Flag Test
    status, details = check_use_neon_flag(jwt_token)
    print_check("Feature Flag Test", status, details)
    if not status:
        all_checks_passed = False
    
    # Summary
    print("\n" + "=" * 50)
    if all_checks_passed:
        print_check("🎉 Setup Verification", True, "All checks passed! Ready for sync.")
        print_info("Run ./scripts/sync-with-neon.sh to perform the sync")
    else:
        print_check("Setup Verification", False, "Some checks failed")
        print_warning("Fix the issues above before running the sync script")
    
    print("\n📋 Environment Variables:")
    print(f"   API_BASE_URL: {API_BASE_URL}")
    print(f"   ADMIN_USERNAME: {ADMIN_USERNAME}")
    print(f"   NEON_DATABASE_URL: {'Set' if os.getenv('NEON_DATABASE_URL') else 'Not set'}")
    print(f"   DEVCYCLE_SERVER_SDK_KEY: {'Set' if os.getenv('DEVCYCLE_SERVER_SDK_KEY') else 'Not set'}")
    print(f"   SECONDARY_DATABASE_ENABLED: {os.getenv('SECONDARY_DATABASE_ENABLED', 'true')}")
    
    return all_checks_passed

if __name__ == "__main__":
    try:
        success = main()
        sys.exit(0 if success else 1)
    except KeyboardInterrupt:
        print("\n❌ Verification interrupted")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Unexpected error: {e}")
        sys.exit(1) 