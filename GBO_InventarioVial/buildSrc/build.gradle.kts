plugins {
    java
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}
