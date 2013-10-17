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

import bruma.BrumaException;
import bruma.master.Control;
import bruma.master.Master;
import bruma.master.MasterFactory;

/**
 * Shows the content of the control record of the master file.
 * @author Heitor Barbieri
 */
public class CtlMfn {

    private static void usage() {
        System.err.println("usage: CtlMfn <master>");
        System.exit(1);
    }

    public static void main(String[] args) throws BrumaException {
        if (args.length < 1) {
            usage();
        }
        final Master master = MasterFactory.getInstance(args[0]).open();
        final Control control = master.getControlRecord();

        System.out.println("Ctlmfn: " + control.getCtlmfn());
        System.out.println("Mfcxx1: " + control.getMfcxx1());
        System.out.println("Mfcxx2: " + control.getMfcxx2());
        System.out.println("Mfcxx3: " + control.getMfcxx3());
        System.out.println("Mftype: " + control.getMftype());
        System.out.println("Nxtmfb: " + control.getNxtmfb());
        System.out.println("Nxtmfn: " + control.getNxtmfn());
        System.out.println("Nxtmfp: " + control.getNxtmfp());
        System.out.println("Reccnt: " + control.getReccnt());
        System.out.println("FFI: " + master.isFFI());
        System.out.println("Shift: " + master.getShift());
        System.out.println("GigaSize: " + master.getGigaSize());
        System.out.println("Swapped: " + master.isSwapped());
        System.out.println();

        master.close();
    }
}
