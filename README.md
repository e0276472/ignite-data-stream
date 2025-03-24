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
   `git clone https://github.com/e0276472/ignite-data-stream.git`
   
⚙️ Configuration
Edit Project Configurations under Build and run
Modify options Add VM options Copy and paste below into the field

```--add-opens=java.base/jdk.internal.access=ALL-UNNAMED
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED
--add-opens=java.base/sun.util.calendar=ALL-UNNAMED
--add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED
--add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED
--add-opens=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED
--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
--add-opens=java.base/java.nio=ALL-UNNAMED
--add-opens=java.base/java.net=ALL-UNNAMED
--add-opens=java.base/java.util=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.lang.invoke=ALL-UNNAMED
--add-opens=java.base/java.math=ALL-UNNAMED
--add-opens=java.sql/java.sql=ALL-UNNAMED
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
--add-opens=java.base/java.time=ALL-UNNAMED
--add-opens=java.base/java.text=ALL-UNNAMED
--add-opens=java.management/sun.management=ALL-UNNAMED
--add-opens java.desktop/java.awt.font=ALL-UNNAMED```

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