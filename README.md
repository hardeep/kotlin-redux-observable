# Redux Observable for Kotlin

[![Release](https://jitpack.io/v/hardeep/kotlin-redux-observable.svg)](https://jitpack.io/#hardeep/kotlin-redux-observable)

> RxJava2 middleware for action side effects in Redux using "Epics"

## Example usage

    import ca.hardeep.kotlin.redux.*
    import ca.hardeep.kotlin.redux.observable.*

    fun main(args: Array<String>) {

        class TestAction: ActionType
        class AnotherTestAction: ActionType
        class EffectTestAction: ActionType

        data class ApplicationState(
            val strings: List<String> = listOf()
        )

        val initialState = ApplicationState()

        val firstReducer = fun(state: ApplicationState, action: Action): ApplicationState {
            return when (action.type::class) {
                TestAction::class -> {
                    val newStrings = state.strings.toMutableList()
                    newStrings.add("Adding first string")
                    return state.copy(strings = newStrings.toList())
                }
                EffectTestAction::class -> {
                    val newStrings = state.strings.toMutableList()
                    newStrings.add("Adding effect string")
                    return state.copy(strings = newStrings.toList())
                }
                else -> {
                    state
                }
            }
        }

        val secondReducer = fun(state: ApplicationState, action: Action): ApplicationState {
            return when (action.type::class) {
                AnotherTestAction::class -> {
                    val newStrings = state.strings.toMutableList()
                    newStrings.add("Adding another string")
                    return state.copy(strings = newStrings.toList())
                }
                else -> {
                    state
                }
            }
        }

        fun <S> testEpic() : Epic<S> {
            return { action, store, dependencies ->
                action.ofActionType(TestAction())
                        .map({ _ ->
                            store.dispatch(Action(EffectTestAction()))
                        })
            }
        } 

        val epic = testEpic<ApplicationState>()

        val merged = mergeEpics(epic)

        val combined = combineReducers<ApplicationState>(firstReducer, secondReducer);
        val epicMiddleware = createEpicMiddleware(merged, null)

        val enhancer = applyMiddleware<ApplicationState>(listOf(epicMiddleware))
        val store = createStoreWithEnhancer<ApplicationState>(combined, initialState, enhancer)

        store.dispatch(Action(TestAction()))
        store.dispatch(Action(AnotherTestAction()))

        println(store.getState())
    }

    // Result: ApplicationState(strings=[Adding first string, Adding effect string, Adding effect string, Adding another string])
