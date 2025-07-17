#!/usr/bin/env python3
"""
Comprehensive Admin Workflow Test Suite
Orchestrates all admin-related tests and generates detailed reports
"""

import os
import sys
import json
import time
import subprocess
import requests
import logging
from datetime import datetime
from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass
from pathlib import Path

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(f'admin_test_report_{datetime.now().strftime("%Y%m%d_%H%M%S")}.log'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

@dataclass
class TestResult:
    name: str
    status: str
    duration: float
    details: str = ""
    error: Optional[str] = None

@dataclass
class TestSuite:
    name: str
    results: List[TestResult]
    total_duration: float
    passed: int
    failed: int

class AdminWorkflowTester:
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url
        self.admin_token: Optional[str] = None
        self.test_results: List[TestResult] = []
        self.start_time = time.time()
        
    def log_test_start(self, test_name: str) -> float:
        """Log test start and return start time"""
        logger.info(f"🧪 Starting test: {test_name}")
        return time.time()
    
    def log_test_result(self, test_name: str, start_time: float, success: bool, details: str = "", error: str = None):
        """Log test result and add to results list"""
        duration = time.time() - start_time
        status = "PASS" if success else "FAIL"
        
        result = TestResult(
            name=test_name,
            status=status,
            duration=duration,
            details=details,
            error=error
        )
        self.test_results.append(result)
        
        emoji = "✅" if success else "❌"
        logger.info(f"{emoji} {test_name} - {status} ({duration:.2f}s)")
        if error:
            logger.error(f"Error: {error}")

    def test_server_availability(self) -> bool:
        """Test if the Spring Boot server is running"""
        start_time = self.log_test_start("Server Availability")
        
        try:
            response = requests.get(f"{self.base_url}/actuator/health", timeout=10)
            if response.status_code == 200:
                health_data = response.json()
                details = f"Status: {health_data.get('status', 'Unknown')}"
                self.log_test_result("Server Availability", start_time, True, details)
                return True
            else:
                self.log_test_result("Server Availability", start_time, False, 
                                   error=f"HTTP {response.status_code}")
                return False
        except Exception as e:
            self.log_test_result("Server Availability", start_time, False, 
                               error=f"Connection failed: {str(e)}")
            return False

    def test_database_connectivity(self) -> bool:
        """Test database connectivity through health endpoint"""
        start_time = self.log_test_start("Database Connectivity")
        
        try:
            response = requests.get(f"{self.base_url}/actuator/health", timeout=10)
            if response.status_code == 200:
                health_data = response.json()
                
                # Check for database components in health check
                components = health_data.get('components', {})
                db_status = components.get('db', components.get('database', {}))
                
                if db_status and db_status.get('status') == 'UP':
                    self.log_test_result("Database Connectivity", start_time, True, 
                                       "Database is UP")
                    return True
                else:
                    self.log_test_result("Database Connectivity", start_time, False, 
                                       error="Database status not UP or not found")
                    return False
            else:
                self.log_test_result("Database Connectivity", start_time, False, 
                                   error=f"Health check failed: {response.status_code}")
                return False
        except Exception as e:
            self.log_test_result("Database Connectivity", start_time, False, 
                               error=f"Request failed: {str(e)}")
            return False

    def test_admin_login(self) -> bool:
        """Test admin user login"""
        start_time = self.log_test_start("Admin Login")
        
        try:
            login_data = {
                "email": "admin@example.com",
                "password": "admin123"
            }
            
            response = requests.post(
                f"{self.base_url}/api/auth/login",
                json=login_data,
                headers={"Content-Type": "application/json"},
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                token = data.get('token') or data.get('accessToken') or data.get('access_token')
                
                if token:
                    self.admin_token = token
                    user_info = data.get('user', {})
                    role = user_info.get('role', 'Unknown')
                    
                    details = f"Role: {role}, Token: {token[:20]}..."
                    self.log_test_result("Admin Login", start_time, True, details)
                    return True
                else:
                    self.log_test_result("Admin Login", start_time, False, 
                                       error="No token in response")
                    return False
            else:
                self.log_test_result("Admin Login", start_time, False, 
                                   error=f"Login failed: {response.status_code} - {response.text}")
                return False
        except Exception as e:
            self.log_test_result("Admin Login", start_time, False, 
                               error=f"Request failed: {str(e)}")
            return False

    def test_admin_get_products(self) -> bool:
        """Test admin product retrieval"""
        start_time = self.log_test_start("Admin Get Products")
        
        if not self.admin_token:
            self.log_test_result("Admin Get Products", start_time, False, 
                               error="No admin token available")
            return False
        
        try:
            headers = {"Authorization": f"Bearer {self.admin_token}"}
            response = requests.get(f"{self.base_url}/api/products", headers=headers, timeout=10)
            
            if response.status_code == 200:
                products = response.json()
                count = len(products) if isinstance(products, list) else "Unknown"
                details = f"Retrieved {count} products"
                self.log_test_result("Admin Get Products", start_time, True, details)
                return True
            else:
                self.log_test_result("Admin Get Products", start_time, False, 
                                   error=f"Request failed: {response.status_code} - {response.text}")
                return False
        except Exception as e:
            self.log_test_result("Admin Get Products", start_time, False, 
                               error=f"Request failed: {str(e)}")
            return False

    def test_admin_create_product(self) -> Tuple[bool, Optional[int]]:
        """Test admin product creation"""
        start_time = self.log_test_start("Admin Create Product")
        
        if not self.admin_token:
            self.log_test_result("Admin Create Product", start_time, False, 
                               error="No admin token available")
            return False, None
        
        try:
            product_data = {
                "name": f"Test Product {datetime.now().isoformat()}",
                "description": "Automated test product",
                "price": 19.99,
                "stock": 100,
                "imageUrl": "https://example.com/test-product.jpg"
            }
            
            headers = {
                "Authorization": f"Bearer {self.admin_token}",
                "Content-Type": "application/json"
            }
            
            response = requests.post(
                f"{self.base_url}/api/products",
                json=product_data,
                headers=headers,
                timeout=10
            )
            
            if response.status_code in [200, 201]:
                product = response.json()
                product_id = product.get('id')
                details = f"Created product ID: {product_id}"
                self.log_test_result("Admin Create Product", start_time, True, details)
                return True, product_id
            else:
                self.log_test_result("Admin Create Product", start_time, False, 
                                   error=f"Request failed: {response.status_code} - {response.text}")
                return False, None
        except Exception as e:
            self.log_test_result("Admin Create Product", start_time, False, 
                               error=f"Request failed: {str(e)}")
            return False, None

    def test_admin_get_users(self) -> bool:
        """Test admin user retrieval"""
        start_time = self.log_test_start("Admin Get Users")
        
        if not self.admin_token:
            self.log_test_result("Admin Get Users", start_time, False, 
                               error="No admin token available")
            return False
        
        try:
            headers = {"Authorization": f"Bearer {self.admin_token}"}
            response = requests.get(f"{self.base_url}/api/admin/users", headers=headers, timeout=10)
            
            if response.status_code == 200:
                users = response.json()
                count = len(users) if isinstance(users, list) else "Unknown"
                details = f"Retrieved {count} users"
                self.log_test_result("Admin Get Users", start_time, True, details)
                return True
            else:
                self.log_test_result("Admin Get Users", start_time, False, 
                                   error=f"Request failed: {response.status_code} - {response.text}")
                return False
        except Exception as e:
            self.log_test_result("Admin Get Users", start_time, False, 
                               error=f"Request failed: {str(e)}")
            return False

    def test_admin_get_orders(self) -> bool:
        """Test admin order retrieval"""
        start_time = self.log_test_start("Admin Get Orders")
        
        if not self.admin_token:
            self.log_test_result("Admin Get Orders", start_time, False, 
                               error="No admin token available")
            return False
        
        try:
            headers = {"Authorization": f"Bearer {self.admin_token}"}
            response = requests.get(f"{self.base_url}/api/admin/orders", headers=headers, timeout=10)
            
            if response.status_code == 200:
                orders = response.json()
                count = len(orders) if isinstance(orders, list) else "Unknown"
                details = f"Retrieved {count} orders"
                self.log_test_result("Admin Get Orders", start_time, True, details)
                return True
            else:
                self.log_test_result("Admin Get Orders", start_time, False, 
                                   error=f"Request failed: {response.status_code} - {response.text}")
                return False
        except Exception as e:
            self.log_test_result("Admin Get Orders", start_time, False, 
                               error=f"Request failed: {str(e)}")
            return False

    def test_database_sync_status(self) -> bool:
        """Test database sync status endpoint"""
        start_time = self.log_test_start("Database Sync Status")
        
        if not self.admin_token:
            self.log_test_result("Database Sync Status", start_time, False, 
                               error="No admin token available")
            return False
        
        try:
            headers = {"Authorization": f"Bearer {self.admin_token}"}
            response = requests.get(
                f"{self.base_url}/api/admin/database-sync/status", 
                headers=headers, 
                timeout=10
            )
            
            if response.status_code == 200:
                sync_data = response.json()
                details = f"Sync status retrieved: {json.dumps(sync_data, indent=2)}"
                self.log_test_result("Database Sync Status", start_time, True, details)
                return True
            else:
                self.log_test_result("Database Sync Status", start_time, False, 
                                   error=f"Request failed: {response.status_code} - {response.text}")
                return False
        except Exception as e:
            self.log_test_result("Database Sync Status", start_time, False, 
                               error=f"Request failed: {str(e)}")
            return False

    def test_unauthorized_access(self) -> bool:
        """Test that admin endpoints properly reject unauthorized access"""
        start_time = self.log_test_start("Unauthorized Access Protection")
        
        try:
            # Test without token
            response = requests.get(f"{self.base_url}/api/admin/users", timeout=10)
            unauthorized_without_token = response.status_code == 401
            
            # Test with invalid token
            headers = {"Authorization": "Bearer invalid-token"}
            response = requests.get(f"{self.base_url}/api/admin/users", headers=headers, timeout=10)
            unauthorized_with_invalid_token = response.status_code == 401
            
            if unauthorized_without_token and unauthorized_with_invalid_token:
                details = "Properly rejects unauthorized access"
                self.log_test_result("Unauthorized Access Protection", start_time, True, details)
                return True
            else:
                error = f"Security issue: without_token={unauthorized_without_token}, invalid_token={unauthorized_with_invalid_token}"
                self.log_test_result("Unauthorized Access Protection", start_time, False, error=error)
                return False
        except Exception as e:
            self.log_test_result("Unauthorized Access Protection", start_time, False, 
                               error=f"Request failed: {str(e)}")
            return False

    def run_maven_tests(self) -> bool:
        """Run the Java integration tests using Maven"""
        start_time = self.log_test_start("Maven Integration Tests")
        
        try:
            # Change to java-backend directory
            original_cwd = os.getcwd()
            java_backend_path = Path(__file__).parent.parent
            os.chdir(java_backend_path)
            
            # Run Maven tests
            result = subprocess.run(
                ["mvn", "test", "-Dtest=AdminWorkflowIntegrationTest"],
                capture_output=True,
                text=True,
                timeout=300  # 5 minutes timeout
            )
            
            os.chdir(original_cwd)
            
            if result.returncode == 0:
                details = "All Maven tests passed"
                self.log_test_result("Maven Integration Tests", start_time, True, details)
                return True
            else:
                error = f"Maven tests failed: {result.stderr}"
                self.log_test_result("Maven Integration Tests", start_time, False, error=error)
                return False
        except subprocess.TimeoutExpired:
            self.log_test_result("Maven Integration Tests", start_time, False, 
                               error="Tests timed out after 5 minutes")
            return False
        except Exception as e:
            self.log_test_result("Maven Integration Tests", start_time, False, 
                               error=f"Failed to run Maven tests: {str(e)}")
            return False

    def generate_report(self) -> Dict:
        """Generate comprehensive test report"""
        total_duration = time.time() - self.start_time
        passed = sum(1 for result in self.test_results if result.status == "PASS")
        failed = sum(1 for result in self.test_results if result.status == "FAIL")
        
        report = {
            "summary": {
                "total_tests": len(self.test_results),
                "passed": passed,
                "failed": failed,
                "success_rate": (passed / len(self.test_results) * 100) if self.test_results else 0,
                "total_duration": total_duration,
                "timestamp": datetime.now().isoformat()
            },
            "tests": [
                {
                    "name": result.name,
                    "status": result.status,
                    "duration": result.duration,
                    "details": result.details,
                    "error": result.error
                }
                for result in self.test_results
            ]
        }
        
        return report

    def save_report(self, report: Dict):
        """Save report to file"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"admin_test_report_{timestamp}.json"
        
        with open(filename, 'w') as f:
            json.dump(report, f, indent=2)
        
        logger.info(f"📊 Test report saved to: {filename}")

    def run_all_tests(self):
        """Run all admin workflow tests"""
        logger.info("🚀 Starting Comprehensive Admin Workflow Test Suite")
        logger.info(f"Base URL: {self.base_url}")
        
        # Core infrastructure tests
        if not self.test_server_availability():
            logger.error("❌ Server not available. Stopping tests.")
            return self.generate_report()
        
        self.test_database_connectivity()
        
        # Authentication tests
        if not self.test_admin_login():
            logger.warning("⚠️  Admin login failed. Some tests will be skipped.")
        
        # Admin functionality tests
        self.test_admin_get_products()
        success, product_id = self.test_admin_create_product()
        self.test_admin_get_users()
        self.test_admin_get_orders()
        self.test_database_sync_status()
        
        # Security tests
        self.test_unauthorized_access()
        
        # Integration tests
        self.run_maven_tests()
        
        # Generate and save report
        report = self.generate_report()
        self.save_report(report)
        
        # Print summary
        summary = report["summary"]
        logger.info("📊 Test Summary:")
        logger.info(f"Total Tests: {summary['total_tests']}")
        logger.info(f"Passed: {summary['passed']}")
        logger.info(f"Failed: {summary['failed']}")
        logger.info(f"Success Rate: {summary['success_rate']:.1f}%")
        logger.info(f"Total Duration: {summary['total_duration']:.2f}s")
        
        if summary['failed'] == 0:
            logger.info("🎉 All tests passed!")
        else:
            logger.warning("⚠️  Some tests failed. Check the detailed report for more information.")
        
        return report

def main():
    """Main entry point"""
    import argparse
    
    parser = argparse.ArgumentParser(description="Comprehensive Admin Workflow Test Suite")
    parser.add_argument("--url", default="http://localhost:8080", 
                       help="Base URL of the application (default: http://localhost:8080)")
    parser.add_argument("--verbose", "-v", action="store_true", 
                       help="Enable verbose logging")
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    tester = AdminWorkflowTester(args.url)
    report = tester.run_all_tests()
    
    # Exit with appropriate code
    if report["summary"]["failed"] > 0:
        sys.exit(1)
    else:
        sys.exit(0)

if __name__ == "__main__":
    main()