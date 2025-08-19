# Admin Workflow Testing Suite

This directory contains comprehensive testing tools for the Java backend admin workflow, specifically designed to diagnose and test admin functionality including product management, user management, and database connectivity issues.

## Overview

The testing suite includes multiple complementary approaches:

1. **Bash Scripts** - Quick command-line tests for immediate feedback
2. **Python Scripts** - Comprehensive testing with detailed reporting
3. **Java Integration Tests** - Unit/integration tests using JUnit and Spring Boot Test
4. **Master Runner** - Orchestrates all tests and generates consolidated reports

## Quick Start

To run all tests at once:

```bash
cd backend/java-backend/scripts
chmod +x run-all-tests.sh
./run-all-tests.sh
```

This will:
- Start the Java backend (if not already running)
- Run all test suites
- Generate comprehensive reports
- Clean up afterwards

## Individual Test Scripts

### 1. Admin Workflow Tests (`test-admin-workflow.sh`)

Tests the complete admin workflow including login, product management, user management, and order management.

```bash
cd backend/java-backend/scripts
chmod +x test-admin-workflow.sh
./test-admin-workflow.sh
```

**Features:**
- Admin authentication testing
- Product CRUD operations
- User and order management verification
- Database sync functionality
- Security validation (unauthorized access protection)

**Configuration:**
- Edit the script to change `BASE_URL`, `ADMIN_EMAIL`, or `ADMIN_PASSWORD`
- Results are logged to timestamped files

### 2. Database Connectivity Tests (`test-database-connectivity.sh`)

Specifically focuses on database connectivity issues, particularly useful for diagnosing Neon DB problems.

```bash
cd backend/java-backend/scripts
chmod +x test-database-connectivity.sh
./test-database-connectivity.sh
```

**Features:**
- Basic database connectivity validation
- Health endpoint analysis
- Application log examination
- Neon DB specific checks
- Diagnostic recommendations

### 3. Comprehensive Python Test Suite (`comprehensive-admin-test.py`)

Advanced testing with detailed reporting and error analysis.

```bash
cd backend/java-backend/scripts
python3 comprehensive-admin-test.py --url http://localhost:8080 --verbose
```

**Features:**
- Detailed test timing and performance metrics
- JSON report generation
- Comprehensive error analysis
- Maven test integration
- Parallel test execution

**Requirements:**
- Python 3.6+
- `requests` library (`pip install requests`)

### 4. Java Integration Tests

JUnit-based integration tests using Spring Boot Test framework.

```bash
cd backend/java-backend
mvn test -Dtest=AdminWorkflowIntegrationTest
```

**Features:**
- Spring Boot context loading
- Database transaction testing
- Mock MVC testing
- Comprehensive admin workflow validation
- Test data cleanup

## Configuration Files

### Test Properties (`src/test/resources/application-test.properties`)

Configures the test environment with:
- H2 in-memory database for isolated testing
- Debug logging levels
- Test-specific JWT configuration
- Disabled external dependencies

### Maven Test Configuration

Tests are configured to run with:
- Test profiles for different environments
- Automatic test data setup and cleanup
- Integration with CI/CD pipelines

## Common Issues and Solutions

### Neon Database Issues

1. **Database Sleep Mode**
   ```
   Problem: Connection timeouts after inactivity
   Solution: The scripts automatically wake up sleeping databases
   ```

2. **Authentication Errors**
   ```
   Problem: Invalid credentials or expired tokens
   Solution: Check environment variables and connection strings
   ```

3. **SSL/TLS Issues**
   ```
   Problem: Certificate validation failures
   Solution: Ensure SSL mode is set to 'require' for Neon
   ```

### Application Issues

1. **Admin User Not Found**
   ```
   Problem: Default admin user doesn't exist
   Solution: Check database seeding scripts and user creation
   ```

2. **JWT Token Issues**
   ```
   Problem: Token generation or validation failures
   Solution: Verify JWT secret configuration and expiration settings
   ```

3. **Port Conflicts**
   ```
   Problem: Server won't start due to port conflicts
   Solution: Change the port in application.yml or stop conflicting services
   ```

## Environment Variables

Set these environment variables for production testing:

```bash
export DATABASE_URL="postgresql://username:password@host:port/database"
export JWT_SECRET="your-jwt-secret-key"
export ADMIN_EMAIL="admin@yourdomain.com"
export ADMIN_PASSWORD="secure-admin-password"
```

## Output Files

The test suite generates several types of output files:

### Log Files
- `test_results_YYYYMMDD_HHMMSS.log` - Bash test results
- `database_test_YYYYMMDD_HHMMSS.log` - Database connectivity results
- `admin_test_report_YYYYMMDD_HHMMSS.log` - Python test logs

### Report Files
- `admin_test_report_YYYYMMDD_HHMMSS.json` - Detailed JSON test report
- `final_test_report_YYYYMMDD_HHMMSS.md` - Consolidated markdown report

### Maven Reports
- `target/surefire-reports/` - JUnit test reports
- `target/site/jacoco/` - Code coverage reports (if configured)

## CI/CD Integration

Add to your pipeline:

```yaml
# GitHub Actions example
- name: Run Admin Workflow Tests
  run: |
    cd backend/java-backend/scripts
    chmod +x run-all-tests.sh
    ./run-all-tests.sh --url http://localhost:8080

- name: Upload Test Reports
  uses: actions/upload-artifact@v2
  with:
    name: test-reports
    path: |
      backend/java-backend/scripts/*_test_*.log
      backend/java-backend/scripts/*_report_*.json
      backend/java-backend/scripts/*_report_*.md
```

## Customization

### Adding New Tests

1. **Bash Tests**: Add new test functions to the existing scripts
2. **Python Tests**: Extend the `AdminWorkflowTester` class
3. **Java Tests**: Add new test methods to `AdminWorkflowIntegrationTest`

### Modifying Test Data

- Update test product data in the scripts
- Modify admin credentials in configuration files
- Adjust timeout values for different environments

### Custom Reporting

- Extend the Python script's `generate_report()` method
- Add custom log parsing in bash scripts
- Implement custom JUnit listeners for Java tests

## Troubleshooting

### Script Permissions
```bash
chmod +x *.sh
```

### Python Dependencies
```bash
pip3 install requests
# or
pip install requests
```

### Maven Issues
```bash
mvn clean install
mvn dependency:resolve
```

### Database Connection
```bash
# Test direct connection
psql $DATABASE_URL

# Check application logs
tail -f backend/java-backend/app.log
```

## Performance Considerations

- Tests run sequentially by default to avoid resource conflicts
- Database operations use transactions for isolation
- Connection pooling is tested under load
- Timeout values are configurable for different environments

## Security Notes

- Test scripts mask sensitive information in logs
- Test databases are isolated from production
- Temporary test data is automatically cleaned up
- Authentication tokens are properly secured

## Support

If you encounter issues:

1. Check the generated log files for detailed error messages
2. Review the diagnostic recommendations in the database connectivity script
3. Verify your environment configuration
4. Ensure all dependencies are properly installed

For additional help, check the application logs and verify your database connectivity manually.