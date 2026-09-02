dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation(project(":common-security"))
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    testImplementation("org.springframework.security:spring-security-test")
    implementation("com.google.cloud.sql:postgres-socket-factory:1.19.1")
    implementation("com.google.cloud:google-cloud-storage:2.40.1")
}