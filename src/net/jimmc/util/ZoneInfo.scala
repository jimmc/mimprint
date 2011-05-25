//Downloaded from http://www.bmsi.com/java/ZoneInfo.java
//Modified and converted to scala by jimmc

/* Copyright (C) 1999 Business Management Systems, Inc.

This code is distributed under the GNU Library General Public License.

http://www.gnu.org/copyleft/lgpl.html

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.

 * $Log: ZoneInfo.java,v $
 * Revision 1.5  2006/02/23 05:34:32  jimmc
 * Rename from jiviewer to mimprint.
 *
 * Revision 1.4  2006/01/31 04:03:37  jimmc
 * Rename packages from jimmc.* to net.jimmc.*
 *
 * Revision 1.3  2006/01/26 05:35:42  jimmc
 * Prepare for release.
 *
 * Revision 1.2  2003/07/03 04:15:53  jimmc
 * Move ZoneInfo into jimmc.mimprint package
 *
 * Revision 1.1  2003/07/03 04:03:59  jimmc
 * Read timezone info from a Unix zoneinfo file.
 *
 * Revision 1.8  2003/03/07 00:11:36  stuart
 * Bug fix from Dave Jarvis: Close zoneinfo file after reading.
 * Changes suggested by Shawn Potter:
 *   Removed dependency on shareware Lava Rocks sprintf classes,
 *     uses java.text.DecimalFormat instead.
 *   Commented out System.err.println in inDaylightTime() method.
 *   Changed default timezone file location to more standard location
 *     "/usr/share/zoneinfo".
 *
 * Revision 1.7  2003/03/06 22:17:00  stuart
 * use 64-bit timestamps for 2038 readiness
 *
 * Revision 1.6  2000/06/05  03:22:28  stuart
 * dump /etc/localtime by default
 *
 * Revision 1.5  1999/07/15  03:27:00  stuart
 * Rename to ZoneInfo
 *
 * Revision 1.4  1999/07/14  04:45:22  stuart
 * tm_offset unused
 *
 * Revision 1.3  1999/07/14  04:42:07  stuart
 * more docs, set proper timezone ID
 * TZDump takes timezones to dump as args
 *
 * Revision 1.2  1999/07/14  03:26:28  stuart
 * tested with GregorianCalendar
 *
 */
package net.jimmc.util

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.util.TimeZone
import java.util.Date
import java.util.GregorianCalendar
import java.util.Calendar
import java.text.DateFormat
import java.text.DecimalFormat

/** Timezone type description.  E.g. EST or EDT.
  This should be an inner class, but 1.1 JDK compiler freaks out
  when using blank final with inner classes.  It is permissible
  for a package private class not used from any other class in the
  package to be in the same source file.  However, some java IDE tools
  freak out over this.
    @author Stuart D. Gathman
 */
class TZType(
      val name:String,  /* Name of type. */
      val offset:Int,   /* Offset from GMT in seconds. */
      val isdst:Boolean) /* True if daylight savings time. */

object ZoneInfo {
  private val SECSPERMIN	= 60
  private val MINSPERHOUR	= 60
  private val HOURSPERDAY	= 24
  private val DAYSPERWEEK	= 7
  private val SECSPERHOUR	= SECSPERMIN * MINSPERHOUR
  private val SECSPERDAY	= SECSPERHOUR * HOURSPERDAY
  private val TM_SUNDAY         = 0
  private val TM_MONDAY         = 1
  private val TM_TUESDAY	= 2
  private val TM_WEDNESDAY	= 3
  private val TM_THURSDAY	= 4
  private val TM_FRIDAY         = 5
  private val TM_SATURDAY	= 6
  private val EPOCH_WDAY	= TM_THURSDAY
  private val EPOCH_YEAR	= 1970

  val DAYSADJ = 25203	// days between 1900 & 1970
  val CENT_WDAY 	= EPOCH_WDAY - DAYSADJ % 7

  /** Local time variables. */
  class TZtm {
    /* Second of day. */
    //var tm_secs:Int = 0
    /** Hour of day, 0 - 23. */
    var tm_hour:Int = 0
    /** Minute of hour, 0 - 59. */
    var tm_min:Int = 0
    /** Second of minute, 0 - 60.  
        Note that value may be 60 on a leap second. */
    var tm_sec:Int = 0
    /** Day of week, 0 - 6, 0 = Sunday */
    var tm_wday:Int = 0
    /** Years since 1900. */
    var tm_year:Int = 0
    /** Day of year, 1 - 366. */
    var tm_yday:Int = 0
    /** Month of year, 0 - 11. */
    var tm_mon:Int = 0
    /** Day of month, 1 - 31. */
    var tm_mday:Int = 0
    /** True if time is DST. */
    var tm_isdst:Boolean = false
    /** Timezone name. */
    var tm_zone:String = null

    private val f = new DecimalFormat("#0")
    private val f0 = new DecimalFormat("00")

    /** Initialize a new TZtm object to calendar day and time offset.
      @param year	years since 1900
      @param mon	month 0-11
      @param day	day of month 1-31
      @param tm_secs	seconds in day
     */
    def this(year:Int,mon:Int,day:Int,tm_secs:Int) {
      this()
      tm_year = year
      tm_mon = mon
      tm_mday = day
      setSecs(tm_secs)
    }

    override def toString():String = {
      f.format(tm_mon + 1) + '/' + f0.format(tm_mday) 
	+ '/' + f0.format(tm_year + 1900)
	+ ' ' + f.format(tm_hour) + ':' + f0.format(tm_min)
	+ ':' + f0.format(tm_sec) + ' ' +
        (if (tm_zone == null) "NUL" else tm_zone)
    }

    def setSecs(tm_secs:Int) {
      //this.tm_secs = tm_secs
      tm_hour = tm_secs / SECSPERHOUR
      val rem = tm_secs % SECSPERHOUR
      tm_min =  rem / SECSPERMIN
      tm_sec = rem % SECSPERMIN
    }

    def compareTo(t:TZtm):Int = {
      if (tm_year != t.tm_year)
        return tm_year - t.tm_year
      if (tm_mon != t.tm_mon)
        return tm_mon - t.tm_mon
      if (tm_mday != t.tm_mday)
        return tm_mday - t.tm_mday
      if (tm_hour != t.tm_hour)
        return tm_hour - t.tm_hour
      if (tm_min != t.tm_min)
        return tm_min - t.tm_min
      return tm_sec - t.tm_sec
      //return tm_secs - t.tm_secs
    }

    override def equals(obj:Any):Boolean = {
      obj match {
        case tt:TZtm => compareTo(tt) == 0
        case _ => false
      }
    }

    override def hashCode():Int =
      (tm_year<<24)+(tm_mon<<20)+(tm_mday<<15)+(tm_hour<<10)+(tm_min<<5)+tm_sec

    /** Set the local time fields from a clock and GMT offset.
        @param clock	seconds since 1970
	@param offset	offset from UT in seconds
     */
    def setClock(clock:Long,offset:Int) {
      var days = (clock / SECSPERDAY).asInstanceOf[Int]
      var tm_secs = (clock % SECSPERDAY).asInstanceOf[Int]
      tm_secs = tm_secs + offset
      while (tm_secs < 0) {
	tm_secs = tm_secs + SECSPERDAY
	days = days - 1
      }
      while (tm_secs >= SECSPERDAY) {
	tm_secs = tm_secs - SECSPERDAY
	days = days + 1
      }
      
      setSecs(tm_secs)
      
      val doc = days + DAYSADJ
      tm_wday = (CENT_WDAY + doc) % DAYSPERWEEK

      // now compute date from days since EPOCH
      var leapyear = 2		/* not leapyear adj = 2 */
      // 1461 days in 4 years
      // FIXME: use code from JulianDate.java for extended range.
      tm_year = (doc - doc/1461 +364)/365	/* calculate year */
      tm_yday = doc - ((tm_year-1)*1461)/4	/* day of year conversion */
      if (tm_year % 4 == 0)			/* is this a leapyear? */
	leapyear = 1				/* yes - reset adj to 1 */
      if (tm_yday > 59 && (tm_yday > 60 || leapyear == 2))
	tm_yday = tm_yday + leapyear		/* correct for leapyear */

      tm_mon = (269 + tm_yday * 9) / 275		/* calculate month */
      tm_mday = tm_yday + 30 - 275 * tm_mon/9	/* calc day of month */
      tm_mon = tm_mon - 1	// unix convention
    }
  }

  /** Test the class by printing corresponding GMT and localized
      GregorianCalendar times before and after each timezone and 
      leapsecond transition, and at the minimum and maximum unix
      format times. */
  class TZDump(zoneName:String) {
    private val zone:String = if (zoneName==null) "localtime" else zone
    private val tz:ZoneInfo =
            if (zoneName==null) new ZoneInfo() else new ZoneInfo(zoneName)
    private val cal:Calendar = new GregorianCalendar()
    private val fmt:DateFormat = DateFormat.getDateTimeInstance(
          DateFormat.LONG,DateFormat.LONG)

    cal.setTimeZone(tz)
    fmt.setCalendar(cal)

    private def dumplcl(time:Int):String = {
      //TZtm t = tz.localtime(time)
      //return t.toString() + " isdst=" + t.tm_isdst
      return fmt.format(new Date(time * 1000L))
    }
    private def dump(time:Int) {
      val t = tz.localtime(time)
      println(zone + ' ' + tz.gmtime(time) + " = " + dumplcl(time))
    }
    private def dumpdst() {
      for (i <- 0 until tz.transTimes.length) {
        val t = tz.transTimes(i)
	dump(t-1)
	dump(t)
      }
    }
    /** Dump leapseconds. */
    private def dumpleap() {
      var i = 0
      while (i < tz.leapSecs.length) {
        val t = tz.leapSecs(i)
	dump(t-1)
	dump(t)
	dump(t+1)
        i = i + 2
      }
    }

    private val now = (System.currentTimeMillis() / 1000).asInstanceOf[Int]

    private def dumpzone(tzname:String) /*throws IOException*/ {
      val tz = new TZDump(tzname)
      println(tz.dumplcl(now))
      tz.dump(Integer.MIN_VALUE)
      tz.dumpdst()
      tz.dumpleap()
      tz.dump(Integer.MAX_VALUE)
    }

    /** Dump DST and leapsecond transitions for each timezone
        on the command line. */
    def main(argv:Array[String]) /*throws Exception*/ {
      if (argv.length == 0)
        dumpzone(null)
      else
        argv.foreach(dumpzone(_))
    }
  }

  def main(argv:Array[String]) /*throws Exception*/ {
    val tz = new ZoneInfo("EST5EDT")
    val now = (System.currentTimeMillis() / 1000).asInstanceOf[Int]
    System.err.println("Now = " + tz.localtime(now))
    val cal:Calendar = new GregorianCalendar()
    cal.setTimeZone(tz)
    cal.setTime(new Date())
    System.err.println("Now = " + cal)
  }
}

/** Reads timezone information from /etc/zoneinfo.  Implements the
    <code>java.util.TimeZone</code> interface and also provides
    work alikes for unix time conversion functions.  Unlike 
    <code>java.util.SimpleTimeZone</code>, this implementation
    supports historical daylight savings time changes and leap seconds.
    <p>
    Unfortunately, the TimeZone API does not support giving a unique
    name to positive leap seconds (for example, 1998 Dec 31 23:59:60 UTC).
    As a result, a positive leap second has the same HMS representation
    as the previous second when using <code>java.util.GregorianCalendar</code>.
    <p>
    Even more unfortunately, <code>java.text.SimpleDateFormat</code>
    decides which TimeZone name to use by comparing the
    <code>DST_OFFSET</code> calendar field to zero.  
    <code>GregorianCalendar</code> computes this by subtracting 
    <code>getRawOffset()</code> from <code>getOffset()</code>.
    Of course, these are never equal once leapseconds kick in, so 
    beginning in 1972, SimpleDateFormat always (incorrectly) uses the
    daylight time abbreviation with this TimeZone implementation.
    <p>
    This code is based loosely
    on the unix localtime package version 4.1.  Rules for each timezone
    are stored in binary form 
    in the <code>/etc/zoneinfo</code> directory.  These binary files are
    produced by the zoneinfo compiler, <code>zic</code>, included with the
    localtime package as C source.  I have not yet ported the compiler
    to Java.  The format accomodates
    historical timezone changes (e.g. war time and the 1987 change in the US),
    and leapseconds.  
    <p>
    Localtime format uses signed 32-bit values, so it peters
    out in 2038.  This can be extended by noting that
    each table has monotonically increasing keys - hence the high order bits 
    can be implied.  However, timezone changes are listed exhaustively (rules
    are interpreted by the zoneinfo compiler), so the tables would be
    quite large if extended to the full range of 64-bit Java time values.
    I propose that the timezone files can be gradually extended, becoming
    larger and larger as required, until a better solution is invented.  
    There is no point at which things suddenly break.
    <p>
    I have not yet implemented the implicitly extended table, so this
    version will break with regards to determining daylight savings time
    and accumulating leapseconds beginning in 2039.  The main purpose
    of this implementation is to point out deficiences in the
    JDK classes.  Another deficiency not mentioned above is that
    a TimeZone can have more than two abbreviations.  For example, 
    Eastern time includes EST,EDT,EWT.
    <p>
    The best way to make this actually useful, besides extending the range
    beyond 2038, is probably to extend, fix, or replace GregorianCalendar
    (to support minutes with leap seconds and the correct DST_OFFSET).
    The two abbreviation limit can be fixed by adding a ZONE_INDEX
    field to <code>java.util.Calendar</code> and using it in 
    <code>SimpleDateFormat</code>.

    @author Stuart D. Gathman
    Copyright (C) 1998 Business Management Systems, Inc.
 */
class ZoneInfo(f:File) extends TimeZone /*throws IOException*/ {
  import ZoneInfo._     //pick up everything from our companion object
  private var transTimes:Array[Int] = _	// transition times
  private var transTypes:Array[Byte] = _
        // timezone description for each transition
  private var tz:Array[TZType] = _		// timezone descriptions
  private var leapSecs:Array[Int] = _	// leapseconds
  private var rawoff = 0
  private var normaltz:TZType = _

  init(f)
  /** Initializes timezone info from a File in the tzfile format. */
  private def init(f:File) /*throws IOException*/ {
    val ds = new DataInputStream(new BufferedInputStream(
    	new FileInputStream(f)))

    try {
      // read header
      ds.skip(28)
      var leapcnt = ds.readInt()
      val timecnt = ds.readInt()
      val typecnt = ds.readInt()
      val charcnt = ds.readInt()

      // load DST transition data
      transTimes = new Array[Int](timecnt)
      for (i <- 0 until timecnt)
	transTimes(i) = ds.readInt()
      transTypes = new Array[Byte](timecnt)
      ds.readFully(transTypes)

      // load TZ type data
      val offset = new Array[Int](typecnt)
      val dst = new Array[Byte](typecnt)
      val idx = new Array[Byte](typecnt)
      for (i <- 0 until typecnt) {
	offset(i) = ds.readInt()
	dst(i) = ds.readByte()
	idx(i) = ds.readByte()
      }
      val str = new Array[Byte](charcnt)
      ds.readFully(str)

      // convert type data
      tz = new Array[TZType](typecnt)
      for (i <- 0 until typecnt) {
	// find string
	val pos = idx(i)
	var end:Int = pos
	while (str(end) != 0)
            end = end + 1
	tz(i) = new TZType(new String(str,pos,end-pos),offset(i),dst(i) != 0)
      }

      // load leap seconds table
      leapSecs = new Array[Int](leapcnt * 2)
      var i = 0
      while (leapcnt > 0) {
	leapSecs(i) = ds.readInt()
        i = i + 1
	leapSecs(i) = ds.readInt()
        i = i + 1
        leapcnt = leapcnt - 1
      }
    }
    finally { ds.close() }

    // find first standard type
    var n = 0
    while (tz(n).isdst && n < tz.length)
      n = n + 1
    normaltz = tz(n)
    setID(normaltz.name)
  }

  /** Return the ZoneInfo for local time on this machine.  For unix,
     we read /etc/localtime, which is a link to the proper zoneinfo file. */
  def this() /*throws IOException*/ {
    this(new File("/etc/localtime"))
  }

  /** Return the ZoneInfo for a timezone name.  For unix, read
     /usr/share/zoneinfo/tzname.
   @param tzname	the name of the timezone to read
   */
  def this(tzname:String) /*throws IOException*/ {
    this(new File("/usr/share/zoneinfo",tzname))
  }

  def getRawOffset():Int = {
    normaltz.offset * 1000 + rawoff
  }

  def setRawOffset(millis:Int) {
    rawoff = millis - normaltz.offset * 1000
  }

  /** Return the offset from UT for a calendar date and time.
      The calendar time we are passed is always computed using
      getRawOffset().
   */
  def getOffset(era:Int,year:Int,month:Int,day:Int,dow:Int,millis:Int):Int = {
    if (era != GregorianCalendar.AD) 
      return getRawOffset()
    val secs = millis/1000
    val then = new TZtm(year - 1900,month,day,secs)
    var ts:Long = 0
    try {
      ts = mktime(then,true)
    } catch {
        case x:IllegalArgumentException =>
            // outside the range of mktime
            return getRawOffset()
    }
    return getOffset(ts)
  }

  /** Return the offset from UT for an epoch time. */
  override def getOffset(ts:Long):Int = {
    var offset = getTZ(ts/1000).offset
    var y = leapSecs.length - 2
    var brk = false
    while (y >= 0 && !brk) {
      val ls_trans = leapSecs(y)
      val ls_corr = leapSecs(y+1)
      if (ts >= ls_trans) {
	offset = offset - ls_corr
	brk = true
      }
      else
        y = y - 2
    }
    return offset * 1000 + rawoff
  }

  /** Return true if a particular instant is considered part of daylight
      time in this timezone. */
  def inDaylightTime(d:Date):Boolean = {
    val tz = getTZ((d.getTime()/1000).asInstanceOf[Int])
    //System.err.println("isdst = " + tz.isdst)
    return tz.isdst
  }

  /** Return true if this timezone has transitions between various offsets
      from UT, such as standard time and daylight time.
  */
  def useDaylightTime():Boolean = {
    return tz.length > 1
  }

  /** Compute UT from clock.  This does not include leap second corrections.
      @param clock	seconds since 1970
      @return a new TZtm object with all time fields computed.
   */
   def gmtime(clock:Long):TZtm = {
    val t = new TZtm()
    t.setClock(clock,0)
    t.tm_zone = "GMT"
    return t
  }

  /** Compute UTC from clock.  This includes leap second corrections if
      compiled into the current timezone file.
      @param clock	seconds since 1970
      @return a new TZtm object with all time fields computed.
   */
  def utctime(clock:Long):TZtm = {
    val t = new TZtm()
    timesub(clock,null,t)
    return t
  }

  /** Compute local time from seconds since the epoch, storing into
      an existing TZtm object.
      @param clock	seconds since 1970
      @param t	a TZtm object to store the computed time fields
      @return the offset of the localtime from UT
   */
  def localtime(clock:Long,t:TZtm):Int = {
    return timesub(clock,getTZ(clock),t)
  }
    
  /** Lookup which timezone a given instant should use.  */
  private def getTZ(clock:Long):TZType = {
    // FIXME: use binary search
    if (transTimes.length > 0 && clock >= transTimes(0)) {
      var i = 1
      var brk = false
      while (i < transTimes.length && !brk) {
        if (clock < transTimes(i)) brk=true
        else i = i + 1
      }
      return this.tz(transTypes(i - 1))
    }
    return normaltz
  }

  /** Compute local time from seconds since the epoch.
      @param clock	seconds since 1970
   */
  def localtime(clock:Long):TZtm = {
    val t = new TZtm()
    localtime(clock,t)
    return t
  }

  /** Calculate seconds since the epoch, the reverse of localtime().  
  Unused fields are computed and stored in <code>yourtm</code>.
  @param yourtm The tm_year,tm_mon,tm_mday,tm_hour,tm_min,tm_sec fields are used
  	and validated.  Other fields are computed.
  @throws IllegalArgumentException If used fields are invalid.
  @return seconds since the epoch.
   */
  def mktime(yourtm:TZtm):Long = mktime(yourtm,false)

  private def mktime(yourtm:TZtm,raw:Boolean):Long = {
    var t = 0
    var bits = 31
    val offset = getRawOffset() / 1000
    val mytm = new TZtm()
    // use binary search
    // FIXME: make smarter initial guess?
    while (true) {      //leave loop via return statement
      if (raw)
        //timesub(t,tz,mytm)
	mytm.setClock(t,offset)
      else
	localtime(t,mytm)
      val direction = mytm.compareTo(yourtm)
      if (direction == 0) {
        yourtm.tm_wday = mytm.tm_wday
        yourtm.tm_yday = mytm.tm_yday
        yourtm.tm_isdst = mytm.tm_isdst
        yourtm.tm_zone = mytm.tm_zone
        return t
      }
      //System.err.println(mytm.toString() + ", " + t + ", " + direction)
      if (bits < 0)
        throw new IllegalArgumentException("bad time: " + yourtm)
      bits = bits - 1
      if (bits < 0)
        t = t - 1
      else if (direction > 0)
	t =  t - (1 << bits)
      else	
        t =  t + (1 << bits)
    }
    t
  }

  /** Compute TZtm variables from clock with leapsecond correction.
      @param clock	Seconds since 1970
      @param tz		timezone 
      @param t		localtime variables to set
      @return	The offset from GMT including timezone, DST, and leap seconds.
   */
  private def timesub(clock:Long, tz:TZType, t:TZtm):Int = {
    var		hit = false
    var offset = if (tz == null) 0 else tz.offset

    var y = leapSecs.length - 2
    var brk = false
    while (y >= 0 && !brk) {
      val ls_trans = leapSecs(y)
      val ls_corr = leapSecs(y+1)
      if (clock >= ls_trans) {
	if (clock == ls_trans)
	  hit = ((y == 0 && ls_corr > 0) || ls_corr > leapSecs(y-1))
	offset = offset - ls_corr
	brk = true
      } else
        y = y - 2
    }

    t.setClock(clock,offset)

    // A positive leap second requires a special
    // representation.  This uses "... ??:59:60".
    if (hit) t.tm_sec = t.tm_sec + 1

    if (tz != null) {
      t.tm_isdst = tz.isdst
      t.tm_zone = tz.name
    }
    else {
      t.tm_isdst = false
      t.tm_zone = "UTC"
    }
    return offset
  }

}
