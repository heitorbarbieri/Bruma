/*=========================================================================

    Copyright © 2011 BIREME/PAHO/WHO

    This file is part of Bruma.

    Bruma is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as 
    published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    Bruma is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public 
    License along with Bruma. If not, see <http://www.gnu.org/licenses/>.

=========================================================================*/

package bruma.utils;

import bruma.BrumaException;
import java.io.*;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.GregorianCalendar;


/** The <code>Util</code> class contains help functions to be used only
  * by internal Bruma package functions.
  * @author Heitor Barbieri
 */
public final class Util {
       private Util() {
    }

    /**
     * Returns the filename with the extension added to it.
     * @param    filename the file that will have the extension added
     * @param    extension the extension to be added/replaced to the filename.
     * @return    the filename with the extension added/replaced to it.
     * @exception BrumaException.
     */
    public static String changeFileExtension(final String filename,
                                               final String extension)
                                                         throws BrumaException {
        final int nsize;
        int curpos;
        char auxCh = 0;
        String fname = filename;
        String ext = extension;
        String nameExt;

        if (fname == null) {
            throw new BrumaException("filename=null");
        }
        if (ext != null) {
            ext = ext.trim();
            if (ext.length() == 0) {
                ext = null;
            } else {
                if (ext.charAt(0) == '.') {
                    ext = ext.substring(1);
                }
            }
        }

        fname = fname.trim();
        nsize = fname.length();

        if (nsize == 0) {
            throw new BrumaException("filename=\"\"");
        }

        curpos = nsize - 1;

        while (curpos >= 0) {
            auxCh = fname.charAt(curpos);
            if ((auxCh == '.') || (auxCh == '/') || (auxCh == '\\')) {
                break;
            }
            curpos--;
        }

        if (curpos < 0) {
            nameExt = fname;
            if (ext != null) {
                nameExt += "." + ext;
            }
        } else {
            if (curpos == 0) {
                throw new BrumaException(ext);
            } else {
                if (curpos == (nsize - 1)) {
                    throw new BrumaException("invalid filename=["
                                                              + filename + "]");
                }
                if ((auxCh == '/') || (auxCh == '\\')) {
                    nameExt = fname;
                    if (ext != null) {
                        nameExt += "." + ext;
                    }
                } else {
                    nameExt = fname.substring(0, curpos);
                    if (ext != null) {
                        nameExt += ("." + ext);
                    }
                }
            }
        }

        return nameExt;
    }

    public static String sTrace2Str(final Exception exc) {
        final String ret;

        if (exc == null) {
            ret = null;
        } else {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final PrintStream pStream = new PrintStream(baos);
//exc.printStackTrace();
            pStream.println(exc.getMessage());
            //exc.printStackTrace(pStream);
//System.writer.println(baos.toString());
            ret = baos.toString();
        }

        return ret;
    }

    public static String sayHello(final String objectName) {
        String ret;

        try {
            final InetAddress IAddr = InetAddress.getLocalHost();
            final Calendar calendar = new GregorianCalendar();
            final String hello = "Hello from a " + objectName +  " object";
            final String host = "host name = " + IAddr.getHostName();
            final String address = "host address = " + IAddr.getHostAddress();
            final String day = "day = " + calendar.get(Calendar.DAY_OF_MONTH);
            final String hour = "hour = " + calendar.get(Calendar.HOUR_OF_DAY);
            final String minute = "minute = " + calendar.get(Calendar.MINUTE);
            final String second = "second = " + calendar.get(Calendar.SECOND);

            ret = hello + "/n/n" + host + "/n" + address + "/n" + day + "/n"
                  + hour + "/n" + minute + "/n" + second;
        } catch(Exception ex) {
            ret = ex.getMessage();
        }

        return ret;
    }

    /**
     * Reads the content of a file if the param name starts with @ symbol,
     * otherwise, it returns the param string unchanged.
     *
     * @param     param @ symbol plus the file path.
     * @return    the content of a file, usually a file having function
     * parameters.
     */
    public static String readParameters(final String param)
                                                         throws BrumaException {
        String ret = null;

        try {
            ret = param.trim();

            if (ret.charAt(0) == '@') {
                BufferedReader reader = new BufferedReader(
                                            new FileReader(ret.substring(1)));
                StringWriter writer = new StringWriter();
                int auxCh = 0;

                while(true) {
                    auxCh = reader.read();
                    if (auxCh == -1) {
                        break;
                    }
                    writer.write(auxCh);
                }

                writer.close();
                ret = writer.toString();
            }
        } catch(Exception ex) {
            throw new BrumaException(ex);
        }

        return ret;
    }
    
    
}
