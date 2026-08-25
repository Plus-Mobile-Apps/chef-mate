rootProject.name = "ChefMate"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

include(":admin")

include(":client:aichat:impl")

include(":client:aichat:impl-robots")

include(":client:aichat:public")

include(":client:auth:data:impl")

include(":client:auth:data:public")

include(":client:auth:data:testing")

include(":client:auth:ui:impl")

include(":client:auth:ui:public")

include(":client:auth:usecase:impl")

include(":client:auth:usecase:public")

include(":client:browser:impl")

include(":client:browser:impl-robots")

include(":client:browser:public")

include(":client:browser:testing")

include(":client:bottomnav:impl")

include(":client:bottomnav:impl-robots")

include(":client:bottomnav:public")

include(":client:composeApp")

include(":client:cook:impl")

include(":client:cook:public")

include(":client:database:core")

include(":client:featureflag:impl")

include(":client:featureflag:public")

include(":client:featureflag:testing")

include(":client:database:testing")

include(":client:grocery:autocomplete:impl")

include(":client:grocery:autocomplete:impl-robots")

include(":client:grocery:autocomplete:public")

include(":client:grocery:category-rules:impl")

include(":client:grocery:category-rules:impl-robots")

include(":client:grocery:category-rules:public")

include(":client:grocery:core:impl")

include(":client:grocery:core:impl-robots")

include(":client:grocery:core:public")

include(":client:grocery:data:impl")

include(":client:grocery:data:public")

include(":client:grocery:data:testing")

include(":client:meal:core:impl")

include(":client:meal:core:public")

include(":client:onboarding:impl")

include(":client:onboarding:impl-robots")

include(":client:onboarding:public")

include(":client:onboarding:demo")

include(":client:meal:data:impl")

include(":client:meal:data:public")

include(":client:meal:data:testing")

include(":client:notifications:data:impl")

include(":client:notifications:data:public")

include(":client:notifications:data:testing")

include(":client:notifications:impl")

include(":client:notifications:impl-robots")

include(":client:notifications:public")

include(":client:recipe:categories:impl")

include(":client:recipe:categories:impl-robots")

include(":client:recipe:categories:public")

include(":client:profile:impl")

include(":client:profile:impl-robots")

include(":client:profile:public")

include(":client:recipe:core:impl")

include(":client:recipe:core:impl-robots")

include(":client:recipe:core:public")

include(":client:recipe:data:impl")

include(":client:recipe:data:public")

include(":client:recipe:data:testing")

include(":client:recipe:exporter:impl")

include(":client:recipe:exporter:impl-robots")

include(":client:recipe:exporter:public")

include(":client:recipe:importer:impl")

include(":client:recipe:importer:impl-robots")

include(":client:recipe:importer:public")

include(":client:recipe:list:impl")

include(":client:recipe:list:impl-robots")

include(":client:recipe:list:public")

include(":client:recipebook:data:impl")

include(":client:recipebook:data:public")

include(":client:recipebook:data:testing")

include(":client:recipebook:edit:impl")

include(":client:recipebook:edit:impl-robots")

include(":client:recipebook:edit:public")

include(":client:root:impl")

include(":client:root:public")

include(":client:settings:impl")

include(":client:settings:impl-robots")

include(":client:settings:public")

include(":client:settings:root:public")

include(":client:settings:root:impl")

include(":client:settings:root:impl-robots")

include(":client:developer-settings:impl")

include(":client:developer-settings:public")

include(":client:shared")

include(":client:subscription:impl")

include(":client:subscription:public")

include(":client:subscription:testing")

include(":client:testing")

include(":client:text:public")

include(":client:toast:impl")

include(":client:toast:public")

include(":client:toast:testing")

include(":client:ui:public")

include(":client:ui:screenshot-test")

include(":client:util:core:public")

include(":client:util:impl")

include(":client:util:public")

include(":client:util:testing")
