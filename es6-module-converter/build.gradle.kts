plugins {
    java
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.guava:guava:28.1-jre")
    implementation("com.squareup.moshi:moshi:1.8.0")

    implementation("com.google.javascript:closure-compiler:v20211107")

    testImplementation("org.assertj:assertj-core:3.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
}

application {
    mainClassName = "eu.cqse.Es6ModuleMasterConverter"
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}
