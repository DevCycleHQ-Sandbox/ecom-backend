# 🚀 Quick Deployment Guide

## **TL;DR - Deploy in 3 Steps**

### **1. Prerequisites**
```bash
# Install Java 17+
sudo apt install openjdk-17-jre

# Install Dynatrace OneAgent
# Download from your Dynatrace environment: Deploy Dynatrace → Start installation
```

### **2. Build & Copy**
```bash
# Build on development machine
mvn clean package -DskipTests

# Copy to target machine
scp target/java-backend-1.0.0.jar user@server:/opt/java-backend/
scp deploy.sh user@server:/opt/java-backend/
```

### **3. Deploy**
```bash
# On target machine
cd /opt/java-backend
chmod +x deploy.sh
./deploy.sh
```

**Done!** The script handles everything else automatically.

---

## **Deployment Options**

| Method | Complexity | Best For |
|--------|------------|----------|
| **🚀 Automated Script** | ⭐ Easy | Production servers |
| **🐳 Docker** | ⭐⭐ Medium | Containerized environments |
| **☸️ Kubernetes** | ⭐⭐⭐ Complex | Scalable cloud deployments |
| **🔧 Manual** | ⭐⭐ Medium | Custom configurations |

## **Key URLs After Deployment**

- **Application**: `http://server:3002/api`
- **Health Check**: `http://server:3002/api/actuator/health`
- **API Docs**: `http://server:3002/api/swagger-ui.html`

## **Management Commands**

```bash
# Start/stop/status
./deploy.sh start
./deploy.sh stop
./deploy.sh status

# Or with systemd
sudo systemctl start java-backend
sudo systemctl status java-backend
sudo journalctl -u java-backend -f
```

## **Verification Checklist**

- [ ] Java 17+ installed: `java -version`
- [ ] OneAgent running: `sudo systemctl status oneagent`
- [ ] Application healthy: `curl http://localhost:3002/api/actuator/health`
- [ ] OneAgent SDK active: Check logs for "OneAgent SDK is active"
- [ ] Traces in Dynatrace: Look for `java-backend` service

## **Important Configuration**

**Security (Production)**: Update these in `.env` file:
```bash
JWT_SECRET=your-very-secure-production-secret
NODE_ENV=production
```

**Performance**: Adjust memory based on your server:
```bash
# For 2GB server
JAVA_OPTS="-Xmx1536m -Xms512m"

# For 4GB server  
JAVA_OPTS="-Xmx3g -Xms1g"
```

---

## **Need More Details?**

📖 **Full Documentation:**
- `DEPLOYMENT_GUIDE.md` - Complete deployment instructions
- `ONEAGENT_SDK_SETUP.md` - OneAgent SDK configuration
- `MIGRATION_SUMMARY.md` - What changed from OpenTelemetry

🆘 **Troubleshooting:**
- Check application logs: `tail -f application.log`
- Verify OneAgent: `sudo systemctl status oneagent`
- Test endpoints: `curl http://localhost:3002/api/actuator/health`

That's it! Your OneAgent SDK Java backend is ready to run on any machine. 🎉