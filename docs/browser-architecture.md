# Browser Architecture

The browser tab lets the user search the web, navigate to a recipe page, and import it into the app's library by editing the extracted fields before saving. It is composed of four BLoCs under a navigation root, plus a platform-specific WebView. This document covers the BLoC layout, the navigation stack, the recipe-extraction flow up to `RecipeRoot`, WebView state preservation, and back-button handling.

For the general BLoC pattern (interface + impl + view-model, factories, scopes), see [`architecture.md`](architecture.md).

## BLoC layout

`BrowserRootBloc` owns a Decompose `ChildStack` of four configurations. Each child is a separate BLoC with its own screen. The Landing, EditQuery, and Browser screens place their search field under the same `sharedElement` key (`"browser-address-bar"`) so the field animates between them via `SharedTransitionLayout` + `AnimatedContent`.

```mermaid
flowchart TD
    BR[BrowserRootBloc] -->|Child.SelectEngine| BSE[BrowserSelectEngineBloc]
    BR -->|Child.Landing| BL[BrowserLandingBloc]
    BR -->|Child.EditQuery| BE[BrowserEditQueryBloc]
    BR -->|Child.Browser| BB[BrowserBloc]

    BSE -.->|BrowserSelectEngineScreen| SES[First-run search engine picker]
    BL -.->|BrowserLandingScreen| LS[Logo + tagline + readOnly field]
    BE -.->|BrowserEditQueryScreen| ES[Focused field + back arrow]
    BB -.->|BrowserScreen| BS[Address bar + PlatformWebView + Download]

    BB -->|holds via instanceKeeper| WV[(WebView state)]
```

| BLoC | Purpose | Responsibilities |
|---|---|---|
| `BrowserRootBloc` | Navigation root | Owns the `ChildStack`. Receives `BrowserBloc.Output.RecipeExtracted` and forwards it to its parent. Hosts the `BackCallback` that drives the WebView back button. Exposes `navigateToUrl(url)` (replaces the whole stack with a `Browser`) for shared-URL handling from the bottom nav. |
| `BrowserSelectEngineBloc` | First-run engine picker | Shown instead of Landing until a default search engine is chosen. Emits `Output.EngineSelected`, which replaces the stack with `Landing`. |
| `BrowserLandingBloc` | New-tab home | Exposes the selected default search engine (changeable from a dropdown) and the active first-run coach mark. Tapping the read-only field emits `Output.OpenEditQuery`; the EditQuery screen is what actually accepts input and submits. |
| `BrowserEditQueryBloc` | URL/search editor | Converts the input to a URL or a search on the default engine (see `toNavigationUrl`). On submit emits `Output.Navigate(url)`; the back arrow emits `Output.Cancel`. |
| `BrowserBloc` | WebView screen | Tracks `currentUrl`, `webViewReportedUrl`, `addressBarText`, and `canGoBack/canGoForward` reported by the WebView. Tapping the address bar emits `Output.NavigateToLanding(currentText)`. The Download button calls `onExtractRecipe()`, which captures the rendered page HTML from the WebView (falling back to fetching the URL) and extracts via `RecipeExtractorService` and emits `Output.RecipeExtracted(extracted)` — it does **not** save to the repo. |

## Navigation state machine

`BrowserRootBlocImpl` manages a `StackNavigation<Configuration>` with four serializable configurations: `SelectEngine` (object), `Landing` (object), `EditQuery(initialText)`, and `Browser(url)`. Decompose enforces uniqueness of configurations within a stack, so two of the navigation transitions go through dedupe helpers (`replaceTopEditQueryWith`, `replaceEditQueryWithBrowser`).

```mermaid
stateDiagram-v2
    [*] --> Browser: initialUrl != null
    [*] --> SelectEngine: no default engine
    [*] --> Landing: otherwise

    SelectEngine --> Landing: EngineSelected

    Landing --> EditQuery: OpenEditQuery (tap field)

    EditQuery --> Browser: Navigate (submit URL)
    EditQuery --> Landing: Cancel / system back
    EditQuery --> Browser: system back\n(if Browser was below)

    Browser --> EditQuery: NavigateToLanding (tap address bar)
    Browser --> Landing: system back\n(if Landing was below)

    Browser --> [*]: RecipeExtracted (output up)
```

### Transitions in code

| Trigger | Origin | Implementation | Notes |
|---|---|---|---|
| Initial child | constructor | `Browser(initialUrl)` if `initialUrl != null`, else `SelectEngine` if no default search engine is set, else `Landing` | Recipe-detail "open source URL" provides `initialUrl`. |
| `BrowserSelectEngineBloc.Output.EngineSelected` | pick engine | `navigation.replaceAll(Landing)` | First run only. |
| `BrowserLandingBloc.Output.OpenEditQuery` | tap landing field | `replaceTopEditQueryWith(EditQuery(""))` | Drops any pre-existing `EditQuery` to avoid duplicate-config crash. The Landing field is read-only so the seed is always empty. |
| `BrowserEditQueryBloc.Output.Navigate` | submit URL | `replaceEditQueryWithBrowser(Browser(url))` | Drops the EditQuery on top **and** any existing same-URL Browser before adding the new Browser. |
| `BrowserEditQueryBloc.Output.Cancel` | back arrow | `navigation.pop()` | Returns to whatever was below the EditQuery. |
| `BrowserBloc.Output.NavigateToLanding` | tap address bar | `replaceTopEditQueryWith(EditQuery(text))` | Same dedupe as Landing's path. |
| `BrowserBloc.Output.RecipeExtracted` | tap Download | forwarded as `BrowserRootBloc.Output.RecipeExtracted(extracted)` | Stack is unchanged; the parent (`BottomNavBlocImpl`/`RootBlocImpl`) is what opens the recipe-edit modal. |

## Recipe extraction flow

When a recipe is extracted, the data — represented as a `@Serializable` `ExtractedRecipeData` in `recipe/data/public` — flows up through four BLoCs and lands in the recipe edit screen pre-populated. The user reviews, edits, and explicitly saves; on save they go to the recipe detail. Pressing back from the detail returns to the browser tab because the entire `RecipeRoot` was pushed onto `RootBloc` *above* the bottom nav.

```mermaid
sequenceDiagram
    autonumber
    participant Screen as BrowserScreen (Download tap)
    participant Browser as BrowserBloc
    participant Service as RecipeExtractorService
    participant BR as BrowserRootBloc
    participant BN as BottomNavBloc
    participant Root as RootBlocImpl
    participant RR as RecipeRootBloc
    participant Edit as EditRecipeBloc
    participant Detail as RecipeDetailBloc
    participant Repo as RecipeRepository

    Screen->>Browser: onExtractRecipe()
    Browser->>Service: extractRecipe(url, renderedHtml)
    Service-->>Browser: ExtractedRecipeData
    Browser->>BR: Output.RecipeExtracted(data)
    BR->>BN: Output.RecipeExtracted(data)
    BN->>Root: Output.OpenExtractedRecipe(data)
    Root->>RR: bringToFront(RecipeRoot(Props.CreateFromExtracted(data)))
    RR->>Edit: create(recipeId = null, extractedRecipe = data)
    Note over Edit: ViewModel seeds title,<br/>ingredients, directions,<br/>times, servings, calories<br/>from data.
    Edit-->>Repo: createRecipe(...) on Save
    Edit->>RR: Output.Finished(savedId)
    RR->>Detail: navigate { listOf(Detail(savedId)) }
    Note over Detail: User can edit/delete/etc.<br/>System back pops RecipeRoot<br/>and returns to Browser tab.
```

### Data shape

`ExtractedRecipeData` carries only the fields that come out of the schema.org/JSON-LD parser, with no `id`, timestamps, or `syncStatus`. It lives in `client/recipe/data/public` so both the browser and recipe modules can depend on it without a feature-cross-feature dependency, and `kotlinx.serialization` handles it as a `Configuration`/`Props` payload across process death.

## Platform WebView state preservation

Switching tabs (and pushing the recipe-edit modal) removes `BrowserScreen` from composition. Without state preservation, the underlying WebView is destroyed and history is lost. The fix differs per platform.

```mermaid
flowchart LR
    subgraph Android_iOS [Android / iOS]
        AS[BrowserScreen] -->|state| RSS[rememberSaveableWebViewState]
        RSS -.->|saved into| SSH[(SaveableStateHolder)]
        SSH -.->|restored across<br/>Children child swap| RSS
        RSS --> WV1[multiplatform-webview WebView]
    end

    subgraph JVM [JVM / Desktop]
        JS[BrowserScreen] -->|instanceKeeper| Holder[WebViewHolder<br/>: InstanceKeeper.Instance]
        Holder --> JFX[JavaFX WebView]
        Holder -->|onDestroy| Free[engine.load 'about:blank']
    end
```

| Platform | Mechanism | Where |
|---|---|---|
| Android, iOS | `rememberSaveableWebViewState` from `compose-webview-multiplatform`. The library's `WebStateSaver` snapshots the WebView state Bundle (URL, history, scroll, page title) into Compose's `SaveableStateHolder`, which Decompose's `Children` retains across child-stack swaps. `lastCommandedUrl` is also `rememberSaveable` so reentry doesn't reload and clobber history. | `PlatformWebView.android.kt`, `PlatformWebView.ios.kt` |
| JVM | The actual JavaFX `WebView` is owned by a `WebViewHolder : InstanceKeeper.Instance` retrieved via `instanceKeeper.getOrCreate {}`, so the same WebView (with its history) survives composition exit/entry as long as the bloc is alive. | `PlatformWebView.jvm.kt` |

JVM also wires the standard 5-button mouse: an `addEventFilter(MOUSE_PRESSED)` on the JavaFX `WebView` maps `MouseButton.BACK`/`MouseButton.FORWARD` to `goBack()`/`goForward()`.

## Back handling

`BrowserRootBlocImpl` enables a `BackCallback` that intercepts the system back press *only* when the active child is a `Browser` and its `canGoBack` is true. Otherwise, Decompose's normal stack handling pops the top configuration.

```mermaid
flowchart TD
    Back[System back press] --> Cb{webViewBackCallback<br/>enabled?}
    Cb -->|yes: Browser canGoBack| GoBack[BrowserBloc.onGoBack:<br/>WebView history -1]
    Cb -->|no| Decompose[Decompose pops the<br/>active configuration]

    Decompose --> Pop1{Active was<br/>EditQuery?}
    Pop1 -->|yes| Reveal[Reveal Browser or Landing<br/>that was below]
    Pop1 -->|no, single config left| Up[Back propagates to<br/>parent BackHandler]
```

The callback's `isEnabled` is driven by a `lifecycle.doOnResume`/`doOnPause` pair (mirroring `BottomNavBlocImpl.observeRouter`). On resume it subscribes to the `ChildStack` and, when the active child is `Browser`, launches a coroutine on `createScope()` that mirrors the bloc's `state.canGoBack` into `webViewBackCallback.isEnabled`. On pause it cancels both the subscription and the inner job and disables the callback.

## Where to look

| Concern | Path |
|---|---|
| Public BLoC + screen contracts | `client/browser/public/src/commonMain/kotlin/com/plusmobileapps/chefmate/browser/` |
| Navigation root + dedupe helpers | `BrowserRootBlocImpl.kt` (impl) |
| URL/search heuristic | `NavigationUrl.kt` (`toNavigationUrl`) |
| Extraction parser | `RecipeExtractorService.kt` |
| Cross-feature data class | `client/recipe/data/public/.../ExtractedRecipeData.kt` |
| Edit-with-extracted-seed wiring | `RecipeRootBloc.Props.CreateFromExtracted`, `EditRecipeBloc.Factory.create(..., extractedRecipe = ...)`, `EditRecipeViewModel.init` |
| Tests covering the dedupe regression | `BrowserRootBlocImplTest` (`When_edit_query_pushed_while_one_already_in_back_stack_Then_no_duplicate_crash`, `When_edit_query_navigate_with_same_url_as_existing_browser_Then_no_duplicate_crash`) |
