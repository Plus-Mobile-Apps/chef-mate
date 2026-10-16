package com.plusmobileapps.chefmate.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chefmate.client.onboarding.public.generated.resources.Res
import chefmate.client.onboarding.public.generated.resources.onboarding_family_sharing_message
import chefmate.client.onboarding.public.generated.resources.onboarding_family_sharing_title
import chefmate.client.onboarding.public.generated.resources.onboarding_next
import chefmate.client.onboarding.public.generated.resources.onboarding_preview_family_sharing
import chefmate.client.onboarding.public.generated.resources.onboarding_preview_family_sharing_dark

@Composable
fun FamilySharingScreen(bloc: FamilySharingBloc, modifier: Modifier = Modifier) {
    OnboardingInfoLayout(
        title = Res.string.onboarding_family_sharing_title,
        message = Res.string.onboarding_family_sharing_message,
        buttonText = Res.string.onboarding_next,
        onButtonClick = bloc::onNextClicked,
        screenTestTag = OnboardingTestTags.FAMILY_SHARING_SCREEN,
        buttonTestTag = OnboardingTestTags.FAMILY_SHARING_NEXT_BUTTON,
        modifier = modifier,
        preview = {
            OnboardingPreviewImage(
                light = Res.drawable.onboarding_preview_family_sharing,
                dark = Res.drawable.onboarding_preview_family_sharing_dark,
            )
        },
    )
}
