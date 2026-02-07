# Passing Arguments to ViewModels (Koin)

This recipe demonstrates how to pass navigation arguments (keys) to a `ViewModel` using Koin for dependency injection.

## How it works

1.  A Koin module is defined that provides the `ViewModel`.
2.  The `koinViewModel` composable function is used to get the `ViewModel` instance.
3.  The navigation key is passed to the `ViewModel`'s constructor using `parametersOf(key)`. This makes the navigation key available to the `ViewModel`.

**Note**: The `rememberViewModelStoreNavEntryDecorator` is added to the `NavDisplay`'s `entryDecorators`. This ensures that `ViewModel`s are correctly scoped to their corresponding `NavEntry`, so that a new `ViewModel` instance is created for each unique navigation key.