package gnu.io;
import javax.comm.*;


/* extend CommApiVersion so we can add RXTX version information  */

public class RXTXVersion
{
/*------------------------------------------------------------------------------
	RXTXVersion  
	accept:       -
	perform:      Set Version.
	return:       -
	exceptions:   Throwable
	comments:     
		      See INSTALL for details.
------------------------------------------------------------------------------*/
	private static String Version;

	static {
		Version = "RXTX-1.5-4";
	}
/*------------------------------------------------------------------------------
	getVersion  
	accept:       -
	perform:      -
	return:       Version
	exceptions:   -
	comments:     -
------------------------------------------------------------------------------*/
	public static String getVersion()
	{
		return(Version);
	}
}
