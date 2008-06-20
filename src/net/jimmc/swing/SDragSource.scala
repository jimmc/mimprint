package net.jimmc.swing

import java.awt.Component
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants
import java.awt.dnd.DragGestureEvent
import java.awt.dnd.DragGestureListener
import java.awt.dnd.DragSource
import java.awt.dnd.DragSourceAdapter
import java.awt.dnd.DragSourceDropEvent
import java.awt.dnd.DragSourceListener
import java.awt.dnd.InvalidDnDOperationException
import java.awt.Image
import java.awt.Point

trait SDragSource {
    protected def getMyDragGestureListener():DragGestureListener
    protected def getMyDragSourceListener():DragSourceListener

    protected val dragSource = DragSource.getDefaultDragSource()
    protected val dragGestureListener = getMyDragGestureListener()
    protected val dragSourceListener = getMyDragSourceListener()

    protected def setupDrag(dgSource:Component, dndAction:Int) {
        dragSource.createDefaultDragGestureRecognizer(
                dgSource,dndAction,dragGestureListener)
    }

    protected def startImageDrag(ev:DragGestureEvent,
            image:Image, offset:Point, path:String) {
        try {
            val transferable:Transferable = new StringSelection(path)
            if (image!=null) {
                ev.startDrag(DragSource.DefaultCopyNoDrop,
                        image, offset, transferable, dragSourceListener)
            } else {
                ev.startDrag(DragSource.DefaultCopyNoDrop,
                        transferable, dragSourceListener)
            }
        } catch {
            case ex:InvalidDnDOperationException =>
                println(ex)             //TODO - better error handling
        }
    }
}
/*
vi:
*/
