package ca.hardeep.kotlin.redux.observable

import ca.hardeep.kotlin.redux.Action
import ca.hardeep.kotlin.redux.ActionType
import ca.hardeep.kotlin.redux.Middleware
import ca.hardeep.kotlin.redux.Next
import ca.hardeep.kotlin.redux.Store

import io.reactivex.Observable
import io.reactivex.Observable.merge
import io.reactivex.subjects.PublishSubject

typealias Epic<S> = (action: Observable<Action>, store: Store<S>, dependencies: Any?) -> Observable<Action>
typealias MergedEpics<S> = (action: Observable<Action>, store: Store<S>, dependencies: Any?) -> Observable<Action>

fun Observable<Action>.ofActionType(type: ActionType) : Observable<Action> {
    return this.filter({ i ->
        i.type::class.java == type::class.java
    })
}

fun <S> mergeEpics(vararg epics: Epic<S>) : MergedEpics<S> {
    return { action, store, dependencies ->
        merge(
            epics.map { epic ->
               epic(action, store, dependencies)
            }
        )
    }
}


fun <S> createEpicMiddleware(rootEpic: MergedEpics<S>, dependencies: Any?): Middleware<S> {

    val actionObservable: PublishSubject<Action> = PublishSubject.create()
    val epicObservable: PublishSubject<MergedEpics<S>> = PublishSubject.create()

    return { store ->
        val next: (Next) -> (Action) -> Action = { next ->
            println("Executing rootEpic")
            epicObservable.switchMap { e ->
                println("Building rootEpic")
                val i = e(actionObservable, store, dependencies)
                println(i::class.java)
                i
            }.subscribe({ action ->
                println("dispatching ${action.type}")
                try {
                    store.dispatch(action)
                } catch (e: Error) {
                    println(e)
                }
            })

            epicObservable.onNext(rootEpic)

            val callNext: (Action) -> Action = { action ->
                val result = next(action)
                actionObservable.onNext(action)
                result
            }
            callNext
        }
        next
    }
}
