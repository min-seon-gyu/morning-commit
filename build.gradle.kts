plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
}

group = "server"
version = "0.0.1-SNAPSHOT"
description = "MorningCommit"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2024.0.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.batch:spring-batch-core")
    implementation("org.springframework.batch:spring-batch-infrastructure")

    // Database
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")

    // Cache (Redis)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Search (Elasticsearch)
    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")

    // External API (AI 요약 연동)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // Messaging & Email (발송기)
    implementation("org.springframework.boot:spring-boot-starter-amqp") // RabbitMQ
    implementation("org.springframework.boot:spring-boot-starter-mail") // 이메일 발송
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf") // 이메일 템플릿

    // Crawling & Parsing (수집기)
    implementation("com.rometools:rome:1.18.0") // RSS/Atom 파싱 표준 라이브러리
    implementation("org.jsoup:jsoup:1.17.2") // HTML 파싱 (본문 스크래핑용)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.batch:spring-batch-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

ktlint {
    verbose.set(true)
    disabledRules.addAll("import-ordering", "no-wildcard-imports", "filename", "indent", "parameter-list-wrapping")
}
