/* ActorPublisher.scala
 *
 * Jim McBeath, June 5, 2008
 */

package net.jimmc.util

import scala.actors.Actor

/** Each subscriber must implement this trait as a marker to help
 * ensure that it expects to receive messages of type E.
 */
trait Subscriber[E] extends Actor

/** To subscribe or subscribe to the E messages, the subscriber
 * must send an instance SubscriberRequest to the publisher.
 */
sealed abstract class SubscriberRequest[E]
case class Subscribe[E](subscriber:Subscriber[E]) extends SubscriberRequest[E]
case class Unsubscribe[E](subscriber:Subscriber[E]) extends SubscriberRequest[E]

/** Manage a set of actor subscribers for an actor.
 * Each subscriber is an actor that is expecting to receive
 * messages of type E from this publisher.
 */
trait ActorPublisher[E] {
    /** Our subscribers. */
    private var subscribers: List[Subscriber[E]] = Nil

    /** The publisher's react or receive method should invoke this
     * method to handle the Subscribe and Unsubscribe messages.
     * If the publisher does not receive any other messages, this
     * can be done with this line:
     *   react handleSubscribe
     * If the publisher also receives other messages, this can be done with:
     *   react (handleSubscribe orElse handleOtherStuff)
     *   val handleOtherStuff: PartialFunction[Any,Unit] = { case ... }
     */
    protected val handleSubscribe: PartialFunction[Any,Unit] = {
        case m:Subscribe[E] =>
            if (!isSubscriber(m.subscriber))
                subscribers = m.subscriber :: subscribers
        case m:Unsubscribe[E] =>
            subscribers = subscribers.filter(_!=m.subscriber)
    }

    /** True if the subscriber is in our list. */
    private def isSubscriber(subscriber:Subscriber[E]) =
            subscribers.exists(_==subscriber)

    /** The publisher calls this method to publish a message to all subscribers.
     * There is no guarantee on the order in which subscribers receive messages.
     * This class is not intended to be called from anywhere but the
     * class which extends this trait.
     */
    protected def publish(message:E) = subscribers.foreach(_ ! message)
}
