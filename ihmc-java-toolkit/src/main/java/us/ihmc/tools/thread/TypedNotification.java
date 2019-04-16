package us.ihmc.tools.thread;

import us.ihmc.commons.exception.DefaultExceptionHandler;
import us.ihmc.commons.exception.ExceptionTools;

/**
 * A thread barrier over an object. An external thread notifies the holder with an Object.
 * The holder polls for notifications, with the option to block with #blockingPoll. Upon
 * polling a notification, it is cleared. After a poll, the value may be read later as
 * much as desired with #read.
 */
public class TypedNotification<T>
{
   private volatile T notification = null;
   private T previousValue = null;

   /**
    * Get the atomic value, store it for a later call to read, and return if new value was present.
    *
    * @return value available
    */
   public synchronized boolean poll()
   {
      previousValue = notification;
      notification = null;
      return previousValue != null;
   }

   /**
    * Block and wait to be notified.
    *
    * @return notification
    */
   public synchronized T blockingPoll()
   {
      ExceptionTools.handle(() -> this.wait(), DefaultExceptionHandler.RUNTIME_EXCEPTION);
      poll();
      return previousValue;
   }

   /**
    * If the initial or polled value was not null.
    *
    * @return polled value was not null
    */
   public boolean hasNext()
   {
      return previousValue != null;
   }

   /**
    * The initial or polled value.
    *
    * @return polled value
    */
   public T read()
   {
      return previousValue;
   }

   /** THREAD 2 ACCESS BELOW THIS POINT TODO: Make this safe somehow? Store thread names? */

   /**
    * Submits a value to the queue.
    *
    * @param value
    */
   public synchronized void add(T value)
   {
      notification = value;

      this.notifyAll(); // if wait has been called, notify it
   }
}
