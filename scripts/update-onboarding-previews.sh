#!/usr/bin/env bash
# Copies the onboarding preview captures recorded by OnboardingCapturesScreenshotTest into the
# onboarding module's drawables, downscaled to 720px wide (2x a ~360dp phone) to keep the bundled
# PNGs small.
#
# Usage, from the repo root, after a design change:
#   rm -rf client/ui/screenshot-test/src/screenshotTestDebug/reference/com/plusmobileapps/chefmate/ui/screenshot/OnboardingCapturesScreenshotTestKt
#   ./gradlew :client:ui:screenshot-test:updateDebugScreenshotTest --tests '*OnboardingCaptures*'
#   ./scripts/update-onboarding-previews.sh
#
# The share-sheet previews (onboarding_preview_share_*) are captures of the OS share sheet, not
# ChefMate UI, so they aren't rendered here.
set -euo pipefail

cd "$(dirname "$0")/.."

src=client/ui/screenshot-test/src/screenshotTestDebug/reference/com/plusmobileapps/chefmate/ui/screenshot/OnboardingCapturesScreenshotTestKt
dst=client/onboarding/public/src/commonMain/composeResources/drawable
width=720

# capture name -> drawable name
captures=(
  "AddToGrocery:add_to_grocery"
  "MealPlanning:meal_planning"
  "CookMode:cook_mode"
  "RecipeBooks:recipe_books"
  "GroceryLists:grocery_lists"
  "SaveRecipes:save_recipes"
  "FamilySharing:family_sharing"
)

for entry in "${captures[@]}"; do
  capture="${entry%%:*}"
  drawable="${entry##*:}"
  for variant in Light Dark; do
    matches=("$src"/OnboardingCapture"$capture$variant"_*.png)
    if [[ ! -f "${matches[0]}" ]]; then
      echo "Missing capture OnboardingCapture$capture$variant — record it first." >&2
      exit 1
    fi
    suffix=""
    [[ "$variant" == Dark ]] && suffix="_dark"
    out="$dst/onboarding_preview_${drawable}${suffix}.png"
    sips --resampleWidth "$width" "${matches[0]}" --out "$out" >/dev/null
    echo "$out"
  done
done
