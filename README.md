# Ignite-Data-Stream
# Apache Ignite & MongoDB Integration (Windows)

A Spring Boot application demonstrating read-through/write-through caching between Apache Ignite and MongoDB.

## 📋 Prerequisites
- **Windows 10/11 (64-bit)**
- **Java JDK 17** ([Download](https://adoptium.net/))
- **Maven 3.8+** ([Download](https://maven.apache.org/download.cgi))
- **MongoDB 6.0+** ([Download](https://www.mongodb.com/try/download/community))

## 🖥️ Current PC Specs (Recommended)
- **CPU**: 4-core processor (Intel/AMD)  
- **RAM**: 8GB+  
- **Storage**: 10GB+ free space (SSD preferred)  
- **OS**: Windows 10/11 Pro  

## 🚀 Installation
1. **Install MongoDB**:  
   - Run the MongoDB installer and select "Install as a Service"
   - Start MongoDB via Services (`Win + R` → `services.msc` → Start **MongoDB Server**)

2. **Set Up Java & Maven**:  
   - Install JDK 17 and add `JAVA_HOME` to environment variables  
   - Install Maven and add `MAVEN_HOME/bin` to `PATH`

3. **Clone Repository**:  
   ```IntelliJ IDEA
   git clone https://github.com/e0276472/ignite-data-stream.git

   
⚙️ Configuration
Main Configuration (src/main/resources/application.properties):
spring.data.mongodb.uri=mongodb://localhost:27017/flightDB
ignite.workDirectory=./ignite/work

Test Configuration (src/test/resources/application-test.properties):
spring.autoconfigure.exclude=org.apache.ignite.configuration.IgniteConfiguration

# Build and test with JVM arguments for Java 17 compatibility
mvn clean test -DargLine="--add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED -Xmx1024m"

🔍 Verify Results
Check MongoDB Data:
mongosh flightDB
> db.flightPlans.find({ flightNumber: "TEST123" })