package net.jimmc.util

/** Manage a subscriber list.
 * There are no guarantees on the order of subscribers in the list.
 * This code is a slightly modified version of ListenerManager
 * as published to my blog in 2008.
 */
trait Publisher[E] {
    type S = (E) => Unit
    private var subscribers: List[S] = Nil

    /** True if the subscriber is already in our list. */
    def isSubscribed(subscriber:S) = {
	val subs = synchronized { subscribers }
	subs.exists(_==subscriber)
    }

    /** Add a subscriber to our list if it is not already there. */
    def subscribe(subscriber:S) = synchronized {
        if (!isSubscribed(subscriber))
            subscribers = subscriber :: subscribers
    }

    /** Remove a subscriber from our list.  If not in the list, ignored. */
    def unsubscribe(subscriber:S):Unit = synchronized {
        subscribers = subscribers.filter(_!=subscriber)
    }

    /** Publish an event to all subscribers on the list. */
    def publish(event:E) = {
	val subs = synchronized { subscribers }
	subs.foreach(_.apply(event))
    }
}
