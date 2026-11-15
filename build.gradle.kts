plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}
dependencies {
    // ... ваши старые зависимости ...
    
    // Для работы с JSON
    implementation("com.google.code.gson:gson:2.10.1")
    // Для WebDAV запросов (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Для безопасного хранения пароля (если еще не добавили)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
