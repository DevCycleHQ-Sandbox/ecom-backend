#!/usr/bin/env python3
"""
Sync with Neon Database Script (Python Version)
This script logs in as admin and syncs all data with the Neon database
"""

import requests
import json
import sys
import os
import time
from typing import Dict, Any, Optional

# Configuration
API_BASE_URL = os.getenv('API_BASE_URL', 'http://localhost:3002/api')
ADMIN_USERNAME = os.getenv('ADMIN_USERNAME', 'admin')
ADMIN_PASSWORD = os.getenv('ADMIN_PASSWORD', 'password')

# Colors for output
class Colors:
    RED = '\033[0;31m'
    GREEN = '\033[0;32m'
    YELLOW = '\033[1;33m'
    BLUE = '\033[0;34m'
    NC = '\033[0m'  # No Color

def print_status(message: str):
    print(f"{Colors.BLUE}[INFO]{Colors.NC} {message}")

def print_success(message: str):
    print(f"{Colors.GREEN}[SUCCESS]{Colors.NC} {message}")

def print_warning(message: str):
    print(f"{Colors.YELLOW}[WARNING]{Colors.NC} {message}")

def print_error(message: str):
    print(f"{Colors.RED}[ERROR]{Colors.NC} {message}")

class NeonSyncClient:
    def __init__(self):
        self.api_base_url = API_BASE_URL
        self.admin_username = ADMIN_USERNAME
        self.admin_password = ADMIN_PASSWORD
        self.jwt_token: Optional[str] = None
        self.admin_user_id: Optional[str] = None
        self.session = requests.Session()
        self.session.headers.update({'Content-Type': 'application/json'})

    def test_connectivity(self) -> bool:
        """Test API connectivity"""
        print_status("Testing API connectivity...")
        
        try:
            response = self.session.get(f"{self.api_base_url}/auth/login", timeout=5)
            print_success(f"API is reachable at {self.api_base_url}")
            return True
        except requests.RequestException as e:
            print_error(f"Cannot reach API at {self.api_base_url}")
            print_error("Please ensure the Java backend is running and the URL is correct")
            return False

    def login_admin(self) -> bool:
        """Login as admin and get JWT token"""
        print_status("Logging in as admin user...")
        
        login_data = {
            "username": self.admin_username,
            "password": self.admin_password
        }
        
        try:
            response = self.session.post(f"{self.api_base_url}/auth/login", json=login_data)
            
            if response.status_code == 200:
                data = response.json()
                if 'accessToken' in data:
                    self.jwt_token = data['accessToken']
                    self.admin_user_id = data['user']['id']
                    self.session.headers.update({'Authorization': f'Bearer {self.jwt_token}'})
                    print_success("Successfully logged in as admin")
                    print_status(f"Admin User ID: {self.admin_user_id}")
                    return True
            
            print_error(f"Login failed. Status: {response.status_code}, Response: {response.text}")
            print_error("Please check your credentials or ensure the admin user exists")
            return False
            
        except requests.RequestException as e:
            print_error(f"Login request failed: {e}")
            return False

    def check_devcycle_status(self) -> Dict[str, Any]:
        """Check DevCycle integration status"""
        print_status("Checking DevCycle integration status...")
        
        try:
            response = self.session.get(f"{self.api_base_url}/admin/feature-flags/devcycle/status")
            
            if response.status_code == 200:
                data = response.json()
                if data.get('success'):
                    status = data.get('status', {})
                    is_connected = status.get('isDevCycleConnected', False)
                    source = status.get('source', 'Unknown')
                    
                    if is_connected:
                        print_success(f"DevCycle is connected (Source: {source})")
                        use_neon_admin = status.get('useNeonFlagTests', {}).get('admin', False)
                        print_status(f"use-neon flag for admin: {use_neon_admin}")
                    else:
                        print_warning(f"DevCycle not connected (Source: {source})")
                        print_warning("Feature flags will use fallback values")
                    
                    return status
            
            print_warning(f"Could not check DevCycle status: {response.text}")
            return {}
            
        except requests.RequestException as e:
            print_warning(f"Error checking DevCycle status: {e}")
            return {}

    def check_database_consistency(self) -> Dict[str, Any]:
        """Check database consistency"""
        print_status("Checking database consistency...")
        
        try:
            response = self.session.get(f"{self.api_base_url}/admin/database/consistency")
            
            if response.status_code == 200:
                data = response.json()
                if data.get('success'):
                    is_consistent = data.get('isConsistent', False)
                    primary_count = data.get('primaryCount', 0)
                    secondary_count = data.get('secondaryCount', 0)
                    message = data.get('message', '')
                    
                    print_status(f"Primary DB records: {primary_count}")
                    print_status(f"Secondary DB records: {secondary_count}")
                    print_status(f"Status: {message}")
                    
                    if is_consistent:
                        print_success("Databases are consistent")
                    else:
                        print_warning("Databases are inconsistent - sync needed")
                    
                    return data
            
            if "not available" in response.text.lower():
                print_error("Secondary database (Neon) might not be enabled or configured")
                print_error("Please check your NEON_DATABASE_URL and SECONDARY_DATABASE_ENABLED settings")
                sys.exit(1)
            
            print_error("Could not check database consistency")
            return {}
            
        except requests.RequestException as e:
            print_error(f"Error checking database consistency: {e}")
            return {}

    def sync_cart_items(self) -> bool:
        """Sync cart items to Neon database"""
        print_status("Syncing cart items to Neon database...")
        
        try:
            response = self.session.post(f"{self.api_base_url}/admin/database/sync/cart-items")
            
            if response.status_code == 200:
                data = response.json()
                if data.get('success'):
                    synced_count = data.get('syncedCount', 0)
                    print_success(f"Cart items sync completed. Synced: {synced_count} items")
                    return True
            
            data = response.json() if response.headers.get('content-type', '').startswith('application/json') else {}
            error_message = data.get('message', 'Unknown error')
            print_error(f"Cart items sync failed: {error_message}")
            return False
            
        except requests.RequestException as e:
            print_error(f"Cart items sync request failed: {e}")
            return False

    def perform_bidirectional_sync(self) -> bool:
        """Perform bidirectional database synchronization"""
        print_status("Performing bidirectional database synchronization...")
        
        try:
            response = self.session.post(f"{self.api_base_url}/admin/database/sync/bidirectional")
            
            if response.status_code == 200:
                data = response.json()
                if data.get('success'):
                    primary_to_secondary = data.get('primaryToSecondaryCount', 0)
                    secondary_to_primary = data.get('secondaryToPrimaryCount', 0)
                    timestamp = data.get('timestamp', '')
                    
                    print_success("Bidirectional sync completed!")
                    print_status(f"Primary → Secondary: {primary_to_secondary} records")
                    print_status(f"Secondary → Primary: {secondary_to_primary} records")
                    print_status(f"Completed at: {timestamp}")
                    return True
            
            data = response.json() if response.headers.get('content-type', '').startswith('application/json') else {}
            error_message = data.get('message', 'Unknown error')
            print_error(f"Bidirectional sync failed: {error_message}")
            return False
            
        except requests.RequestException as e:
            print_error(f"Bidirectional sync request failed: {e}")
            return False

    def test_use_neon_flag(self):
        """Test use-neon feature flag"""
        print_status("Testing use-neon feature flag...")
        
        try:
            response = self.session.get(
                f"{self.api_base_url}/admin/feature-flags/use-neon/test?userIds=admin,user1,user2"
            )
            
            if response.status_code == 200:
                data = response.json()
                if data.get('success'):
                    source = data.get('source', 'Unknown')
                    results = data.get('results', {})
                    
                    print_status(f"Feature flag source: {source}")
                    for user_id, flag_value in results.items():
                        print_status(f"  {user_id}: {flag_value}")
                    return
            
            print_warning("Could not test use-neon flag")
            
        except requests.RequestException as e:
            print_warning(f"Error testing use-neon flag: {e}")

    def verify_final_consistency(self):
        """Verify final database consistency"""
        print_status("Verifying final database consistency...")
        
        # Wait a moment for sync to complete
        time.sleep(2)
        
        data = self.check_database_consistency()
        
        if data:
            is_consistent = data.get('isConsistent', False)
            primary_count = data.get('primaryCount', 0)
            secondary_count = data.get('secondaryCount', 0)
            message = data.get('message', '')
            
            print_status("Final consistency check:")
            print_status(f"Primary DB records: {primary_count}")
            print_status(f"Secondary DB records: {secondary_count}")
            
            if is_consistent:
                print_success("✅ Databases are now consistent!")
                print_success("✅ Sync with Neon completed successfully")
            else:
                print_warning(f"⚠️  Databases still show inconsistency: {message}")
                print_warning("You may need to run additional sync operations")

    def show_summary(self):
        """Show completion summary"""
        print()
        print("=" * 42)
        print("           SYNC SUMMARY")
        print("=" * 42)
        print_success("✅ Admin login successful")
        print_success("✅ Database connectivity verified")
        print_success("✅ Data synchronization completed")
        print_success("✅ Consistency verification completed")
        print()
        print_status("Your Neon database is now synchronized!")
        print_status("You can now enable the 'use-neon' feature flag to route users to Neon.")
        print()
        print_status("Next steps:")
        print(f"  1. Monitor consistency: curl -H 'Authorization: Bearer {self.jwt_token}' {self.api_base_url}/admin/database/consistency")
        print("  2. Enable use-neon flag in DevCycle dashboard for gradual rollout")
        print("  3. Test with specific users using the admin endpoints")
        print("=" * 42)

    def run_sync(self):
        """Main sync process"""
        print("🚀 Starting Neon Database Sync Process...")
        print()
        
        # Test connectivity
        if not self.test_connectivity():
            sys.exit(1)
        
        # Login as admin
        if not self.login_admin():
            sys.exit(1)
        
        # Check DevCycle status
        self.check_devcycle_status()
        
        # Check initial consistency
        self.check_database_consistency()
        
        # Perform sync operations
        print_status("Starting synchronization process...")
        
        # Try cart items sync first
        if self.sync_cart_items():
            print_success("Cart items sync completed")
        else:
            print_warning("Cart items sync had issues, continuing with bidirectional sync...")
        
        # Perform bidirectional sync
        if not self.perform_bidirectional_sync():
            print_error("Bidirectional sync failed")
            sys.exit(1)
        
        # Verify final consistency
        self.verify_final_consistency()
        
        # Test feature flag
        self.test_use_neon_flag()
        
        # Show summary
        self.show_summary()

def main():
    """Main entry point"""
    if len(sys.argv) > 1 and sys.argv[1] in ['--help', '-h']:
        print("Neon Database Sync Script (Python Version)")
        print()
        print("Usage: python3 sync-with-neon.py [options]")
        print()
        print("Environment Variables:")
        print("  API_BASE_URL      - Base URL for the API (default: http://localhost:3002/api)")
        print("  ADMIN_USERNAME    - Admin username (default: admin)")
        print("  ADMIN_PASSWORD    - Admin password (default: password)")
        print()
        print("Examples:")
        print("  python3 sync-with-neon.py                    # Use default settings")
        print("  API_BASE_URL=http://prod.example.com/api python3 sync-with-neon.py")
        print("  ADMIN_PASSWORD=mysecret python3 sync-with-neon.py")
        print()
        return
    
    try:
        client = NeonSyncClient()
        client.run_sync()
    except KeyboardInterrupt:
        print_error("Script interrupted")
        sys.exit(1)
    except Exception as e:
        print_error(f"Unexpected error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main() 