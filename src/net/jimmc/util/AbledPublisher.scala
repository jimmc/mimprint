package net.jimmc.util

//For subscrobers of things that turn on and off
class AbledPublisher extends Publisher[AbledPublisher.Abled]

// use "import AbledPublisher._" to pick up these definitions
object AbledPublisher {

    abstract class Abled { val state:Boolean }
    case object Enabled extends Abled { override val state = true }
    case object Disabled extends Abled { override val state = false }

    object Abled { def apply(b:Boolean) = if (b) Enabled else Disabled }
}
