plugins {
    alias(libs.plugins.vinkit.android.feature)
}

android {
    namespace = "io.github.vinaooo.xo.feature.game"
}

val vinkit = providers.gradleProperty("vinkit.tag").get()

dependencies {
    implementation("com.github.vinaooo.vinkit:shell:$vinkit")
    implementation("com.github.vinaooo.vinkit:designsystem:$vinkit")
    implementation(libs.androidx.compose.material.icons.extended)
}
