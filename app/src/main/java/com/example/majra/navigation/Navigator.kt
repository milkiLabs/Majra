package com.example.majra.navigation

import androidx.navigation3.runtime.NavKey

// Small helper that mutates back stacks in response to UI events.
class Navigator(private val state: NavigationState) {
    // Switch tabs when a top-level key is selected; otherwise push onto current stack.
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            if (route == state.topLevelRoute) {
                popToRoot(state.currentBackStack())
                return
            }
            state.topLevelRoute = route
            return
        }
        state.currentBackStack().add(route)
    }

    // Pop within the tab; if at the tab root, fall back to the start tab.
    fun goBack() {
        val currentStack = state.currentBackStack()
        val currentRoute = currentStack.last()

        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }

    private fun popToRoot(stack: MutableList<NavKey>) {
        while (stack.size > 1) {
            stack.removeLast()
        }
    }
}
