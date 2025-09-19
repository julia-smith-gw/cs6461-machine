1.Requirements:
Java Development Kit (JDK) 24
Download: https://www.oracle.com/java/technologies/downloads/tools/
Apache Maven:
Download: https://maven.apache.org/download.cgi

2.Verify Installation: 
java --version
mvn --version

3.Build the JAR file:
In the root directory, where pom.xml is located, run:
mvn clean package
This compiles the project and builds a JAR file.
The output JAR file should be located in:
target/computer-simulator-1.0.JAR
Extract the JAR contents into the current directory.

4.Unpack the JAR file
If the JAR includes a 'main-class' defined in 'META-INF/MANIFEST.MF  then run:
java-jar target/computer-simulator-1.0.jar

Or, If the 'main-class' is NOT defined, then you need to specify the class manually:
java -cp target/computer-simulatro-1.0.jar xyz.main.ClassName (i.e fully qualified class name)

5. It is important to ensure that you're using JDK 24 or above to match the configuration of 'pom.xml':

<maven.compiler.source>24</maven.compiler.source>
<maven.compiler.target>24</maven.compiler.target>
If you are using an IDE like VS Code, BLueJ or Eclipse, please configure it to use JDK 24

6. To test the project- Run tests:
mvn test
4.11 version






