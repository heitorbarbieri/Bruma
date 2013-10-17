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

package bruma.tools;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 *
 * @author Heitor Barbieri
 */
public class DumpCNT {
    private static void usage() {
        System.err.println("usage: DumpCNT <index>");
        System.exit(1);
    }

    public static void main(final String[] args) throws IOException {
        if (args.length != 1) {
            usage();
        }

        DataInputStream dis = new DataInputStream(
                              new BufferedInputStream(
                              new FileInputStream(args[0])));

        System.out.println("-------- RECORD 1 ----------\n");
        System.out.println("IDTYPE:" + dis.readShort());
        System.out.println("ORDN:" + dis.readShort());
        System.out.println("ORDF:" + dis.readShort());
        System.out.println("N:" + dis.readShort());
        System.out.println("K:" + dis.readShort());
        System.out.println("LIV:" + dis.readShort());
        System.out.println("POSRX" + dis.readInt());
        System.out.println("NMAXPOS" + dis.readInt());
        System.out.println("FMAXPOS" + dis.readInt());
        System.out.println("ABNORMAL:" + dis.readShort());

        System.out.println("-------- RECORD 2 ----------\n");
        System.out.println("IDTYPE:" + dis.readShort());
        System.out.println("ORDN:" + dis.readShort());
        System.out.println("ORDF:" + dis.readShort());
        System.out.println("N:" + dis.readShort());
        System.out.println("K:" + dis.readShort());
        System.out.println("LIV:" + dis.readShort());
        System.out.println("POSRX" + dis.readInt());
        System.out.println("NMAXPOS" + dis.readInt());
        System.out.println("FMAXPOS" + dis.readInt());
        System.out.println("ABNORMAL:" + dis.readShort());

        dis.close();
    }
}
