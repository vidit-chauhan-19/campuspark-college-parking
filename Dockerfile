FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
COPY src ./src
COPY WebContent ./WebContent
RUN mvn --batch-mode --no-transfer-progress package

FROM tomcat:10.1-jdk21-temurin
RUN rm -rf /usr/local/tomcat/webapps/ROOT \
    /usr/local/tomcat/webapps/docs \
    /usr/local/tomcat/webapps/examples \
    /usr/local/tomcat/webapps/host-manager \
    /usr/local/tomcat/webapps/manager
COPY --from=build /build/target/CampusPark.war /usr/local/tomcat/webapps/CampusPark.war
ENV CAMPUSPARK_DB_URL="jdbc:mysql://db:3306/college_parking?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
ENV CAMPUSPARK_DB_USER="campuspark"
EXPOSE 8080
