# List-Detail Scene Recipe

This example shows how to create a list-detail layout using the Scenes API.

A `ListDetailSceneStrategy` will return a `ListDetailScene` if:

-   the window width is over 600dp
-   A `Detail` entry is the last item in the back stack
-   A `List` entry is in the back stack

The `ListDetailScene` provides a `CompositionLocal` named `LocalBackButtonVisibility` that can be used by the detail `NavEntry` to control whether it displays a back button. This is useful when the detail entry usually displays a back button but should not display it when being displayed in a `ListDetailScene`. See https://github.com/android/nav3-recipes/issues/151 for more details on this use case.

See `ListDetailScene.kt` for more implementation details.
