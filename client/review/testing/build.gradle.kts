plugins { alias(libs.plugins.kmpLibrary) }

kotlin { sourceSets { commonMain.dependencies { implementation(projects.client.review.public) } } }

plusLibrary { namespace = "com.plusmobileapps.chefmate.review.testing" }
