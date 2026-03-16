plugins {
    id(Plugin.Id.JavaLibrary.value)
    id(Plugin.Id.KotlinJvm.value)
}

dependencies {
    implementation(Library.KotlinStd)
    implementation(project(Project.Logger.Core.name))
}
