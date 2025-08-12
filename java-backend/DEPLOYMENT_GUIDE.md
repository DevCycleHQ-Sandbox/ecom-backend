# 🚀 Deployment Guide - OneAgent SDK Java Backend

This guide explains how to deploy the OneAgent SDK Java Backend application on different machines and environments.

## 📋 Prerequisites

### **System Requirements**
- **Operating System**: Linux, Windows, or macOS
- **Java Runtime**: OpenJDK 17 or higher
- **Memory**: Minimum 1GB RAM (2GB+ recommended)
- **Disk Space**: 500MB for application + database
- **Network**: Port 3002 available (configurable)

### **Required Software**
- **Dynatrace OneAgent** (Critical for telemetry)
- **Java 17+** Runtime Environment
- **curl** (for health checks)

## 🏗️ **Installation Options**

### **Option 1: Quick Deployment (Recommended)**

**Step 1:** Prepare your machine
```bash
# Install Java 17
sudo apt install openjdk-17-jre  # Ubuntu/Debian
sudo yum install java-17-openjdk # CentOS/RHEL

# Install OneAgent from your Dynatrace environment
# Get the installer from: Deploy Dynatrace → Start installation
```

**Step 2:** Copy application files
```bash
# Copy the built JAR file to target machine
scp target/java-backend-1.0.0.jar user@target-machine:/opt/java-backend/
scp deploy.sh user@target-machine:/opt/java-backend/
```

**Step 3:** Deploy with automated script
```bash
cd /opt/java-backend
chmod +x deploy.sh
./deploy.sh
```

The script will:
- ✅ Check Java installation
- ✅ Verify OneAgent status
- ✅ Create data directories
- ✅ Setup environment variables
- ✅ Create systemd service (Linux)
- ✅ Start the application

---

### **Option 2: Manual Deployment**

**Step 1:** Install prerequisites
```bash
# Install Java
java -version  # Should show 17+

# Verify OneAgent
sudo systemctl status oneagent  # Linux
# or check for OneAgent files
ls -la /opt/dynatrace/oneagent/
```

**Step 2:** Create application directory
```bash
sudo mkdir -p /opt/java-backend
cd /opt/java-backend
```

**Step 3:** Copy application files
```bash
# Copy JAR file
cp /path/to/java-backend-1.0.0.jar .

# Create data directory
mkdir -p data
```

**Step 4:** Configure environment
```bash
# Create .env file
cat > .env << 'EOF'
NODE_ENV=production
PORT=3002
DATABASE_URL=./data/database.sqlite
SECONDARY_DATABASE_ENABLED=false
JWT_SECRET=your-very-secure-jwt-secret-change-this
DEVCYCLE_SERVER_SDK_KEY=your-devcycle-key
TELEMETRY_PROJECT=shopper-backend-prod
TELEMETRY_ENVIRONMENT_ID=production
JAVA_OPTS=-Xmx1g -Xms512m
EOF
```

**Step 5:** Start the application
```bash
# Load environment
source .env

# Start application
java $JAVA_OPTS -jar java-backend-1.0.0.jar
```

---

### **Option 3: Docker Deployment**

**Step 1:** Build the Docker image
```bash
# In the source directory
mvn clean package -DskipTests
docker build -t java-backend:1.0.0 .
```

**Step 2:** Run with Docker Compose
```bash
docker-compose up -d
```

**Step 3:** Verify deployment
```bash
docker-compose logs -f java-backend
curl http://localhost:3002/api/actuator/health
```

---

### **Option 4: Kubernetes Deployment**

**Step 1:** Create Kubernetes manifests

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: java-backend
  annotations:
    # Enable Dynatrace OneAgent injection
    dynatrace.com/inject: "true"
spec:
  replicas: 2
  selector:
    matchLabels:
      app: java-backend
  template:
    metadata:
      labels:
        app: java-backend
    spec:
      containers:
      - name: java-backend
        image: java-backend:1.0.0
        ports:
        - containerPort: 3002
        env:
        - name: NODE_ENV
          value: "production"
        - name: PORT
          value: "3002"
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: java-backend-secrets
              key: jwt-secret
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /api/actuator/health
            port: 3002
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /api/actuator/health
            port: 3002
          initialDelaySeconds: 30
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: java-backend-service
spec:
  selector:
    app: java-backend
  ports:
  - port: 80
    targetPort: 3002
  type: LoadBalancer
```

**Step 2:** Deploy to Kubernetes
```bash
kubectl apply -f deployment.yaml
kubectl get pods -l app=java-backend
```

## ⚙️ **Configuration Options**

### **Environment Variables**

| Variable | Default | Description |
|----------|---------|-------------|
| `NODE_ENV` | `development` | Environment mode |
| `PORT` | `3002` | Application port |
| `DATABASE_URL` | `./database.sqlite` | Database file path |
| `SECONDARY_DATABASE_ENABLED` | `false` | Enable dual database |
| `JWT_SECRET` | *(required)* | JWT signing secret |
| `DEVCYCLE_SERVER_SDK_KEY` | *(optional)* | Feature flags key |
| `TELEMETRY_PROJECT` | `shopper-backend` | Telemetry project name |
| `JAVA_OPTS` | `-Xmx512m -Xms256m` | JVM options |

### **Database Configuration**

**SQLite (Default)**
```bash
DATABASE_URL=./data/database.sqlite
SECONDARY_DATABASE_ENABLED=false
```

**Dual Database (SQLite + PostgreSQL)**
```bash
DATABASE_URL=./data/database.sqlite
SECONDARY_DATABASE_ENABLED=true
NEON_DATABASE_URL=postgresql://user:pass@host:5432/db
NEON_DATABASE_USERNAME=username
NEON_DATABASE_PASSWORD=password
```

## 🔧 **Management Commands**

### **Using Deploy Script**
```bash
# Start application
./deploy.sh start

# Stop application
./deploy.sh stop

# Check status
./deploy.sh status

# Full deployment
./deploy.sh
```

### **Using Systemd (Linux)**
```bash
# Control service
sudo systemctl start java-backend
sudo systemctl stop java-backend
sudo systemctl restart java-backend
sudo systemctl status java-backend

# View logs
sudo journalctl -u java-backend -f
```

### **Manual Process Management**
```bash
# Start in background
nohup java -jar java-backend-1.0.0.jar > app.log 2>&1 &
echo $! > app.pid

# Stop application
kill $(cat app.pid)

# View logs
tail -f app.log
```

## 🔍 **Verification & Testing**

### **Health Checks**
```bash
# Basic health check
curl http://localhost:3002/api/actuator/health

# Detailed health info
curl http://localhost:3002/api/actuator/health | jq .

# Application info
curl http://localhost:3002/api/actuator/info
```

### **Test OneAgent SDK Integration**
```bash
# Test authentication tracing
curl -X POST http://localhost:3002/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'

# Test login tracing
curl -X POST http://localhost:3002/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'

# Test database tracing
curl http://localhost:3002/api/products
```

### **Check Application Logs**
```bash
# Look for OneAgent SDK status
grep -i "oneagent" application.log

# Check for successful startup
grep -i "started" application.log

# Monitor for errors
tail -f application.log | grep -i error
```

### **Verify in Dynatrace**
1. Open your Dynatrace environment
2. Go to **Applications & Microservices** → **Services**
3. Look for `java-backend` service
4. Check for custom services:
   - `user_registration.register`
   - `user_authentication.login`
   - `feature_flag_evaluation.*`
5. Verify database request traces
6. Check custom attributes in trace details

## 🛠️ **Troubleshooting**

### **Common Issues**

**1. OneAgent SDK Inactive**
```bash
# Check OneAgent status
sudo systemctl status oneagent

# Check OneAgent logs
sudo tail -f /var/log/dynatrace/oneagent/oneagent.log

# Restart OneAgent
sudo systemctl restart oneagent
```

**2. Application Won't Start**
```bash
# Check Java version
java -version

# Check port availability
sudo netstat -tulpn | grep 3002

# Check permissions
ls -la java-backend-1.0.0.jar

# Check logs
tail -f application.log
```

**3. Database Connection Issues**
```bash
# Check database file permissions
ls -la data/database.sqlite

# Check disk space
df -h

# Verify database path in config
grep DATABASE_URL .env
```

**4. No Telemetry in Dynatrace**
```bash
# Verify OneAgent is running
sudo systemctl status oneagent

# Check application logs for OneAgent SDK status
grep -i "oneagent" application.log

# Ensure sufficient load to trigger traces
for i in {1..10}; do curl http://localhost:3002/api/actuator/health; done
```

### **Performance Tuning**

**JVM Options for Production**
```bash
# For 2GB RAM server
JAVA_OPTS="-Xmx1536m -Xms512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

# For 4GB RAM server
JAVA_OPTS="-Xmx3g -Xms1g -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

# For containers
JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
```

## 🔒 **Security Considerations**

### **Production Security**
1. **Change default JWT secret**: Update `JWT_SECRET` to a strong, unique value
2. **Use HTTPS**: Configure reverse proxy with SSL/TLS
3. **Firewall rules**: Restrict access to port 3002
4. **User permissions**: Run application as non-root user
5. **Regular updates**: Keep Java and dependencies updated

### **Environment Variables Security**
```bash
# Set secure file permissions
chmod 600 .env
chown app-user:app-group .env

# Use secrets management in production
# Instead of plain text in .env file
```

## 📊 **Monitoring & Maintenance**

### **Log Management**
```bash
# Rotate logs (Linux)
sudo logrotate -f /etc/logrotate.d/java-backend

# Archive old logs
find ./logs -name "*.log" -mtime +30 -delete
```

### **Database Maintenance**
```bash
# SQLite maintenance
sqlite3 data/database.sqlite "VACUUM;"
sqlite3 data/database.sqlite "PRAGMA integrity_check;"
```

### **Application Updates**
```bash
# Stop application
./deploy.sh stop

# Backup database
cp data/database.sqlite data/database.sqlite.backup

# Update JAR file
cp new-version/java-backend-1.0.1.jar ./

# Start application
./deploy.sh start
```

---

## 📞 **Support & Resources**

- **Application Logs**: `application.log` or `journalctl -u java-backend`
- **OneAgent SDK Docs**: [Dynatrace OneAgent SDK](https://docs.dynatrace.com/docs/extend-dynatrace/oneagent-sdk)
- **Health Endpoint**: `http://localhost:3002/api/actuator/health`
- **Configuration Guide**: `ONEAGENT_SDK_SETUP.md`
- **Migration Summary**: `MIGRATION_SUMMARY.md`

For additional help, check the application logs and verify OneAgent status before troubleshooting further.