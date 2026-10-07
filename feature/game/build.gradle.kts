plugins {
    alias(libs.plugins.vinkit.android.feature)
}

android {
    namespace = "io.github.vinaooo.xo.feature.game"
}

val vinkit = providers.gradleProperty("vinkit.tag").get()

dependencies {
    implementation("com.github.vinaooo.vinkit:shell:$vinkit")
    implementation("com.github.vinaooo.vinkit:bugreport:$vinkit")
    implementation("com.github.vinaooo.vinkit:designsystem:$vinkit")
    implementation("com.github.vinaooo.vinkit:settings:$vinkit")
    implementation("com.github.vinaooo.vinkit:scores:$vinkit")
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.compose.material.icons.extended)
}
