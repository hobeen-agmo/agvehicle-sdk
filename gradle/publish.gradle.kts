// 모듈 공통 배포 설정 — 각 모듈 build.gradle.kts에서 apply(from=...)로 끌어 쓴다.
// GitHub Packages(Maven)에 <group>:<모듈명>:<version> 좌표로 올린다.  ./gradlew publish
plugins.apply("maven-publish")

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("release") {
            afterEvaluate { from(components["release"]) }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/hobeen-agmo/agvehicle-sdk")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
